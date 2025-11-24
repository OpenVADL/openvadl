import argparse
from pathlib import Path
import asyncio
import yaml
import compiler

async def run_cosim(le: str, be: str, out: Path):
    proc = await asyncio.create_subprocess_exec(
        "vadl-cosim-broker",
        "--config", "/cosim_configs/ppc64_config.toml",
        "--test-exec", le,
        "--test-exec", be,
        "--output-file", str(out)
    )
    await proc.wait()

async def run_test(t: dict, results: Path):
    tid = str(t["id"])
    comp = await compiler.compile(tid, str(t["asm_core"]))
    await run_cosim(str(comp["elf_le"]), str(comp["elf_be"]), results / f"result-{tid}")

async def main(testsuite_path: Path):
    config = yaml.safe_load(testsuite_path.read_text())
    results = Path("/work/results")
    results.mkdir(parents=True, exist_ok=True)

    tasks = [run_test(t, results) for t in config.get("tests", [])]

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config")
    args = parser.parse_args()
    asyncio.run(main(Path(args.config)))