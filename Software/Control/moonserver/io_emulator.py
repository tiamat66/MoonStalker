import threading
from enum import Enum


class CommandType(Enum):
    MVST = "MVST?"
    MVS = "MVS"
    MVE = "MVE"
    MV = "MV"
    BTRY = "BTRY?"


class MessageType(Enum):
    MVS_ACK = "MVS_ACK"
    MVE_ACK = "MVE_ACK"
    MV_ACK = "MV_ACK"
    NOT_RDY = "NOT_RDY"
    READY = "READY"
    SENT = "SENT"
    TIMEOUT = "TIMEOUT"
    BTRY = "BTRY"
    ERROR = "ERROR"
    WARNING = "WARNING"
    INFO = "INFO"

class MachineState(Enum):
    READY = "ready"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    MOVING = "moving"
    PENDING = "pending"
    ERROR = "error"


class IOEmulator:
    def __init__(self, command_queue, response_queue):
        self.state = MachineState.READY
        self.error_data = None
        self.command_queue = command_queue
        self.response_queue = response_queue
        self._lock = threading.Lock()

    #             self._send_response(True, MessageType.BTRY, "10.74V")

    def _send_response(self, success, message: MessageType, data=None, warning=None, info=None):
        self.response_queue.put({
            "success": success,
            "message": message.value,
            "state": self.state.value,
            "error_data": None if success else self.error_data,
            "data": data,
            "warning": warning,
            "info": info
        })

    def _simulate_warning(self, warning_message):
        with self._lock:
            self._send_response(True, MessageType.WARNING, warning=warning_message)
            print(f"Emulator: warning simulated: {warning_message}")

    def _simulate_info(self, info_message):
        with self._lock:
            self._send_response(True, MessageType.INFO, info=info_message)
            print(f"Emulator: info simulated: {info_message}")

    def _simulate_error(self):
        with self._lock:
            self.state = MachineState.ERROR
            self.error_data = "Stop switch activated"
            self._send_response(False, MessageType.ERROR, self.error_data)
            print("Emulator: stop switch activated, machine in error state")   

    # lahko vrne samo ready in not ready
    def _simulate_connect(self):
        # poslje lahko samo ready in not ready
        with self._lock:           
            self.state = MachineState.CONNECTED
            self.error_data = None
            self._send_response(True, MessageType.READY)
            print("Emulator: connect succeeded")

    # lahko vrne samo MVS_ACK in NOT_RDY
    def _simulate_move(self, steps_a, steps_e):
        with self._lock:
            self.state = MachineState.MOVING
            self.error_data = None
            self._send_response(True, MessageType.MV_ACK)
            print(f"Emulator: move of {steps_a} units along axis A and {steps_e} units along axis E acknowledged")
        threading.Timer(2.0, self._simulate_target_reached).start()

    # lahko vrne samo MVS_ACK in NOT_RDY
    def _simulate_move_start(self, direction: str, speed: int):
        with self._lock:
            self.state = MachineState.MOVING
            self.error_data = None
            self._send_response(True, MessageType.MVS_ACK)
            print(f"Emulator: move start of {direction} at speed {speed} acknowledged")

     # lahko vrne samo MVE_ACK in NOT_RDY
    def _simulate_move_end(self):
        with self._lock:
            self.state = MachineState.CONNECTED
            self.error_data = None
            self._send_response(True, MessageType.MVE_ACK)
            print(f"Emulator: move end acknowledged")

    def _simulate_target_reached(self):
        with self._lock:
            self.state = MachineState.CONNECTED
            self.error_data = None
            self._send_response(True, MessageType.READY)
            print(f"Emulator: target reached")   

    def _handle_connect(self):
        with self._lock:
            self.state = MachineState.CONNECTING
            self.error_data = None
            print("Emulator: received connect command, waiting to respond...")

        threading.Timer(3.0, self._simulate_connect).start()

        # simulate a stop switch activation after 10 seconds for testing purposes
        # threading.Timer(30.0, self._simulate_error).start()
        threading.Timer(15.0, self._simulate_warning, args=("Low battery warning",)).start()
        threading.Timer(20.0, self._simulate_info, args=("Info: Maintenance required",)).start()

    
    def _handle_move_start(self, direction: str, speed: int):
        with self._lock:
            self.state = MachineState.PENDING
            self.error_data = None
            print(f"Emulator: received move start command for {direction} at speed {speed}, waiting to respond...")

        threading.Timer(1.0, self._simulate_move_start, args=(direction, speed)).start()

    def _handle_battery(self):
        with self._lock:
            self._send_response(True, MessageType.BTRY, "10.74V")
            print("Emulator: battery status requested, responding with BTRY")

    def _handle_move_end(self):
        with self._lock:
            self.state = MachineState.PENDING
            self.error_data = None
            print("Emulator: received move end command, waiting to respond...")

        threading.Timer(1.0, self._simulate_move_end).start()

    def _handle_move(self, steps_e, steps_a):
        with self._lock:
            self.state = MachineState.PENDING
            self.error_data = None
            print(f"Emulator: received move command for {steps_a} units along axis A and {steps_e} units along axis E, waiting to respond...")
        threading.Timer(2.0, self._simulate_move, args=(steps_a, steps_e)).start()

    def _handle_command(self, command):
        command_type = command.get("type")
        if command_type == CommandType.MVST.value:
            print("Emulator: handling connect command")
            self._handle_connect()
            return
        if command_type == CommandType.MV.value:
            self._handle_move(command.get("steps_e", 0), command.get("steps_a", 0))
            return
        if command_type == CommandType.MVS.value:
            self._handle_move_start(command.get("direction", "unknown"), command.get("speed", 0))
            return
        if command_type == CommandType.MVE.value:
            self._handle_move_end()
            return
        if command_type == CommandType.BTRY.value:
            self._handle_battery()
            return

        print(f"Emulator: unknown command {command_type}")
        self.response_queue.put({
            "success": False,
            "message": f"Unknown command: {command_type}",
            "state": self.state.value,
            "error_data": self.error_data,
        })


    def _run(self):
        print("I/O emulator started and waiting for commands on the queue.")
        while True:
            try:
                command = self.command_queue.get()
            except:
                print("Emulator: no command received, checking again...")
                continue

            if not isinstance(command, dict):
                continue

            print(f"Emulator: processing command {command}")
            self._handle_command(command)

    def start(self):
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()


def start_emulator(command_queue, response_queue):
    emulator = IOEmulator(command_queue, response_queue)
    emulator.start()

