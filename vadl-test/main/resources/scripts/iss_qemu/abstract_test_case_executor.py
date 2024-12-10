import os
import yaml

from models import TestResult, TestSpec, RegResultType
from utils import run_cmd_fail

RV64_ELF = "riscv64-unknown-elf-"


class AbstractTestCaseExecutor:
    spec: TestSpec
    test_result: TestResult
    test_elf: str
    qemu_exec: str

    def __init__(self, qemu_exec: str, spec: TestSpec):
        self.qemu_exec = qemu_exec
        self.spec = spec
        reg_tests_result: RegResultType = {str(key): {'expected': str(value), 'actual': 'unknown'} for key, value in
                                           spec.reg_tests.items()}
        self.test_result = TestResult(status='PASS', completed_stages=[], reg_tests=reg_tests_result, errors=[],
                                      duration="0ms")

    async def exec(self):
        raise NotImplementedError("This method must be implemented by the subclass")

    async def compile_and_link(self, arch: str = "rv64i_zicsr", abi: str = "lp64"):
        # build assembly source
        asm_source = self._tmp_file(f"asm-{self.spec.id}.s")
        self._build_assembly_test(self.spec.asm_core, asm_source)

        with open(asm_source, "r") as f:
            self.test_result.full_asm = f.read()

        # compile source
        cmp_out = self._tmp_file(f"cmp-{self.spec.id}.o")
        await run_cmd_fail(RV64_ELF + "as", f"-march={arch}",
                           f"-mabi={abi}", "-o", cmp_out, asm_source)
        self.test_result.completed_stages.append('COMPILE')

        # build the linker script
        linker_script = self._tmp_file(f"linker-{self.spec.id}.ld")
        self._build_linker_script(cmp_out, linker_script)

        # link and produce elf
        self.test_elf = self._tmp_file(f"elf-{self.spec.id}")
        await run_cmd_fail(RV64_ELF + "ld", "-T",
                           linker_script, "-o",
                           self.test_elf, cmp_out)
        self.test_result.completed_stages.append('LINK')

    def _build_linker_script(self, obj_file: str, out_path: str) -> str:
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

    def _build_assembly_test(self, core: str, out_path: str) -> str:
        content = f"""
        .global _start
        .section .text.test
        
        _start:	
        {core}
        
        j end1
        end1:

        j end2
        end2:
        
        # abort the test here
        """

        with open(out_path, "w") as f:
            f.write(content)

    def get_test_result_map(self):
        return {
            'id': self.spec.id,
            'result': {
                'status': self.test_result.status,
                'completedStages': self.test_result.completed_stages,
                'regTests': self.test_result.reg_tests,
                'errors': self.test_result.errors,
                'duration': self.test_result.duration,
                'qemuLog': self.test_result.qemu_log,
            }
        }

    async def _set_results(self, vadl_reg_results: dict[str, str],
                           ref_reg_results: dict[str, str] = {}):
        self._check_hardcoded_regs(vadl_reg_results)
        self._check_ref_regs(vadl_reg_results, ref_reg_results)

    def _check_hardcoded_regs(self, reg_results: dict[str, str]):
        for reg, val in self.test_result.reg_tests.items():
            expected = val['expected'].lower().strip()
            actual = reg_results[reg].lower().strip()
            val['actual'] = actual
            if expected != actual:
                self.test_result.status = 'FAIL'
                self.test_result.errors.append(f'Harcoded register test {reg} failed: {expected} != {actual}')

    def _check_ref_regs(self, vadl_reg_results: dict[str, str], ref_reg_results: dict[str, str]):
        result_reg_tests = self.test_result.reg_tests

        for reg, ref_val in ref_reg_results.items():
            ref_val = ref_val.lower().strip()
            vadl_val = vadl_reg_results.get(reg, "").lower().strip()  # Use get with default empty string
            if ref_val != vadl_val:
                self._add_error(f'Reference register test {reg} failed: {vadl_val} (vadl) != {ref_val} (ref)')

                # Properly check and initialize result_reg_tests[reg]
            if reg not in result_reg_tests or result_reg_tests[reg] is None:
                # Initialize with a dict if not already done
                result_reg_tests[reg] = {'expected': ref_val}
            else:
                stored_expected = result_reg_tests[reg]['expected'].lower().strip()
                if stored_expected != ref_val:
                    # fail if the hardcoded value is different from the reference value
                    self._add_error(f'Invalid hardcoded reg {reg}: {stored_expected} (hardcoded) != {ref_val} (ref)')

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

    def _tmp_file(self, name: str) -> str:
        build_dir = f"/tmp/build-{self.spec.id}/"
        os.makedirs(build_dir, exist_ok=True)
        return f"{build_dir}/{name}"

    def _add_error(self, msg: str):
        self.test_result.status = 'FAIL'
        self.test_result.errors.append(msg)
