DOXYGEN ?= ./doxygen-openvadl
LATEST_VERSION := $(shell git describe --tags --abbrev=0 | sed 's/^v//')

default: docs

.PHONY: docs clean clean-html clean-latex

docs: latex html

doxygen: $(DOXYGEN)
	@mkdir -p obj/doc

OPEN_VADL_DOC_INPUT_REFMAN=\
./refman/authors.md \
./refman/introduction.md \
./refman/tutorial.md \
./refman/refmanual.md \
../vadl/main/vadl/ast/vadl.ATG \
./refman/acronyms.md


OPEN_VADL_HTML_DOCS=\
handbook/main.md \
refman/tutorial.md \
refman/refmanual.md \
handbook/iss.md \
handbook/lcb.md \
handbook/cosim.md \
../vadl/main/vadl/ast/vadl.ATG \
refman/acronyms.md

html: open-vadl-html-docs
latex: open-vadl-latex

open-vadl-html-docs: doxygen
	@echo "-- Generating HTML Documentation"
	LATEST_VERSION=$(LATEST_VERSION) \
	PROJECT_NAME="OpenVADL" \
	PROJECT_NUMBER="(`(git describe --tags --always --dirty)`)" \
	PROJECT_BRIEF="" \
	PROJECT_LOGO="./assets/img/logo-icon-dark.png" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-docs" \
	INPUT="$(OPEN_VADL_HTML_DOCS)" \
	$(DOXYGEN) html.cfg
	@echo "-- Generating HTML Documentation refman [DONE]"

open-vadl-latex: doxygen
	@echo "-- Generating LaTeX Documentation"
	PROJECT_NAME="Vienna Architecture Description Language" \
	PROJECT_NUMBER="(`(git describe --tags --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="./assets/img/logo-icon-dark.png" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT="$(OPEN_VADL_DOC_INPUT_REFMAN)" \
	$(DOXYGEN) latex.cfg
	bash ./tools/sed-latex "obj/doc/open-vadl-refman/latex"
	@(cd obj/doc/open-vadl-refman/latex; make)
	@cp obj/doc/open-vadl-refman/latex/refman.pdf obj/doc/open-vadl-refman/open-vadl.pdf
	@echo "-- Generating LaTeX Documentation [DONE]"

clean: clean-html clean-latex

clean-html:
	@echo "-- Cleaning HTML documentation"
	@rm -rf obj/doc/open-vadl-docs

clean-latex:
	@echo "-- Cleaning LaTeX documentation"
	@rm -rf obj/doc/open-vadl-refman
