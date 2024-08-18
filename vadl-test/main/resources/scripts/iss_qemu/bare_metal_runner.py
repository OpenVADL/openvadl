import time
import asyncio
from test_case import TestCase


async def main():
    test_cases = [TestCase(i, "hello64") for i in range(500)]
    start_time = time.time()

    # Create a semaphore to limit concurrent test cases
    semaphore = asyncio.Semaphore(10)

    async def run_test(test_case):
        async with semaphore:
            print(f"Start {test_case.test_bin} for test case {test_case.id}...")
            await test_case.run()

    # Create tasks with semaphore-controlled concurrency
    tasks = [asyncio.create_task(run_test(test_case)) for test_case in test_cases]

    # Wait for all tasks to complete
    await asyncio.gather(*tasks)

    end_time = time.time()
    print(f"Total time: {end_time - start_time:.3f}s")


if __name__ == "__main__":
    asyncio.run(main())
