import argparse
from pathlib import Path
import subprocess
from concurrent.futures import ProcessPoolExecutor
import yaml
import os
import compiler
import shutil

def dump_debug_info(tid: str, results: Path, comp: dict):
    debug_dir = results / f"{tid}_debug"
    os.makedirs(debug_dir, exist_ok=True)
    shutil.copy(comp["asm"], debug_dir)
    shutil.copy(comp["lnscript"], debug_dir)
    shutil.copy(comp["elf"], debug_dir)
    shutil.copy(comp["objdump"], debug_dir)

def run_cosim(elf: str, out: Path, cosim_config: Path):
    e = os.environ.copy()
    e["RUST_BACKTRACE"] = "1"
    subprocess.run([
        "vadl-cosim-broker",
        "--config", cosim_config,
        "--test-exec", elf,
        "--output-file", str(out)
        ],
        env=e,
    )

def compile_test(t: dict, results: Path) -> dict:
    tid = str(t["id"])
    debug = t["debug"]
    comp = compiler.compile(tid, str(t["asm_core"]), debug)
    if debug:
        dump_debug_info(tid, results, comp)
    return comp

def run_test(t: dict, results: Path, cosim_config: Path):
    tid = t["id"]
    comp = compile_test(t, results)
    try:
        run_cosim(str(comp["elf"]), results / f"result-{tid}", cosim_config)
    except Exception as e:
        print(f"error for test=\"{tid}\": ", e)

def main(testsuite_path: Path):
    config = yaml.safe_load(testsuite_path.read_text())
    results = Path(config.get("result_dir", "/work/results"))
    results.mkdir(parents=True, exist_ok=True)

    num_cores = os.cpu_count()
    if num_cores is None:
        num_cores = 1 # safe fallback

    cosim_config = config.get("cosim_config", "/cosim_config/ppc64_config.toml")

    with ProcessPoolExecutor(num_cores) as executor:
        for t in config.get("tests", []):
            executor.submit(run_test, t, results, cosim_config)
    executor.shutdown(wait=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config")
    args = parser.parse_args()
    main(Path(args.config))
