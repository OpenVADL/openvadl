ifndef TAG
TAG=$(shell git rev-parse HEAD)
endif

DOCKER_REPOSITORY=c2.complang.tuwien.ac.at:5000
IMAGE=embench-ci:$(TAG)

default: build

build:
	docker build --build-arg ACCESS_TOKEN=$(ACCESS_TOKEN) -t $(IMAGE) -f Dockerfile .

publish:
	docker image tag $(IMAGE) $(DOCKER_REPOSITORY)/$(IMAGE)
	docker push $(DOCKER_REPOSITORY)/$(IMAGE)
