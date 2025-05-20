# OpenVADL QEMU Image

This image contains the sources and prebuild of QEMU with Aarch64.

To build a push the image run

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t open-vadl/qemu-aarch64:latest -f Dockerfile --push .
```

This will quite some time. You want to ensure a stable
internet connection.