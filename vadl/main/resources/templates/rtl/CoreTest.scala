[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3.simulator.EphemeralSimulator._
import circt.stage.ChiselStage
import org.scalatest.flatspec.AnyFlatSpec

import java.math.BigInteger

/**
 * Simple static test for RVI3264
 */
class CoreTest extends AnyFlatSpec {

  /**
   * addi x1 , x0,   10
   * addi x1 , x1,   20
   * addi x2 , x1,  -10
   * addi x2 , x2,  -20
   * addi x3 , x2,   10
   * j pc
   */
  val code = "00a00093" +
    "01408093" +
    "ff608113" +
    "fec10113" +
    "00a10193" +
    "0000006f"

  simulate(
    new [(${projectName})],
  ) { dut =>
    val prog = new BigInteger(code, 16).toByteArray
    var run = true
    var cycles = 0

    dut.reset.poke(true)
    dut.clock.step(1)
    dut.reset.poke(false)

//    while (run && cycles < 100) {
//      val addr = dut.io.rd_out.address.peekValue().asBigInt.toInt
//      if (addr < prog.size - 3) {
//        dut.io.rd_out.data(3).poke(prog(addr))
//        dut.io.rd_out.data(2).poke(prog(addr + 1))
//        dut.io.rd_out.data(1).poke(prog(addr + 2))
//        dut.io.rd_out.data(0).poke(prog(addr + 3))
//      } else {
//        dut.io.rd_out.data(3).poke(0)
//        dut.io.rd_out.data(2).poke(0)
//        dut.io.rd_out.data(1).poke(0)
//        dut.io.rd_out.data(0).poke(0)
//        run = false
//      }
//      dut.clock.step()
//      cycles += 1
//    }
  }

}

/**
 * Generate Verilog sources and save it in file [(${projectName})].v
 */
object CoreTest extends App {
  ChiselStage.emitSystemVerilogFile(
    new [(${projectName})],
    firtoolOpts = Array(
      "-disable-all-randomization"
    )
  )
}
