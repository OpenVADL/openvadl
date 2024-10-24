FROM kper1337/llvm17_base:latest
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV SCCACHE_REDIS_ENDPOINT=${SCCACHE_REDIS_ENDPOINT}
COPY . /src
WORKDIR /src
RUN mv /llvm llvm-final
RUN make
RUN mkdir /output
CMD /src/llvm-final/build/bin/clang --target=${TARGET} -S -c ./input.c -o /output/input.s