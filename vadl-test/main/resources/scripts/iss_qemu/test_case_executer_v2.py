import asyncio
import os
import subprocess
import time
from dataclasses import dataclass
from typing import Literal, Dict, Optional

import yaml

from qemu.qmp import QMPClient, EventListener

from abstract_test_case_executor import AbstractTestCaseExecutor
from utils import run_cmd_fail
from models import TestSpec, RegResultType, TestResult

TEST_TIMEOUT_SEC = 1
SIGNAL_REG = "t1"
SIGNAL_CONTENT = "de"
QEMU_EXEC = "qemu-system-riscv64"
RV64_ELF = "riscv64-unknown-elf-"


class LogTestCaseExecutor(AbstractTestCaseExecutor):
    """
    Executes a test case by running QEMU with the -d cpu option to log CPU execution.
    This is currently not functional. Use the QMPTestCaseExecutor instead.
    """
    def __init__(self, spec: TestSpec):
        super().__init__(spec)
    

    async def exec(self):
        await self._start_qemu()
        await self._shutdown()

        print("Log file:", self.log_file)

    async def _start_qemu(self):
        self.log_file = self._tmp_file(f"qemu-{self.spec.id}.log")
        self.process = await asyncio.create_subprocess_exec(
            QEMU_EXEC,
            "-nographic",
            "-machine", "virt",
            "-bios", self.test_elf,
            "-D", self.log_file,
            "-d", "cpu",
        )

    async def _shutdown(self):
        print("Shutting down QEMU...")
        if self.process.returncode is None:
            self.process.kill()
            print("Process killed", self.process.pid)