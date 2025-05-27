[# th:if="${package != ''}"]package [(${package})]

[/]import chisel3._

class [(${name})] extends Module {

  class IO extends Bundle {
    [# th:each="port : ${ports}" ]val [(${port.name})] = [#
    th:if="${port.input}"  ]Input([(${port.ioType})])[/][#
    th:if="${port.output}" ]Output([(${port.ioType})])[/]
    [/]
  }

  val io = IO(new IO)

  // io.out := (io.sel & io.in1) | (~io.sel & io.in0)

  [# th:each="child : ${children}" ]val [(${child.name})] = Module(new [(${child.name})])
  [/]

  [# th:each="resource : ${resources}" ]val [(${resource.name})] = [#
  th:if="${resource.resourceSize} > 1"  ]Mem([(${resource.resourceSize})], [(${resource.resultType})])[/][#
  th:if="${resource.resourceSize} == 1" ]Reg([(${resource.resultType})])[/]
  [/]
}