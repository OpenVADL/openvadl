import os
import socket
import time
import asyncio
import traceback
import argparse
from dataclasses import dataclass
from typing import List

import yaml

from test_case_executer_v1 import QMPTestCaseExecutor, TestSpec


@dataclass
class TestSuiteConfig:
  tests: List[TestSpec]

async def main(qemu_exec: str):
  print(f"Starting runner with qemu executable: {qemu_exec}")

  test_config = load_test_config("test-suite.yaml")

  # produces testcases
  # test_cases = [TestCaseExecutor2(spec) for (i, spec) in enumerate(test_config.tests)]
  test_cases = [QMPTestCaseExecutor(qemu_exec, spec) for (spec) in test_config.tests]

  start_time = time.time()

  # Get the number of cores available
  num_cores = os.cpu_count()
  # Create a semaphore to limit concurrent test cases
  semaphore = asyncio.Semaphore(num_cores)

  test_results = []

  # define how tests should be executed
  async def run_test(test_case):
    async with semaphore:
      test_start_time = time.time()
      try:
        await test_case.compile_and_link()
        await test_case.exec()
      except Exception as e:
        test_case.test_result.status = 'FAIL'
        test_case.test_result.errors.append(str(e))
        print(traceback.format_exc())
      finally:
        test_end_time = time.time()
        test_case.test_result.duration = f"{(test_end_time - test_start_time) * 1000:.2f}ms"
        status = test_case.test_result.status == 'PASS' and "✅ PASS" or "❌ FAIL"
        print(f"[{status}] Finish test case {test_case.spec.id} in {test_case.test_result.duration}")
        # await test_case.emit_result(dir="results", prefix="result-")
        result = test_case.get_test_result_map()
        test_results.append(result)

  # Create tasks with semaphore-controlled concurrency
  tasks = [asyncio.create_task(run_test(test_case)) for test_case in test_cases]

  # Wait for all tasks to complete
  await asyncio.gather(*tasks)

  result_file = f"results.yaml"
  with open(result_file, 'w') as f:
    yaml.dump(test_results, f)

  end_time = time.time()
  print(f"Total time: {end_time - start_time:.3f}s")


def load_test_config(filename: str) -> TestSuiteConfig:
  with open(filename, 'r') as file:
    data = yaml.safe_load(file)  # Load the YAML file
    print(f"HalliHallo {data}")
    tests = [TestSpec(**test) for test in data['tests']]  # Create TestSpec instances
    return TestSuiteConfig(tests=tests)  # Create TestSuiteConfig instance


if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.add_argument('simexec', type=str, help='Path to the qemu executable', default="qemu/build/qemu-system-vadl")
  args = parser.parse_args()
  asyncio.run(main(args.simexec))
