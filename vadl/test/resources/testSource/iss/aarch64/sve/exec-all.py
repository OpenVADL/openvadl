import subprocess
from pathlib import Path

OUT_DIR = "out"
RESULTS_DIR = Path("results")
QEMU_TIMEOUT = 1

def run(cmd, timeout=None):
    try:
        p = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=timeout,
        )
        return p.returncode, p.stdout
    except subprocess.TimeoutExpired as e:
        return None, e.stdout or "Command timed out.\n"

def execute_tests():
    RESULTS_DIR.mkdir(exist_ok=True)

    for test in Path("tests").glob("*.S"):
        name = test.stem
        result_file = RESULTS_DIR / f"{name}.result"

        build_rc, build_out = run(["make", f"build-{name}"], timeout=10)
        if build_rc != 0:
            result_file.write_text(
                f"FAILURE\nBuild failed.\n{build_out}"
            )
            continue

        run_rc, run_out = run(["make", f"run-{name}"], timeout=QEMU_TIMEOUT)

        status = "SUCCESS" if run_rc == 0 else "FAILURE"
        extra = (
            ""
            if run_rc == 0
            else "Run timed out.\n" if run_rc is None
            else f"Run failed (rc={run_rc}).\n"
        )

        result_file.write_text(
            f"{status}\n{extra}\n"
            f"Build output:\n{build_out}\n"
            f"Run output:\n{run_out}"
        )

        subprocess.run(["stty", "sane"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

if __name__ == "__main__":
    execute_tests()