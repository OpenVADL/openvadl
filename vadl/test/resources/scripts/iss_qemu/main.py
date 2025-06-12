import argparse
import asyncio
import os
import shutil
import time
import traceback
import yaml
import sys
import subprocess
from dataclasses import dataclass
from typing import List

from config_loader import load_config
from test_case_plugin_executor import TestCasePluginExecutor


def reset_terminal():
    """Resets terminal state using stty if available."""
    if shutil.which("stty") is not None:
        try:
            subprocess.run(["stty", "sane"], check=True)
        except subprocess.CalledProcessError as e:
          pass

async def main(testsuite_path: argparse.FileType):
    test_config = load_config(testsuite_path)


    # produces testcases
    # test_cases = [TestCaseExecutor2(spec) for (i, spec) in enumerate(test_config.tests)]
    # test_cases = [QMPTestCaseExecutor(qemu_exec, spec) for spec in test_config.tests]
    test_cases = [TestCasePluginExecutor(test, test_config) for test in test_config.tests]

    start_time = time.time()

    # Get the number of cores available
    num_cores = os.cpu_count()
    # Create a semaphore to limit concurrent test cases
    semaphore = asyncio.Semaphore(num_cores)


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
                reset_terminal()
                test_end_time = time.time()
                test_case.test_result.duration = f"{(test_end_time - test_start_time) * 1000:.2f}ms"
                status = test_case.test_result.status == 'PASS' and "✅ PASS" or "❌ FAIL"
                print(f"[{status}] Finish test case {test_case.test.id} in {test_case.test_result.duration}")
                await test_case.emit_result(dir="results", prefix="result-")

    # Create tasks with semaphore-controlled concurrency
    tasks = [asyncio.create_task(run_test(test_case)) for test_case in test_cases]

    # Wait for all tasks to complete
    await asyncio.gather(*tasks)

    end_time = time.time()
    print(f"Total time: {end_time - start_time:.3f}s")



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config", type=str)
    args = parser.parse_args()
    asyncio.run(main(args.config))
