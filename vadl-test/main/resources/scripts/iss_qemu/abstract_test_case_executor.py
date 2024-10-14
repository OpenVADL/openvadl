

import os

import yaml
from models import TestResult, TestSpec, RegResultType
from utils import run_cmd_fail


RV64_ELF = "riscv64-unknown-elf-"

class AbstractTestCaseExecutor:
    spec: TestSpec
    test_result: TestResult
    test_elf: str

    def __init__(self, spec: TestSpec):
        self.spec = spec
        reg_tests_result: RegResultType = {str(key): {'expected': str(value), 'actual': 'unknown'} for key, value in
                                           spec.reg_tests.items()}
        self.test_result = TestResult(status='PASS', completed_stages=[], reg_tests=reg_tests_result, errors=[],
                                      duration="0ms")

    async def exec(self):
        raise NotImplementedError("This method must be implemented by the subclass")

    async def compile_and_link(self, arch: str = "rv64i", abi: str = "lp64"):
        # build assembly source
        asm_source = self._tmp_file(f"asm-{self.spec.id}.s")
        self._build_assembly_test(self.spec.asm_core, asm_source)

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
                'duration': self.test_result.duration
            }
        }
    
    async def _set_results(self):
        self.test_result.completed_stages.append('RUN')

        for reg, val in self.test_result.reg_tests.items():
            expected = val['expected'].lower().strip()
            actual = await self._reg_info(reg)
            val['actual'] = actual
            if expected != actual:
                self.test_result.status = 'FAIL'
                self.test_result.errors.append(f'Reg test {reg} failed: {expected} != {actual}')

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