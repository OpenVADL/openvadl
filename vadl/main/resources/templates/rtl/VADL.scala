[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._

object VADL {

  class RegReadPort[T <: Data](tpe: T) extends Bundle {
    val data = Output(tpe)
  }

  class RegWritePort[T <: Data](tpe: T) extends Bundle {
    val enable = Input(Bool())
    val data = Input(tpe)
  }

  class RegFileReadPort[T <: Data](tpe: T, addrWidth: Int) extends RegReadPort(tpe) {
    val enable = Input(Bool())
    val address = Input(UInt(addrWidth.W))
  }

  class RegFileWritePort[T <: Data](tpe: T, addrWidth: Int) extends RegWritePort(tpe) {
    val address = Input(UInt(addrWidth.W))
  }

  class MemReadPort[T <: Data](tpe: T, addrWidth: Int) extends RegFileReadPort(tpe, addrWidth) {
    val valid = Output(Bool())
  }

  class MemWritePort[T <: Data](tpe: T, addrWidth: Int) extends RegFileWritePort(tpe, addrWidth) {
    val valid = Output(Bool())
  }

  // Helpers

  implicit class BitsSExt(a: Bits) {
    def sext(width: Width): Bits = {
      val s = Wire(SInt(width))
      s := a.asSInt
      s.asUInt
    }
  }

  // Builtins

  def NEG(a: Bits): SInt = 0.S - a.asUInt.zext

  def ADD(a: Bits, b: Bits): Bits = a.asUInt + b.asUInt

  def SUB(a: Bits, b: Bits): UInt = a.asUInt - b.asUInt

  def MUL(a: Bits, b: Bits): Bits = a.asUInt * b.asUInt
  def SMULL(a: Bits, b: Bits): Bits = (a.asSInt * b.asSInt).asUInt
  def UMULL(a: Bits, b: Bits): Bits = a.asUInt * b.asUInt
  def SUMULL(a: Bits, b: Bits): Bits = (a.asSInt * b.asUInt).asUInt

  def SMOD(a: Bits, b: Bits): Bits = (a.asSInt % b.asSInt).asUInt
  def UMOD(a: Bits, b: Bits): Bits = a.asUInt % b.asUInt

  def SDIV(a: Bits, b: Bits): Bits = (a.asSInt / b.asSInt).asUInt
  def UDIV(a: Bits, b: Bits): Bits = a.asUInt / b.asUInt

  def NOT(a: Bits): Bits = ~a
  def AND(a: Bits, b: Bits): Bits = a.asUInt & b.asUInt
  def XOR(a: Bits, b: Bits): Bits = a.asUInt ^ b.asUInt
  def OR(a: Bits, b: Bits): Bits = a.asUInt | b.asUInt

  def EQU(a: Bits, b: Bits): Bool = a === b
  def NEQ(a: Bits, b: Bits): Bool = !(a === b)

  def SLTH(a: Bits, b: Bits): Bool = a.asSInt < b.asSInt
  def ULTH(a: Bits, b: Bits): Bool = a.asUInt < b.asUInt

  def SLEQ(a: Bits, b: Bits): Bool = a.asSInt <= b.asSInt
  def ULEQ(a: Bits, b: Bits): Bool = a.asUInt <= b.asUInt

  def SGTH(a: Bits, b: Bits): Bool = a.asSInt > b.asSInt
  def UGTH(a: Bits, b: Bits): Bool = a.asUInt > b.asUInt

  def SGEQ(a: Bits, b: Bits): Bool = a.asSInt >= b.asSInt
  def UGEQ(a: Bits, b: Bits): Bool = a.asUInt >= b.asUInt

  def LSL(a: Bits, b: Bits): Bits = {
    val amt = b.asUInt % a.getWidth.U
    (a << amt).asUInt
  }
  def ASR(a: Bits, b: Bits): Bits = {
    val amt = b.asUInt % a.getWidth.U
    (a.asSInt >> amt).asUInt
  }
  def LSR(a: Bits, b: Bits): Bits = {
    val amt = b.asUInt % a.getWidth.U
    (a >> amt).asUInt
  }

  // See https://github.com/chipsalliance/chisel/issues/1135
  def ROL(a: Bits, b: Bits): Bits = {
    val sel = Wire(UInt(a.getWidth.W))
    val amt = b.asUInt % a.getWidth.U
    sel := a
    for (i <- 1 until a.getWidth-1) {
      when(amt === i.U) {
        sel := a.tail(i) ## a.head(i)
      }
    }
    sel
  }
  def ROR(a: Bits, b: Bits): Bits = {
    val sel = Wire(UInt(a.getWidth.W))
    val amt = b.asUInt % a.getWidth.U
    sel := a
    for (i <- 1 until a.getWidth-1) {
      when(amt === i.U) {
        sel := a.tail(a.getWidth-i) ## a.head(a.getWidth-i)
      }
    }
    sel
  }

}