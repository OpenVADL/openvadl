FROM kper1337/llvm17_base:v1
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV SCCACHE_REDIS_ENDPOINT=${SCCACHE_REDIS_ENDPOINT}
COPY . /src
WORKDIR /src
RUN mv /opt/sccache/sccache /usr/bin/sccache
RUN make && sccache -s
RUN mkdir /output
CMD /src/llvm-final/build/bin/clang --target=${TARGET} -S -c /src/inputs/$INPUT -o /output/$INPUT.s && diff -y --color --ignore-all-space /output/$INPUT.s /assertions/$INPUT.s | cat -n | grep -v -e '($'
