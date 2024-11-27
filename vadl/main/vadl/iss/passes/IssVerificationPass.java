package vadl.iss.passes;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticBuilder;
import vadl.error.DiagnosticList;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;

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

    checkIsaExists(viam, diagnostics);
    checkProgramCounter(viam, diagnostics);
    checkRegister(viam, diagnostics);
    checkRegisterFiles(viam, diagnostics);
    checkFormats(viam, diagnostics);
    checkMemory(viam, diagnostics);

    if (!diagnostics.isEmpty()) {
      // if we found diagnostics, we throw them
      throw new DiagnosticList(diagnostics.stream().map(DiagnosticBuilder::build).toList());
    }
    return null;
  }

  private void checkIsaExists(Specification viam, List<DiagnosticBuilder> diagnostics) {
    if (viam.isa().isEmpty()) {
      diagnostics.add(
          error("No Instruction Set Architecture found",
              viam.identifier.sourceLocation())
              .help("Add a `instruction set architecture` definition to your specification.")

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
          error("No Program Counter found", isa.identifier.sourceLocation())
              .locationDescription(isa.sourceLocation(),
                  "No `program counter` definition found.")
              .locationHelp(isa.sourceLocation(), "Add a `program counter` definition.")

      );
    } else {
      var pc = isa.pc();
      Objects.requireNonNull(pc);
      if (!(pc instanceof Counter.RegisterCounter)) {
        diagnostics.add(
            error("Only `program counter` definitions supported",
                pc.sourceLocation())
                .locationDescription(pc.sourceLocation(),
                    "This is an alias program counter to a register file. "
                        + "However the ISS generator currently only supports register "
                        + "cell program counters.")
                .note("We have to implement this!")

        );
      } else {
        var pcReg = ((Counter.RegisterCounter) pc).registerRef();
        if (pcReg.resultType().bitWidth() != 64 && pcReg.resultType().bitWidth() != 32) {
          diagnostics.add(
              error("Unsupported PC size", pcReg.sourceLocation())
                  .locationDescription(
                      pcReg.sourceLocation(),
                      "The PC has %s.", pcReg.resultType().bitWidth())
                  .description("We currently only support PCs with a bit-width of 64 or 32.")
                  .note("We have to implement this!")

          );
        }
      }
    }
  }

  private void checkRegister(Specification viam, List<DiagnosticBuilder> diagnostics) {
    withIsa(viam, isa -> {
      isa.ownRegisters()
          .stream()
          .map(f -> {
            var resWidth = f.resultType().bitWidth();
            if (resWidth != 64 && resWidth != 32) {
              return error("Invalid register result size", f)
                  .description("ISS only supports register of size 32 or 64 bits.")
                  ;
            }
            if (targetWidth(resWidth) != resWidth) {
              return error("Different register result sizes", f)
                  .locationDescription(f, "Also found result size of %s in ISA.", foundTargetWidth)
                  .description(
                      "The ISS requires all registers and register files to have the same result size.");
            }
            return null;
          })
          .filter(Objects::nonNull)
          .forEach(diagnostics::add);
    });
  }

  private void checkRegisterFiles(Specification viam, List<DiagnosticBuilder> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return;
    }
    optIsa.get().ownRegisterFiles()
        .stream()
        .map(f -> {
          var resWidth = f.resultType().bitWidth();
          if (resWidth != 64 && resWidth != 32) {
            return error("Invalid register file result width", f)
                .description("The ISS only supports register files of size 32 or 64 bits.")
                ;
          }
          if (targetWidth(resWidth) != resWidth) {
            return error("Different register file result sizes", f)
                .locationDescription(f, "Also found result size of %s in ISA.", foundTargetWidth)
                .description(
                    "The ISS requires all registers and register files to have the same result size.");
          }
          return null;
        })
        .filter(Objects::nonNull)
        .forEach(diagnostics::add);
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
      var location = isa.identifier.sourceLocation();
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

  private int targetWidth(int thisWidth) {
    if (this.foundTargetWidth == 0) {
      this.foundTargetWidth = thisWidth;
    }
    return foundTargetWidth;
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
