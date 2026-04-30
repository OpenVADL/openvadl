import argparse
import asyncio
from concurrent.futures import ProcessPoolExecutor, as_completed
import os
import shutil
import subprocess
import sys
import time
import traceback

from config_loader import load_config
from test_case_plugin_executor import TestCasePluginExecutor


def reset_terminal():
    """Resets terminal state using stty if available."""
    if shutil.which("stty") is not None:
        try:
            subprocess.run(["stty", "sane"], check=True)
        except subprocess.CalledProcessError as e:
          pass


def report_progress(completed: int, total: int):
    width = 30
    filled = width if total == 0 else int(width * completed / total)
    bar = "#" * filled + "-" * (width - filled)
    print(f"[{bar}] {completed}/{total}", file=sys.stderr, flush=True)


def get_num_jobs():
    configured_jobs = os.environ.get("VADL_ISS_JOBS")
    if configured_jobs is not None:
        try:
            jobs = int(configured_jobs)
            if jobs > 0:
                return jobs
        except ValueError:
            pass

    num_cores = os.cpu_count()
    if num_cores is None:
        return 1
    return num_cores

async def run_test_case(test_case):
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
        # status = test_case.test_result.status == 'PASS' and "✅ PASS" or "❌ FAIL"
        # print(f"[{status}] Finish test case {test_case.test.id} in {test_case.test_result.duration}")
        await test_case.emit_result(dir="results", prefix="result-")


def run_test(test, test_config):
    test_case = TestCasePluginExecutor(test, test_config)
    asyncio.run(run_test_case(test_case))


def main(testsuite_path: str):
    test_config = load_config(testsuite_path)
    tests = test_config.tests
    total_tests = len(tests)

    if total_tests == 0:
        report_progress(0, 0)
        return

    start_time = time.time()
    num_jobs = get_num_jobs()

    with ProcessPoolExecutor(num_jobs) as executor:
        futures = [
            executor.submit(run_test, test, test_config)
            for test in tests
        ]

        report_progress(0, total_tests)
        completed = 0
        for future in as_completed(futures):
            future.result()
            completed += 1
            if completed % 100 == 0 or completed == total_tests:
                report_progress(completed, total_tests)

    end_time = time.time()
    print(f"Total time: {end_time - start_time:.3f}s")



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config", type=str)
    args = parser.parse_args()
    main(args.config)
