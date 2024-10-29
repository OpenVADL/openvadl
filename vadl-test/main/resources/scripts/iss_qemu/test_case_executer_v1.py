import asyncio
import os
import time
from typing import Optional

from qemu.qmp import QMPClient, EventListener

from abstract_test_case_executor import AbstractTestCaseExecutor
from models import TestSpec
from qemu_executer import QEMUExecuter

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

    def __init__(self, qemu_exec: str, spec: TestSpec):
        super().__init__(qemu_exec, spec)

    async def exec(self, port: int):
        # combiniation of spec.reg_tests.keys and spec.reference_regs
        regs_of_interest = list(set(self.spec.reg_tests.keys())
                             .union(set(self.spec.reference_regs)))

        # test with vadl generated qemu
        vadl_reg_results = await self._execute_qemu_sim(
            f"vadl-{self.spec.id}",
            self.qemu_exec,
            port,
            regs_of_interest
        )

        self.test_result.completed_stages.append('RUN')

        ref_reg_results = {}
        if self.spec.reference_exec != "":
            ref_reg_results = await self._execute_qemu_sim(
                f"reference-{self.spec.id}",
                self.spec.reference_exec,
                port,
                regs_of_interest
            )
            self.test_result.completed_stages.append('RUN_REF')

        await self._set_results(vadl_reg_results, ref_reg_results)

    async def _execute_qemu_sim(self, prefix: str, 
                                qemu_exec: str,
                                port: int,
                                result_regs: list[str]) -> dict[str, str]:
        instance_name = f"{prefix}-{self.spec.id}"
        qemu_executer = QEMUExecuter(instance_name, 
                                    qemu_exec,
                                    port)
        
        self.test_result.qemu_log[instance_name] = qemu_executer.logs

        return await qemu_executer.execute(
            self.test_elf,
            result_regs,
            SIGNAL_REG,
            SIGNAL_CONTENT,
            TEST_TIMEOUT_SEC
        )

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
