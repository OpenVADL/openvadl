[# th:if="${package != ''}"]package [(${package})]

[/]import org.scalatest.funspec.AnyFunSpec

import java.nio.file.{Files, Paths}

class CoreEmit extends AnyFunSpec with ChiselSim {

  it("emit") {
    val sv = ChiselStage.emitSystemVerilog(
      new [(${topModule})],
      firtoolOpts = Array(
        "--lowering-options=disallowPackedArrays,disallowLocalVariables",
        "--strip-debug-info",
        "--extract-test-code",
        "--disable-layers=Verification"
      )
    )
    Files.writeString(Paths.get("[(${topModule})].sv"), sv)
  }
}
