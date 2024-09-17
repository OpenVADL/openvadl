[# th:each="register, iterStat : ${registers}" ]
def [(${register.id})] : Register<"[(${register.id})]">
{
    let Namespace = "[(${register.namespace})]";
    let AsmName = "[(${register.asmName})]";
    let AltNames = [ [(${register.altNames})] ];
    let Aliases = [ [(${register.aliases})] ];
    let SubRegs = [ [(${register.subRegs})] ];
    let SubRegIndices = [ [(${register.subRegIndices})] ];
    let RegAltNameIndices = [];
    let DwarfNumbers = [ [(${iterStat.index})] ];
    list<int> CostPerUse = [0];
    let CoveredBySubRegs = [(${register.coveredBySubRegs})];
    [#th:block th:if="${register.hwEncodingMsb != 0}"]
    let HWEncoding{[(${register.hwEncodingMsb})]-0} = [(${register.hwEncodingValue})];
    [/th:block]
    [#th:block th:if="${register.hwEncodingMsb == 0}"]
    let HWEncoding = [(${register.hwEncodingValue})];
    [/th:block]
    let isArtificial = 0;
}
[/]

[# th:each="registerFile : ${registerFiles}" ]
def [(${registerFile.name})] : RegisterClass
< /* namespace = */ "[(${registerFile.namespace})]"
, /* regTypes  = */ [ [(${registerFile.regType})] ]
, /* alignment = */ [(${registerFile.alignment})]
, /* regList   = */
  ( add [(${registerFile.regList})] )
>;
[/]
