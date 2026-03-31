[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._
import chisel3.util._

object VADL {

  class RegReadPort[T <: Data](tpe: T) extends Bundle {
    val data = Output(tpe)

    def :=(data: T): Unit = {
      this.data := data
    }
  }

  class RegWritePort[T <: Data](tpe: T) extends Bundle {
    val enable = Input(Bool())
    val data = Input(tpe)

    def :=(data: T): Unit = {
      when(this.enable) {
        data := this.data
      }
    }
  }

  class RegFileReadPort[T <: Data](tpe: T, addrWidth: Int) extends RegReadPort(tpe) {
    val enable = Input(Bool())
    val address = Input(UInt(addrWidth.W))

    def :=(mem: Mem[T]): Unit = {
      this.data := 0.U.asTypeOf(this.data)
      when(this.enable) {
        this.data := mem(this.address)
      }
    }
  }

  class RegFileWritePort[T <: Data](tpe: T, addrWidth: Int) extends RegWritePort(tpe) {
    val address = Input(UInt(addrWidth.W))

    def :=(mem: Mem[T]): Unit = {
      when(this.enable) {
        mem(this.address) := this.data
      }
    }
  }

  class MemReadPort[T <: Data](tpe: T, n: Int, addrWidth: Int) extends RegFileReadPort(Vec(n, tpe), addrWidth) {
    val words = Input(Bits(log2Ceil(n+1).W))

    [# th:if="${memoryValid}"]val valid = Output(Bool())[/]
  }

  class MemWritePort[T <: Data](tpe: T, n: Int, addrWidth: Int) extends RegFileWritePort(Vec(n, tpe), addrWidth) {
    val words = Input(Bits(log2Ceil(n+1).W))

    [# th:if="${memoryValid}"]val valid = Output(Bool())[/]
  }

  // Helpers

  implicit class BitsExtTrunc(a: Bits) {
    def sext(width: Width): UInt = {
      val s = Wire(SInt(width))
      s := a.asSInt
      s.asUInt
    }

    def trunc(width: Width): UInt = {
      val s = Wire(Bits(width))
      s := a
      s.asUInt
    }
  }

  // Builtins

  def neg(a: Bits): SInt = 0.S - a.asUInt.zext

  def add(a: Bits, b: Bits): Bits = a.asUInt + b.asUInt

  def sub(a: Bits, b: Bits): UInt = a.asUInt - b.asUInt

  def mul(a: Bits, b: Bits): Bits = a.asUInt * b.asUInt
  def smull(a: Bits, b: Bits): Bits = (a.asSInt * b.asSInt).asUInt
  def umull(a: Bits, b: Bits): Bits = a.asUInt * b.asUInt
  def sumull(a: Bits, b: Bits): Bits = (a.asSInt * b.asUInt).asUInt

  def smod(a: Bits, b: Bits): Bits = (a.asSInt % b.asSInt).asUInt
  def umod(a: Bits, b: Bits): Bits = a.asUInt % b.asUInt

  def sdiv(a: Bits, b: Bits): Bits = (a.asSInt / b.asSInt).asUInt
  def udiv(a: Bits, b: Bits): Bits = a.asUInt / b.asUInt

  def not(a: Bits): Bits = ~a
  def and(a: Bits, b: Bits): Bits = a.asUInt & b.asUInt
  def xor(a: Bits, b: Bits): Bits = a.asUInt ^ b.asUInt
  def or(a: Bits, b: Bits): Bits = a.asUInt | b.asUInt

  def equ(a: Bits, b: Bits): Bool = a.asUInt === b.asUInt
  def neq(a: Bits, b: Bits): Bool = !(a.asUInt === b.asUInt)

  def slth(a: Bits, b: Bits): Bool = a.asSInt < b.asSInt
  def ulth(a: Bits, b: Bits): Bool = a.asUInt < b.asUInt

  def sleq(a: Bits, b: Bits): Bool = a.asSInt <= b.asSInt
  def uleq(a: Bits, b: Bits): Bool = a.asUInt <= b.asUInt

  def sgth(a: Bits, b: Bits): Bool = a.asSInt > b.asSInt
  def ugth(a: Bits, b: Bits): Bool = a.asUInt > b.asUInt

  def sgeq(a: Bits, b: Bits): Bool = a.asSInt >= b.asSInt
  def ugeq(a: Bits, b: Bits): Bool = a.asUInt >= b.asUInt

  def lsl(a: Bits, b: Bits): Bits = {
    val s = Wire(UInt(a.getWidth.W))
    val amt = b.asUInt % a.getWidth.U
    s := (a << amt).asUInt
    s
  }
  def asr(a: Bits, b: Bits): Bits = {
    val s = Wire(UInt(a.getWidth.W))
    val amt = b.asUInt % a.getWidth.U
    s := (a.asSInt >> amt).asUInt
    s
  }
  def lsr(a: Bits, b: Bits): Bits = {
    val s = Wire(UInt(a.getWidth.W))
    val amt = b.asUInt % a.getWidth.U
    s := (a >> amt).asUInt
    s
  }

  // See https://github.com/chipsalliance/chisel/issues/1135
  def rol(a: Bits, b: Bits): Bits = {
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
  def ror(a: Bits, b: Bits): Bits = {
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