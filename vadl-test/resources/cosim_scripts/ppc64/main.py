import argparse
from pathlib import Path
import asyncio
import yaml
import os
import sys
import compiler
import shutil

async def run_cosim(le: str, be: str, out: Path, cosim_config: Path):
    e = os.environ.copy()
    e["RUST_BACKTRACE"] = "1"
    proc = await asyncio.create_subprocess_exec(
        "vadl-cosim-broker",
        "--config", cosim_config,
        "--test-exec", le,
        "--test-exec", be,
        "--output-file", str(out),
        env=e
    )
    await proc.wait()

async def run_test(t: dict, results: Path, cosim_config: Path):
    tid = str(t["id"])
    try:
        comp = await compiler.compile(tid, str(t["asm_core"]))

        if t["debug"]:
            debug_dir = results / f"{tid}_debug"
            os.makedirs(debug_dir, exist_ok=True)
            shutil.copy(comp["asm"], debug_dir)
            shutil.copy(comp["lnscript"], debug_dir)
            shutil.copy(comp["elf_le"], debug_dir)
            shutil.copy(comp["elf_be"], debug_dir)

        await run_cosim(str(comp["elf_le"]), str(comp["elf_be"]), results / f"result-{tid}", cosim_config)
    except Exception as e:
        print(f"error for test=\"{tid}\": ", e)

async def main(testsuite_path: Path):
    config = yaml.safe_load(testsuite_path.read_text())
    results = Path(config.get("result_dir", "/work/results"))
    results.mkdir(parents=True, exist_ok=True)

    cosim_config = config.get("cosim_config", "/cosim_config/ppc64_config.toml")
    #tasks = [run_test(t, results) for t in config.get("tests", [])]
    tasks = []; [await run_test(t, results, cosim_config) for t in config.get("tests", [])]

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config")
    args = parser.parse_args()
    asyncio.run(main(Path(args.config)))