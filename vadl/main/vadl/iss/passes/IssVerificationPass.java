// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.passes;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * A pass that verifies that all necessary information required to generate a QEMU ISS are present.
 * It also checks if the provided specification uses features that are not yet supported.
 */
public class IssVerificationPass extends AbstractIssPass {
  public IssVerificationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Verification Pass");
  }

  private int foundTargetWidth = 0;

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    // create list of diagnostics to collect them
    var diagnostics = new ArrayList<DiagnosticBuilder>();

    checkMipExists(viam, diagnostics);
    checkProgramCounter(viam, diagnostics);
    checkRegisterTensors(viam, diagnostics);
    checkFormats(viam, diagnostics);
    checkMemory(viam, diagnostics);

    // behavior checks
    checkSlice(viam, diagnostics);

    // check that all resources access provide every index and only
    // access the innermost dimension
    checkResourceAccesses(viam, diagnostics);

    if (!diagnostics.isEmpty()) {
      // if we found diagnostics, we throw them
      throw new DiagnosticList(diagnostics.stream().map(DiagnosticBuilder::build).toList());
    }
    return null;
  }

  @SuppressWarnings("UnusedVariable")
  @Deprecated
  private void checkMipExists(Specification viam, List<DiagnosticBuilder> diagnostics) {
    if (viam.processor().isEmpty()) {
      diagnostics.add(
          error("No Processor Definition found",
              viam)
              .help("Add a `processor` definition to your specification.")
      );
    }
  }

  private void checkProgramCounter(Specification viam, List<DiagnosticBuilder> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return;
    }
    var isa = optIsa.get();
    if (isa.pc() == null) {
      diagnostics.add(
          error("No Program Counter found", isa.identifier.location())
              .locationDescription(isa.location(),
                  "No `program counter` definition found.")
              .locationHelp(isa.location(), "Add a `program counter` definition.")

      );
    } else {
      var pc = isa.pc();
      Objects.requireNonNull(pc);
      if (!pc.registerTensor().isSingleRegister()) {
        diagnostics.add(
            error("Only `program counter` definitions supported",
                pc.location())
                .locationDescription(pc.location(),
                    "This is an alias program counter to a register file. "
                        + "However the ISS generator currently only supports register "
                        + "cell program counters.")
                .note("We have to implement this!")

        );
      } else {
        var pcReg = pc.registerTensor();
        if (pcReg.resultType().bitWidth() != 64 && pcReg.resultType().bitWidth() != 32) {
          diagnostics.add(
              error("Unsupported PC size", pcReg.location())
                  .locationDescription(
                      pcReg.location(),
                      "The PC has %s.", pcReg.resultType().bitWidth())
                  .description("We currently only support PCs with a bit-width of 64 or 32.")
                  .note("We have to implement this!")

          );
        }
      }
      foundTargetWidth = pc.registerTensor().resultType().bitWidth();
    }
  }

  private void checkRegisterTensors(Specification viam, List<DiagnosticBuilder> diagnostics) {
    withIsa(viam, isa -> isa.registerTensors()
        .stream()
        .map(f -> {
          if (f.dimCount() > 2) {
            return error("Invalid register dimension", f)
                .description("The ISS currently only supports 1D and 2D register tensors");
          }

          var resWidth = f.resultType().bitWidth();
          if (foundTargetWidth < resWidth) {
            return error("Invalid register size", f)
                .locationDescription(f, "Program counter has a size of %s bits.", foundTargetWidth)
                .description(
                    "The ISS requires all registers "
                        + "to be smaller or equal to the program counter.");
          }
          return null;
        })
        .filter(Objects::nonNull)
        .forEach(diagnostics::add));
  }

  private void checkFormats(Specification viam, List<DiagnosticBuilder> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty() || optIsa.get().pc() == null) {
      return;
    }
    var isa = optIsa.get();
    var formatWidths = isa.ownInstructions().stream()
        .map(Instruction::format)
        .distinct()
        .map(f -> f.type().bitWidth())
        .distinct()
        .toList();

    if (formatWidths.size() > 1) {
      // check that all used formats have the same bit-width
      var diffFormats = formatWidths.stream().map(w -> viam.findAllFormats()
          .filter(f -> f.type().bitWidth() == w)
          .findFirst()
          .get()
      ).toList();
      var location = isa.identifier.location();
      var errBuilder = error("Different format sizes", location)
          .locationDescription(location,
              "Found %s different format sizes", formatWidths.size())
          .description(
              "The ISS generator currently requires that all instruction's formats have "
                  + "the same size.");
      for (var f : diffFormats) {
        errBuilder.locationDescription(f.identifier,
            "has a bit-width of %s", f.type().bitWidth()
        );
      }

      diagnostics.add(errBuilder);
    }

  }

  private void checkMemory(Specification viam, List<DiagnosticBuilder> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty() || optIsa.get().pc() == null) {
      return;
    }
    var isa = optIsa.get();
    var memories = isa.ownMemories();
    if (memories.size() != 1) {
      diagnostics.add(
          error("Invalid number of `memory` definitions", isa.identifier)
              .description("Expected exactly one `memory` definition, but found %s.",
                  memories.size())
              .note("This is a current limitation of the ISS generator and has to be fixed.")

      );
    } else {
      var mem = memories.get(0);
      if (mem.wordSize() != 8) {
        diagnostics.add(error("Unsupported memory word size", mem)
            .locationDescription(mem,
                "Has a word size of %s bits.", mem.wordSize())
            .description("Currently the memory word must be 8 bits wide.")
            .note("This is going to be relaxed in the future.")
        );
      }
    }

  }

  // checks if all slices are not greater than 64 bit
  private void checkSlice(Specification viam, List<DiagnosticBuilder> diagnostics) {
    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(b -> b.behaviors().stream())
        .flatMap(b -> b.getNodes(SliceNode.class))
        .filter(s -> s.bitSlice().bitSize() > 64)
        .forEach(s -> {
          diagnostics.add(error("Slice result greater than 64 bit", s)
              .description("Currently the ISS requires slices to be less or equal 64 bit.")
              .note("This is because the QEMU implementation only handles 64 bit.")
          );
        });
  }

  // checks if all slices are not greater than 64 bit
  private void checkResourceAccesses(Specification viam, List<DiagnosticBuilder> diagnostics) {
    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(b -> b.behaviors().stream())
        .flatMap(b -> b.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class)))
        .forEach(n -> {
          RegisterTensor tensor;
          int accessIndicesCount;
          if (n instanceof ReadRegTensorNode read) {
            tensor = read.resourceDefinition();
            accessIndicesCount = read.indices().size();
          } else {
            var write = (WriteRegTensorNode) n;
            tensor = write.resourceDefinition();
            accessIndicesCount = write.indices().size();
          }

          if (tensor.maxNumberOfAccessIndices() != accessIndicesCount) {
            diagnostics.add(error("Invalid register access", n)
                .description(
                    "Currently the ISS only allows register accesses to the innermost dimension.")
            );
          }
        });
  }

  private void withIsa(Specification viam, Consumer<InstructionSetArchitecture> func) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty() || optIsa.get().pc() == null) {
      return;
    }
    var isa = optIsa.get();
    func.accept(isa);
  }
}
