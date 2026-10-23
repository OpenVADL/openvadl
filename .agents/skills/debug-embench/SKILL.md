---
name: debug-embench
description: Reproduce and investigate OpenVADL Embench compiler regressions using retained Docker images, generated assembly, and upstream compiler comparisons.
---

# Debug Embench regressions

Work from the OpenVADL repository root. Benchmark sources and scripts are in
`vadl/test/resources/embench`; JUnit drivers are in
`vadl/test/vadl/lcb/riscv/riscv32` and `riscv64`.

## Reproduce

Inspect the requested driver's optimization method and script. Disabled cases
may be removed with `rm -r ../src/<benchmark>` between the upstream and LCB runs.
Enable the requested case in the resource script before generating the image.

For RV32 O3:

```sh
VADL_KEEP_TEST_IMAGES=true ./gradlew :vadl:test \
  --tests vadl.lcb.riscv.riscv32.EmbenchSpikeRiscv32SimulationTest.runO3 \
  --console=plain
```

The environment flag skips `RemovingDockerImagesTeardown` image removal. It
does not preserve the Testcontainers execution container. Gradle captures most
test logging: inspect `vadl/build/test-results/test/TEST-*.xml` after completion
and `vadl/build/test-results/test/binary/output-events.bin` with `strings` while
running. The first compiler image build can take several minutes.

`LcbDockerExecutionTest` generates the backend into a temporary `lcb` directory,
copies Embench there, and builds the Dockerfile from
`vadl/test/resources/images/lcb_execution_test_rv32im/Dockerfile`.
`DockerRiscvImageProvider` names the resulting image `tc_spike_riscv_rv32im`.
Read the corresponding methods for other targets rather than guessing names.

## Keep generated files

The image contains the compiled compiler, but benchmarks are built when its
execution container runs. Start a named container with an overridden command:

```sh
docker run -d --name embench-debug --entrypoint sh tc_spike_riscv_rv32im \
  -c 'sleep infinity'
docker exec -it embench-debug bash
```

Inside, work in `/src/embench`. Run the selected `build_spike-*.sh` script with
`--builddir` to give upstream and LCB builds different directories; their default
`bd` would overwrite the comparison. Inspect `build_all.py --help` for filtering
and compiler timeout arguments. Build logs live in `logs` by default.
The RV32 `lcbw32/lcb-wrapper` first emits `<source-basename>.s` with
`/src/llvm-final/build/bin/clang --target=rv32im`, then assembles it with that
Clang's upstream RISC-V target. Assembly is already retained in the benchmark's
build directory; locate it with `find <builddir> -name '*.s'`.

Check `__run_spike.sh` before assuming the simulator: the current RV32 script
uses `qemu-system-riscv32 -machine spike`, despite the JUnit test's name.
Run individual binaries through that script with an external timeout when
investigating hangs. `support/main.c` returns 1 when `verify_benchmark` fails.

## Narrow the cause

First distinguish compiler/assembler failure, wrong result, simulator crash,
and timeout using the build logs and direct binary exit status. A benchmark
summary alone can hide the underlying failure.

Compare upstream and LCB at identical optimization settings, headers, libraries,
and board startup code. Preserve the failing assembly and exact compile command
before rebuilding. For a runtime failure, inspect calls into libgcc/libm as well
as generated arithmetic: RV32 soft-float and wide values can expose calling
convention mismatches. Compare caller argument/result placement with the actual
linked callee disassembly. Reduce a suspect call to a small C reproducer and
vary one factor at a time (optimization, types, compiler).

Report the first demonstrated divergence, the supporting instruction sequence
or diagnostic, and any remaining uncertainty. A workaround passing is evidence
for localization, not proof of a compiler fix. Keep backend changes separate
from a requested diagnosis unless fixing the defect is authorized.

Copy useful logs/assembly out with `docker cp` before removing the named
container. Retained images consume disk; remove only debugging resources you
created when they are no longer needed. Do not prune unrelated images.

## Confirmed RV32 cubic example

For a worked diagnosis of a quad-precision helper ABI mismatch and a tested
minimal reproducer, read [the cubic investigation](references/cubic-rv32-o3.md).
Use it when investigating wide soft-float calls; do not assume every Embench
failure has this cause.
