# LLVM Image

This dockerfile provides the images to compile LLVM in a container. It contains the necessary build tools, caching 
tools and finally it clones the git repository at build time.

To build an push the image run

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t open-vadl/llvm17_base -f Dockerfile --push .
```

This will take a couple minutes. You want to ensure a stable
internet connection.