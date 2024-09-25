package vadl.iss.passes;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class IssVerificationPass extends AbstractIssPass {
  public IssVerificationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Verification Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    // create list of diagnostics to collect them
    var diagnostics = new ArrayList<Diagnostic>();

    checkIsaExists(viam, diagnostics);
    checkProgramCounter(viam, diagnostics);
    checkRegisterFiles(viam, diagnostics);
    checkFormats(viam, diagnostics);
    checkMemory(viam, diagnostics);

    if (!diagnostics.isEmpty()) {
      // if we found diagnostics, we throw them
      throw new DiagnosticList(diagnostics);
    }
    return null;
  }

  private void checkIsaExists(Specification viam, List<Diagnostic> diagnostics) {
    if (viam.isa().isEmpty()) {
      diagnostics.add(
          error("No Instruction Set Architecture found",
              viam.identifier.sourceLocation())
              .help("Add a `instruction set architecture` definition to your specification.")
              .build()
      );
    }
  }

  private void checkProgramCounter(Specification viam, List<Diagnostic> diagnostics) {
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
              .build()
      );
    } else {
      var pc = isa.pc();
      Objects.requireNonNull(pc);
      if (!(pc instanceof Counter.RegisterCounter)) {
        diagnostics.add(
            error("Only `program counter` definitions supported",
                pc.sourceLocation())
                .locationDescription(pc.sourceLocation(),
                    "This is an alias program counter to a register file. " +
                        "However the ISS generator currently only supports register cell program counters.")
                .note("We have to implement this!")
                .build()
        );
      } else {
        var pcReg = ((Counter.RegisterCounter) pc).registerRef();
        if (pcReg.resultType().bitWidth() != 64) {
          diagnostics.add(
              error("Unsupported PC size", pcReg.sourceLocation())
                  .locationDescription(
                      pcReg.sourceLocation(),
                      "The PC has " + pcReg.resultType().bitWidth() + ".")
                  .description("We currently only support PCs with a bit with of 64.")
                  .note("We have to implement this!")
                  .build()
          );
        }
      }
    }
  }

  private void checkRegisterFiles(Specification viam, List<Diagnostic> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return;
    }
    optIsa.get().ownRegisterFiles()
        .stream()
        .map(f -> {
          // TODO: Add checks for register files
          return (Diagnostic) null;
        })
        .filter(Objects::nonNull)
        .forEach(diagnostics::add);
  }

  private void checkFormats(Specification viam, List<Diagnostic> diagnostics) {
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
              "Found %s different format sizes".formatted(formatWidths.size()))
          .description(
              "The ISS generator currently requires that all instruction's formats have the same size.");
      for (var f : diffFormats) {
        errBuilder.locationDescription(f.identifier.sourceLocation(),
            "has a bit-width of %s".formatted(f.type().bitWidth())
        );
      }

      diagnostics.add(errBuilder.build());
    }

  }

  private void checkMemory(Specification viam, List<Diagnostic> diagnostics) {
    var optIsa = viam.isa();
    if (optIsa.isEmpty() || optIsa.get().pc() == null) {
      return;
    }
    var isa = optIsa.get();
    var memories = isa.ownMemories();
    if (memories.size() != 1) {
      diagnostics.add(
          error("Invalid number of `memory` definitions", isa.identifier.sourceLocation())
              .description("Expected exactly one `memory` definition, but found %s.".formatted(
                  memories.size()))
              .note("This is a current limitation of the ISS generator and has to be fixed.")
              .build()
      );
    } else {
      var mem = memories.get(0);
      if (mem.wordSize() != 8) {
        diagnostics.add(error("Unsupported memory word size", mem.sourceLocation())
            .locationDescription(mem.sourceLocation(),
                "Has a word size of %s bits.".formatted(mem.wordSize()))
            .description("Currently the memory word must be 8 bits wide.")
            .note("This is going to be relaxed in the future.")
            .build());
      }
    }

  }
}
