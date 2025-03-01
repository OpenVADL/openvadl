#

TARGET=vadl

MAVEN=mvn
GRADLE=GRADLE_OPTS=-Xmx4096m ./.gradlew
GRADLE_CACHED=$(GRADLE) --build-cache --no-daemon
GRADLE_DAEMON=$(GRADLE) --parallel --daemon

DOXYGEN=doxygen

OS = $(shell uname -s)

ifeq ($(OS), Linux)
 PROFILE_CMD=perf stat
else
 PROFILE_CMD=
endif

default: build

.PHONY: build doxy


all: clean build


clean:
	$(GRADLE_CACHED) clean

gradle-clean: clean

maven-clean:
	$(MAVEN) clean -o


PATCH = pkg/at.ac.tuwien.complang.vadl/src/at/ac/tuwien/complang/vadl/utils/Version.xtend

patch: $(PATCH)

%.xtend: metadata %.xtend.template
	@cat $(filter %.xtend.template,$^) \
	| sed "s/{{version-name}}/`  cat obj/name | tr -d '\n' | tr -d '\r\n'`/g" \
	| sed "s/{{version-commit}}/`cat obj/ref  | tr -d '\n' | tr -d '\r\n'`/g" \
	| sed "s/{{version-tag}}/`   cat obj/tag  | tr -d '\n' | tr -d '\r\n'`/g" \
	> $@

build: patch
	$(GRADLE_DAEMON) fatJar
	$(MAKE) deploy

deploy:
	mkdir -p obj/bin
	mkdir -p obj/lib
	cp -f app/vadl obj/bin/vadl
	cp -f pkg/at.ac.tuwien.complang.vadl.cli/build/libs/at.ac.tuwien.complang.vadl.cli-1.0.0-SNAPSHOT.jar obj/lib/vadl.jar


gradle-build: patch
	$(GRADLE_CACHED) fatJar
	$(MAKE) deploy

# gradle-build
bundle: deploy
	(cd obj; zip -r vadl.zip release ./bin/vadl ./lib/vadl.jar)

test:
	$(GRADLE_DAEMON) test

gradle-test:
	$(GRADLE_CACHED) test -i


documentation: default docs


info:
	$(GRADLE_CACHED) --version
	git status
	git submodule


update:
	$(MAVEN) dependency:go-offline


CLEAN = build/*,bin/*,bin/.*.jar,src-gen/*,test-bin/*,xtend-gen/*,xtend-gen/.*.xtendbin,model/*,target/*

# This command cleans the java/xtend generated build resources. This is sometimes required by
# xtend as it is for example unable to remove missing file dependencies on its own.
clean-java:
	rm -rf .gradle
	rm -rf pkg/at.ac.tuwien.complang.vadl/{${CLEAN}}
	rm -rf pkg/at.ac.tuwien.complang.vadl.tests/{${CLEAN}}
	rm -rf pkg/at.ac.tuwien.complang.vadl.cli/{${CLEAN}}

clean-all: clean-java
	rm -rf obj


mom:
	git checkout -b mom/`date --iso`

ci:
	docker run -it --rm --name docker-mvn \
	-v "$(PWD):/usr/src/$(TARGET)" \
	-w /usr/src/$(TARGET) \
	ppaulweber/docker-mvn:refs-tags-v1.0.0 \
	make info && \
	make gradle-build


delivery:
	git tag $@-`date -I`


archive: documentation
	rm -f obj/release*
	rm -f obj/vadl*
	mkdir -p obj
	echo "`date -I`_`git describe --always --dirty`" > obj/release
	echo "vadl_`cat obj/release`.zip" > obj/release_zip
	echo "vadl_`cat obj/release`-src.zip" > obj/release_src_zip
	echo "vadl_`cat obj/release`-pdf.zip" > obj/release_pdf_zip
	echo "vadl_`cat obj/release`-docs.zip" > obj/release_docs_zip
	echo "vadl_`cat obj/release`-docs-impl.zip" > obj/release_docs_impl_zip
	git archive --format=zip HEAD > obj/`cat obj/release_src_zip`
	@(cd obj/doc/refman; zip -r ../../`cat ../../release_pdf_zip` vadl.pdf)
	@(cd obj/doc/refman; zip -r ../../`cat ../../release_docs_zip` html)
	@(cd obj/doc/impl; zip -r ../../`cat ../../release_docs_impl_zip` html)


encrypt:
	@(cd obj; zip -P `cat /drone/src/.attic/zipcode` -r `cat release_zip` vadl_*.zip llvm_*.zip risc-v_*.zip dsp-a_*.zip mips_*.zip aarch64_*.zip aarch32_*.zip)


decrypt:
	@(cd obj; unzip -P `cat /drone/src/.attic/zipcode` `cat release_zip`)



GITEA_API=https://${ACCESS_TOKEN}:@ea.complang.tuwien.ac.at/api/v1
GITEA_REPO=$(GITEA_API)/repos/vadl/vadl


metadata:
	@mkdir -p obj
	@echo "`date -I`_`git describe --always --dirty`" > obj/release
	@git describe --always --dirty --tags > obj/tag
	@git describe --always --dirty > obj/ref

	@cat RELEASE-NOTES.md | grep "## Delivery" | sed "s/## //g" | head -n1 > obj/name
	@cat obj/name | sed "s/ /-/g" | sed "s/\./-/g" | sed "s/(//g" | sed "s/)//g" | tr '[:upper:]' '[:lower:]' > obj/name_ref
	@echo "https://ea.complang.tuwien.ac.at/vadl/vadl/src/branch/master/RELEASE-NOTES.md\#`cat obj/name_ref`" > obj/link


nightly: bundle
	@curl -X GET "$(GITEA_REPO)/releases" | yq e -P > obj/releases

	@yq e '.[] | select(.name == "nightly").id' obj/releases > obj/release_nightly

	@for i in `yq e '.[] | select(.name == "nightly").assets[].id' obj/releases`; do \
		echo $$i; \
		curl -X DELETE "$(GITEA_REPO)/releases/`cat obj/release_nightly`/assets/$$i" | yq e -P; \
	done

	@curl \
	-X POST "$(GITEA_REPO)/releases/`cat obj/release_nightly`/assets?name=vadl.zip" \
	-H "accept: application/json" \
	-H "Content-Type: multipart/form-data" \
	-F "attachment=@obj/vadl.zip;type=application/zip"

	@curl -X GET "$(GITEA_REPO)/releases" | yq e -P > obj/releases

	@yq e -P '.[] | select(.name == "nightly")' obj/releases


release: archive metadata
	@echo "-- Releasing '`cat obj/tag`'"
	@make release-create-`cat obj/tag`

release-create-%:
	$(eval RELEASE_TAG := $(patsubst release-create-%,%,$@))
	@echo "-- Release  Tag: '$(RELEASE_TAG)'"

	$(eval RELEASE_NAME := $(shell cat obj/name))
	@echo "-- Release Name: '$(RELEASE_NAME)'"

	$(eval RELEASE_LINK := $(shell cat obj/link))
	@echo "-- Release Link: '$(RELEASE_LINK)'"

	@curl \
	-d '{ "tag_name":"$(RELEASE_TAG)", "name":"$(RELEASE_NAME)", "body":"[Release Notes]($(RELEASE_LINK))" }' \
	-H "Content-Type: application/json" \
	-X POST "$(GITEA_REPO)/releases" | yq e '.id' > obj/id

	@make release-assets-`cat obj/id`

release-assets-%:
	$(eval RELEASE_ID := $(patsubst release-assets-%,%,$@))
	@echo "-- Release   Id: '$(RELEASE_ID)'"

	@curl -X GET "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets" | yq e -P '.' > obj/assets
	@echo "-- Release Blob: '`cat obj/assets`'"

	@curl \
	-X POST "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets?name=`cat obj/release_zip`" \
	-H "accept: application/json" \
	-H "Content-Type: multipart/form-data" \
	-F "attachment=@obj/`cat obj/release_zip`;type=application/zip"

	@curl -X GET "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets" | yq e -P '.' > obj/assets
	@echo "-- Release Blob: '`cat obj/assets`'"

	@curl \
	-X POST "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets?name=`cat obj/release_src_zip`" \
	-H "accept: application/json" \
	-H "Content-Type: multipart/form-data" \
	-F "attachment=@obj/`cat obj/release_src_zip`;type=application/zip"

	@curl -X GET "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets" | yq e -P '.' > obj/assets
	@echo "-- Release Blob: '`cat obj/assets`'"

	@curl \
	-X POST "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets?name=`cat obj/release_docs_zip`" \
	-H "accept: application/json" \
	-H "Content-Type: multipart/form-data" \
	-F "attachment=@obj/`cat obj/release_docs_zip`;type=application/zip"

	@curl -X GET "$(GITEA_REPO)/releases/$(RELEASE_ID)/assets" | yq e -P '.' > obj/assets
	@echo "-- Release Blob: '`cat obj/assets`'"


$(DOXYGEN):
	@echo "-- Installation Doxygen '$(DOXYGEN)'"
	@curl \
	-L https://$(ACCESS_TOKEN):@ea.complang.tuwien.ac.at/attachments/57bdf0f0-1d35-43b2-8c1c-beed47038915 \
	-o $(DOXYGEN)
	@chmod 775 $(DOXYGEN)
	@(cat obj/bin/doxygen | head -n1 | grep -v html \
		&& echo "-- Installation Doxygen '$(DOXYGEN)' [DONE]" \
		|| (rm -f $(DOXYGEN); \
			echo "-- ERROR: unable to access 'doxygen' artifact"; \
			) \
	)


docs: latex html


doxygen: $(DOXYGEN) metadata
	@mkdir -p obj/doc

OPEN_VADL_DOC_INPUT_IMPL="\
./open-vadl/vadl \
./open-vadl/vadl-cli \
./open-vadl/java-annotations \
"

OPEN_VADL_DOC_INPUT_REFMAN="\
./open-vadl/doc/introduction.md \
./open-vadl/doc/authors.md \
./open-vadl/doc/tutorial.md \
./open-vadl/doc/reference.md \
"

DOC_INPUT_IMPL="\
./pkg/at.ac.tuwien.complang.vadl/src/at/ac/tuwien/complang/vadl/ \
./pkg/at.ac.tuwien.complang.vadl/src-gen/at/ac/tuwien/complang/vadl \
./pkg/at.ac.tuwien.complang.vadl/xtend-gen/at/ac/tuwien/complang/vadl \
"

DOC_INPUT_REFMAN="\
./doc/introduction.md \
./doc/design_rationale.md \
./doc/getting_started.md \
./doc/code_generation.md \
./doc/language_reference.md \
./pkg/at.ac.tuwien.complang.vadl/src/at/ac/tuwien/complang/vadl/VADL.xtext \
./RELEASE-NOTES.md \
./doc/acronyms.md \
./doc/authors.md \
"

html: html-impl html-refman open-vadl-html-impl open-vadl-html-refman

open-vadl-html-impl: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="(`(cd open-vadl; git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-impl" \
	INPUT=$(OPEN_VADL_DOC_INPUT_IMPL) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation impl [DONE]"

open-vadl-html-refman: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="(`(cd open-vadl; git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT=$(OPEN_VADL_DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation refman [DONE]"


html-impl: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="`cat obj/tag` (`cat obj/ref`)" \
	PROJECT_BRIEF="`cat obj/name`" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/impl" \
	INPUT=$(DOC_INPUT_IMPL) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation impl [DONE]"

html-refman: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="`cat obj/tag` (`cat obj/ref`)" \
	PROJECT_BRIEF="`cat obj/name`" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/refman" \
	INPUT=$(DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation refman [DONE]"

open-vadl-latex: doxygen
	@echo "-- Generating LaTeX Documentation"
	PROJECT_NUMBER="(`(cd open-vadl; git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT=$(OPEN_VADL_DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.latex.doxygen
	bash ./tools/sed-latex "obj/doc/open-vadl-refman/latex"
	@(cd obj/doc/open-vadl-refman/latex; make)
	@cp obj/doc/open-vadl-refman/latex/refman.pdf obj/doc/open-vadl-refman/open-vadl.pdf
	@echo "-- Generating LaTeX Documentation [DONE]"

latex: doxygen
	@echo "-- Generating LaTeX Documentation"
	PROJECT_NUMBER="`cat obj/tag`" \
	PROJECT_BRIEF="`cat obj/name`" \
	PROJECT_LOGO="`cat obj/ref`" \
	OUTPUT_DIRECTORY="./obj/doc/refman" \
	INPUT=$(DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.latex.doxygen
	bash ./tools/sed-latex "obj/doc/refman/latex"
	@(cd obj/doc/refman/latex; make)
	@cp obj/doc/refman/latex/refman.pdf obj/doc/refman/vadl.pdf
	@echo "-- Generating LaTeX Documentation [DONE]"


BIN=./obj/bin/vadl
CLI=$(BIN) -d -p -o obj --vir-emit-uses --vir-dump --vir-invariant-assertion

EXAMPLES  = toy
EXAMPLES += oisc8
EXAMPLES += oisc8_p1
EXAMPLES += oisc8_p2
EXAMPLES += risc8_mmu
EXAMPLES += risc8
EXAMPLES += risc8_p1
EXAMPLES += risc8_p1_cacheL1L2_addrTrans
EXAMPLES += risc8_p1_cacheL1wb
EXAMPLES += risc8_p1_wb
EXAMPLES += risc8_p2
EXAMPLES += risc8_p3
EXAMPLES += risc8_pipeline
EXAMPLES += risc8a
EXAMPLES += risc8a_p1
EXAMPLES += risc8a_p1_cacheL1L2_addrTrans
EXAMPLES += risc8a_p1_cacheL1wb
EXAMPLES += risc8a_p1_wb
EXAMPLES += risc8a_p3
EXAMPLES += risc16
EXAMPLES += risc32
EXAMPLES += TriLen
EXAMPLES += TriLen_p1
EXAMPLES += risc8_is_synthesis
EXAMPLES += risc8_is_synthesis_p1
EXAMPLES += vliw_toy

# $(EXAMPLES:%=example-%):
#	make hdl-example-$(patsubst example-%,%,$@)

#
#
# HDL
#

$(EXAMPLES:%=example-%-hdl):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-hdl-gen):
	$(CLI) --hdl sys/examples/src/$(patsubst example-%-hdl-gen,%,$@).vadl

$(EXAMPLES:%=example-%-hdl-dbg):
	(cd obj/$(patsubst example-%-hdl-dbg,%,$@)/CPU/hdl; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-hdl-dbg,%,$@)/CPU/hdl/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-hdl-rel):
	(cd obj/$(patsubst example-%-hdl-rel,%,$@)/CPU/hdl; make release FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-hdl-rel,%,$@)/CPU/hdl/obj/rel/CPU

#
#
# CAS
#

$(EXAMPLES:%=example-%-cas):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-cas-gen):
	$(CLI) --cas sys/examples/src/$(patsubst example-%-cas-gen,%,$@).vadl

$(EXAMPLES:%=example-%-cas-dbg):
	(cd obj/$(patsubst example-%-cas-dbg,%,$@)/CPU/cas; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-cas-dbg,%,$@)/CPU/cas/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-cas-rel):
	(cd obj/$(patsubst example-%-cas-rel,%,$@)/CPU/cas; make release FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-cas-rel,%,$@)/CPU/cas/obj/rel/CPU

#
#
# ISS
#

$(EXAMPLES:%=example-%-iss):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-iss-gen):
	$(CLI) --iss sys/examples/src/$(patsubst example-%-iss-gen,%,$@).vadl

$(EXAMPLES:%=example-%-iss-dbg):
	(cd obj/$(patsubst example-%-iss-dbg,%,$@)/CPU/iss; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-iss-dbg,%,$@)/CPU/iss/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-iss-rel):
	(cd obj/$(patsubst example-%-iss-rel,%,$@)/CPU/iss; make release FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-iss-rel,%,$@)/CPU/iss/obj/rel/CPU

#
#
# ISS UME
#

$(EXAMPLES:%=example-%-ume):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-ume-gen):
	$(CLI) --iss sys/examples/src/$(patsubst example-%-ume-gen,%,$@).vadl

$(EXAMPLES:%=example-%-ume-dbg):
	(cd obj/$(patsubst example-%-ume-dbg,%,$@)/CPU/iss/ume; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-ume-dbg,%,$@)/CPU/iss/ume/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-ume-rel):
	(cd obj/$(patsubst example-%-ume-rel,%,$@)/CPU/iss/ume; make release FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-ume-rel,%,$@)/CPU/iss/ume/obj/rel/CPU

#
#
# ISS DTC
#

$(EXAMPLES:%=example-%-dtc):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-dtc-gen):
	$(CLI) --dtc sys/examples/src/$(patsubst example-%-dtc-gen,%,$@).vadl

$(EXAMPLES:%=example-%-dtc-dbg):
	(cd obj/$(patsubst example-%-dtc-dbg,%,$@)/CPU/iss/dtc; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-dtc-dbg,%,$@)/CPU/iss/dtc/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-dtc-rel):
	(cd obj/$(patsubst example-%-dtc-rel,%,$@)/CPU/iss/dtc; make release FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-dtc-rel,%,$@)/CPU/iss/dtc/obj/rel/CPU

#
#
# ISS DTC UME
#

$(EXAMPLES:%=example-%-dtc-ume):
	make $@-gen
	make $@-dbg

$(EXAMPLES:%=example-%-dtc-ume-gen):
	$(CLI) --dtc sys/examples/src/$(patsubst example-%-dtc-ume-gen,%,$@).vadl

$(EXAMPLES:%=example-%-dtc-ume-dbg):
	(cd obj/$(patsubst example-%-dtc-ume-dbg,%,$@)/CPU/iss/dtc/ume; make debug FLAGS="-D DEBUG")
	$(PROFILE_CMD) ./obj/$(patsubst example-%-dtc-ume-dbg,%,$@)/CPU/iss/dtc/ume/obj/dbg/CPU 2>&1 | tee obj/$@.log

$(EXAMPLES:%=example-%-dtc-ume-rel):
	(cd obj/$(patsubst example-%-dtc-ume-rel,%,$@)/CPU/iss/dtc/ume; make release)
	$(PROFILE_CMD) ./obj/$(patsubst example-%-dtc-ume-rel,%,$@)/CPU/iss/dtc/ume/obj/rel/CPU
#
#
# LCB
#

$(EXAMPLES:%=example-%-lcb):
	make $@-gen
#	make $@-run

$(EXAMPLES:%=example-%-lcb-gen):
	$(CLI) --lcb --gcb-dump sys/examples/src/$(patsubst example-%-lcb-gen,%,$@).vadl

# $(filter %.vadl,$^)

#
# Build Management
#

check-image-version:
ifndef NEW_IMAGE_TAG
	$(error NEW_IMAGE_TAG is undefined)
endif
SED_COMMAND='s/vadl-ci:[0-9a-fA-F]*/vadl-ci:$(NEW_IMAGE_TAG)/'

update-image-version: check-image-version
	sed -i $(SED_COMMAND) .gitea/src/drone_aarch32.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_aarch64.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_gradle.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_mini_riscv.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_mips.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_risc8.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_riscv.yml
	sed -i $(SED_COMMAND) .gitea/src/drone_start.yml
	make -C .gitea
	git add .gitea && git commit -m "change CI image"


#
# OpenVADL Submodule
#
update-openvadl:
	git submodule update --init --recursive --remote ./open-vadl
	git submodule update
