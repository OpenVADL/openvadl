# RISC-V Toolchain Image

This dockerfile provides the instructions to build an ubuntu docker image with the [RISC-V
GNU toolchain]() installed.

This is trivial for `linux/amd64` targets, as there already exists nightly builds for
`amd64`.
However to build a native image for `arm64` we have to build the toolchain our-self.
The dockerfile is multi-staged and produces the correct platform image depending on
the platform provided by the `--platform` option when calling `docker build`.
By default the host platform is chosen.

To build an push the image run

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t open-vadl/riscv-toolchain:latest -f RiscvToolchain.Dockerfile --push .
```

This will take up to two hours. You want to ensure a stable
internet connection.