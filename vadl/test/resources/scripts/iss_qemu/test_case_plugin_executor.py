import os
import shutil
from pathlib import Path

import yaml

from compiler import load_and_compile, CompInfo
from models import Test, Config, TestResult
from qemu_executer import QEMUExecuter

class TestCasePluginExecutor:
  test: Test
  config: Config
  test_result: TestResult
  compinfo: CompInfo

  def __init__(self, test: Test, config: Config):
    self.test = test
    self.config = config
    self.test_result = TestResult(status='PASS', completed_stages=[], reg_tests={}, errors=[],
                                  duration="0ms")

  async def exec(self):
    if not self.compinfo:
      raise ValueError("Compile information not available. Not yet compiled? (call compile_and_link first)")

    # run gen
    gen = QEMUExecuter(self.config, self.compinfo, ref=False)
    success = await gen.execute()
    self.test_result.sim_logs['stdout'] = gen.stdout
    self.test_result.sim_logs['stderr'] = gen.stderr
    if not success:
      raise Exception(f"Generated QEMU execution failed with: {gen.process.returncode}")
    self.test_result.completed_stages.append("RUN")

    ref = QEMUExecuter(self.config, self.compinfo, ref=True)
    ref_succ = await ref.execute()
    self.test_result.ref_logs['stdout'] = ref.stdout
    self.test_result.ref_logs['stderr'] = ref.stderr
    if not ref_succ:
      raise Exception(f"Reference QEMU execution failed with: {ref.process.returncode}")
    self.test_result.completed_stages.append("RUN_REF")

    self.compare(await gen.extract_regs(), await ref.extract_regs())
    self.test_result.completed_stages.append("COMPARE")


  def compare(self, gen_regs: dict[str, int], ref_regs: dict[str, int]):

    for reg, val in gen_regs.items():
      ref_reg = self.config.gdbregmap.get(reg)
      if not ref_reg:
        self.test_result.status = 'FAIL'
        self.test_result.errors.append(f"No reference register found for {reg}")
        continue

      ref_val = ref_regs.get(ref_reg)

      self.test_result.reg_tests[reg] = {
        'act': f"0x{val:08x}",
        'exp': f"0x{ref_val:08x}",
      }

      if val != ref_val:
        self.test_result.status = 'FAIL'
        self.test_result.errors.append(f"Wrong reg val {reg} != {ref_reg} : 0x{val:x} != 0x{ref_val:x}")


  async def compile_and_link(self):
    self.compinfo = await load_and_compile(self.config, self.test)
    self.test_result.completed_stages.append("COMPILE")


  async def get_result(self):
    return self.test_result

  async def emit_result(self, dir: str = "results", prefix: str = "result-"):
    # Ensure the directory exists
    output_dir = Path(dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # Construct the filename
    filename = output_dir / f"{prefix}{self.test.id}.yaml"

    # Prepare the data to be written
    data = {
        'id': self.test.id,
        'status': self.test_result.status,
        'duration': self.test_result.duration,
        'completedStages': self.test_result.completed_stages,
        'errors': self.test_result.errors,
        'regTests': self.test_result.reg_tests,
        'simLogs': self.test_result.sim_logs,
        'refLogs': self.test_result.ref_logs,
    }

    # Write data to the YAML file asynchronously
    with open(filename, 'w') as f:
      yaml_str = yaml.safe_dump(data, default_flow_style=False, sort_keys=False)
      f.write(yaml_str)

    if self.test.debug:
      debug_dir = output_dir / f"{self.test.id}_debug"
      # Ensure the output directory exists
      os.makedirs(debug_dir, exist_ok=True)

      # Copy compile data to result
      shutil.copy(self.compinfo.elf, debug_dir)
      shutil.copy(self.compinfo.asm, debug_dir)
      shutil.copy(self.compinfo.lnscript, debug_dir)
