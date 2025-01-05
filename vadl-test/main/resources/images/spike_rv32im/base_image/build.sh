#docker buildx build --platform linux/amd64,linux/arm64 -t kper1337/riscv32-spike-lcb-test:v3 -f Dockerfile . --push
docker buildx build --platform linux/amd64 -t kper1337/riscv32-spike-lcb-test:v6 -f Dockerfile . --push
