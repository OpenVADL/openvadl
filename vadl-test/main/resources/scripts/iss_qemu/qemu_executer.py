import asyncio
import os
import signal
import socket
import time
from qemu.qmp import QMPClient, EventListener
from typing import Optional

from utils import get_unique_sock_addr


class QEMUExecuter:

    def __init__(self, name: str, qemu_exec: str):
        self.qemu_exec = qemu_exec
        self.qmp = QMPClient(name)
        self.listener = EventListener()
        self.qmp.register_listener(self.listener)
        self.event_handle_task = asyncio.Task(self._event_handler())
        self.logs = []
        self.sock_addr = get_unique_sock_addr()

    def __del__(self):
        try:
            # remove socket file if it exists
            os.remove(self.sock_addr)
        except Exception as e:
            pass

    async def execute(self, test_elf: str,
                      result_regs: list[str],
                      signal_reg: str,
                      signal_content: str,
                      timeout_sec: int):
#         print(f"[QEMU_EXECUTOR] Starting QEMU ({self.qemu_exec}) with {test_elf}")
        await self._start_qemu(test_elf)
        await self._connect_qmp(timeout_sec)
        await self.qmp.execute('cont')
#         print(f"[QEMU_EXECUTOR] Wait until test is finished... ", end="", flush=True)
        await self._wait_until_done(signal_reg, signal_content, timeout_sec)
        print(f"done.")
        reg_results = await self._fetch_result_regs(result_regs)

        await self._shutdown()
        return reg_results

    async def _start_qemu(self, test_elf: str):
        qmp_addr = f"unix:{self.sock_addr}"
        qmp_conn = f"{qmp_addr},server=on,wait=off"
        self.process = await asyncio.create_subprocess_exec(
            self.qemu_exec,
            "-nographic",
            "-S",  # pause on start to wait for debugger
            "-qmp", qmp_conn,
            "-machine", "virt",
            "-bios", test_elf,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        self.log_task = asyncio.create_task(self._capture_logs())

    async def _capture_logs(self):
        async def capture_stream(stream, prefix):
            async for line in stream:
                decoded_line = line.decode().strip()
                self.logs.append(f"{prefix}{decoded_line}")

        # Create tasks for capturing stdout and stderr concurrently
        await asyncio.gather(
            capture_stream(self.process.stdout, "[STDOUT]"),
            capture_stream(self.process.stderr, "[STDERR]")
        )

    async def _event_handler(self):
        try:
            async for event in self.listener:
                # do nothing
                None
        except asyncio.CancelledError:
            return

    async def _fetch_result_regs(self, result_regs: list[str]) -> dict[str, str]:
        result = {}
        for reg in result_regs:
            result[reg] = await self._reg_info(reg)
        return result

    async def _shutdown(self, force: bool = False):
#         print(f"[QEMU_EXECUTOR] Shutting down QEMU")

        try:
            if not force:
                await self.qmp.execute('stop')
        except Exception as e:
            print(f"[QEMU_EXECUTOR] Error during graceful shutdown: {e}")

        
        self.qmp.remove_listener(self.listener)
        await self.qmp.disconnect()

        # Cancel the event handle task if it's running
        if self.event_handle_task:
            self.event_handle_task.cancel()
            try:
                await self.event_handle_task
            except asyncio.CancelledError:
                pass

        # Forcefully terminate the process if it's still running
        if self.process.returncode is None:
            try:
#                 print(f"[QEMU_EXECUTOR] Terminating process...")
                self.process.terminate()
                await self.process.wait()
            except Exception as e:
                print(f"[QEMU_EXECUTOR] Terminate failed, trying kill: {e}")
                try:
                    # Send a SIGKILL signal as a last resort
                    os.kill(self.process.pid, signal.SIGKILL)
                    print(f"[QEMU_EXECUTOR] Sent SIGKILL to process")
                except Exception as kill_error:
                    print(f"[QEMU_EXECUTOR] Failed to force kill process: {kill_error}")

#         print(f"[QEMU_EXECUTOR] Shutdown complete.")

    async def _wait_until_done(self, signal_reg: str, signal_content: str, timeout_sec: int):
        start_time = time.time()
        while True:
            poll_time = time.time()
            sig_reg = await self._reg_info(signal_reg)
            if sig_reg.endswith(signal_content):
                break

            diff_time = poll_time - start_time
            if diff_time > timeout_sec:
                print(f"timed out.")
                await self._shutdown(force=True)
                raise Exception(
                    f"Timeout: Timeout of finish signal {signal_content} in {signal_reg} ({diff_time:.4f}s)")

    async def _connect_qmp(self, timeout_sec: int):
        # TODO: make try counter
        first_time = time.time()
        while True:
            try:
                if (self.process.returncode is not None):
                    raise Exception(f"QEMU process has terminated with return code {self.process.returncode}")
                await self.qmp.connect(self.sock_addr)
                break
            except Exception as e:
                if (time.time() - first_time) > timeout_sec:
                    raise e

    def _get_qmp_port(self) -> int:
        return self.port

    async def _reg_info(self, reg: str) -> str:
        response = await self.qmp.execute('human-monitor-command', {'command-line': f'info registers'})
        result = self._extract_register_value(response, reg)
        if result is None:
            raise Exception(f"Failed to extract register value for {reg}. Full dump:\n{response}")
        return result

    def _extract_register_value(self, registers_info: str, register_name: str) -> Optional[str]:
        # Split the registers_info into lines
        lines = registers_info.split('\n')

        reg_name_lower = register_name.lower()

        # Iterate through each line
        for line in lines:
            # Split the line into register name-value pairs
            parts = line.split()

            # Iterate through each register name-value pair
            for i in range(0, len(parts), 2):
                # Check if the current part matches the register name
                if reg_name_lower in parts[i].strip().lower():
                    # Return the corresponding value without formatting
                    return parts[i + 1]

        # If register name is not found, return None
        return None


async def _capture_logs(self):
    async def capture_stream(stream, prefix):
        while True:
            line = await stream.readline()
            if not line:  # EOF received, stream is closed
                break
            decoded_line = line.decode().strip()
            self.logs.append(f"{prefix}{decoded_line}")

    # Create tasks for capturing stdout and stderr concurrently
    tasks = [
        asyncio.create_task(capture_stream(self.process.stdout, "[STDOUT]")),
        asyncio.create_task(capture_stream(self.process.stderr, "[STDERR]"))
    ]

    # Wait for the process to complete
    await self.process.wait()

    # Once the process is done, cancel the capturing tasks if still running
    for task in tasks:
        task.cancel()
        try:
            await task  # Await task to handle cancellation
        except asyncio.CancelledError:
            pass  # Task was cancelled, so we ignore the cancellation error

    # Optionally, log that process has terminated
    self.logs.append("[INFO] Process terminated.")

    async def _find_free_port(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('', 0))  # Bind to a free port provided by the host.
        address, port = s.getsockname()
        return s, port
