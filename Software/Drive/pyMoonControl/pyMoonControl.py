#!/usr/bin/env python3
"""
pyMoonControl - A GUI application to control MoonStalker Drive Unit
over serial communication with an Arduino Micro.

Requires: PySide6, pyserial
"""

import sys
import serial
import serial.tools.list_ports
from PySide6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QGridLayout, QGroupBox, QPushButton, QLabel, QLineEdit,
    QTextEdit, QComboBox, QSpinBox, QMessageBox
)
from PySide6.QtCore import Qt, QThread, Signal, QTimer
from PySide6.QtGui import QFont


# ============================================================================
# Serial Thread
# ============================================================================

class SerialThread(QThread):
    """Background thread for reading serial data without blocking the GUI."""
    data_received = Signal(str)
    connection_status = Signal(bool)

    def __init__(self, parent=None):
        super().__init__(parent)
        self.serial_port = None
        self.running = False

    def connect_port(self, port_name: str, baud: int = 115200):
        """Open and configure the serial port."""
        if self.serial_port and self.serial_port.is_open:
            self.disconnect_port()
        try:
            self.serial_port = serial.Serial(
                port=port_name,
                baudrate=baud,
                bytesize=serial.EIGHTBITS,
                parity=serial.PARITY_NONE,
                stopbits=serial.STOPBITS_ONE,
                timeout=0.1
            )
            self.running = True
            self.connection_status.emit(True)
            if not self.isRunning():
                self.start()
        except Exception as e:
            self.connection_status.emit(False)
            raise e

    def disconnect_port(self):
        """Close the serial port."""
        self.running = False
        if self.serial_port and self.serial_port.is_open:
            try:
                self.serial_port.close()
            except Exception:
                pass
        self.serial_port = None
        self.connection_status.emit(False)

    def send_command(self, command: str):
        """Send a command string over serial."""
        if self.serial_port and self.serial_port.is_open:
            try:
                self.serial_port.write(command.encode('utf-8'))
                return True
            except Exception as e:
                print(f"Send error: {e}")
                return False
        return False

    def run(self):
        """Continuously read from the serial port."""
        buffer = ""
        while self.running:
            if self.serial_port and self.serial_port.is_open:
                try:
                    if self.serial_port.in_waiting > 0:
                        data = self.serial_port.read(self.serial_port.in_waiting)
                        buffer += data.decode('utf-8', errors='replace')
                        # Process complete lines (ending with '>')
                        while '>' in buffer:
                            line, buffer = buffer.split('>', 1)
                            line = line.strip()
                            if line:
                                # Re-add the closing bracket for display
                                self.data_received.emit(line + '>')
                except Exception as e:
                    print(f"Read error: {e}")
            else:
                self.msleep(10)

    def stop(self):
        """Stop the thread safely."""
        self.running = False
        self.wait()


# ============================================================================
# Serial Port Selector Widget
# ============================================================================

class SerialPortSelector(QWidget):
    """A widget for selecting and connecting to a serial port."""
    port_changed = Signal(str, int)

    def __init__(self, parent=None):
        super().__init__(parent)
        self.build_ui()
        self.refresh_ports()
        # Auto-refresh ports every 2 seconds
        self.refresh_timer = QTimer(self)
        self.refresh_timer.timeout.connect(self.refresh_ports)
        self.refresh_timer.start(2000)

    def build_ui(self):
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)

        self.port_combo = QComboBox()
        self.port_combo.setMinimumWidth(200)
        self.port_combo.currentIndexChanged.connect(self.on_port_changed)
        layout.addWidget(QLabel("Port:"))
        layout.addWidget(self.port_combo)

        self.baud_combo = QComboBox()
        self.baud_combo.addItems(["9600", "19200", "38400", "57600", "115200", "230400"])
        self.baud_combo.setCurrentText("115200")
        layout.addWidget(QLabel("Baud:"))
        layout.addWidget(self.baud_combo)

        self.connect_btn = QPushButton("Connect")
        self.connect_btn.setAutoDefault(False)
        self.connect_btn.clicked.connect(self.toggle_connection)
        layout.addWidget(self.connect_btn)

        self.status_label = QLabel("Disconnected")
        self.status_label.setStyleSheet("color: red; font-weight: bold;")
        layout.addWidget(self.status_label)

        layout.addStretch()

    def refresh_ports(self):
        """Refresh the list of available serial ports."""
        current = self.port_combo.currentText()
        self.port_combo.blockSignals(True)
        self.port_combo.clear()
        ports = serial.tools.list_ports.comports()
        if ports:
            for port in sorted(ports, key=lambda p: p.device):
                desc = f"{port.device} - {port.description}" if port.description else port.device
                self.port_combo.addItem(desc, port.device)
            # Try to restore previous selection
            idx = self.port_combo.findText(current)
            if idx >= 0:
                self.port_combo.setCurrentIndex(idx)
        else:
            self.port_combo.addItem("No ports found", None)
        self.port_combo.blockSignals(False)

    def on_port_changed(self):
        pass

    def toggle_connection(self):
        if self.connect_btn.text() == "Connect":
            self.do_connect()
        else:
            self.do_disconnect()

    def do_connect(self):
        port_data = self.port_combo.currentData()
        if not port_data:
            QMessageBox.warning(self, "Connection Error", "No serial port selected.")
            return
        baud = int(self.baud_combo.currentText())
        self.port_changed.emit(port_data, baud)

    def do_disconnect(self):
        self.port_changed.emit("", 0)

    def set_connected(self, connected: bool):
        if connected:
            self.connect_btn.setText("Disconnect")
            self.status_label.setText("Connected")
            self.status_label.setStyleSheet("color: green; font-weight: bold;")
            self.port_combo.setEnabled(False)
            self.baud_combo.setEnabled(False)
        else:
            self.connect_btn.setText("Connect")
            self.status_label.setText("Disconnected")
            self.status_label.setStyleSheet("color: red; font-weight: bold;")
            self.port_combo.setEnabled(True)
            self.baud_combo.setEnabled(True)


# ============================================================================
# Main Application Window
# ============================================================================

class MoonControlWindow(QMainWindow):
    """Main application window for pyMoonControl."""

    def __init__(self):
        super().__init__()
        self.setWindowTitle("pyMoonControl - MoonStalker Drive Unit")
        self.setMinimumSize(900, 700)

        # Serial communication
        self.serial_thread = SerialThread(self)
        self.serial_thread.data_received.connect(self.on_data_received)
        self.serial_thread.connection_status.connect(self.on_connection_status)

        # Free run state tracking
        self._active_direction: str | None = None
        self._pending_direction: str | None = None

        self.build_ui()
        self.apply_styles()

    def build_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        main_layout = QVBoxLayout(central)
        main_layout.setSpacing(6)

        # ---- Serial Port Selector (top) ----
        self.port_selector = SerialPortSelector()
        self.port_selector.port_changed.connect(self.on_port_changed)
        main_layout.addWidget(self.port_selector)

        # ---- Main Content Area (log left, commands + directions right) ----
        content_layout = QHBoxLayout()
        content_layout.setSpacing(10)

        # === Left: Communication Log ===
        left_panel = self.build_log_panel()
        content_layout.addWidget(left_panel, 1)

        # === Right: Commands (top) + Direction Pad (middle) + Clear Log (bottom) ===
        right_side = QVBoxLayout()
        right_side.setSpacing(6)

        commands_panel = self.build_commands_panel()
        right_side.addWidget(commands_panel)

        direction_panel = self.build_direction_panel()
        right_side.addWidget(direction_panel)

        clear_btn = QPushButton("Clear Log")
        clear_btn.clicked.connect(self.log_text.clear)
        clear_btn.setMaximumWidth(120)
        clear_wrap = QHBoxLayout()
        clear_wrap.addStretch()
        clear_wrap.addWidget(clear_btn)
        clear_wrap.addStretch()
        right_side.addLayout(clear_wrap)

        right_side.addStretch()

        right_widget = QWidget()
        right_widget.setLayout(right_side)
        content_layout.addWidget(right_widget)

        main_layout.addLayout(content_layout, 1)

        # Status bar
        self.statusBar().showMessage("Ready - Not connected")

    # ------------------------------------------------------------------
    # Direction Pad (MVS buttons)
    # ------------------------------------------------------------------

    def build_direction_panel(self):
        group = QGroupBox("Free Run (MVS)")
        layout = QVBoxLayout(group)

        # Speed input
        speed_row = QHBoxLayout()
        speed_row.addWidget(QLabel("Speed (RPM):"))
        self.mvs_speed_input = QSpinBox()
        self.mvs_speed_input.setRange(1, 9999)
        self.mvs_speed_input.setValue(60)
        self.mvs_speed_input.setMinimumWidth(80)
        speed_row.addWidget(self.mvs_speed_input)
        speed_row.addStretch()
        layout.addLayout(speed_row)

        # Direction pad grid (2 rows above, 3 middle, 2 below)
        pad_layout = QGridLayout()
        pad_layout.setSpacing(4)
        pad_layout.setHorizontalSpacing(4)

        btn_size = 40

        # Direction button references for state tracking
        self._dir_buttons: dict[str, QPushButton] = {}

        # Row 0: NW - N - NE
        self._dir_buttons["NW"] = self.make_dir_button("NW", btn_size)
        self._dir_buttons["N"] = self.make_dir_button("N", btn_size)
        self._dir_buttons["NE"] = self.make_dir_button("NE", btn_size)
        pad_layout.addWidget(self._dir_buttons["NW"], 0, 0, Qt.AlignCenter)
        pad_layout.addWidget(self._dir_buttons["N"], 0, 1, Qt.AlignCenter)
        pad_layout.addWidget(self._dir_buttons["NE"], 0, 2, Qt.AlignCenter)

        # Row 1: W - (center/stop) - E
        self._dir_buttons["W"] = self.make_dir_button("W", btn_size)
        self.btn_stop = QPushButton("STOP")
        self.btn_stop.setAutoDefault(False)
        self.btn_stop.setFixedSize(btn_size, btn_size)
        self.btn_stop.setStyleSheet("""
            QPushButton {
                background-color: #d32f2f;
                color: white;
                font-weight: bold;
                font-size: 8pt;
                border-radius: 6px;
            }
            QPushButton:pressed {
                background-color: #b71c1c;
            }
        """)
        self.btn_stop.clicked.connect(self.send_stop)
        self._dir_buttons["E"] = self.make_dir_button("E", btn_size)
        pad_layout.addWidget(self._dir_buttons["W"], 1, 0, Qt.AlignCenter)
        pad_layout.addWidget(self.btn_stop, 1, 1, Qt.AlignCenter)
        pad_layout.addWidget(self._dir_buttons["E"], 1, 2, Qt.AlignCenter)

        # Row 2: SW - S - SE
        self._dir_buttons["SW"] = self.make_dir_button("SW", btn_size)
        self._dir_buttons["S"] = self.make_dir_button("S", btn_size)
        self._dir_buttons["SE"] = self.make_dir_button("SE", btn_size)
        pad_layout.addWidget(self._dir_buttons["SW"], 2, 0, Qt.AlignCenter)
        pad_layout.addWidget(self._dir_buttons["S"], 2, 1, Qt.AlignCenter)
        pad_layout.addWidget(self._dir_buttons["SE"], 2, 2, Qt.AlignCenter)

        layout.addLayout(pad_layout)

        return group

    def make_dir_button(self, direction: str, size: int):
        """Create a direction button with click-to-toggle logic."""
        btn = QPushButton(direction)
        btn.setAutoDefault(False)
        btn.setFixedSize(size, size)
        btn.setStyleSheet("""
            QPushButton {
                background-color: #1976d2;
                color: white;
                font-weight: bold;
                font-size: 8pt;
                border-radius: 6px;
            }
            QPushButton:pressed {
                background-color: #0d47a1;
            }
        """)
        btn.clicked.connect(lambda checked, d=direction: self.on_dir_button_clicked(d))
        return btn

    def on_dir_button_clicked(self, direction: str):
        """Handle direction button click (toggle on/off).
        Uses a guard flag to prevent multiple emissions from a single click.
        """
        # Guard against rapid re-entry / duplicate clicks
        if getattr(self, '_click_guard', False):
            return
        self._click_guard = True
        import time
        # Re-enable after 300ms (well past any double-click or bounce)
        QTimer.singleShot(300, lambda: setattr(self, '_click_guard', False))

        # If already active and same direction clicked -> send MVE to deactivate
        if self._active_direction == direction:
            self.send_serial("<MVE>")
            return

        # If a different direction is active -> ignore this click
        if self._active_direction is not None:
            return

        # No active direction -> send MVS to activate
        speed = self.mvs_speed_input.value()
        cmd = f"<MVS {direction} {speed}>"
        self._pending_direction = direction
        self.send_serial(cmd)

    def _set_button_active(self, direction: str | None, active: bool):
        """Paint a direction button green (active) or blue (inactive)."""
        for d, btn in self._dir_buttons.items():
            if d == direction:
                if active:
                    btn.setStyleSheet("""
                        QPushButton {
                            background-color: #2e7d32;
                            color: white;
                            font-weight: bold;
                            font-size: 8pt;
                            border-radius: 6px;
                        }
                        QPushButton:pressed {
                            background-color: #1b5e20;
                        }
                    """)
                else:
                    btn.setStyleSheet("""
                        QPushButton {
                            background-color: #1976d2;
                            color: white;
                            font-weight: bold;
                            font-size: 8pt;
                            border-radius: 6px;
                        }
                        QPushButton:pressed {
                            background-color: #0d47a1;
                        }
                    """)
                break

    # ------------------------------------------------------------------
    # Other Commands Panel
    # ------------------------------------------------------------------

    def build_commands_panel(self):
        layout = QVBoxLayout()
        layout.setContentsMargins(0, 0, 0, 0)

        # --- MV (Move) ---
        mv_box = QGroupBox("MV - Finite Step Move")

        mv_grid = QGridLayout(mv_box)
        mv_grid.addWidget(QLabel("Horiz Steps:"), 0, 0)
        self.mv_horiz = QSpinBox()
        self.mv_horiz.setRange(-99999, 99999)
        self.mv_horiz.setValue(200)
        mv_grid.addWidget(self.mv_horiz, 0, 1)
        mv_grid.addWidget(QLabel("Vert Steps:"), 1, 0)
        self.mv_vert = QSpinBox()
        self.mv_vert.setRange(-99999, 99999)
        self.mv_vert.setValue(100)
        mv_grid.addWidget(self.mv_vert, 1, 1)
        mv_grid.addWidget(QLabel("RPM:"), 2, 0)
        self.mv_rpm = QSpinBox()
        self.mv_rpm.setRange(1, 9999)
        self.mv_rpm.setValue(60)
        mv_grid.addWidget(self.mv_rpm, 2, 1)
        mv_btn = QPushButton("Send MV")
        mv_btn.setAutoDefault(False)
        mv_btn.clicked.connect(self.send_mv)
        mv_grid.addWidget(mv_btn, 3, 0, 1, 2)
        mv_grid.setColumnStretch(1, 1)

        layout.addWidget(mv_box)

        # --- Query Commands row ---
        query_box = QGroupBox("Queries / System")
        query_layout = QVBoxLayout(query_box)

        # First row
        row1 = QHBoxLayout()
        btry_btn = QPushButton("BTRY?")
        btry_btn.setAutoDefault(False)
        btry_btn.clicked.connect(lambda: self.send_serial("<BTRY?>"))
        row1.addWidget(btry_btn)

        mvst_btn = QPushButton("MVST?")
        mvst_btn.setAutoDefault(False)
        mvst_btn.clicked.connect(lambda: self.send_serial("<MVST?>"))
        row1.addWidget(mvst_btn)

        lim_btn = QPushButton("LIM?")
        lim_btn.setAutoDefault(False)
        lim_btn.clicked.connect(lambda: self.send_serial("<LIM?>"))
        row1.addWidget(lim_btn)

        alarms_btn = QPushButton("ALARMS?")
        alarms_btn.setAutoDefault(False)
        alarms_btn.clicked.connect(lambda: self.send_serial("<ALARMS?>"))
        row1.addWidget(alarms_btn)

        query_layout.addLayout(row1)

        # Second row
        row2 = QHBoxLayout()
        coords_btn = QPushButton("COORDS?")
        coords_btn.setAutoDefault(False)
        coords_btn.clicked.connect(lambda: self.send_serial("<COORDS?>"))
        row2.addWidget(coords_btn)

        step_counter_btn = QPushButton("STEP_CNT?")
        step_counter_btn.setAutoDefault(False)
        step_counter_btn.clicked.connect(lambda: self.send_serial("<STEP_COUNTER?>"))
        row2.addWidget(step_counter_btn)

        step_counter_rst_btn = QPushButton("STEP_CNT_RST")
        step_counter_rst_btn.setAutoDefault(False)
        step_counter_rst_btn.clicked.connect(lambda: self.send_serial("<STEP_COUNTER_RESET>"))
        row2.addWidget(step_counter_rst_btn)

        query_layout.addLayout(row2)

        # Third row
        row3 = QHBoxLayout()
        debug_btn = QPushButton("DEBUG")
        debug_btn.setAutoDefault(False)
        debug_btn.clicked.connect(lambda: self.send_serial("<DEBUG>"))
        row3.addWidget(debug_btn)

        stop_btn = QPushButton("STOP")
        stop_btn.setAutoDefault(False)
        stop_btn.clicked.connect(self.send_stop)
        stop_btn.setStyleSheet("background-color: #d32f2f; color: white; font-weight: bold;")
        row3.addWidget(stop_btn)

        mve_btn = QPushButton("MVE")
        mve_btn.setAutoDefault(False)
        mve_btn.clicked.connect(lambda: self.send_serial("<MVE>"))
        row3.addWidget(mve_btn)

        query_layout.addLayout(row3)
        layout.addWidget(query_box)

        # --- Custom command ---
        custom_box = QGroupBox("Custom Command")
        custom_layout = QHBoxLayout(custom_box)
        self.custom_cmd_input = QLineEdit()
        self.custom_cmd_input.setPlaceholderText("e.g. <MV 100 100 30>")
        custom_layout.addWidget(self.custom_cmd_input)
        custom_send_btn = QPushButton("Send")
        custom_send_btn.setAutoDefault(False)
        custom_send_btn.clicked.connect(self.send_custom)
        custom_layout.addWidget(custom_send_btn)
        layout.addWidget(custom_box)

        wrap = QWidget()
        wrap.setLayout(layout)
        return wrap

    # ------------------------------------------------------------------
    # Log Panel
    # ------------------------------------------------------------------

    def build_log_panel(self):
        group = QGroupBox("Communication Log")
        layout = QVBoxLayout(group)

        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        self.log_text.setFont(QFont("Consolas", 10))
        layout.addWidget(self.log_text, 1)

        return group

    # ------------------------------------------------------------------
    # Styles
    # ------------------------------------------------------------------

    def apply_styles(self):
        self.setStyleSheet("""
            QGroupBox {
                font-weight: bold;
                border: 1px solid #888;
                border-radius: 6px;
                margin-top: 8px;
                padding-top: 14px;
            }
            QGroupBox::title {
                subcontrol-origin: margin;
                subcontrol-position: top left;
                padding: 2px 6px;
                background-color: #e0e0e0;
                border-radius: 3px;
            }
        """)

    # ------------------------------------------------------------------
    # Serial Communication
    # ------------------------------------------------------------------

    def on_port_changed(self, port_name: str, baud: int):
        """Handle serial port connection/disconnection."""
        if port_name and baud > 0:
            try:
                self.serial_thread.connect_port(port_name, baud)
            except Exception as e:
                QMessageBox.critical(self, "Connection Error", f"Could not connect: {e}")
        else:
            self.serial_thread.disconnect_port()

    def on_connection_status(self, connected: bool):
        """Update UI when connection status changes."""
        self.port_selector.set_connected(connected)
        if connected:
            self.statusBar().showMessage("Connected")
        else:
            self.statusBar().showMessage("Disconnected")

    def send_serial(self, command: str):
        """Send a command over serial and log it.
        Includes a 200ms debounce to prevent duplicate sends."""
        import time
        if not hasattr(self, '_last_cmd_time'):
            self._last_cmd_time = 0
            self._last_cmd = None
        now = time.time()
        # If same command was sent within the last 200ms, ignore it
        if command == self._last_cmd and (now - self._last_cmd_time) < 0.2:
            return
        self._last_cmd = command
        self._last_cmd_time = now
        if self.serial_thread.send_command(command):
            self.log_message(">>>", command)
        else:
            self.log_message("!!!", "Not connected - cannot send")
            self.statusBar().showMessage("Not connected!")

    def on_data_received(self, data: str):
        """Handle incoming serial data."""
        self.log_message("<<<", data)

        # Data arrives as e.g. "<MVS_ACK N 60>" or "<MVS_NACK LIMIT_BLOCKED>"
        # Strip leading '<' for easier matching
        stripped = data.lstrip("<")

        # Parse ACK/NACK responses for free run commands
        if stripped.startswith("MVS_ACK") and self._pending_direction is not None:
            # Free run activated successfully
            self._active_direction = self._pending_direction
            self._pending_direction = None
            self._set_button_active(self._active_direction, True)

        elif stripped.startswith("MVS_NACK") and self._pending_direction is not None:
            # Free run rejected (limit blocked, not ready, unknown direction)
            self._pending_direction = None

        elif stripped.startswith("MVE_ACK") and self._active_direction is not None:
            # Free run stopped successfully
            self._set_button_active(self._active_direction, False)
            self._active_direction = None

        elif stripped.startswith("MVE_NACK") and self._active_direction is not None:
            # MVE rejected (e.g. not in FREE_RUN_MODE) — force clear anyway
            self._set_button_active(self._active_direction, False)
            self._active_direction = None

        elif stripped.startswith("STOP_ACK") and self._active_direction is not None:
            # STOP also ends free run
            self._set_button_active(self._active_direction, False)
            self._active_direction = None
            self._pending_direction = None

    def log_message(self, prefix: str, message: str):
        """Append a formatted message to the log window with a timestamp."""
        from datetime import datetime
        ts = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        self.log_text.append(f"[{ts}] {prefix} {message}")
        # Auto-scroll to bottom
        scrollbar = self.log_text.verticalScrollBar()
        scrollbar.setValue(scrollbar.maximum())

    # ------------------------------------------------------------------
    # Command Senders
    # ------------------------------------------------------------------

    def send_mv(self):
        """Send a finite step move command."""
        h = self.mv_horiz.value()
        v = self.mv_vert.value()
        r = self.mv_rpm.value()
        cmd = f"<MV {h} {v} {r}>"
        self.send_serial(cmd)

    def send_stop(self):
        """Send emergency stop command and clear any active free run state."""
        self.send_serial("<STOP>")
        # Optimistically clear active direction (STOP_ACK handler will also do this)
        if self._active_direction is not None:
            self._set_button_active(self._active_direction, False)
            self._active_direction = None
            self._pending_direction = None

    def send_custom(self):
        """Send a custom command from the text input."""
        cmd = self.custom_cmd_input.text().strip()
        if cmd:
            # Ensure it has angle brackets
            if not cmd.startswith("<"):
                cmd = "<" + cmd
            if not cmd.endswith(">"):
                cmd = cmd + ">"
            self.send_serial(cmd)
            self.custom_cmd_input.clear()

    # ------------------------------------------------------------------
    # Cleanup
    # ------------------------------------------------------------------

    def closeEvent(self, event):
        """Clean up on close."""
        self.serial_thread.stop()
        event.accept()


# ============================================================================
# Entry Point
# ============================================================================

def main():
    app = QApplication(sys.argv)
    app.setApplicationName("pyMoonControl")
    window = MoonControlWindow()
    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
