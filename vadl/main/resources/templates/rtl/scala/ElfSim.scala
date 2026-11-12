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

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}

class ElfSimBase(directory: String) extends AnyFunSpec with ChiselSim {

  implicit val testingDirectory: HasTestingDirectory = new HasTestingDirectory {
    override def getDirectory: Path = svsim.Workspace.getProjectRootOrCwd
      .resolve("build")
      .resolve(directory)
  }

  implicit val hasSimulator: HasSimulator = HasSimulator.simulators.verilator(
    verilatorSettings = svsim.verilator.Backend.CompilationSettings.default
      .withTraceStyle(Some(
        svsim.verilator.Backend.CompilationSettings.TraceStyle(
          kind = svsim.verilator.Backend.CompilationSettings.TraceKind.Vcd
        )
      )),
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

  // run simulator with an executable and analyze output for success or fail
  def run(
           file: File,
           cycles: Int = 10_000,
           traceFile: Option[String] = None,
           outputFile: Option[String] = None,
           successLine: String => Boolean = _.contains(ElfSim.PASS),
           failLine: String => Boolean = _.contains(ElfSim.FAIL)
         ): Unit = {

    if (!Files.exists(Path.of(executable))) {
      build()
    }

    val settings = CommonSimulationSettings.default.copy(
      plusArgs = Seq(
        new svsim.PlusArg("elf", Some(f"$file")),
        new svsim.PlusArg("run", Some(cycles.toString)),
        new svsim.PlusArg("trace", traceFile)
      )
    )

    val command = Seq(executable) ++ settings.plusArgs.map(_.simulatorFlags)

    val process = new ProcessBuilder(command: _*)
      .redirectErrorStream(true)
      .start()

    var success: Option[String] = None
    var fail: Option[String] = None
    val output = process.inputReader.lines().toList
    output.forEach(line => {
      if (successLine(line)) {
        success = Some(line)
      }
      if (failLine(line)) {
        fail = Some(line)
      }
    })

    outputFile.foreach {
      file => Files.write(Path.of(file), output,
        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    if (process.waitFor() != 0) {
      throw new Exception("exit failure")
    }

    fail match {
      case Some(line) => throw new Exception(s"$line")
      case None =>
    }
    success match {
      case Some(line) =>
      case None => throw new Exception("timeout")
    }

  }

}

/**
 * Build the ElfSim simulator.
 */
class ElfSimBuild extends ElfSimBase("elfsim") {

  it("builds") {
    build()
  }

}

/**
 * Build the ElfSim simulator and run the riscv-tests on it.
 */
class ElfSimTest extends ElfSimBase("elfsimtest") with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    build()
  }

  new File("test").mkdirs()

  // run riscv-tests
  new java.io.File("/riscv-tests/isa")
    .listFiles(_.getName.matches("[(${#strings.substring(isaName, 0, 4).toLowerCase()})]ui-p-[^.]*"))
    .filter(file => !file.getName.contains("fence")) // not supported
    .foreach(file => {
      it(f"${file.getName}") {
        run(
          file,
          traceFile = Some(s"test/${file.getName}"),
          outputFile = Some(s"test/${file.getName}.log")
        )
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
object ElfSim {
  val PASS = "ELFSIM_PASS"
  val FAIL = "ELFSIM_FAIL"
}
class ElfSim[T <: Module](module: => T, getIO: T => Bundle, getResetVector: T => Option[Data]) extends Module {

  val core = Module(module)
  val core_io = getIO(core)

  val addrWidth = core_io.getElements.map {
    case rd: VADL.MemReadPort[?] => rd.address.getWidth
    case wr: VADL.MemWritePort[?] => wr.address.getWidth
    case _ => 0
  }.max

  val simSymb = Module(new SimMemSymbols("elf", addrWidth))
  simSymb.clock := clock
  simSymb.reset := reset

  getResetVector(core) match {
    case Some(d) => d := simSymb.entry
    case _ =>
  }

  core_io.getElements.foreach {
    case rd: VADL.MemReadPort[Bits] =>
      val simRead = Module(new SimMemRead("elf", rd))
      simRead.clock := clock
      simRead.reset := reset
      simRead.enable := rd.enable
      simRead.address := rd.address
      simRead.words := rd.words
      for (i <- rd.data.indices) {
        val w = rd.data(i).getWidth
        rd.data(i) := simRead.data(w * (i + 1) - 1, w * i)
      }
      rd.valid := simRead.valid
      when(rd.enable && rd.address === simSymb.fromhost) {
        printf(cf"fromhost not implemented\n")
      }
    case wr: VADL.MemWritePort[Bits] =>
      val simWrite = Module(new SimMemWrite("elf", wr))
      simWrite.clock := clock
      simWrite.reset := reset
      simWrite.enable := wr.enable
      simWrite.address := wr.address
      simWrite.words := wr.words
      simWrite.data := Cat(wr.data.reverse)
      wr.valid := simWrite.valid
      when(wr.enable && wr.address === simSymb.tohost) {
        val value = Cat(wr.data.reverse)
        printf(cf"%T tohost: $value\n")
        when(value(0) === 1.U) {
          val result = value >> 1
          when(result === 0.U) {
            printf(cf"%T ${ElfSim.PASS}\n")
          }.otherwise {
            printf(cf"%T ${ElfSim.FAIL} $result\n")
          }
          stop()
        }
      }
    case elem => dontTouch(elem)
  }
}

// SimMem black boxes that connect to the C++ simulation model of the memory

abstract class SimMem[T <: Data](name: String, dataWidth: Int, addrWidth: Int, wordWidth: Int)
  extends ExtModule(Map(
    "NAME" -> name,
    "DATA_WIDTH" -> dataWidth,
    "ADDR_WIDTH" -> addrWidth,
    "WORD_WIDTH" -> wordWidth
  )) {

  val clock = IO(Input(Clock()))
  val reset = IO(Input(Reset()))

  val enable = IO(Input(Bool()))
  val address = IO(Input(Bits(addrWidth.W)))
  val words = IO(Input(Bits((log2Ceil(dataWidth/wordWidth+1)).W)))
  val valid = IO(Output(Bool()))

  addResource("/SimMem.sv")
  addResource("/SimMem.cpp")
  addResource("/simulation-driver-bypass.cpp")

}

class SimMemRead[T <: Data](name: String, port: MemReadPort[T]) extends SimMem(
  name,
  port.data.getWidth,
  port.address.getWidth,
  port.data.getElements.map(_.getWidth).max
) {
  val data = IO(Output(Bits(port.data.getWidth.W)))
}

class SimMemWrite[T <: Data](name: String, port: MemWritePort[T]) extends SimMem(
  name,
  port.data.getWidth,
  port.address.getWidth,
  port.data.getElements.map(_.getWidth).max
) {
  val data = IO(Input(Bits(port.data.getWidth.W)))
}

class SimMemSymbols[T <: Data](name: String, addrWidth: Int) extends ExtModule(Map(
  "NAME" -> name,
  "ADDR_WIDTH" -> addrWidth
)) {
  val clock = IO(Input(Clock()))
  val reset = IO(Input(Reset()))

  val entry = IO(Output(Bits(addrWidth.W)))
  val fromhost = IO(Output(Bits(addrWidth.W)))
  val tohost = IO(Output(Bits(addrWidth.W)))
}
