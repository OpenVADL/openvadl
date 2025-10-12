FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update && \
    apt install -y autoconf automake autotools-dev curl python3 python3-pip libmpc-dev libmpfr-dev \
    libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev \
    ninja-build git cmake libglib2.0-dev libslirp-dev curl libsqlite3-dev

# install cargo via rustup.rs
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH=/root/.cargo/bin:$PATH

# build cosim
RUN git clone https://github.com/OpenVADL/openvadl.git
WORKDIR /work/openvadl/vadl-cosim
RUN cargo build --release -p vadl-cosim-broker
RUN mkdir /opt/cosim && cp ./target/release/vadl-cosim-broker /opt/cosim/

FROM ubuntu:22.04 AS cosim-broker
COPY --from=build /opt/cosim /opt/cosim
ENV PATH=/opt/cosim:$PATH

RUN apt update && \
    apt install -y libsqlite3-0

# testing broker
RUN vadl-cosim-broker --help
