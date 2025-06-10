[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._
import chisel3.util._
import VADL._

class [(${name})] extends Module {

  class IO extends Bundle {
    [# th:each="port : ${ports}" ]val [(${port.name})] = [(${port.ioType})]
    [/]
  }

  val io = IO(new IO)

  // io.out := (io.sel & io.in1) | (~io.sel & io.in0)  --> BEHAVIOR

  [# th:each="child : ${children}" ]val [(${child.name})] = Module(new [(${child.name})])
  [/]

  [# th:each="res : ${resources}" ]val [(${res.name})] = [#
  th:if="${res.signal}"  ]Wire([(${res.resultType})])[/][#
  th:if="!${res.signal}"][#
    th:if="${res.resourceSize} > 1"  ]Mem([(${res.resourceSize})], [(${res.resultType})])[/][#
    th:if="${res.resourceSize} == 1" ]Reg([(${res.resultType})])[/][/]
  [/]

  [# th:each="con : ${connections}" ][(${con.output})][#
  th:if="${con.biDir}"  ] :<>= [/][#
  th:if="${!con.biDir}" ] := [/][(${con.input})]
  [/]
}