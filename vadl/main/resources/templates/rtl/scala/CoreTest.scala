[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._
import chisel3.simulator._
import chisel3.testing._
import net.fornwall.jelf.{ElfFile, ElfSegment}
import org.scalatest.funspec.AnyFunSpec
import svsim.verilator.Backend.CompilationSettings.TraceKind.Vcd

import java.io.FileInputStream
import java.nio.file.Path
import scala.collection.mutable
import scala.util.Random

/**
 * Simulate the core connected to a memory, load an ELF file to memory.
 */
class CoreTest extends AnyFunSpec with ChiselSim {

  implicit val testingDirectory: HasTestingDirectory = new HasTestingDirectory {
    override def getDirectory: Path = svsim.Workspace.getProjectRootOrCwd
      .resolve("build")
      .resolve("chiselsim")
  }

  implicit val verilator: HasSimulator = HasSimulator.simulators
    .verilator(verilatorSettings =
      svsim.verilator.Backend.CompilationSettings(
        traceStyle = Some(
          svsim.verilator.Backend.CompilationSettings.TraceStyle(kind = Vcd)
        )
      )
    )

  val mem = new mutable.HashMap[BigInt, UInt]()

  def read(addr: BigInt) =
    mem.get(addr) match {
      case None => mem.put(addr, 0.U); 0.U
      case Some(value) => value
    }

  def write(addr: BigInt, value: UInt): Unit =
    mem.put(addr, value)

  it("[(${#strings.substring(isaName, 0, 4).toLowerCase()})]ui-p-*") {
    var passed = Set[String]()
    var failed = Set[String]()
    var timeout = Set[String]()

    simulate(new [(${topModule})]) { dut =>
      enableWaves()

      new java.io.File("/riscv-tests/isa").listFiles(_.getName.matches("[(${#strings.substring(isaName, 0, 4).toLowerCase()})]ui-p-[^.]*"))
        .filter(file => !file.getName.contains("fence")) // not supported
        .foreach(file => {
          println(f"test $file")

          // load elf
          mem.clear()
          val elf = ElfFile.from(file)
          val toAddr = (addr: Long) => if (elf.is32Bits()) addr & 0xffff_ffffL else addr
          val optSym = (sym: String) => Option(elf.getELFSymbol(sym)).map(_.st_value).map(toAddr)
          val start = toAddr(elf.getELFSymbol("_start").st_value)
          val fromhost = optSym("fromhost")
          val tohost = optSym("tohost")
          var i = 0
          var load = true
          while (load) {
            try {
              val header = elf.getProgramHeader(i)
              if (header.p_type == ElfSegment.PT_LOAD) {
                val addr = if (elf.is32Bits()) header.p_paddr & 0xffff_ffffL else header.p_paddr
                val size = header.p_filesz
                val reader = new FileInputStream(file)
                reader.skip(header.p_offset)
                for (a <- addr until addr + size) {
                  val char = reader.read()
                  if (char >= 0) {
                    write(a, char.U)
                  }
                }
                reader.close()
              }
            } catch {
              case e: ArrayIndexOutOfBoundsException => load = false
            }
            i += 1
          }

          var run = true
          var cycles = 0
          val rand = new Random
          val delayRd = () => 0 // rand.nextInt(4) // test stalling
          val delayWr = () => 0 // rand.nextInt(4) // test stalling
          val pending = new mutable.HashMap[Object, Int]()

          [# th:if="${resetVector != null}"]dut.io.[(${resetVector})]_in.poke(start)[/]
          [# th:if="${memoryValid}"]
          dut.io.getElements.foreach {
            case rd: VADL.MemReadPort[?] =>
              rd.valid.poke(false)
            case wr: VADL.MemWritePort[?] =>
              wr.valid.poke(false)
            case _ =>
          }[/]

          dut.reset.poke(true.B)
          dut.clock.step(2)
          dut.reset.poke(false.B)
          dut.clock.step(1)

          while (run) {
            dut.io.getElements.foreach {
              case rd: VADL.MemReadPort[Bits] =>
                // read port
                val en = rd.enable.peekBoolean()
                if (en) {
                  val addr = rd.address.peekValue().asBigInt
                  for (i <- rd.data.indices) {
                    rd.data(i).poke(read(addr + i))
                  }
                  if (fromhost.isDefined && addr.toLong.equals(fromhost.get)) {
                    println(f"fromhost not implemented")
                    for (i <- rd.data.indices) {
                      rd.data(i).poke(1.U)
                    }
                  }[# th:if="${memoryValid}"]
                  val p = pending.getOrElse(rd, delayRd())
                  rd.valid.poke(p == 0)
                  pending.put(rd, if (p == 0) delayRd() else (p - 1))[/]
                } else {
                  [# th:if="${memoryValid}"]rd.valid.poke(false)[/]
                }
              case wr: VADL.MemWritePort[Bits] =>
                // write port
                val en = wr.enable.peekBoolean()
                if (en) {
                  val addr = wr.address.peekValue().asBigInt
                  for (i <- wr.data.indices) {
                    write(addr + i, wr.data(i).peek().asUInt)
                  }
                  if (tohost.isDefined && addr.toLong.equals(tohost.get)) {
                    val elems = wr.data.getElements.map(e => e.peek().asUInt)
                    var value = BigInt(0)
                    for (elem <- elems.reverse) {
                      value = (value << elem.getWidth) | elem.litValue
                    }
                    println(f"tohost: $value")
                    if (value.testBit(0)) {
                      val result = value >> 1
                      if (result == 0) {
                        println(f"RVTEST_PASS: ${file.getName}")
                        passed += file.getName
                      } else {
                        println(f"RVTEST_FAIL $result")
                        failed += file.getName
                      }
                      run = false
                    } else {
                      println(f"tohost not implemented")
                    }
                  }[# th:if="${memoryValid}"]
                  val p = pending.getOrElse(wr, delayWr())
                  wr.valid.poke(p == 0)
                  pending.put(wr, if (p == 0) delayWr() else (p - 1))[/]
                } else {
                  [# th:if="${memoryValid}"]wr.valid.poke(false)[/]
                }
              case _ =>
            }

            dut.clock.step()
            cycles += 1

            if (cycles > 2_000) {
              println("RVTEST_TIMEOUT")
              timeout += file.getName
              run = false
            }
          }

      })
    }

    val result = f"passed ${passed.mkString(" ")}\nfailed ${failed.mkString(" ")}\ntimeout ${timeout.mkString(" ")}\n"
    if (failed.nonEmpty || timeout.nonEmpty) {
      fail(result)
    } else {
      println(f"All Passed!")
      println(f"Tests run: ${passed.mkString(" ")}")
    }
  }
}
