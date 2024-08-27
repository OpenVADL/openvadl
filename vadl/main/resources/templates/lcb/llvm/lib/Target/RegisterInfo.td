
[# th:each="register : ${registers}" ]

def [(${register.id})] : Register<"[(${register.id})]">
{
    let Namespace = "[(${register.namespace})]";
    let AsmName = "[(${register.asmName})]";
    let AltNames = [ [(${register.altNames})] ];
    let Aliases = [ [(${register.aliases})] ];
    let SubRegs = [ [(${register.subRegs})] ];
    let SubRegIndices = [ [(${register.subRegIndices})] ];
    let RegAltNameIndices = [];
    let DwarfNumbers = [ [(${register.dwarfNumber })] ];
    list<int> CostPerUse = [0];
    let CoveredBySubRegs = "[(${register.coveredBySubRegs})]";
    let HWEncoding{[(${register.hwEncodingMsb})]-0} = [(${register.hwEncodingValue})];
    let isArtificial = 0;
}

[/]