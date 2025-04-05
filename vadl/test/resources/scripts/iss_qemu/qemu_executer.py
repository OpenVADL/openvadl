import asyncio
import os
import signal
from pathlib import Path

from compiler import CompInfo
from models import Config, Tool


class QEMUExecuter:
    config: Config
    compinfo: CompInfo
    ref: bool # if this executes the reference
    qemu_exec: Tool
    custom_args: str
    timeout: float

    def __init__(self, config: Config, compinfo: CompInfo, ref: bool, timeout: float = 1):
        self.process = None
        self.config = config
        self.compinfo = compinfo
        self.ref = ref
        self.stdout = []
        self.stderr = []
        self.qemu_exec = config.ref if ref else config.sim
        self.timeout = timeout

    async def execute(self) -> bool:
        try:
            self.process = await asyncio.create_subprocess_exec(
                self.qemu_exec.path,
                "-nographic",
                "-plugin", self.config.stateplugin,
                "-d", "plugin",
                *self.qemu_exec.args.split(),
                self.compinfo.elf,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )

            async def read_stream(stream: asyncio.StreamReader, line_list: list):
                async for line in stream:
                    decoded_line = line.decode().strip()
                    line_list.append(decoded_line)

            # Create tasks to read stdout and stderr concurrently
            stdout_task = asyncio.create_task(read_stream(self.process.stdout, self.stdout))
            stderr_task = asyncio.create_task(read_stream(self.process.stderr, self.stderr))

            # Wait for the subprocess to finish and for both tasks to complete
            await asyncio.wait_for(self.process.wait(), timeout=self.timeout)
            await asyncio.gather(stdout_task, stderr_task)

            return self.process.returncode == 0
        except asyncio.TimeoutError:
            print("[QEMU_EXECUTOR] Process exceeded timeout. Terminating...")
            try:
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
            finally:
                return False

    async def extract_regs(self) -> dict[str, int]:
        """
        Extracts register name-value pairs from self.stdout lines in the format `[REG] <name>: 0x<val>`.
        Returns:
           dict: A dictionary where keys are register names and values are their corresponding values in hex.
        """
        # gdbregmap maps sim regs to reference maps. here we get the right ones
        regs_names = self.config.gdbregmap.values() if self.ref else self.config.gdbregmap.keys()
        regs: dict[str, any] = {reg_name: None for reg_name in regs_names}
        for line in self.stderr:
            if line.startswith("[REG]"):
                try:
                    _, reg_info = line.split(" ", 1)
                    name, val = reg_info.split(": ")
                    if name not in regs_names:
                        continue
                    regs[name] = int(val, 16)
                except ValueError:
                    print(f"[QEMU_EXECUTOR] Failed to parse register line: {line}")

        for (name, val) in regs.items():
            if val is None:
                raise Exception(f"[QEMU_EXECUTOR] Register {name} not found in stdout")

        return regs

        
        
             