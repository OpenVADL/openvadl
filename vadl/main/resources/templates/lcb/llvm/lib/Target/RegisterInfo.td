[# th:each="register, iterStat : ${aliasRegisters}" ]
[# th:each="index : ${register.maxList}" ]
def [(${register.name})]_SUB_32_[(${index})]  : SubRegIndex<32>;
def [(${register.name})]_SUB_32_HI_[(${index})] : SubRegIndex<32, 32>;
def [(${register.name})]_FULL_64_[(${index})]    : SubRegIndex<64>;
[/]
def [(${register.name})] : Register<"[(${register.name})]">
{
    let Namespace = "[(${register.namespace.value})]";
    let AsmName = "[(${register.asmName})]";
    let AltNames = [ [(${register.altNamesString})]  ];
    let Aliases = [ ];
    let SubRegs = [ [(${register.subRegs})] ];
    let SubRegIndices = [ [(${register.subRegIndices})] ];
    let RegAltNameIndices = [];
    let DwarfNumbers = [ [(${register.dwarfNumber})] ];
    list<int> CostPerUse = [0];
    let CoveredBySubRegs = 0;
    let HWEncoding = [(${register.hwEncodingValue})];
    let isArtificial = [(${register.isArtificial})];
}
[/]

[# th:each="register, iterStat : ${registers}" ]
[# th:each="index : ${register.maxList}" ]
def [(${register.name})]_SUB_32_[(${index})]  : SubRegIndex<32>;
def [(${register.name})]_SUB_32_HI_[(${index})] : SubRegIndex<32, 32>;
def [(${register.name})]_FULL_64_[(${index})]    : SubRegIndex<64>;
[/]
def [(${register.name})] : Register<"[(${register.name})]">
{
    let Namespace = "[(${register.namespace.value})]";
    let AsmName = "[(${register.asmName})]";
    let AltNames = [ [(${register.altNamesString})]  ];
    let Aliases = [ ];
    let SubRegs = [ [(${register.subRegs})] ];
    let SubRegIndices = [ [(${register.subRegIndices})] ];
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
    let isArtificial = [(${register.isArtificial})];
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

[# th:each="wrapped : ${aliasRegisterFiles}" ]
def [(${wrapped.registerFile.name})] : RegisterClass
< /* namespace = */ "[(${wrapped.registerFile.namespace})]"
, /* regTypes  = */  [  [(${wrapped.registerFile.regTypesString})] ]
, /* alignment = */ [(${wrapped.registerFile.alignment})]
, /* regList   = */
  ( add [(${wrapped.allocationSequence})] )
> {
}
[/]