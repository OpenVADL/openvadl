# OpenVADL QEMU Image

This image contains the sources and prebuild of QEMU used by the
OpenVADL Instruction Set Simulator (ISS).
It is used to run tests of the generated QEMU frontend and because of
the QEMU prebuild, the compile time of the generated QEMU target
halfs. This allows more efficient testing, especially in the CI.

To build an push the image run

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t open-vadl/qemu:latest -f RiscvToolchain.Dockerfile --push .
```

This will quite some time. You want to ensure a stable
internet connection.