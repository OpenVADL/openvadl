import asyncio
import os
import subprocess
import time
from dataclasses import dataclass
from typing import Literal, Dict, Optional

import yaml

from qemu.qmp import QMPClient, EventListener

from utils import run_cmd_fail

TEST_TIMEOUT_SEC = 1
SIGNAL_REG = "t1"
SIGNAL_CONTENT = "de"
QEMU_EXEC = "/qemu/build/qemu-system-riscv64"
RV64_ELF = "riscv64-unknown-elf-"


@dataclass
class TestSpec:
    """Specifies a test case"""
    id: str
    asm_core: str
    reg_tests: Dict[str, str]


RegResultType = Dict[str, Dict[Literal['expected', 'actual'], str]]


@dataclass
class TestResult:
    status: Literal['PASS', 'FAIL']
    completed_stages: [Literal['COMPILE', 'LINK', 'RUN']]
    reg_tests: RegResultType
    errors: [str]
    duration: str


class TestCase:
    port: int
    spec: TestSpec
    qmp: QMPClient
    listener: EventListener
    event_handle_task: asyncio.Task
    test_result: TestResult
    test_elf: str

    def __init__(self, spec: TestSpec, port: int):
        self.port = port
        self.spec = spec
        self.qmp = QMPClient(f"test-{spec.id}")
        self.listener = EventListener()
        self.qmp.register_listener(self.listener)
        self.event_handle_task = asyncio.Task(self._event_handler())
        reg_tests_result: RegResultType = {str(key): {'expected': str(value), 'actual': 'unknown'} for key, value in
                                           spec.reg_tests.items()}
        self.test_result = TestResult(status='PASS', completed_stages=[], reg_tests=reg_tests_result, errors=[],
                                      duration="0ms")

    async def exec(self):
        await self._start_qemu()
        await self._connect_qmp()
        await self.qmp.execute('cont')
        await self._wait_until_done()
        await self._set_results()
        await self._shutdown()

    async def compile_and_link(self, arch: str = "rv64i", abi: str = "lp64"):
        # build assembly source
        asm_source = self._tmp_file(f"asm-{self.spec.id}.s")
        _build_assembly_test(self.spec.asm_core, asm_source)

        # compile source
        cmp_out = self._tmp_file(f"cmp-{self.spec.id}.o")
        await run_cmd_fail(RV64_ELF + "as", f"-march={arch}",
                           f"-mabi={abi}", "-o", cmp_out, asm_source)
        self.test_result.completed_stages.append('COMPILE')

        # build the linker script
        linker_script = self._tmp_file(f"linker-{self.spec.id}.ld")
        _build_linker_script(cmp_out, linker_script)

        # link and produce elf
        self.test_elf = self._tmp_file(f"elf-{self.spec.id}")
        await run_cmd_fail(RV64_ELF + "ld", "-T",
                           linker_script, "-o",
                           self.test_elf, cmp_out)
        self.test_result.completed_stages.append('LINK')

    async def emit_result(self, dir: str = "results", prefix: str = "result-"):
        os.makedirs(dir, exist_ok=True)
        filename = f"{dir}/{prefix}{self.spec.id}.yaml"
        data = {
            'id': self.spec.id,
            'result': {
                'status': self.test_result.status,
                'completedStages': self.test_result.completed_stages,
                'regTests': self.test_result.reg_tests,
                'errors': self.test_result.errors,
                'duration': self.test_result.duration
            }
        }
        with open(filename, 'w') as f:
            yaml.dump(data, f)

    async def _set_results(self):
        self.test_result.completed_stages.append('RUN')

        for reg, val in self.test_result.reg_tests.items():
            expected = val['expected'].lower().strip()
            actual = await self._reg_info(reg)
            val['actual'] = actual
            if expected != actual:
                self.test_result.status = 'FAIL'
                self.test_result.errors.append(f'Reg test {reg} failed: {expected} != {actual}')

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
            "/qemu/build/qemu-system-riscv64",
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
        return _extract_register_value(response, reg)

    def _tmp_file(self, name: str) -> str:
        build_dir = f"/tmp/build-{self.spec.id}/"
        os.makedirs(build_dir, exist_ok=True)
        return f"{build_dir}/{name}"


def _build_linker_script(obj_file: str, out_path: str) -> str:
    # set our test assembly at 0x80000000
    content = f"""
    MEMORY {{
      dram_space (rwx) : ORIGIN = 0x80000000, LENGTH = 0x8000000
    }}
    
    SECTIONS {{
      .text : {{
        {obj_file}(.text.test)
      }} > dram_space
    }}
    """

    with open(out_path, "w") as f:
        f.write(content)


def _build_assembly_test(core: str, out_path: str) -> str:
    if "t1" in core or "x6" in core:
        raise Exception(
            "Failed to build assembly: You cannot use the t1 register, as it is used as internal signal register!")

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


def _extract_register_value(registers_info: str, register_name: str) -> Optional[str]:
    # Split the registers_info into lines
    lines = registers_info.split('\n')

    # Iterate through each line
    for line in lines:
        # Split the line into register name-value pairs
        parts = line.split()

        # Iterate through each register name-value pair
        for i in range(0, len(parts), 2):
            # Check if the current part matches the register name
            if register_name in parts[i].strip():
                # Return the corresponding value without formatting
                return parts[i + 1]

    # If register name is not found, return None
    return None
