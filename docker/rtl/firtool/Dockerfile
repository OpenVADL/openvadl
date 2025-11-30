FROM ubuntu:22.04 AS build

WORKDIR /build

RUN apt update &&  \
    apt install -y \
    curl \
    git \
    build-essential  \
    ninja-build  \
    python3  \
    python3-venv  \
    python3-pip \
    cmake

# Clone the repository and its submodules
RUN git clone https://github.com/llvm/circt.git --recursive

WORKDIR /build/circt

# Configure the build
RUN cmake -G Ninja llvm/llvm -B build \
    -DCMAKE_BUILD_TYPE=RelWithDebInfo \
    -DLLVM_ENABLE_ASSERTIONS=ON \
    -DLLVM_TARGETS_TO_BUILD=host \
    -DLLVM_ENABLE_PROJECTS=mlir \
    -DLLVM_EXTERNAL_PROJECTS=circt \
    -DLLVM_EXTERNAL_CIRCT_SOURCE_DIR=$PWD

# Run the build
RUN ninja -C build firtool

# Final image is just the binaries without the OS
FROM scratch AS firtool

COPY --from=build /build/circt/build/bin /opt/circt

