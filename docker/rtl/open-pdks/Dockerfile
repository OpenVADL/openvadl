FROM ubuntu:22.04 AS build

ENV DEBIAN_FRONTEND=noninteractive

# Install some dependencies
RUN apt update &&  \
    apt install -y \
    git \
    autoconf \
    build-essential \
    python3 \
    tk8.6-dev

# Build magic from source
WORKDIR /opt
RUN git clone https://github.com/RTimothyEdwards/magic.git
WORKDIR /opt/magic
RUN ./configure
RUN make
RUN make install

# Build Skywater 130 PDK
WORKDIR /opt
RUN git clone https://github.com/RTimothyEdwards/open_pdks.git
WORKDIR /opt/open_pdks
RUN ./configure --enable-sky130-pdk --with-sky130-variants=A --prefix=/opt/pdk
RUN make
RUN make install

