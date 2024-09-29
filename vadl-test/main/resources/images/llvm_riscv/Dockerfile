FROM kper1337/llvm17_base:latest
ARG BUCKET
ARG REGION
ARG KEY_ID
ARG ACCESS_KEY
ARG TARGET
WORKDIR /llvm/build
ENV SCCACHE_BUCKET=${BUCKET}
ENV SCCACHE_REGION=${REGION}
ENV AWS_ACCESS_KEY_ID=${KEY_ID}
ENV AWS_SECRET_ACCESS_KEY=${ACCESS_KEY}
COPY . /src
WORKDIR /src
RUN mv /llvm llvm-final
RUN make build
CMD /src/llvm-final/build/bin/clang --target=${TARGET} -S -c ./input.c -o /output/input.s
