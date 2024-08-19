import time
import asyncio
import traceback

from test_case import TestCase, TestSpec


async def main():
    test_specs = [TestSpec(
        id=str(i), reg_tests={'a0': 'Abcdef9876543210'},
        asm_core="""
        li a0, 0xABCDEF9876543210
        li a1, 0x10000000
        li a2, 0xFFFFFF0001000FFF
        """

    ) for i in range(1)]
    test_cases = [TestCase(spec, 1200 + i) for (i, spec) in enumerate(test_specs)]
    start_time = time.time()

    # Create a semaphore to limit concurrent test cases
    semaphore = asyncio.Semaphore(10)

    async def run_test(test_case):
        async with semaphore:
            print(f"Start test case {test_case.spec.id}...")
            start_time = time.time()
            try:
                await test_case.compile_and_link()
                await test_case.exec()
            except Exception as e:
                test_case.test_result.status = 'FAIL'
                test_case.test_result.errors.append(repr(e))
                print(traceback.format_exc())
            finally:
                end_time = time.time()
                test_case.test_result.duration = f"{(end_time - start_time) * 1000:.2f}ms"
                print(f"Finish test case {test_case.spec.id} in {test_case.test_result.duration}")
                await test_case.emit_result(dir="results", prefix="result-")

    # Create tasks with semaphore-controlled concurrency
    tasks = [asyncio.create_task(run_test(test_case)) for test_case in test_cases]

    # Wait for all tasks to complete
    await asyncio.gather(*tasks)

    end_time = time.time()
    print(f"Total time: {end_time - start_time:.3f}s")


if __name__ == "__main__":
    asyncio.run(main())
