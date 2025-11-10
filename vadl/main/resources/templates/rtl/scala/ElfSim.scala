[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.simulator._
import chisel3.testing._
import svsim._

import VADL._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec

import java.nio.file.Path

/**
 * Build the ElfSim simulator.
 */
class ElfSimBuild extends AnyFunSpec with ChiselSim {

  implicit val testingDirectory: HasTestingDirectory = new HasTestingDirectory {
    override def getDirectory: Path = svsim.Workspace.getProjectRootOrCwd
      .resolve("build")
      .resolve("elfsim")
  }

  implicit val hasSimulator: HasSimulator = HasSimulator.simulators.verilator(
    verilatorSettings = svsim.verilator.Backend.CompilationSettings(
      traceStyle = Some(
        svsim.verilator.Backend.CompilationSettings.TraceStyle(
          kind = svsim.verilator.Backend.CompilationSettings.TraceKind.Vcd
        )
      )
    ),
    compilationSettings = CommonCompilationSettings.default.copy(
      simulationSettings = CommonSimulationSettings.default.copy(
        plusArgs = Seq(new svsim.PlusArg("elf", Some("")))
      )
    )
  )

  val simulator = hasSimulator.getSimulator
  val workingDirectoryPrefix = simulator.workingDirectoryPrefix
  val workingDirectoryTag = simulator.tag
  val workingDirectory = testingDirectory.getDirectory
    .resolve(s"$workingDirectoryPrefix-$workingDirectoryTag")
  val executable = workingDirectory.resolve("simulation").toString


  // build ElfSim by running an empty simulation
  def build(): Unit = {
    simulate (new ElfSim[# th:inline="none"][[/][(${topModule})][# th:inline="none"]][/](new [(${topModule})], _.io, m => [#
    th:if="${resetVector != null}"]Some(m.io.[(${resetVector})]_in)[/][#
    th:if="${resetVector == null}"]None[/])) { dut => }
  }

  it("builds") {
    build()
  }

}

/**
 * Build the ElfSim simulator and run the riscv-tests on it.
 */
class ElfSimTest extends ElfSimBuild with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    build()
  }

  // run riscv-tests
  new java.io.File("riscv-tests/isa").listFiles(_.getName.matches("[(${#strings.substring(isaName, 0, 4).toLowerCase()})]ui-p-[^.]*"))
    .filter(file => !file.getName.contains("fence")) // not supported
    .foreach(file => {
      it(f"test $file") {
        val relFile = workingDirectory.toAbsolutePath.relativize(file.toPath.toAbsolutePath)
        val settings = CommonSimulationSettings.default.copy(
          plusArgs = Seq(new svsim.PlusArg("elf", Some(f"$relFile")))
        )

        val command = Seq(executable) ++ settings.plusArgs.map(_.simulatorFlags)

        val process = new ProcessBuilder(command: _*)
          .directory(workingDirectory.toFile)
          .redirectErrorStream(true)
          .start()

        val cycles = 2_000
        val trace = false
        process.outputWriter.write(
          s"""R a
             |S 1 0
             |R a
             |S 1 1
             |T 0 0,1-5*1
             |S 1 0
             |R 0
             |W ${if (trace) 1 else 0}
             |T 0 0,1-5*${cycles.toHexString}
             |D\n""".stripMargin)

        var success: Option[String] = None
        var fail: Option[String] = None
        process.inputReader.lines().forEach(line => {
          if (line.contains("RVTEST_SUCCESS")) {
            success = Some(line)
          }
          if (line.contains("RVTEST_FAIL")) {
            fail = Some(line)
          }
        })

        process.waitFor()

        fail match {
          case Some(line) => throw new Exception(s"$line")
          case None =>
        }
        success match {
          case Some(line) =>
          case None => throw new Exception("timeout")
        }
      }
    })

}

/**
 * ElfSim test bench that connects all memory ports to the SimMem, optionally reads the reset vector from it.
 *
 * @param module core module
 * @param getIO function to get IO bundle from core module
 * @param getResetVector function to get optional reset vector from core module
 * @tparam T core module type
 */
class ElfSim[T <: Module](module: => T, getIO: T => Bundle, getResetVector: T => Option[Data]) extends Module {

  val core = Module(module)
  val core_io = getIO(core)

  val addrWidth = core_io.getElements.map {
    case rd: VADL.MemReadPort[?] => rd.address.getWidth
    case wr: VADL.MemWritePort[?] => wr.address.getWidth
    case _ => 0
  }.max

  val simSymb = Module(new SimMemSymbols("elf", addrWidth))
  simSymb.io.clock := clock
  simSymb.io.reset := reset

  getResetVector(core) match {
    case Some(d) => d := simSymb.io.entry
    case _ =>
  }

  core_io.getElements.foreach {
    case rd: VADL.MemReadPort[Bits] =>
      val simRead = Module(new SimMemRead("elf", rd))
      simRead.io.clock := clock
      simRead.io.reset := reset
      simRead.io.enable := rd.enable
      simRead.io.address := rd.address
      simRead.io.words := rd.words
      for (i <- rd.data.indices) {
        val w = rd.data(i).getWidth
        rd.data(i) := simRead.io.data(w * (i + 1) - 1, w * i)
      }
      rd.valid := simRead.io.valid
      when(rd.enable && rd.address === simSymb.io.fromhost) {
        printf(cf"fromhost not implemented\n")
      }
    case wr: VADL.MemWritePort[Bits] =>
      val simWrite = Module(new SimMemWrite("elf", wr))
      simWrite.io.clock := clock
      simWrite.io.reset := reset
      simWrite.io.enable := wr.enable
      simWrite.io.address := wr.address
      simWrite.io.words := wr.words
      simWrite.io.data := Cat(wr.data.reverse)
      wr.valid := simWrite.io.valid
      when(wr.enable && wr.address === simSymb.io.tohost) {
        val value = Cat(wr.data.reverse)
        printf(cf"tohost: $value\n")
        when(value(0) === 1.U) {
          val result = value >> 1
          when(result === 0.U) {
            printf(cf"RVTEST_PASS\n")
          }.otherwise {
            printf(cf"RVTEST_FAIL $result\n")
          }
          stop()
        }
      }
    case elem => dontTouch(elem)
  }
}

// SimMem black boxes that connect to the C++ simulation model of the memory

abstract class SimMem[T <: Data](name: String, dataWidth: Int, addrWidth: Int, wordWidth: Int,
                                 params: Map[String, Param] = Map.empty)
  extends BlackBox(Map.newBuilder[String, Param]
    .addOne("NAME" -> StringParam(name))
    .addOne("DATA_WIDTH" -> IntParam(dataWidth))
    .addOne("ADDR_WIDTH" -> IntParam(addrWidth))
    .addOne("WORD_WIDTH" -> IntParam(wordWidth))
    .addAll(params)
    .result()
  )
    with HasBlackBoxResource {

  val dataType = Bits(dataWidth.W)

  def getIO(dataOut: Boolean) = new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())

    val enable = Input(Bool())
    val address = Input(Bits(addrWidth.W))
    val words = Input(Bits((log2Ceil(dataWidth/wordWidth+1)).W))
    val data = if (dataOut) Output(dataType) else Input(dataType)
    val valid = Output(Bool())
  }

  addResource("/SimMem.sv")
  addResource("/SimMem.cpp")

}

class SimMemRead[T <: Data](name: String, port: MemReadPort[T]) extends SimMem(
  name,
  port.data.getWidth,
  port.address.getWidth,
  port.data.getElements.map(_.getWidth).max
) {
  val io = IO(getIO(true))
}

class SimMemWrite[T <: Data](name: String, port: MemWritePort[T]) extends SimMem(
  name,
  port.data.getWidth,
  port.address.getWidth,
  port.data.getElements.map(_.getWidth).max
) {
  val io = IO(getIO(false))
}

class SimMemSymbols[T <: Data](name: String, addrWidth: Int) extends SimMem(name, 0 /* not used */, addrWidth, 0) {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())

    val entry = Output(Bits(addrWidth.W))
    val fromhost = Output(Bits(addrWidth.W))
    val tohost = Output(Bits(addrWidth.W))
  })
}
