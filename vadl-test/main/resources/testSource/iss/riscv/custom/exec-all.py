import os
import subprocess
import sys
import signal

OUT_DIR = "out"
RESULTS_DIR = "results"
MAKEFILE = "Makefile"
QEMU_TIMEOUT = 1  # Timeout in seconds for QEMU execution

def reset_terminal():
    """Resets terminal state using stty."""
    try:
        subprocess.run(["stty", "sane"], check=True)
    except Exception as e:
        print(f"Warning: Failed to reset terminal state: {e}", file=sys.stderr)

def run_with_check_output(cmd, timeout):
    """Run a command using check_output with a timeout."""
    try:
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True, timeout=timeout)
        return 0, output  # Return 0 (success) and output
    except subprocess.TimeoutExpired:
        return None, "Command timed out."
    except subprocess.CalledProcessError as e:
        return e.returncode, e.output  # Return non-zero return code and captured output

def execute_tests():
    # Ensure results directory exists
    os.makedirs(RESULTS_DIR, exist_ok=True)

    # List all test files in the `tests` directory
    test_files = [f for f in os.listdir("tests") if f.endswith(".S")]
    
    for test in test_files:
        test_name = os.path.splitext(test)[0]
        result_file = os.path.join(RESULTS_DIR, f"{test_name}.result")
        try:
            # Build the test using `build-%`
            build_cmd = ["make", f"build-{test_name}"]
            build_returncode, build_output = run_with_check_output(build_cmd, timeout=10)
            
            # Check if build was successful
            if build_returncode != 0:
                with open(result_file, "w") as f:
                    f.write("FAILURE\n")
                    f.write("Build failed.\n")
                    f.write("Build output:\n")
                    f.write(build_output)
                continue
            
            # Run the test using `run-%` with a timeout
            run_cmd = ["make", f"run-{test_name}"]
            run_returncode, run_output = run_with_check_output(run_cmd, QEMU_TIMEOUT)

            # Write the result file
            with open(result_file, "w") as f:
                if run_returncode == 0:
                    f.write("SUCCESS\n")
                else:
                    f.write("FAILURE\n")
                    if run_returncode is None:
                        f.write(f"Run timed out after {QEMU_TIMEOUT} second.\n")
                    else:
                        f.write(f"Run failed with return code {run_returncode}.\n")
                
                f.write("Build output:\n")
                f.write(build_output)
                f.write("\nRun output:\n")
                f.write(run_output)
        except Exception as e:
            # Handle any unexpected errors during execution
            with open(result_file, "w") as f:
                f.write("FAILURE\n")
                f.write(f"Exception: {str(e)}\n")
        finally:
            # Always reset the terminal state after running QEMU
            reset_terminal()

if __name__ == "__main__":
    execute_tests()