# Automatic run

```bash
docker build -t embench . --build-arg num_jobs=3 # --progress=plain
./build_vadl_sims.sh
./run_in_docker.sh embench "./benchmark-extras/run-benchmarks-aarch64.sh"
./run_in_docker.sh embench "./benchmark-extras/run-benchmarks-rv32.sh"
```

You can easily run scripts in a Docker container using for example `./run_in_docker.sh embench ./build_aarch64.sh vadl`

# Building

Several scripts are provided to build for different simulation platforms:

* `build_aarch64.sh`: Builds AArch64 binaries (armv8-a softfp) for either *vadl*, UME (*gem5*) or ARM semihosting (*qemu*).
* `build_rv32.sh`: Builds static riscv binaries. Can be used with *qemu* and *gem5*.
* `build_rv32im.sh`: Builds static ricsv binaries limited to the base instruction set and M-extension. Can be used with *qemu* and *gem5*.
* `build_spike.sh`: Builds riscv binaries executable by *spike*.
* `build_vadl.sh`: Builds riscv binaries executable by *vadl*.

All scripts assume that you hava a RV32 toolchain installed on your system. All parameters are passed on the `build_all.py` script.

# Benchmarking

The following scripts are provided to simplify benchmark simulation:

* `benchmark_gem5.sh`: Benchmark gem5. The script expects two arguments where the first points to an gem5 executable and the second to the gem5 configuration script. NOTE: The path to the script must be absolute.
* `benchmark_qemu.sh`: Benchmark qemu. The script expects the base qemu invocation as arguments, so ``
* `benchmark_spike.sh`: Benchmark spike limited to the RV32I spec.
* `benchmark_vadl.sh`: Benchmark VADL. The script expects an argument pointing to a VADL CAS.
