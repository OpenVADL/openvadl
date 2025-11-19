[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import VADL._

class [(${name})] extends Module {

  class IO extends Bundle {
    [# th:each="port : ${ports}" ]val [(${port.name})] = [(${port.ioType})]
    [/]
  }
  val io = IO(new IO)

  [# th:if="${syncReset}"]val resetBool = RegInit(true.B)
  resetBool := false.B
  withReset(resetBool) {
  [/][# th:each="child : ${children}" ]val [(${child.name})] = Module(new [(${child.name})])
  [/]

  [# th:each="res : ${resources}" ]val [(${res.name})] = [#
  th:if="${res.signal}" ][#
    th:if="${res.keepSignal}"]dontTouch([/
    ]Wire([(${res.resultType})])[#
    th:if="${res.keepSignal}"])[/][/][#
  th:if="!${res.signal}"][#
    th:if="${res.resourceSize} > 1" ]Mem([(${res.resourceSize})], [(${res.resultType})])[/][#
    th:if="${res.resourceSize == 1 && res.reset == null}" ]RegInit(0.U.asTypeOf([(${res.resultType})]))[/][#
    th:if="${res.resourceSize == 1 && res.reset != null}" ]RegInit([(${res.resultType})], [(${res.reset})])[/][/]
  [/]

  [# th:each="con : ${connections}" ][#
  th:if="${con.isConditional}" ]when (([(${con.condition})]).asBool) {
    [/][(${con.output})][# th:if="${con.isStatement}" ][(${con.statement})][/][#
    th:if="${!con.isStatement}" ][#
    th:if="${con.biDir}"  ] :<>= [/][#
    th:if="${!con.biDir}" ] := [/][(${con.input})][/][#
    th:if="${con.isConditional}" ]
  }[/]
  [/]
  [# th:if="${syncReset}"]}[/]
}