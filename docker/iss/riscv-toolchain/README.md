# RISC-V Toolchain Image

This dockerfile provides the instructions to build an ubuntu docker image with the [RISC-V
GNU toolchain]() installed.

The RISC-V GNU toolchain is pretty large and takes some time to build.
Thus is it very time-consuming to build a multi-platform image on one machine
because the non-native image will take forever.
Therefore the faster option is to build the image on two machine (on arm64 and one amd64) and
then merge the manifest once pushed.

On both machines execute the following

```bash
docker buildx build \
  -t <username>/riscv-toolchain \
  -f Dockerfile \
  --output push-by-digest=true,type=image,push=true \
  .
```

This will directly push the image to the dockerhub. However, you won't see the image yet.
Once this is done on both machines, you must look at the last output of the docker build, e.i.
the `exporting manifest list` line.
This looks like

```
 => exporting to image                                                                                      394.0s
 => => exporting layers                                                                                      61.2s
 => => exporting manifest sha256:1c5fb8ae970800a0944f69a275c54f86281c400216c570041f2a26e13568771d             0.0s
 => => exporting config sha256:9257698dd437fd2386ff582dc6a678af57c5b3eb599e4c2dc7e47271296636ec               0.0s
 => => exporting attestation manifest sha256:dd63a4f96b40689044bf5247372e551be1a63b700a299d4d1bb585a650fee93  0.0s
 => => exporting manifest list sha256:e7567a835255bbb69922fb32b1c193b985ce3dae1d516c3baf3b2c4e41586e10        0.0s  <----
 => => pushing layers                                                                                       330.7s
 => => pushing manifest for docker.io/jozott/riscv-toolchain                                                  2.1s
```

For the above output the hash of relevance is `sha256:e7567a835255bbb69922fb32b1c193b985ce3dae1d516c3baf3b2c4e41586e10`.
Copy both hashes and place paste them as argument to this command

```bash
docker buildx imagetools create \
    -t <username>/riscv-toolchain:<version-tag> \
    <username>/riscv-toolchain@<sha256-hash-machine-1> \
    <username>/riscv-toolchain@<sha256-hash-machine-2>
```

Now you can inspect the image build using

```bash
docker buildx imagetools inspect <username>/riscv-toolchain:<version-tag>
```

It is also available on the container repository now.

