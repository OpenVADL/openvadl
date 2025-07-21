def NZCV_N : Register<"NZCV_N">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_N";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 0 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def NZCV_Z : Register<"NZCV_Z">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_Z";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 1 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def NZCV_C : Register<"NZCV_C">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_C";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 2 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def NZCV_V : Register<"NZCV_V">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_V";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 3 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def CurrentEL : Register<"CurrentEL">
{
let Namespace = "aarch64temp";
let AsmName = "CurrentEL";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 4 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def SPSel : Register<"SPSel">
{
let Namespace = "aarch64temp";
let AsmName = "SPSel";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 5 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def SP_EL0 : Register<"SP_EL0">
{
let Namespace = "aarch64temp";
let AsmName = "SP_EL0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 6 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def SP_EL1 : Register<"SP_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "SP_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 7 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def ELR_EL1 : Register<"ELR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "ELR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 8 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def VBAR_EL1 : Register<"VBAR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "VBAR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 9 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def ESR_EL1 : Register<"ESR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "ESR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 10 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def SPSR_EL1 : Register<"SPSR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "SPSR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 11 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def PC : Register<"PC">
{
let Namespace = "aarch64temp";
let AsmName = "PC";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 12 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = 0;
}
def S0 : Register<"S0">
{
let Namespace = "aarch64temp";
let AsmName = "X0";
let AltNames = [ "X0"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 15 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = 0;
}
def S1 : Register<"S1">
{
let Namespace = "aarch64temp";
let AsmName = "X1";
let AltNames = [ "X1"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 16 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = 0;
}
def S2 : Register<"S2">
{
let Namespace = "aarch64temp";
let AsmName = "X2";
let AltNames = [ "X2"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 17 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = 0;
}
def S3 : Register<"S3">
{
let Namespace = "aarch64temp";
let AsmName = "X3";
let AltNames = [ "X3"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 18 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = 0;
}
def S4 : Register<"S4">
{
let Namespace = "aarch64temp";
let AsmName = "X4";
let AltNames = [ "X4"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 19 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = 0;
}
def S5 : Register<"S5">
{
let Namespace = "aarch64temp";
let AsmName = "X5";
let AltNames = [ "X5"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 20 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = 0;
}
def S6 : Register<"S6">
{
let Namespace = "aarch64temp";
let AsmName = "X6";
let AltNames = [ "X6"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 21 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = 0;
}
def S7 : Register<"S7">
{
let Namespace = "aarch64temp";
let AsmName = "X7";
let AltNames = [ "X7"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 22 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = 0;
}
def S8 : Register<"S8">
{
let Namespace = "aarch64temp";
let AsmName = "X8";
let AltNames = [ "X8"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 23 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = 0;
}
def S9 : Register<"S9">
{
let Namespace = "aarch64temp";
let AsmName = "X9";
let AltNames = [ "X9"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 24 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = 0;
}
def S10 : Register<"S10">
{
let Namespace = "aarch64temp";
let AsmName = "X10";
let AltNames = [ "X10"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 25 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = 0;
}
def S11 : Register<"S11">
{
let Namespace = "aarch64temp";
let AsmName = "X11";
let AltNames = [ "X11"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 26 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = 0;
}
def S12 : Register<"S12">
{
let Namespace = "aarch64temp";
let AsmName = "X12";
let AltNames = [ "X12"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 27 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = 0;
}
def S13 : Register<"S13">
{
let Namespace = "aarch64temp";
let AsmName = "X13";
let AltNames = [ "X13"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 28 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = 0;
}
def S14 : Register<"S14">
{
let Namespace = "aarch64temp";
let AsmName = "X14";
let AltNames = [ "X14"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 29 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = 0;
}
def S15 : Register<"S15">
{
let Namespace = "aarch64temp";
let AsmName = "X15";
let AltNames = [ "X15"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 30 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = 0;
}
def S16 : Register<"S16">
{
let Namespace = "aarch64temp";
let AsmName = "X16";
let AltNames = [ "X16"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 31 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = 0;
}
def S17 : Register<"S17">
{
let Namespace = "aarch64temp";
let AsmName = "X17";
let AltNames = [ "X17"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 32 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = 0;
}
def S18 : Register<"S18">
{
let Namespace = "aarch64temp";
let AsmName = "X18";
let AltNames = [ "X18"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 33 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = 0;
}
def S19 : Register<"S19">
{
let Namespace = "aarch64temp";
let AsmName = "X19";
let AltNames = [ "X19"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 34 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = 0;
}
def S20 : Register<"S20">
{
let Namespace = "aarch64temp";
let AsmName = "X20";
let AltNames = [ "X20"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 35 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = 0;
}
def S21 : Register<"S21">
{
let Namespace = "aarch64temp";
let AsmName = "X21";
let AltNames = [ "X21"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 36 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = 0;
}
def S22 : Register<"S22">
{
let Namespace = "aarch64temp";
let AsmName = "X22";
let AltNames = [ "X22"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 37 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = 0;
}
def S23 : Register<"S23">
{
let Namespace = "aarch64temp";
let AsmName = "X23";
let AltNames = [ "X23"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 38 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = 0;
}
def S24 : Register<"S24">
{
let Namespace = "aarch64temp";
let AsmName = "X24";
let AltNames = [ "X24"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 39 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = 0;
}
def S25 : Register<"S25">
{
let Namespace = "aarch64temp";
let AsmName = "X25";
let AltNames = [ "X25"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 40 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = 0;
}
def S26 : Register<"S26">
{
let Namespace = "aarch64temp";
let AsmName = "X26";
let AltNames = [ "X26"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 41 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = 0;
}
def S27 : Register<"S27">
{
let Namespace = "aarch64temp";
let AsmName = "X27";
let AltNames = [ "X27"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 42 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = 0;
}
def S28 : Register<"S28">
{
let Namespace = "aarch64temp";
let AsmName = "X28";
let AltNames = [ "X28"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 43 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = 0;
}
def S29 : Register<"S29">
{
let Namespace = "aarch64temp";
let AsmName = "A_FP";
let AltNames = [ "A_FP"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 44 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = 0;
}
def S30 : Register<"S30">
{
let Namespace = "aarch64temp";
let AsmName = "A_LR";
let AltNames = [ "A_LR"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 45 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = 0;
}
def S31 : Register<"S31">
{
let Namespace = "aarch64temp";
let AsmName = "A_SP";
let AltNames = [ "A_SP"  ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 46 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = 0;
}
def X0 : Register<"X0">
{
let Namespace = "aarch64temp";
let AsmName = "X0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 47 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = 1;
}
def X1 : Register<"X1">
{
let Namespace = "aarch64temp";
let AsmName = "X1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 48 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = 1;
}
def X2 : Register<"X2">
{
let Namespace = "aarch64temp";
let AsmName = "X2";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 49 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = 1;
}
def X3 : Register<"X3">
{
let Namespace = "aarch64temp";
let AsmName = "X3";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 50 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = 1;
}
def X4 : Register<"X4">
{
let Namespace = "aarch64temp";
let AsmName = "X4";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 51 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = 1;
}
def X5 : Register<"X5">
{
let Namespace = "aarch64temp";
let AsmName = "X5";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 52 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = 1;
}
def X6 : Register<"X6">
{
let Namespace = "aarch64temp";
let AsmName = "X6";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 53 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = 1;
}
def X7 : Register<"X7">
{
let Namespace = "aarch64temp";
let AsmName = "X7";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 54 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = 1;
}
def X8 : Register<"X8">
{
let Namespace = "aarch64temp";
let AsmName = "X8";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 55 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = 1;
}
def X9 : Register<"X9">
{
let Namespace = "aarch64temp";
let AsmName = "X9";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 56 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = 1;
}
def X10 : Register<"X10">
{
let Namespace = "aarch64temp";
let AsmName = "X10";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 57 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = 1;
}
def X11 : Register<"X11">
{
let Namespace = "aarch64temp";
let AsmName = "X11";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 58 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = 1;
}
def X12 : Register<"X12">
{
let Namespace = "aarch64temp";
let AsmName = "X12";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 59 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = 1;
}
def X13 : Register<"X13">
{
let Namespace = "aarch64temp";
let AsmName = "X13";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 60 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = 1;
}
def X14 : Register<"X14">
{
let Namespace = "aarch64temp";
let AsmName = "X14";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 61 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = 1;
}
def X15 : Register<"X15">
{
let Namespace = "aarch64temp";
let AsmName = "X15";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 62 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = 1;
}
def X16 : Register<"X16">
{
let Namespace = "aarch64temp";
let AsmName = "X16";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 63 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = 1;
}
def X17 : Register<"X17">
{
let Namespace = "aarch64temp";
let AsmName = "X17";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 64 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = 1;
}
def X18 : Register<"X18">
{
let Namespace = "aarch64temp";
let AsmName = "X18";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 65 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = 1;
}
def X19 : Register<"X19">
{
let Namespace = "aarch64temp";
let AsmName = "X19";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 66 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = 1;
}
def X20 : Register<"X20">
{
let Namespace = "aarch64temp";
let AsmName = "X20";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 67 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = 1;
}
def X21 : Register<"X21">
{
let Namespace = "aarch64temp";
let AsmName = "X21";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 68 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = 1;
}
def X22 : Register<"X22">
{
let Namespace = "aarch64temp";
let AsmName = "X22";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 69 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = 1;
}
def X23 : Register<"X23">
{
let Namespace = "aarch64temp";
let AsmName = "X23";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 70 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = 1;
}
def X24 : Register<"X24">
{
let Namespace = "aarch64temp";
let AsmName = "X24";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 71 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = 1;
}
def X25 : Register<"X25">
{
let Namespace = "aarch64temp";
let AsmName = "X25";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 72 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = 1;
}
def X26 : Register<"X26">
{
let Namespace = "aarch64temp";
let AsmName = "X26";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 73 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = 1;
}
def X27 : Register<"X27">
{
let Namespace = "aarch64temp";
let AsmName = "X27";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 74 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = 1;
}
def X28 : Register<"X28">
{
let Namespace = "aarch64temp";
let AsmName = "X28";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 75 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = 1;
}
def X29 : Register<"X29">
{
let Namespace = "aarch64temp";
let AsmName = "X29";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 76 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = 1;
}
def X30 : Register<"X30">
{
let Namespace = "aarch64temp";
let AsmName = "X30";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 77 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = 1;
}
def X31 : Register<"X31">
{
let Namespace = "aarch64temp";
let AsmName = "X31";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 78 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = 1;
}
def W0 : Register<"W0">
{
let Namespace = "aarch64temp";
let AsmName = "W0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 79 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = 1;
}
def W1 : Register<"W1">
{
let Namespace = "aarch64temp";
let AsmName = "W1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 80 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = 1;
}
def W2 : Register<"W2">
{
let Namespace = "aarch64temp";
let AsmName = "W2";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 81 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = 1;
}
def W3 : Register<"W3">
{
let Namespace = "aarch64temp";
let AsmName = "W3";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 82 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = 1;
}
def W4 : Register<"W4">
{
let Namespace = "aarch64temp";
let AsmName = "W4";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 83 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = 1;
}
def W5 : Register<"W5">
{
let Namespace = "aarch64temp";
let AsmName = "W5";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 84 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = 1;
}
def W6 : Register<"W6">
{
let Namespace = "aarch64temp";
let AsmName = "W6";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 85 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = 1;
}
def W7 : Register<"W7">
{
let Namespace = "aarch64temp";
let AsmName = "W7";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 86 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = 1;
}
def W8 : Register<"W8">
{
let Namespace = "aarch64temp";
let AsmName = "W8";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 87 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = 1;
}
def W9 : Register<"W9">
{
let Namespace = "aarch64temp";
let AsmName = "W9";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 88 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = 1;
}
def W10 : Register<"W10">
{
let Namespace = "aarch64temp";
let AsmName = "W10";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 89 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = 1;
}
def W11 : Register<"W11">
{
let Namespace = "aarch64temp";
let AsmName = "W11";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 90 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = 1;
}
def W12 : Register<"W12">
{
let Namespace = "aarch64temp";
let AsmName = "W12";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 91 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = 1;
}
def W13 : Register<"W13">
{
let Namespace = "aarch64temp";
let AsmName = "W13";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 92 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = 1;
}
def W14 : Register<"W14">
{
let Namespace = "aarch64temp";
let AsmName = "W14";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 93 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = 1;
}
def W15 : Register<"W15">
{
let Namespace = "aarch64temp";
let AsmName = "W15";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 94 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = 1;
}
def W16 : Register<"W16">
{
let Namespace = "aarch64temp";
let AsmName = "W16";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 95 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = 1;
}
def W17 : Register<"W17">
{
let Namespace = "aarch64temp";
let AsmName = "W17";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 96 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = 1;
}
def W18 : Register<"W18">
{
let Namespace = "aarch64temp";
let AsmName = "W18";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 97 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = 1;
}
def W19 : Register<"W19">
{
let Namespace = "aarch64temp";
let AsmName = "W19";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 98 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = 1;
}
def W20 : Register<"W20">
{
let Namespace = "aarch64temp";
let AsmName = "W20";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 99 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = 1;
}
def W21 : Register<"W21">
{
let Namespace = "aarch64temp";
let AsmName = "W21";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 100 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = 1;
}
def W22 : Register<"W22">
{
let Namespace = "aarch64temp";
let AsmName = "W22";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 101 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = 1;
}
def W23 : Register<"W23">
{
let Namespace = "aarch64temp";
let AsmName = "W23";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 102 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = 1;
}
def W24 : Register<"W24">
{
let Namespace = "aarch64temp";
let AsmName = "W24";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 103 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = 1;
}
def W25 : Register<"W25">
{
let Namespace = "aarch64temp";
let AsmName = "W25";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 104 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = 1;
}
def W26 : Register<"W26">
{
let Namespace = "aarch64temp";
let AsmName = "W26";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 105 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = 1;
}
def W27 : Register<"W27">
{
let Namespace = "aarch64temp";
let AsmName = "W27";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 106 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = 1;
}
def W28 : Register<"W28">
{
let Namespace = "aarch64temp";
let AsmName = "W28";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 107 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = 1;
}
def W29 : Register<"W29">
{
let Namespace = "aarch64temp";
let AsmName = "W29";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 108 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = 1;
}
def W30 : Register<"W30">
{
let Namespace = "aarch64temp";
let AsmName = "W30";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 109 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = 1;
}
def W31 : Register<"W31">
{
let Namespace = "aarch64temp";
let AsmName = "W31";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 110 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = 1;
}



def SP : Register<"SP">
{
let Namespace = "aarch64temp";
let AsmName = "SP";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 13 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;
let HWEncoding = 31;
let isArtificial = 1;
}
def LR : Register<"LR">
{
let Namespace = "aarch64temp";
let AsmName = "LR";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [ ];
let SubRegIndices = [ ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 14 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;
let HWEncoding = 30;
let isArtificial = 1;
}


defvar aarch64temp = DefaultMode;

def SLenRI : RegInfoByHwMode<
[ aarch64temp ],
[RegInfo<64,64,64>]>;
def S : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i64 ]
, /* alignment = */ 32
, /* regList   = */
( add S9, S10, S11, S12, S13, S14, S15, S19, S20, S21, S22, S23, S24, S25, S26, S27, S28, S0, S1, S2, S3, S4, S5, S6, S7, S8, S16, S17, S18, S29, S30, S31 )
> {
let RegInfos = SLenRI;
}
def XLenRI : RegInfoByHwMode<
[ aarch64temp ],
[RegInfo<64,64,64>]>;
def X : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i64 ]
, /* alignment = */ 32
, /* regList   = */
( add X0, X1, X2, X3, X4, X5, X6, X7, X8, X9, X10, X11, X12, X13, X14, X15, X16, X17, X18, X19, X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X30, X31 )
> {
let RegInfos = XLenRI;
}
def WLenRI : RegInfoByHwMode<
[ aarch64temp ],
[RegInfo<64,64,64>]>;
def W : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i64 ]
, /* alignment = */ 32
, /* regList   = */
( add W0, W1, W2, W3, W4, W5, W6, W7, W8, W9, W10, W11, W12, W13, W14, W15, W16, W17, W18, W19, W20, W21, W22, W23, W24, W25, W26, W27, W28, W29, W30, W31 )
> {
let RegInfos = WLenRI;
}