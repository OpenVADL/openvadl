FROM jozott/riscv64-toolchain 

# install all dependencies
RUN apt update && \
    apt install -y git build-essential libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev ninja-build python3 python3-venv python3-pip

WORKDIR /qemu

# clone qemu 9.0.2
RUN git clone --depth 1 --branch v9.0.2 https://github.com/qemu/qemu.git .

# build riscv64-toolchain
RUN mkdir build && cd build && \
    ../configure --target-list=riscv64-softmmu && \
    make && make install

WORKDIR /scripts

# create python venv and
# install qemu.qmp pip dependency for tests
RUN python3 -m venv .venv
RUN . .venv/bin/activate && pip install qemu.qmp

WORKDIR /work
