DOXYGEN ?= obj/bin/doxygen

default: docs

.PHONY: docs


docs: latex html

doxygen: $(DOXYGEN)
	@mkdir -p obj/doc

OPEN_VADL_DOC_INPUT_IMPL="\
./vadl \
./vadl-cli \
./java-annotations \
"

OPEN_VADL_DOC_INPUT_REFMAN="\
./doc/authors.md \
./doc/introduction.md \
./doc/tutorial.md \
./doc/refmanual.md \
./doc/acronyms.md \
"

html: open-vadl-html-impl open-vadl-html-refman
latex: open-vadl-latex

open-vadl-html-impl: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="(`(git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-impl" \
	INPUT=$(OPEN_VADL_DOC_INPUT_IMPL) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation impl [DONE]"

open-vadl-html-refman: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NUMBER="(`(git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT=$(OPEN_VADL_DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation refman [DONE]"

open-vadl-latex: doxygen
	@echo "-- Generating LaTeX Documentation"
	PROJECT_NUMBER="(`(git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT=$(OPEN_VADL_DOC_INPUT_REFMAN) \
	$(DOXYGEN) Doxyfile.latex.doxygen
	bash ./tools/sed-latex "obj/doc/open-vadl-refman/latex"
	@(cd obj/doc/open-vadl-refman/latex; make)
	@cp obj/doc/open-vadl-refman/latex/refman.pdf obj/doc/open-vadl-refman/open-vadl.pdf
	@echo "-- Generating LaTeX Documentation [DONE]"

