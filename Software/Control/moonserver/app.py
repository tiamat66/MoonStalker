import threading
import queue
from enum import Enum
from flask import Flask, jsonify, request
import secrets
import datetime
from functools import wraps
from io_emulator import start_emulator, CommandType
from star_altaz import KNOWN_STARS, calculate_alt_az
from telescope import convert_radec_to_az_el, degrees_to_dms, degrees_to_hms
from skyfield.api import load

app = Flask(__name__)

# koliko korakov je potrebno za premik za 1 stopinjo
MOTOR_STEPS_NUM = 3200.0
REDUCTOR_TRANSLATION = 30.0
BELT_TRANSLATION = 48.0 / 14.0
# na obrat
K = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION
# 914 microsteps per stopinja
K_E = K / 360
K_A = K / 360

# sky_objects = {
#     "Polaris": {"ra": 37.95456067, "dec": 89.26410897},
#     "Sirius": {"ra": 101.28715533, "dec": -16.71611586},
#     "Betelgeuse": {"ra": 88.792939, "dec": 7.407064},
# }

class MachineState(Enum):
    READY = "ready"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    MOVING = "moving"
    TRACKING = "tracking"
    ERROR = "error"

class StateMachine:
    def __init__(self):
        self.message = "NOT_RDY"
        self.state = MachineState.READY
        self.error_data = None
        self._lock = threading.Lock()
        self._connect_timeout = None
        self._ping_timeout = None
        self.is_calibrated = False
        self.cur_object = None
        self.alt = 0.0
        self.az = 0.0
        self.to_obj = None
        self.batery = ""
        self.warning = ""
        self.info = ""
        self.ts = load.timescale()
        self.eph = load('de421.bsp')

    def _set_state(self, state, error_data=None):
        self.state = state
        self.error_data = error_data

    def _cancel_timeout(self):
        if self._connect_timeout is not None:
            self._connect_timeout.cancel()
            self._connect_timeout = None
        if self._ping_timeout is not None:
            self._ping_timeout.cancel()
            self._ping_timeout = None

    def _connect_timeout_handler(self):
        with self._lock:
            if self.state != MachineState.CONNECTING:
                return
            print("I/O timeout: no response from I/O, switching to error state")
            self._set_state(
                MachineState.ERROR,
                "Connection failed: no response from I/O",
            )
            self._connect_timeout = None

    def _ping_timeout_handler(self):
        with self._lock:
            if self.state != MachineState.MOVING:
                return
            print("I/O timeout: no movement acknowledgement, switching to error state")
            self._set_state(
                MachineState.ERROR,
                "Move failed: no acknowledgement from I/O",
            )
            self._move_timeout = None

    def _send_response(self, success, message: str, error_data=None, data=None, warning=None, info=None):
        self.message = message
        self.error_data = error_data
        self.batery = data
        self.warning = warning
        self.info = info

        return {
                    "success": success,
                    "message": self.message,
                    "state": self.state.value,
                    "error_data": self.error_data,
                    "data": data,
                    "warning": warning,
                    "info": info
        }

    def calculate_steps(self, to_alt, to_az):
        steps_e = int((to_alt - machine.alt) * K_E)
        steps_a = int((to_az - machine.az) * K_A)
        print(f"Calculated steps: {steps_e} for elevation, {steps_a} for azimuth")
        if steps_e != 0:
            machine.alt = to_alt
        if steps_a != 0:    
            machine.az = to_az
            
        return machine.move(steps_e, steps_a)

    def connect(self):
        with self._lock:         
            self._set_state(MachineState.CONNECTING)

            print("I/O action: attempting to connect...")

            self._connect_timeout = threading.Timer(10.0, self._connect_timeout_handler)
            self._connect_timeout.daemon = True
            self._connect_timeout.start()

            return self._send_response(True, "SENT", "Sent MV_ST? command to I/O.")

    def move_start(self, direction: str, speed: int):
        with self._lock:
            self._set_state(MachineState.MOVING)

            print(f"I/O action: starting move in direction '{direction}' with speed {speed}...")

            return self._send_response(True, "SENT", f"Sent MVS command to I/O for direction '{direction}' at speed {speed}.")

    def position(self):
        with self._lock:
            return self._send_response(True, f"POSITION {self.alt} {self.az}")

    def calibrated(self):
        with self._lock:
            self.is_calibrated = True
            self.cur_object = "polaris"
            self.to_obj = "polaris"
            self.alt, self.az = calculate_alt_az(KNOWN_STARS[self.cur_object], self.ts, self.eph)
            print(f"I/O action: calibration complete.")

            return self._send_response(True, "CALIBRATED", "Calibration complete.")

    def move_end(self):
        with self._lock:
            self._set_state(MachineState.MOVING)

            print(f"I/O action: stopping previous move...")

            return self._send_response(True, "SENT", f"Sent MVE command to I/O")

    def track(self, action: str):
        with self._lock:
            if action == "start_track":
                print("I/O action: starting tracking...")
                start_track_thread()
                return self._send_response(True, "TRACKING", "Tracking started.")
            elif action == "stop_track":
                print("I/O action: stopping tracking...")
                stop_track_thread()
                return self._send_response(True, "STOPPED", "Tracking stopped.")
            else:
                return self._send_response(False, "INVALID_ACTION", f"Invalid tracking action: {action}")

    def move(self, steps_e: int, steps_a: int):
        if steps_a == 0 and steps_e == 0:
            return self._send_response(True, "NOP", "Ignore, too small number of steps to move")
        
        with self._lock:
            if self.state != MachineState.CONNECTED:
                return self._send_response(False, "NOT_RDY", "Cannot move: I/O not connected or ready.")
            self._set_state(MachineState.MOVING)

            print(f"I/O action: moving {steps_a} units along axis A and {steps_e} units along axis E...")
            command_queue.put({"type": CommandType.MV.value, "steps_a": steps_a, "steps_e": steps_e})

            return self._send_response(True, "SENT", "Sent MV command to I/O.")
            
    def ping(self):
        #  Ask for battery status after each response
        if self.state == MachineState.CONNECTED:
            command_queue.put({"type": CommandType.BTRY.value})

        with self._lock:
            res = {
                "success": True,
                "message": self.message,
                "state": self.state.value,
                "error_data": self.error_data,
                "data": self.batery,
                "warning": self.warning,
                "info": self.info
            }
            self.warning = ""
            self.info = ""
            return res

    def receive_io_response(self, success: bool, message: str, state: str, data=None, warning=None, info=None):
        with self._lock:
            self._cancel_timeout()

            if state == "error":
                print("Received error state from I/O:", data, message)
                self._set_state(MachineState.ERROR, data)


            elif message == "WARNING":
                print("Received warning from I/O:", warning)
                self.warning = warning

            elif message == "INFO":
                print("Received info from I/O:", info)
                self.info = info

            elif message == "BTRY":
                print("Received battery status from I/O:", data)
                self.batery = data

            elif self.state == MachineState.CONNECTING: # MV_ST?
                if message == "READY":
                    self._set_state(MachineState.CONNECTED)
                    return self._send_response(True, "READY", "Connection established")
                elif message == "NOT_RDY":
                    self._set_state(MachineState.READY)
                    return self._send_response(True, "NOT_RDY", "Connection failed: I/O reported not ready")
               
            if self.state == MachineState.MOVING:
                print("...........................Received I/O response:", message)

                if message == "MVS_ACK":
                    self._set_state(MachineState.MOVING)
                    return self._send_response(True, "MVS_ACK", "Start moving")
                elif message == "MVE_ACK":
                    self._set_state(MachineState.CONNECTED)
                    return self._send_response(True, "MVE_ACK", "Move ended")
                elif message == "NOT_RDY":
                    self._set_state(MachineState.READY)
                    return self._send_response(False, "NOT_RDY", "Move failed: I/O reported not ready")
                elif message == "MV_ACK":
                    self._set_state(MachineState.MOVING)
                    return self._send_response(True, "MV_ACK", "Move acknowledged")
                elif message == "READY":
                    self._set_state(MachineState.CONNECTED)
                    return self._send_response(True, "READY", "Move completed and I/O is ready")

            return {
                "success": False,
                "message": f"No pending operation to complete from state {self.state.value}.",
                "state": self.state.value,
                "error_data": self.error_data,
            }

    def status(self):
        with self._lock:
            return {
                "state": self.state.value,
                "error_data": self.error_data,
            }

machine = StateMachine()
command_queue = queue.Queue()
response_queue = queue.Queue()

# --- Simple in-memory token auth (static credentials) ---
# Static username/password for now (placeholder for real user store)
STATIC_USERNAME = "android"
STATIC_PASSWORD = "password123"

# token -> {"username": str, "expires": datetime}
TOKENS: dict[str, dict] = {}
TOKEN_TTL_SECONDS = 60 * 60  # 1 hour

def _generate_token(username: str) -> str:
    token = secrets.token_urlsafe(32)
    expires = datetime.datetime.utcnow() + datetime.timedelta(seconds=TOKEN_TTL_SECONDS)
    TOKENS[token] = {"username": username, "expires": expires}
    return token

def _validate_token(token: str) -> bool:
    if not token:
        return False
    info = TOKENS.get(token)
    if not info:
        return False
    if info["expires"] < datetime.datetime.utcnow():
        # expired
        del TOKENS[token]
        return False
    return True

def auth_required(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        auth_header = request.headers.get("Authorization", "")
        token = None
        if auth_header.startswith("Bearer "):
            token = auth_header.split(None, 1)[1].strip()
        if not token:
            token = request.headers.get("X-Auth-Token")

        if not _validate_token(token):
            return jsonify({"success": False, "message": "Unauthorized: invalid or missing token"}), 401
        return func(*args, **kwargs)

    return wrapper

track_stop_event = threading.Event()
track_thread = None


def tracker():
    print("Tracker thread started, tracking the telescope position...")
    while not track_stop_event.is_set():
        update_position()
        print(f"Tracking: Current position - Alt: {machine.alt}, Az: {machine.az}")
        track_stop_event.wait(3)  # Wait for 3 seconds before next tracking update

def response_listener() -> None:
    print("Response listener thread started, waiting for responses from the emulator...")
    while True:
        try:
            response = response_queue.get()
        except:
            print(f"Response queue empty")
            continue

        if not isinstance(response, dict):
            continue

        success = response.get("success", False)
        message = response.get("message")
        state = response.get("state")

        # Pass the response to the state machine to handle it
        machine.receive_io_response(success=success, message=message, state=state, data=response.get("data"), warning=response.get("warning"), info=response.get("info"))

def start_response_thread() -> None:
    response_thread = threading.Thread(target=response_listener, daemon=True)
    response_thread.start()

def start_track_thread() -> None:
    global track_thread, track_stop_event

    if track_thread is not None and track_thread.is_alive():
        return

    track_stop_event = threading.Event()
    track_thread = threading.Thread(target=tracker, daemon=True)
    track_thread.start()
    app.track_thread = track_thread


def stop_track_thread() -> None:
    global track_thread, track_stop_event

    if track_stop_event is not None:
        track_stop_event.set()

    if track_thread is not None and track_thread.is_alive():
        track_thread.join(timeout=1)

    app.track_thread = track_thread

def update_position():
    if machine.to_obj is not None:
        print(f"Updating position for object: {machine.to_obj}")

        to_alt, to_az = calculate_alt_az(KNOWN_STARS[machine.to_obj], machine.ts, machine.eph)

        machine.calculate_steps(to_alt, to_az)

# API endpoints

@app.route("/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    password = data.get("password")
    if username == STATIC_USERNAME and password == STATIC_PASSWORD:
        token = _generate_token(username)
        return jsonify({"success": True, "token": token, "expires_in": TOKEN_TTL_SECONDS})

    return jsonify({"success": False, "message": "Invalid credentials"}), 401

@app.route("/command/connect", methods=["POST"])
@auth_required
def command_connect():
    result = machine.connect()
    command_queue.put({"type": CommandType.MVST.value})
    return jsonify(result)

@app.route("/command/movestart", methods=["POST"])
def command_movestart():
    data = request.get_json(silent=True) or {}
    direction = data.get("p1")
    speed = data.get("p2")

    result = machine.move_start(direction, speed)

    if result["success"]:
        command_queue.put({"type": CommandType.MVS.value, "direction": direction, "speed": speed})
    return jsonify(result)

@app.route("/command/moveend", methods=["POST"])
def command_moveend():
    result = machine.move_end()

    if result["success"]:
        command_queue.put({"type": CommandType.MVE.value})
    return jsonify(result)

@app.route("/command/move", methods=["POST"])
def command_move():
    data = request.get_json(silent=True) or {}
    machine.to_obj = data.get("p1").lower()
    to_alt, to_az = calculate_alt_az(KNOWN_STARS[machine.to_obj], machine.ts, machine.eph)

    print(f"Received move command with object: {machine.to_obj}, target elevation: {to_alt}, target azimuth: {to_az}")

    result = machine.calculate_steps(to_alt, to_az)

    return jsonify(result)

@app.route("/command/ping", methods=["POST"])
def command_ping():
    result = machine.ping()
    return jsonify(result)

@app.route("/command/track", methods=["POST"])
def command_track():
    data = request.get_json(silent=True) or {}
    action = data.get("p1")
    result = machine.track(action)

    return jsonify(result)

@app.route("/command/calibrated", methods=["POST"])
def set_calibrated():
    result = machine.calibrated()
    return jsonify(result)

@app.route("/command/position", methods=["POST"])
def get_position():
    result = machine.position()
    return jsonify(result)

@app.route("/command/battery", methods=["POST"])
def get_battery():
    result = machine.battery()
    return jsonify(result)

@app.route("/command/reset", methods=["POST"])
def reset():
    result = machine._set_state(MachineState.READY)
    return jsonify(result)

@app.route("/command/getastrodata", methods=["POST"])
def getastrodata():
    def format_coord(ra_deg: float, dec_deg: float):
        ra_hms = degrees_to_hms(ra_deg)
        dec_dms = degrees_to_dms(dec_deg)
        return {
            "ra": {
                "hours": ra_hms[0],
                "minutes": ra_hms[1],
                "seconds": round(ra_hms[2], 3),
            },
            "dec": {
                "degrees": dec_dms[0],
                "minutes": dec_dms[1],
                "seconds": round(dec_dms[2], 3),
            },
        }

    return {
        "data": {KNOWN_STARS[name].names[0]: format_coord(KNOWN_STARS[name].ra.degrees, KNOWN_STARS[name].dec.degrees) for name in KNOWN_STARS.keys()}
    }


if __name__ == "__main__":
    # Start the I/O emulator in a separate thread before starting the Flask app
    start_emulator(command_queue, response_queue)
    start_response_thread()
    app.run(host="0.0.0.0", port=5000, debug=True)
