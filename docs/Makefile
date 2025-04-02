DOXYGEN ?= ./doxygen

default: docs

.PHONY: docs

docs: latex html

doxygen: $(DOXYGEN)
	@mkdir -p obj/doc

OPEN_VADL_DOC_INPUT_REFMAN=\
./refman/authors.md \
./refman/introduction.md \
./refman/tutorial.md \
./refman/refmanual.md \
./refman/acronyms.md


OPEN_VADL_HTML_DOCS=\
handbook/main.md \
refman/tutorial.md \
refman/refmanual.md \
handbook/iss.md \
handbook/lcb.md \
refman/acronyms.md

html: open-vadl-html-docs
latex: open-vadl-latex

open-vadl-html-docs: doxygen
	@echo "-- Generating HTML Documentation"
	PROJECT_NAME="OpenVADL" \
	PROJECT_NUMBER="(`(git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-docs" \
	INPUT="$(OPEN_VADL_HTML_DOCS)" \
	$(DOXYGEN) Doxyfile.html.doxygen
	@echo "-- Generating HTML Documentation refman [DONE]"

open-vadl-latex: doxygen
	@echo "-- Generating LaTeX Documentation"
	PROJECT_NAME="Vienna Architecture Description Language" \
	PROJECT_NUMBER="(`(git describe --always --dirty)`)" \
	PROJECT_BRIEF="open-vadl" \
	PROJECT_LOGO="" \
	OUTPUT_DIRECTORY="./obj/doc/open-vadl-refman" \
	INPUT="$(OPEN_VADL_DOC_INPUT_REFMAN)" \
	$(DOXYGEN) Doxyfile.latex.doxygen
	bash ./tools/sed-latex "obj/doc/open-vadl-refman/latex"
	@(cd obj/doc/open-vadl-refman/latex; make)
	@cp obj/doc/open-vadl-refman/latex/refman.pdf obj/doc/open-vadl-refman/open-vadl.pdf
	@echo "-- Generating LaTeX Documentation [DONE]"

