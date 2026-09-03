# moonserver
Server for Moonstalker project.

## Flask State Machine Service

This project provides a simple Flask REST API around a state machine with the following states:
- `ready`
- `connecting`
- `connected`
- `moving`
- `error`

The app exposes commands for connecting, moving, reporting I/O responses, checking state, and resetting.

## Setup

Install dependencies:

```bash
pip install -r requirements.txt
```

Run the emulator and server:

```bash
python io_emulator.py
python app.py
```

Optional: send direct emulator commands with a small client script.

```bash
python emulator_client.py connect
python emulator_client.py move 100
python emulator_client.py watch --duration 30
```

Run a workflow demo that starts the emulator and Flask app, then exercises connect and move:

```bash
python workflow_demo.py
```

The Flask app sends `connect` and `move` commands to the emulator via a local multiprocessing queue manager on `127.0.0.1:5001`.
The emulator processes those commands and sends responses back on the same queue, which the Flask app uses to complete the state transitions.

## Endpoints

### `POST /command/connect`
Start a connect command.

Response example:

```json
{
  "success": true,
  "message": "Connection attempt started.",
  "state": "connecting",
  "error_data": null
}
```

### `POST /command/connect/response`
Report an I/O response for connect or move operations.

Request example:

```json
{
  "success": true
}
```

If the response indicates failure:

```json
{
  "success": false,
  "message": "Connection failed: example reason"
}
```

### `POST /command/move`
Send a move command while the machine is `connected`.

Request example:

```json
{
  "distance": 500
}
```

Response example:

```json
{
  "success": true,
  "message": "Move command started.",
  "state": "moving",
  "error_data": null
}
```

### `POST /command/connect/response`
Report an I/O response for either connect or move.

Request example for movement acknowledgement:

```json
{
  "success": true
}
```

Request example for movement error:

```json
{
  "success": false,
  "message": "Move failed: obstacle detected"
}
```

### `GET /state`
Read the current machine state and any error data.

### `POST /command/reset`
Reset the machine back to `ready`.

## Behavior

When `/command/connect` is received, the machine moves to `connecting` and starts a 3-second timeout.
If no I/O response is received in that window, the machine transitions to `error` with:

```json
{
  "state": "error",
  "error_data": "Connection failed: no response from I/O"
}
```

When `/command/move` is received while the machine is `connected`, the machine moves to `moving` and starts another 3-second timeout.
If the I/O device returns an acknowledgement (`success: true`), the machine returns to `connected`.
If the acknowledgement is missing or reports failure, the machine transitions to `error`.

This flow is designed for frontend clients such as Android applications to trigger connect/move commands and then poll or receive state updates.
