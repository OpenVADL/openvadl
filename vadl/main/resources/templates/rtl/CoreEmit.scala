[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3.simulator.ChiselSim
import circt.stage.ChiselStage
import org.scalatest.funspec.AnyFunSpec

import java.nio.file.{Files, Path, Paths}

class CoreEmit extends AnyFunSpec with ChiselSim {

  it("emit") {
    Files.createDirectories(Path.of("build"))
    val sv = ChiselStage.emitSystemVerilog(
      new [(${topModule})],
      firtoolOpts = Array(
        "--lowering-options=disallowPackedArrays,disallowLocalVariables",
        "--strip-debug-info",
        "--extract-test-code",
        "--disable-layers=Verification"
      )
    )
    Files.writeString(Paths.get("build/[(${topModule})].sv"), sv)
  }
}
