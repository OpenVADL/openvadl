[# th:each="register, iterStat : ${registers}" ]
def [(${register.name})] : Register<"[(${register.name})]">
{
    let Namespace = "[(${register.namespace.value})]";
    let AsmName = "[(${register.asmName})]";
    let AltNames = [ [(${register.altNamesString})]  ];
    let Aliases = [ ];
    let SubRegs = [ ];
    let SubRegIndices = [ ];
    let RegAltNameIndices = [];
    let DwarfNumbers = [ [(${register.dwarfNumber})] ];
    list<int> CostPerUse = [0];
    let CoveredBySubRegs = 0;
    [#th:block th:if="${register.hwEncodingMsb != 0}"]
    let HWEncoding{[(${register.hwEncodingMsb})]-0} = [(${register.hwEncodingValue})];
    [/th:block]
    [#th:block th:if="${register.hwEncodingMsb == 0}"]
    let HWEncoding = [(${register.hwEncodingValue})];
    [/th:block]
    let isArtificial = 0;
}
[/]

defvar [(${namespace})] = DefaultMode;
[# th:each="wrapped : ${registerFiles}" ]
def [(${wrapped.registerFile.name})]LenRI : RegInfoByHwMode<
      [ [(${namespace})] ],
      [RegInfo<[(${wrapped.registerFile.regSize})],[(${wrapped.registerFile.regSize})],[(${pointerAlignment})]>]>;
def [(${wrapped.registerFile.name})] : RegisterClass
< /* namespace = */ "[(${wrapped.registerFile.namespace})]"
, /* regTypes  = */  [  [(${wrapped.registerFile.regTypesString})] ]
, /* alignment = */ [(${wrapped.registerFile.alignment})]
, /* regList   = */
  ( add [(${wrapped.allocationSequence})] )
> {
  let RegInfos = [(${wrapped.registerFile.name})]LenRI;
}
[/]
