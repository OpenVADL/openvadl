package vadl.iss.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Counter;
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

    if (diagnostics.size() > 0) {
      // if we found diagnostics, we throw them
      throw new DiagnosticList(diagnostics);
    }
    return null;
  }

  private void checkIsaExists(Specification viam, List<Diagnostic> diagnostics) {
    if (viam.isa().isEmpty()) {
      diagnostics.add(
          Diagnostic.error("No Instruction Set Architecture found",
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
          Diagnostic.error("No Program Counter found", isa.identifier.sourceLocation())
              .locationDescription(isa.sourceLocation(),
                  "No `program counter` definition found.")
              .locationHelp(isa.sourceLocation(), "Add a `program counter` definition.")
              .build()
      );
    } else {
      var pc = isa.pc();
      if (pc != null && !(pc instanceof Counter.RegisterCounter)) {
        diagnostics.add(
            Diagnostic.error("Only `program counter` definitions supported",
                    pc.sourceLocation())
                .locationDescription(pc.sourceLocation(),
                    "This is an alias program counter to a register file. " +
                        "However the ISS generator currently only supports register cell program counters.")
                .note("We have to implement this!")
                .build()
        );
      }
    }
  }
}
