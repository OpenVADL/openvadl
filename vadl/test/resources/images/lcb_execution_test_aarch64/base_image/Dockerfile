FROM ghcr.io/openvadl/lcb-execution-test-rv64im-base-image@sha256:f1f2a3560465321a19db8ad0dcc19c48723651e349c8b33aa06c157b40560b88

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:da90881797b77a7d4b626d89a7c26d8a3baab9ae0bd5660fbf2544a255ce1e03
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libslirp0 time
COPY --from=base /opt/aarch64 /opt/aarch64
COPY --from=base /opt/qemu /opt/qemu
COPY --from=base /usr/local/bin/ /usr/local/bin
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
