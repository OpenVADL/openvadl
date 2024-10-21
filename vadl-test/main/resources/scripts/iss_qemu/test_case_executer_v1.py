import asyncio
import os
import time
from typing import Optional

from qemu.qmp import QMPClient, EventListener

from abstract_test_case_executor import AbstractTestCaseExecutor
from models import TestSpec

TEST_TIMEOUT_SEC = 1
SIGNAL_REG = "X6"
SIGNAL_CONTENT = "de"
QEMU_EXEC = "/qemu/build/qemu-system-vadl"
RV64_ELF = "riscv64-unknown-elf-"


class QMPTestCaseExecutor(AbstractTestCaseExecutor):
    """
    Executes a test case by running QEMU with QMP to control the test execution.
    It listens for a signal register to check if the test has finished.
    """
    port: int
    qmp: QMPClient
    listener: EventListener
    event_handle_task: asyncio.Task

    def __init__(self, spec: TestSpec, port: int):
        super().__init__(spec)
        self.port = port
        self.qmp = QMPClient(f"test-{spec.id}")
        self.listener = EventListener()
        self.qmp.register_listener(self.listener)
        self.event_handle_task = asyncio.Task(self._event_handler())

    async def exec(self):
        await self._start_qemu()
        await self._connect_qmp()
        await self.qmp.execute('cont')
        await self._wait_until_done()
        await self._set_results()
        await self._shutdown()

    async def _event_handler(self):
        try:
            async for event in self.listener:
                # do nothing
                None
        except asyncio.CancelledError:
            return

    async def _shutdown(self):
        await self.qmp.execute('stop')
        self.event_handle_task.cancel()
        await self.event_handle_task
        if self.process.returncode is None:
            self.process.kill()
        self.qmp.remove_listener(self.listener)
        await self.qmp.disconnect()

    async def _wait_until_done(self):
        start_time = time.time()
        while True:
            poll_time = time.time()
            sig_reg = await self._reg_info(SIGNAL_REG)
            if sig_reg.endswith(SIGNAL_CONTENT):
                break

            diff_time = poll_time - start_time
            if diff_time > TEST_TIMEOUT_SEC:
                await self._shutdown()
                raise Exception(f"Timeout: Test failed due to timeout of finish signal ({diff_time:.4f}s)")

    async def _connect_qmp(self):
        # TODO: make try counter
        qmp_port = self._get_qmp_port()
        first_time = time.time()
        while True:
            try:
                await self.qmp.connect(("localhost", qmp_port))
                break
            except Exception as e:
                if (time.time() - first_time) > TEST_TIMEOUT_SEC:
                    raise e

    async def _start_qemu(self):
        qmp_addr = f"localhost:{self._get_qmp_port()}"
        self.process = await asyncio.create_subprocess_exec(
            QEMU_EXEC,
            "-nographic",
            "-S",  # pause on start to wait for debugger
            "-qmp", f"tcp:{qmp_addr},server=on,wait=off",
            "-machine", "virt",
            "-bios", self.test_elf
        )

    def _get_qmp_port(self) -> int:
        return self.port

    async def _reg_info(self, reg: str) -> str:
        response = await self.qmp.execute('human-monitor-command', {'command-line': f'info registers'})
        result = self._extract_register_value(response, reg)
        if result is None:
            raise Exception(f"Failed to extract register value for {reg}. Full dump:\n{response}")
        return result

    def _tmp_file(self, name: str) -> str:
        build_dir = f"/tmp/build-{self.spec.id}/"
        os.makedirs(build_dir, exist_ok=True)
        return f"{build_dir}/{name}"

    def _build_assembly_test(self, core: str, out_path: str) -> str:
        if SIGNAL_REG.lower() in core.lower():
            raise Exception(
                f"Failed to build assembly: You cannot use the {SIGNAL_REG} register, as it is used as internal signal register!")

        content = f"""
        .global _start
        .section .text.test
        
        _start:	
        {core}
        
        # the qmp script polls this t1 to check whether the test has ended
        signal_stop:
        addi t1, x0, 0xde
        
        loop:	j loop
        """

        with open(out_path, "w") as f:
            f.write(content)

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
