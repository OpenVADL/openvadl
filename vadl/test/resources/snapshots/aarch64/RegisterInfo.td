def SP : Register<"SP">
{
let Namespace = "aarch64temp";
let AsmName = "SP";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
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
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 14 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;
let HWEncoding = 30;
let isArtificial = 1;
}




def NZCV_N_SUB_32_0  : SubRegIndex<32>;
def NZCV_N_SUB_32_HI_0 : SubRegIndex<32, 32>;
def NZCV_N_FULL_64_0    : SubRegIndex<64>;

def NZCV_N : Register<"NZCV_N">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_N";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 0 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def NZCV_Z_SUB_32_0  : SubRegIndex<32>;
def NZCV_Z_SUB_32_HI_0 : SubRegIndex<32, 32>;
def NZCV_Z_FULL_64_0    : SubRegIndex<64>;

def NZCV_Z : Register<"NZCV_Z">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_Z";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 1 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def NZCV_C_SUB_32_0  : SubRegIndex<32>;
def NZCV_C_SUB_32_HI_0 : SubRegIndex<32, 32>;
def NZCV_C_FULL_64_0    : SubRegIndex<64>;

def NZCV_C : Register<"NZCV_C">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_C";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 2 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def NZCV_V_SUB_32_0  : SubRegIndex<32>;
def NZCV_V_SUB_32_HI_0 : SubRegIndex<32, 32>;
def NZCV_V_FULL_64_0    : SubRegIndex<64>;

def NZCV_V : Register<"NZCV_V">
{
let Namespace = "aarch64temp";
let AsmName = "NZCV_V";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 3 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def CurrentEL_SUB_32_0  : SubRegIndex<32>;
def CurrentEL_SUB_32_HI_0 : SubRegIndex<32, 32>;
def CurrentEL_FULL_64_0    : SubRegIndex<64>;

def CurrentEL : Register<"CurrentEL">
{
let Namespace = "aarch64temp";
let AsmName = "CurrentEL";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 4 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def SPSel_SUB_32_0  : SubRegIndex<32>;
def SPSel_SUB_32_HI_0 : SubRegIndex<32, 32>;
def SPSel_FULL_64_0    : SubRegIndex<64>;

def SPSel : Register<"SPSel">
{
let Namespace = "aarch64temp";
let AsmName = "SPSel";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 5 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def SP_EL0_SUB_32_0  : SubRegIndex<32>;
def SP_EL0_SUB_32_HI_0 : SubRegIndex<32, 32>;
def SP_EL0_FULL_64_0    : SubRegIndex<64>;

def SP_EL0 : Register<"SP_EL0">
{
let Namespace = "aarch64temp";
let AsmName = "SP_EL0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 6 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def SP_EL1_SUB_32_0  : SubRegIndex<32>;
def SP_EL1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def SP_EL1_FULL_64_0    : SubRegIndex<64>;

def SP_EL1 : Register<"SP_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "SP_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 7 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def ELR_EL1_SUB_32_0  : SubRegIndex<32>;
def ELR_EL1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def ELR_EL1_FULL_64_0    : SubRegIndex<64>;

def ELR_EL1 : Register<"ELR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "ELR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 8 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def VBAR_EL1_SUB_32_0  : SubRegIndex<32>;
def VBAR_EL1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def VBAR_EL1_FULL_64_0    : SubRegIndex<64>;

def VBAR_EL1 : Register<"VBAR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "VBAR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 9 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def ESR_EL1_SUB_32_0  : SubRegIndex<32>;
def ESR_EL1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def ESR_EL1_FULL_64_0    : SubRegIndex<64>;

def ESR_EL1 : Register<"ESR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "ESR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 10 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def SPSR_EL1_SUB_32_0  : SubRegIndex<32>;
def SPSR_EL1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def SPSR_EL1_FULL_64_0    : SubRegIndex<64>;

def SPSR_EL1 : Register<"SPSR_EL1">
{
let Namespace = "aarch64temp";
let AsmName = "SPSR_EL1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 11 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def PC_SUB_32_0  : SubRegIndex<32>;
def PC_SUB_32_HI_0 : SubRegIndex<32, 32>;
def PC_FULL_64_0    : SubRegIndex<64>;

def PC : Register<"PC">
{
let Namespace = "aarch64temp";
let AsmName = "PC";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 12 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;


let HWEncoding = 0;

let isArtificial = false;
}

def R0_SUB_32_0  : SubRegIndex<32>;
def R0_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R0_FULL_64_0    : SubRegIndex<64>;

def R0 : Register<"R0">
{
let Namespace = "aarch64temp";
let AsmName = "R0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 47 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = false;
}

def R1_SUB_32_0  : SubRegIndex<32>;
def R1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R1_FULL_64_0    : SubRegIndex<64>;

def R1 : Register<"R1">
{
let Namespace = "aarch64temp";
let AsmName = "R1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 48 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = false;
}

def R2_SUB_32_0  : SubRegIndex<32>;
def R2_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R2_FULL_64_0    : SubRegIndex<64>;

def R2 : Register<"R2">
{
let Namespace = "aarch64temp";
let AsmName = "R2";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 49 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = false;
}

def R3_SUB_32_0  : SubRegIndex<32>;
def R3_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R3_FULL_64_0    : SubRegIndex<64>;

def R3 : Register<"R3">
{
let Namespace = "aarch64temp";
let AsmName = "R3";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 50 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = false;
}

def R4_SUB_32_0  : SubRegIndex<32>;
def R4_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R4_FULL_64_0    : SubRegIndex<64>;

def R4 : Register<"R4">
{
let Namespace = "aarch64temp";
let AsmName = "R4";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 51 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = false;
}

def R5_SUB_32_0  : SubRegIndex<32>;
def R5_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R5_FULL_64_0    : SubRegIndex<64>;

def R5 : Register<"R5">
{
let Namespace = "aarch64temp";
let AsmName = "R5";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 52 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = false;
}

def R6_SUB_32_0  : SubRegIndex<32>;
def R6_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R6_FULL_64_0    : SubRegIndex<64>;

def R6 : Register<"R6">
{
let Namespace = "aarch64temp";
let AsmName = "R6";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 53 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = false;
}

def R7_SUB_32_0  : SubRegIndex<32>;
def R7_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R7_FULL_64_0    : SubRegIndex<64>;

def R7 : Register<"R7">
{
let Namespace = "aarch64temp";
let AsmName = "R7";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 54 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = false;
}

def R8_SUB_32_0  : SubRegIndex<32>;
def R8_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R8_FULL_64_0    : SubRegIndex<64>;

def R8 : Register<"R8">
{
let Namespace = "aarch64temp";
let AsmName = "R8";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 55 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = false;
}

def R9_SUB_32_0  : SubRegIndex<32>;
def R9_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R9_FULL_64_0    : SubRegIndex<64>;

def R9 : Register<"R9">
{
let Namespace = "aarch64temp";
let AsmName = "R9";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 56 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = false;
}

def R10_SUB_32_0  : SubRegIndex<32>;
def R10_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R10_FULL_64_0    : SubRegIndex<64>;

def R10 : Register<"R10">
{
let Namespace = "aarch64temp";
let AsmName = "R10";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 57 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = false;
}

def R11_SUB_32_0  : SubRegIndex<32>;
def R11_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R11_FULL_64_0    : SubRegIndex<64>;

def R11 : Register<"R11">
{
let Namespace = "aarch64temp";
let AsmName = "R11";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 58 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = false;
}

def R12_SUB_32_0  : SubRegIndex<32>;
def R12_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R12_FULL_64_0    : SubRegIndex<64>;

def R12 : Register<"R12">
{
let Namespace = "aarch64temp";
let AsmName = "R12";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 59 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = false;
}

def R13_SUB_32_0  : SubRegIndex<32>;
def R13_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R13_FULL_64_0    : SubRegIndex<64>;

def R13 : Register<"R13">
{
let Namespace = "aarch64temp";
let AsmName = "R13";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 60 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = false;
}

def R14_SUB_32_0  : SubRegIndex<32>;
def R14_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R14_FULL_64_0    : SubRegIndex<64>;

def R14 : Register<"R14">
{
let Namespace = "aarch64temp";
let AsmName = "R14";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 61 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = false;
}

def R15_SUB_32_0  : SubRegIndex<32>;
def R15_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R15_FULL_64_0    : SubRegIndex<64>;

def R15 : Register<"R15">
{
let Namespace = "aarch64temp";
let AsmName = "R15";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 62 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = false;
}

def R16_SUB_32_0  : SubRegIndex<32>;
def R16_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R16_FULL_64_0    : SubRegIndex<64>;

def R16 : Register<"R16">
{
let Namespace = "aarch64temp";
let AsmName = "R16";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 63 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = false;
}

def R17_SUB_32_0  : SubRegIndex<32>;
def R17_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R17_FULL_64_0    : SubRegIndex<64>;

def R17 : Register<"R17">
{
let Namespace = "aarch64temp";
let AsmName = "R17";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 64 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = false;
}

def R18_SUB_32_0  : SubRegIndex<32>;
def R18_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R18_FULL_64_0    : SubRegIndex<64>;

def R18 : Register<"R18">
{
let Namespace = "aarch64temp";
let AsmName = "R18";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 65 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = false;
}

def R19_SUB_32_0  : SubRegIndex<32>;
def R19_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R19_FULL_64_0    : SubRegIndex<64>;

def R19 : Register<"R19">
{
let Namespace = "aarch64temp";
let AsmName = "R19";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 66 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = false;
}

def R20_SUB_32_0  : SubRegIndex<32>;
def R20_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R20_FULL_64_0    : SubRegIndex<64>;

def R20 : Register<"R20">
{
let Namespace = "aarch64temp";
let AsmName = "R20";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 67 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = false;
}

def R21_SUB_32_0  : SubRegIndex<32>;
def R21_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R21_FULL_64_0    : SubRegIndex<64>;

def R21 : Register<"R21">
{
let Namespace = "aarch64temp";
let AsmName = "R21";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 68 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = false;
}

def R22_SUB_32_0  : SubRegIndex<32>;
def R22_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R22_FULL_64_0    : SubRegIndex<64>;

def R22 : Register<"R22">
{
let Namespace = "aarch64temp";
let AsmName = "R22";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 69 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = false;
}

def R23_SUB_32_0  : SubRegIndex<32>;
def R23_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R23_FULL_64_0    : SubRegIndex<64>;

def R23 : Register<"R23">
{
let Namespace = "aarch64temp";
let AsmName = "R23";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 70 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = false;
}

def R24_SUB_32_0  : SubRegIndex<32>;
def R24_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R24_FULL_64_0    : SubRegIndex<64>;

def R24 : Register<"R24">
{
let Namespace = "aarch64temp";
let AsmName = "R24";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 71 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = false;
}

def R25_SUB_32_0  : SubRegIndex<32>;
def R25_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R25_FULL_64_0    : SubRegIndex<64>;

def R25 : Register<"R25">
{
let Namespace = "aarch64temp";
let AsmName = "R25";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 72 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = false;
}

def R26_SUB_32_0  : SubRegIndex<32>;
def R26_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R26_FULL_64_0    : SubRegIndex<64>;

def R26 : Register<"R26">
{
let Namespace = "aarch64temp";
let AsmName = "R26";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 73 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = false;
}

def R27_SUB_32_0  : SubRegIndex<32>;
def R27_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R27_FULL_64_0    : SubRegIndex<64>;

def R27 : Register<"R27">
{
let Namespace = "aarch64temp";
let AsmName = "R27";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 74 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = false;
}

def R28_SUB_32_0  : SubRegIndex<32>;
def R28_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R28_FULL_64_0    : SubRegIndex<64>;

def R28 : Register<"R28">
{
let Namespace = "aarch64temp";
let AsmName = "R28";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 75 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = false;
}

def R29_SUB_32_0  : SubRegIndex<32>;
def R29_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R29_FULL_64_0    : SubRegIndex<64>;

def R29 : Register<"R29">
{
let Namespace = "aarch64temp";
let AsmName = "R29";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 76 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = false;
}

def R30_SUB_32_0  : SubRegIndex<32>;
def R30_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R30_FULL_64_0    : SubRegIndex<64>;

def R30 : Register<"R30">
{
let Namespace = "aarch64temp";
let AsmName = "R30";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 77 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = false;
}

def R31_SUB_32_0  : SubRegIndex<32>;
def R31_SUB_32_HI_0 : SubRegIndex<32, 32>;
def R31_FULL_64_0    : SubRegIndex<64>;

def R31 : Register<"R31">
{
let Namespace = "aarch64temp";
let AsmName = "R31";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 78 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = false;
}

def X0_SUB_32_0  : SubRegIndex<32>;
def X0_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X0_FULL_64_0    : SubRegIndex<64>;

def X0 : Register<"X0">
{
let Namespace = "aarch64temp";
let AsmName = "X0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 79 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = false;
}

def X1_SUB_32_0  : SubRegIndex<32>;
def X1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X1_FULL_64_0    : SubRegIndex<64>;

def X1 : Register<"X1">
{
let Namespace = "aarch64temp";
let AsmName = "X1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 80 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = false;
}

def X2_SUB_32_0  : SubRegIndex<32>;
def X2_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X2_FULL_64_0    : SubRegIndex<64>;

def X2 : Register<"X2">
{
let Namespace = "aarch64temp";
let AsmName = "X2";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 81 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = false;
}

def X3_SUB_32_0  : SubRegIndex<32>;
def X3_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X3_FULL_64_0    : SubRegIndex<64>;

def X3 : Register<"X3">
{
let Namespace = "aarch64temp";
let AsmName = "X3";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 82 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = false;
}

def X4_SUB_32_0  : SubRegIndex<32>;
def X4_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X4_FULL_64_0    : SubRegIndex<64>;

def X4 : Register<"X4">
{
let Namespace = "aarch64temp";
let AsmName = "X4";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 83 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = false;
}

def X5_SUB_32_0  : SubRegIndex<32>;
def X5_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X5_FULL_64_0    : SubRegIndex<64>;

def X5 : Register<"X5">
{
let Namespace = "aarch64temp";
let AsmName = "X5";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 84 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = false;
}

def X6_SUB_32_0  : SubRegIndex<32>;
def X6_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X6_FULL_64_0    : SubRegIndex<64>;

def X6 : Register<"X6">
{
let Namespace = "aarch64temp";
let AsmName = "X6";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 85 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = false;
}

def X7_SUB_32_0  : SubRegIndex<32>;
def X7_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X7_FULL_64_0    : SubRegIndex<64>;

def X7 : Register<"X7">
{
let Namespace = "aarch64temp";
let AsmName = "X7";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 86 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = false;
}

def X8_SUB_32_0  : SubRegIndex<32>;
def X8_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X8_FULL_64_0    : SubRegIndex<64>;

def X8 : Register<"X8">
{
let Namespace = "aarch64temp";
let AsmName = "X8";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 87 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = false;
}

def X9_SUB_32_0  : SubRegIndex<32>;
def X9_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X9_FULL_64_0    : SubRegIndex<64>;

def X9 : Register<"X9">
{
let Namespace = "aarch64temp";
let AsmName = "X9";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 88 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = false;
}

def X10_SUB_32_0  : SubRegIndex<32>;
def X10_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X10_FULL_64_0    : SubRegIndex<64>;

def X10 : Register<"X10">
{
let Namespace = "aarch64temp";
let AsmName = "X10";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 89 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = false;
}

def X11_SUB_32_0  : SubRegIndex<32>;
def X11_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X11_FULL_64_0    : SubRegIndex<64>;

def X11 : Register<"X11">
{
let Namespace = "aarch64temp";
let AsmName = "X11";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 90 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = false;
}

def X12_SUB_32_0  : SubRegIndex<32>;
def X12_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X12_FULL_64_0    : SubRegIndex<64>;

def X12 : Register<"X12">
{
let Namespace = "aarch64temp";
let AsmName = "X12";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 91 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = false;
}

def X13_SUB_32_0  : SubRegIndex<32>;
def X13_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X13_FULL_64_0    : SubRegIndex<64>;

def X13 : Register<"X13">
{
let Namespace = "aarch64temp";
let AsmName = "X13";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 92 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = false;
}

def X14_SUB_32_0  : SubRegIndex<32>;
def X14_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X14_FULL_64_0    : SubRegIndex<64>;

def X14 : Register<"X14">
{
let Namespace = "aarch64temp";
let AsmName = "X14";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 93 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = false;
}

def X15_SUB_32_0  : SubRegIndex<32>;
def X15_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X15_FULL_64_0    : SubRegIndex<64>;

def X15 : Register<"X15">
{
let Namespace = "aarch64temp";
let AsmName = "X15";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 94 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = false;
}

def X16_SUB_32_0  : SubRegIndex<32>;
def X16_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X16_FULL_64_0    : SubRegIndex<64>;

def X16 : Register<"X16">
{
let Namespace = "aarch64temp";
let AsmName = "X16";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 95 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = false;
}

def X17_SUB_32_0  : SubRegIndex<32>;
def X17_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X17_FULL_64_0    : SubRegIndex<64>;

def X17 : Register<"X17">
{
let Namespace = "aarch64temp";
let AsmName = "X17";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 96 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = false;
}

def X18_SUB_32_0  : SubRegIndex<32>;
def X18_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X18_FULL_64_0    : SubRegIndex<64>;

def X18 : Register<"X18">
{
let Namespace = "aarch64temp";
let AsmName = "X18";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 97 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = false;
}

def X19_SUB_32_0  : SubRegIndex<32>;
def X19_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X19_FULL_64_0    : SubRegIndex<64>;

def X19 : Register<"X19">
{
let Namespace = "aarch64temp";
let AsmName = "X19";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 98 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = false;
}

def X20_SUB_32_0  : SubRegIndex<32>;
def X20_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X20_FULL_64_0    : SubRegIndex<64>;

def X20 : Register<"X20">
{
let Namespace = "aarch64temp";
let AsmName = "X20";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 99 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = false;
}

def X21_SUB_32_0  : SubRegIndex<32>;
def X21_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X21_FULL_64_0    : SubRegIndex<64>;

def X21 : Register<"X21">
{
let Namespace = "aarch64temp";
let AsmName = "X21";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 100 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = false;
}

def X22_SUB_32_0  : SubRegIndex<32>;
def X22_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X22_FULL_64_0    : SubRegIndex<64>;

def X22 : Register<"X22">
{
let Namespace = "aarch64temp";
let AsmName = "X22";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 101 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = false;
}

def X23_SUB_32_0  : SubRegIndex<32>;
def X23_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X23_FULL_64_0    : SubRegIndex<64>;

def X23 : Register<"X23">
{
let Namespace = "aarch64temp";
let AsmName = "X23";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 102 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = false;
}

def X24_SUB_32_0  : SubRegIndex<32>;
def X24_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X24_FULL_64_0    : SubRegIndex<64>;

def X24 : Register<"X24">
{
let Namespace = "aarch64temp";
let AsmName = "X24";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 103 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = false;
}

def X25_SUB_32_0  : SubRegIndex<32>;
def X25_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X25_FULL_64_0    : SubRegIndex<64>;

def X25 : Register<"X25">
{
let Namespace = "aarch64temp";
let AsmName = "X25";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 104 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = false;
}

def X26_SUB_32_0  : SubRegIndex<32>;
def X26_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X26_FULL_64_0    : SubRegIndex<64>;

def X26 : Register<"X26">
{
let Namespace = "aarch64temp";
let AsmName = "X26";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 105 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = false;
}

def X27_SUB_32_0  : SubRegIndex<32>;
def X27_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X27_FULL_64_0    : SubRegIndex<64>;

def X27 : Register<"X27">
{
let Namespace = "aarch64temp";
let AsmName = "X27";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 106 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = false;
}

def X28_SUB_32_0  : SubRegIndex<32>;
def X28_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X28_FULL_64_0    : SubRegIndex<64>;

def X28 : Register<"X28">
{
let Namespace = "aarch64temp";
let AsmName = "X28";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 107 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = false;
}

def X29_SUB_32_0  : SubRegIndex<32>;
def X29_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X29_FULL_64_0    : SubRegIndex<64>;

def X29 : Register<"X29">
{
let Namespace = "aarch64temp";
let AsmName = "X29";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 108 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = false;
}

def X30_SUB_32_0  : SubRegIndex<32>;
def X30_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X30_FULL_64_0    : SubRegIndex<64>;

def X30 : Register<"X30">
{
let Namespace = "aarch64temp";
let AsmName = "X30";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 109 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = false;
}

def X31_SUB_32_0  : SubRegIndex<32>;
def X31_SUB_32_HI_0 : SubRegIndex<32, 32>;
def X31_FULL_64_0    : SubRegIndex<64>;

def X31 : Register<"X31">
{
let Namespace = "aarch64temp";
let AsmName = "X31";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 110 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = false;
}

def W0_SUB_32_0  : SubRegIndex<32>;
def W0_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W0_FULL_64_0    : SubRegIndex<64>;

def W0 : Register<"W0">
{
let Namespace = "aarch64temp";
let AsmName = "W0";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 111 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = false;
}

def W1_SUB_32_0  : SubRegIndex<32>;
def W1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W1_FULL_64_0    : SubRegIndex<64>;

def W1 : Register<"W1">
{
let Namespace = "aarch64temp";
let AsmName = "W1";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 112 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = false;
}

def W2_SUB_32_0  : SubRegIndex<32>;
def W2_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W2_FULL_64_0    : SubRegIndex<64>;

def W2 : Register<"W2">
{
let Namespace = "aarch64temp";
let AsmName = "W2";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 113 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = false;
}

def W3_SUB_32_0  : SubRegIndex<32>;
def W3_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W3_FULL_64_0    : SubRegIndex<64>;

def W3 : Register<"W3">
{
let Namespace = "aarch64temp";
let AsmName = "W3";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 114 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = false;
}

def W4_SUB_32_0  : SubRegIndex<32>;
def W4_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W4_FULL_64_0    : SubRegIndex<64>;

def W4 : Register<"W4">
{
let Namespace = "aarch64temp";
let AsmName = "W4";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 115 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = false;
}

def W5_SUB_32_0  : SubRegIndex<32>;
def W5_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W5_FULL_64_0    : SubRegIndex<64>;

def W5 : Register<"W5">
{
let Namespace = "aarch64temp";
let AsmName = "W5";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 116 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = false;
}

def W6_SUB_32_0  : SubRegIndex<32>;
def W6_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W6_FULL_64_0    : SubRegIndex<64>;

def W6 : Register<"W6">
{
let Namespace = "aarch64temp";
let AsmName = "W6";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 117 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = false;
}

def W7_SUB_32_0  : SubRegIndex<32>;
def W7_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W7_FULL_64_0    : SubRegIndex<64>;

def W7 : Register<"W7">
{
let Namespace = "aarch64temp";
let AsmName = "W7";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 118 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = false;
}

def W8_SUB_32_0  : SubRegIndex<32>;
def W8_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W8_FULL_64_0    : SubRegIndex<64>;

def W8 : Register<"W8">
{
let Namespace = "aarch64temp";
let AsmName = "W8";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 119 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = false;
}

def W9_SUB_32_0  : SubRegIndex<32>;
def W9_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W9_FULL_64_0    : SubRegIndex<64>;

def W9 : Register<"W9">
{
let Namespace = "aarch64temp";
let AsmName = "W9";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 120 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = false;
}

def W10_SUB_32_0  : SubRegIndex<32>;
def W10_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W10_FULL_64_0    : SubRegIndex<64>;

def W10 : Register<"W10">
{
let Namespace = "aarch64temp";
let AsmName = "W10";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 121 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = false;
}

def W11_SUB_32_0  : SubRegIndex<32>;
def W11_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W11_FULL_64_0    : SubRegIndex<64>;

def W11 : Register<"W11">
{
let Namespace = "aarch64temp";
let AsmName = "W11";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 122 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = false;
}

def W12_SUB_32_0  : SubRegIndex<32>;
def W12_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W12_FULL_64_0    : SubRegIndex<64>;

def W12 : Register<"W12">
{
let Namespace = "aarch64temp";
let AsmName = "W12";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 123 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = false;
}

def W13_SUB_32_0  : SubRegIndex<32>;
def W13_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W13_FULL_64_0    : SubRegIndex<64>;

def W13 : Register<"W13">
{
let Namespace = "aarch64temp";
let AsmName = "W13";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 124 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = false;
}

def W14_SUB_32_0  : SubRegIndex<32>;
def W14_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W14_FULL_64_0    : SubRegIndex<64>;

def W14 : Register<"W14">
{
let Namespace = "aarch64temp";
let AsmName = "W14";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 125 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = false;
}

def W15_SUB_32_0  : SubRegIndex<32>;
def W15_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W15_FULL_64_0    : SubRegIndex<64>;

def W15 : Register<"W15">
{
let Namespace = "aarch64temp";
let AsmName = "W15";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 126 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = false;
}

def W16_SUB_32_0  : SubRegIndex<32>;
def W16_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W16_FULL_64_0    : SubRegIndex<64>;

def W16 : Register<"W16">
{
let Namespace = "aarch64temp";
let AsmName = "W16";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 127 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = false;
}

def W17_SUB_32_0  : SubRegIndex<32>;
def W17_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W17_FULL_64_0    : SubRegIndex<64>;

def W17 : Register<"W17">
{
let Namespace = "aarch64temp";
let AsmName = "W17";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 128 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = false;
}

def W18_SUB_32_0  : SubRegIndex<32>;
def W18_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W18_FULL_64_0    : SubRegIndex<64>;

def W18 : Register<"W18">
{
let Namespace = "aarch64temp";
let AsmName = "W18";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 129 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = false;
}

def W19_SUB_32_0  : SubRegIndex<32>;
def W19_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W19_FULL_64_0    : SubRegIndex<64>;

def W19 : Register<"W19">
{
let Namespace = "aarch64temp";
let AsmName = "W19";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 130 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = false;
}

def W20_SUB_32_0  : SubRegIndex<32>;
def W20_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W20_FULL_64_0    : SubRegIndex<64>;

def W20 : Register<"W20">
{
let Namespace = "aarch64temp";
let AsmName = "W20";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 131 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = false;
}

def W21_SUB_32_0  : SubRegIndex<32>;
def W21_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W21_FULL_64_0    : SubRegIndex<64>;

def W21 : Register<"W21">
{
let Namespace = "aarch64temp";
let AsmName = "W21";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 132 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = false;
}

def W22_SUB_32_0  : SubRegIndex<32>;
def W22_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W22_FULL_64_0    : SubRegIndex<64>;

def W22 : Register<"W22">
{
let Namespace = "aarch64temp";
let AsmName = "W22";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 133 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = false;
}

def W23_SUB_32_0  : SubRegIndex<32>;
def W23_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W23_FULL_64_0    : SubRegIndex<64>;

def W23 : Register<"W23">
{
let Namespace = "aarch64temp";
let AsmName = "W23";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 134 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = false;
}

def W24_SUB_32_0  : SubRegIndex<32>;
def W24_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W24_FULL_64_0    : SubRegIndex<64>;

def W24 : Register<"W24">
{
let Namespace = "aarch64temp";
let AsmName = "W24";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 135 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = false;
}

def W25_SUB_32_0  : SubRegIndex<32>;
def W25_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W25_FULL_64_0    : SubRegIndex<64>;

def W25 : Register<"W25">
{
let Namespace = "aarch64temp";
let AsmName = "W25";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 136 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = false;
}

def W26_SUB_32_0  : SubRegIndex<32>;
def W26_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W26_FULL_64_0    : SubRegIndex<64>;

def W26 : Register<"W26">
{
let Namespace = "aarch64temp";
let AsmName = "W26";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 137 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = false;
}

def W27_SUB_32_0  : SubRegIndex<32>;
def W27_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W27_FULL_64_0    : SubRegIndex<64>;

def W27 : Register<"W27">
{
let Namespace = "aarch64temp";
let AsmName = "W27";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 138 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = false;
}

def W28_SUB_32_0  : SubRegIndex<32>;
def W28_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W28_FULL_64_0    : SubRegIndex<64>;

def W28 : Register<"W28">
{
let Namespace = "aarch64temp";
let AsmName = "W28";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 139 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = false;
}

def W29_SUB_32_0  : SubRegIndex<32>;
def W29_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W29_FULL_64_0    : SubRegIndex<64>;

def W29 : Register<"W29">
{
let Namespace = "aarch64temp";
let AsmName = "W29";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 140 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = false;
}

def W30_SUB_32_0  : SubRegIndex<32>;
def W30_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W30_FULL_64_0    : SubRegIndex<64>;

def W30 : Register<"W30">
{
let Namespace = "aarch64temp";
let AsmName = "W30";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 141 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = false;
}

def W31_SUB_32_0  : SubRegIndex<32>;
def W31_SUB_32_HI_0 : SubRegIndex<32, 32>;
def W31_FULL_64_0    : SubRegIndex<64>;

def W31 : Register<"W31">
{
let Namespace = "aarch64temp";
let AsmName = "W31";
let AltNames = [   ];
let Aliases = [ ];
let SubRegs = [  ];
let SubRegIndices = [  ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 142 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = false;
}

def S0_SUB_32_0  : SubRegIndex<32>;
def S0_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S0_FULL_64_0    : SubRegIndex<64>;
def S0_SUB_32_1  : SubRegIndex<32>;
def S0_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S0_FULL_64_1    : SubRegIndex<64>;

def S0 : Register<"S0">
{
let Namespace = "aarch64temp";
let AsmName = "X0";
let AltNames = [ "X0"  ];
let Aliases = [ ];
let SubRegs = [ R0, X0, W0 ];
let SubRegIndices = [ S0_SUB_32_0, S0_FULL_64_0, S0_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 15 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 0;


let isArtificial = false;
}

def S1_SUB_32_0  : SubRegIndex<32>;
def S1_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S1_FULL_64_0    : SubRegIndex<64>;
def S1_SUB_32_1  : SubRegIndex<32>;
def S1_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S1_FULL_64_1    : SubRegIndex<64>;

def S1 : Register<"S1">
{
let Namespace = "aarch64temp";
let AsmName = "X1";
let AltNames = [ "X1"  ];
let Aliases = [ ];
let SubRegs = [ R1, X1, W1 ];
let SubRegIndices = [ S1_SUB_32_0, S1_FULL_64_0, S1_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 16 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 1;


let isArtificial = false;
}

def S2_SUB_32_0  : SubRegIndex<32>;
def S2_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S2_FULL_64_0    : SubRegIndex<64>;
def S2_SUB_32_1  : SubRegIndex<32>;
def S2_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S2_FULL_64_1    : SubRegIndex<64>;

def S2 : Register<"S2">
{
let Namespace = "aarch64temp";
let AsmName = "X2";
let AltNames = [ "X2"  ];
let Aliases = [ ];
let SubRegs = [ R2, X2, W2 ];
let SubRegIndices = [ S2_SUB_32_0, S2_FULL_64_0, S2_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 17 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 2;


let isArtificial = false;
}

def S3_SUB_32_0  : SubRegIndex<32>;
def S3_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S3_FULL_64_0    : SubRegIndex<64>;
def S3_SUB_32_1  : SubRegIndex<32>;
def S3_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S3_FULL_64_1    : SubRegIndex<64>;

def S3 : Register<"S3">
{
let Namespace = "aarch64temp";
let AsmName = "X3";
let AltNames = [ "X3"  ];
let Aliases = [ ];
let SubRegs = [ R3, X3, W3 ];
let SubRegIndices = [ S3_SUB_32_0, S3_FULL_64_0, S3_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 18 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 3;


let isArtificial = false;
}

def S4_SUB_32_0  : SubRegIndex<32>;
def S4_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S4_FULL_64_0    : SubRegIndex<64>;
def S4_SUB_32_1  : SubRegIndex<32>;
def S4_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S4_FULL_64_1    : SubRegIndex<64>;

def S4 : Register<"S4">
{
let Namespace = "aarch64temp";
let AsmName = "X4";
let AltNames = [ "X4"  ];
let Aliases = [ ];
let SubRegs = [ R4, X4, W4 ];
let SubRegIndices = [ S4_SUB_32_0, S4_FULL_64_0, S4_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 19 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 4;


let isArtificial = false;
}

def S5_SUB_32_0  : SubRegIndex<32>;
def S5_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S5_FULL_64_0    : SubRegIndex<64>;
def S5_SUB_32_1  : SubRegIndex<32>;
def S5_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S5_FULL_64_1    : SubRegIndex<64>;

def S5 : Register<"S5">
{
let Namespace = "aarch64temp";
let AsmName = "X5";
let AltNames = [ "X5"  ];
let Aliases = [ ];
let SubRegs = [ R5, X5, W5 ];
let SubRegIndices = [ S5_SUB_32_0, S5_FULL_64_0, S5_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 20 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 5;


let isArtificial = false;
}

def S6_SUB_32_0  : SubRegIndex<32>;
def S6_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S6_FULL_64_0    : SubRegIndex<64>;
def S6_SUB_32_1  : SubRegIndex<32>;
def S6_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S6_FULL_64_1    : SubRegIndex<64>;

def S6 : Register<"S6">
{
let Namespace = "aarch64temp";
let AsmName = "X6";
let AltNames = [ "X6"  ];
let Aliases = [ ];
let SubRegs = [ R6, X6, W6 ];
let SubRegIndices = [ S6_SUB_32_0, S6_FULL_64_0, S6_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 21 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 6;


let isArtificial = false;
}

def S7_SUB_32_0  : SubRegIndex<32>;
def S7_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S7_FULL_64_0    : SubRegIndex<64>;
def S7_SUB_32_1  : SubRegIndex<32>;
def S7_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S7_FULL_64_1    : SubRegIndex<64>;

def S7 : Register<"S7">
{
let Namespace = "aarch64temp";
let AsmName = "X7";
let AltNames = [ "X7"  ];
let Aliases = [ ];
let SubRegs = [ R7, X7, W7 ];
let SubRegIndices = [ S7_SUB_32_0, S7_FULL_64_0, S7_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 22 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 7;


let isArtificial = false;
}

def S8_SUB_32_0  : SubRegIndex<32>;
def S8_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S8_FULL_64_0    : SubRegIndex<64>;
def S8_SUB_32_1  : SubRegIndex<32>;
def S8_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S8_FULL_64_1    : SubRegIndex<64>;

def S8 : Register<"S8">
{
let Namespace = "aarch64temp";
let AsmName = "X8";
let AltNames = [ "X8"  ];
let Aliases = [ ];
let SubRegs = [ R8, X8, W8 ];
let SubRegIndices = [ S8_SUB_32_0, S8_FULL_64_0, S8_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 23 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 8;


let isArtificial = false;
}

def S9_SUB_32_0  : SubRegIndex<32>;
def S9_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S9_FULL_64_0    : SubRegIndex<64>;
def S9_SUB_32_1  : SubRegIndex<32>;
def S9_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S9_FULL_64_1    : SubRegIndex<64>;

def S9 : Register<"S9">
{
let Namespace = "aarch64temp";
let AsmName = "X9";
let AltNames = [ "X9"  ];
let Aliases = [ ];
let SubRegs = [ R9, X9, W9 ];
let SubRegIndices = [ S9_SUB_32_0, S9_FULL_64_0, S9_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 24 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 9;


let isArtificial = false;
}

def S10_SUB_32_0  : SubRegIndex<32>;
def S10_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S10_FULL_64_0    : SubRegIndex<64>;
def S10_SUB_32_1  : SubRegIndex<32>;
def S10_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S10_FULL_64_1    : SubRegIndex<64>;

def S10 : Register<"S10">
{
let Namespace = "aarch64temp";
let AsmName = "X10";
let AltNames = [ "X10"  ];
let Aliases = [ ];
let SubRegs = [ R10, X10, W10 ];
let SubRegIndices = [ S10_SUB_32_0, S10_FULL_64_0, S10_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 25 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 10;


let isArtificial = false;
}

def S11_SUB_32_0  : SubRegIndex<32>;
def S11_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S11_FULL_64_0    : SubRegIndex<64>;
def S11_SUB_32_1  : SubRegIndex<32>;
def S11_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S11_FULL_64_1    : SubRegIndex<64>;

def S11 : Register<"S11">
{
let Namespace = "aarch64temp";
let AsmName = "X11";
let AltNames = [ "X11"  ];
let Aliases = [ ];
let SubRegs = [ R11, X11, W11 ];
let SubRegIndices = [ S11_SUB_32_0, S11_FULL_64_0, S11_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 26 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 11;


let isArtificial = false;
}

def S12_SUB_32_0  : SubRegIndex<32>;
def S12_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S12_FULL_64_0    : SubRegIndex<64>;
def S12_SUB_32_1  : SubRegIndex<32>;
def S12_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S12_FULL_64_1    : SubRegIndex<64>;

def S12 : Register<"S12">
{
let Namespace = "aarch64temp";
let AsmName = "X12";
let AltNames = [ "X12"  ];
let Aliases = [ ];
let SubRegs = [ R12, X12, W12 ];
let SubRegIndices = [ S12_SUB_32_0, S12_FULL_64_0, S12_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 27 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 12;


let isArtificial = false;
}

def S13_SUB_32_0  : SubRegIndex<32>;
def S13_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S13_FULL_64_0    : SubRegIndex<64>;
def S13_SUB_32_1  : SubRegIndex<32>;
def S13_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S13_FULL_64_1    : SubRegIndex<64>;

def S13 : Register<"S13">
{
let Namespace = "aarch64temp";
let AsmName = "X13";
let AltNames = [ "X13"  ];
let Aliases = [ ];
let SubRegs = [ R13, X13, W13 ];
let SubRegIndices = [ S13_SUB_32_0, S13_FULL_64_0, S13_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 28 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 13;


let isArtificial = false;
}

def S14_SUB_32_0  : SubRegIndex<32>;
def S14_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S14_FULL_64_0    : SubRegIndex<64>;
def S14_SUB_32_1  : SubRegIndex<32>;
def S14_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S14_FULL_64_1    : SubRegIndex<64>;

def S14 : Register<"S14">
{
let Namespace = "aarch64temp";
let AsmName = "X14";
let AltNames = [ "X14"  ];
let Aliases = [ ];
let SubRegs = [ R14, X14, W14 ];
let SubRegIndices = [ S14_SUB_32_0, S14_FULL_64_0, S14_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 29 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 14;


let isArtificial = false;
}

def S15_SUB_32_0  : SubRegIndex<32>;
def S15_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S15_FULL_64_0    : SubRegIndex<64>;
def S15_SUB_32_1  : SubRegIndex<32>;
def S15_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S15_FULL_64_1    : SubRegIndex<64>;

def S15 : Register<"S15">
{
let Namespace = "aarch64temp";
let AsmName = "X15";
let AltNames = [ "X15"  ];
let Aliases = [ ];
let SubRegs = [ R15, X15, W15 ];
let SubRegIndices = [ S15_SUB_32_0, S15_FULL_64_0, S15_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 30 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 15;


let isArtificial = false;
}

def S16_SUB_32_0  : SubRegIndex<32>;
def S16_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S16_FULL_64_0    : SubRegIndex<64>;
def S16_SUB_32_1  : SubRegIndex<32>;
def S16_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S16_FULL_64_1    : SubRegIndex<64>;

def S16 : Register<"S16">
{
let Namespace = "aarch64temp";
let AsmName = "X16";
let AltNames = [ "X16"  ];
let Aliases = [ ];
let SubRegs = [ R16, X16, W16 ];
let SubRegIndices = [ S16_SUB_32_0, S16_FULL_64_0, S16_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 31 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 16;


let isArtificial = false;
}

def S17_SUB_32_0  : SubRegIndex<32>;
def S17_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S17_FULL_64_0    : SubRegIndex<64>;
def S17_SUB_32_1  : SubRegIndex<32>;
def S17_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S17_FULL_64_1    : SubRegIndex<64>;

def S17 : Register<"S17">
{
let Namespace = "aarch64temp";
let AsmName = "X17";
let AltNames = [ "X17"  ];
let Aliases = [ ];
let SubRegs = [ R17, X17, W17 ];
let SubRegIndices = [ S17_SUB_32_0, S17_FULL_64_0, S17_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 32 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 17;


let isArtificial = false;
}

def S18_SUB_32_0  : SubRegIndex<32>;
def S18_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S18_FULL_64_0    : SubRegIndex<64>;
def S18_SUB_32_1  : SubRegIndex<32>;
def S18_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S18_FULL_64_1    : SubRegIndex<64>;

def S18 : Register<"S18">
{
let Namespace = "aarch64temp";
let AsmName = "X18";
let AltNames = [ "X18"  ];
let Aliases = [ ];
let SubRegs = [ R18, X18, W18 ];
let SubRegIndices = [ S18_SUB_32_0, S18_FULL_64_0, S18_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 33 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 18;


let isArtificial = false;
}

def S19_SUB_32_0  : SubRegIndex<32>;
def S19_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S19_FULL_64_0    : SubRegIndex<64>;
def S19_SUB_32_1  : SubRegIndex<32>;
def S19_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S19_FULL_64_1    : SubRegIndex<64>;

def S19 : Register<"S19">
{
let Namespace = "aarch64temp";
let AsmName = "X19";
let AltNames = [ "X19"  ];
let Aliases = [ ];
let SubRegs = [ R19, X19, W19 ];
let SubRegIndices = [ S19_SUB_32_0, S19_FULL_64_0, S19_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 34 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 19;


let isArtificial = false;
}

def S20_SUB_32_0  : SubRegIndex<32>;
def S20_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S20_FULL_64_0    : SubRegIndex<64>;
def S20_SUB_32_1  : SubRegIndex<32>;
def S20_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S20_FULL_64_1    : SubRegIndex<64>;

def S20 : Register<"S20">
{
let Namespace = "aarch64temp";
let AsmName = "X20";
let AltNames = [ "X20"  ];
let Aliases = [ ];
let SubRegs = [ R20, X20, W20 ];
let SubRegIndices = [ S20_SUB_32_0, S20_FULL_64_0, S20_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 35 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 20;


let isArtificial = false;
}

def S21_SUB_32_0  : SubRegIndex<32>;
def S21_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S21_FULL_64_0    : SubRegIndex<64>;
def S21_SUB_32_1  : SubRegIndex<32>;
def S21_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S21_FULL_64_1    : SubRegIndex<64>;

def S21 : Register<"S21">
{
let Namespace = "aarch64temp";
let AsmName = "X21";
let AltNames = [ "X21"  ];
let Aliases = [ ];
let SubRegs = [ R21, X21, W21 ];
let SubRegIndices = [ S21_SUB_32_0, S21_FULL_64_0, S21_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 36 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 21;


let isArtificial = false;
}

def S22_SUB_32_0  : SubRegIndex<32>;
def S22_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S22_FULL_64_0    : SubRegIndex<64>;
def S22_SUB_32_1  : SubRegIndex<32>;
def S22_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S22_FULL_64_1    : SubRegIndex<64>;

def S22 : Register<"S22">
{
let Namespace = "aarch64temp";
let AsmName = "X22";
let AltNames = [ "X22"  ];
let Aliases = [ ];
let SubRegs = [ R22, X22, W22 ];
let SubRegIndices = [ S22_SUB_32_0, S22_FULL_64_0, S22_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 37 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 22;


let isArtificial = false;
}

def S23_SUB_32_0  : SubRegIndex<32>;
def S23_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S23_FULL_64_0    : SubRegIndex<64>;
def S23_SUB_32_1  : SubRegIndex<32>;
def S23_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S23_FULL_64_1    : SubRegIndex<64>;

def S23 : Register<"S23">
{
let Namespace = "aarch64temp";
let AsmName = "X23";
let AltNames = [ "X23"  ];
let Aliases = [ ];
let SubRegs = [ R23, X23, W23 ];
let SubRegIndices = [ S23_SUB_32_0, S23_FULL_64_0, S23_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 38 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 23;


let isArtificial = false;
}

def S24_SUB_32_0  : SubRegIndex<32>;
def S24_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S24_FULL_64_0    : SubRegIndex<64>;
def S24_SUB_32_1  : SubRegIndex<32>;
def S24_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S24_FULL_64_1    : SubRegIndex<64>;

def S24 : Register<"S24">
{
let Namespace = "aarch64temp";
let AsmName = "X24";
let AltNames = [ "X24"  ];
let Aliases = [ ];
let SubRegs = [ R24, X24, W24 ];
let SubRegIndices = [ S24_SUB_32_0, S24_FULL_64_0, S24_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 39 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 24;


let isArtificial = false;
}

def S25_SUB_32_0  : SubRegIndex<32>;
def S25_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S25_FULL_64_0    : SubRegIndex<64>;
def S25_SUB_32_1  : SubRegIndex<32>;
def S25_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S25_FULL_64_1    : SubRegIndex<64>;

def S25 : Register<"S25">
{
let Namespace = "aarch64temp";
let AsmName = "X25";
let AltNames = [ "X25"  ];
let Aliases = [ ];
let SubRegs = [ R25, X25, W25 ];
let SubRegIndices = [ S25_SUB_32_0, S25_FULL_64_0, S25_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 40 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 25;


let isArtificial = false;
}

def S26_SUB_32_0  : SubRegIndex<32>;
def S26_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S26_FULL_64_0    : SubRegIndex<64>;
def S26_SUB_32_1  : SubRegIndex<32>;
def S26_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S26_FULL_64_1    : SubRegIndex<64>;

def S26 : Register<"S26">
{
let Namespace = "aarch64temp";
let AsmName = "X26";
let AltNames = [ "X26"  ];
let Aliases = [ ];
let SubRegs = [ R26, X26, W26 ];
let SubRegIndices = [ S26_SUB_32_0, S26_FULL_64_0, S26_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 41 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 26;


let isArtificial = false;
}

def S27_SUB_32_0  : SubRegIndex<32>;
def S27_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S27_FULL_64_0    : SubRegIndex<64>;
def S27_SUB_32_1  : SubRegIndex<32>;
def S27_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S27_FULL_64_1    : SubRegIndex<64>;

def S27 : Register<"S27">
{
let Namespace = "aarch64temp";
let AsmName = "X27";
let AltNames = [ "X27"  ];
let Aliases = [ ];
let SubRegs = [ R27, X27, W27 ];
let SubRegIndices = [ S27_SUB_32_0, S27_FULL_64_0, S27_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 42 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 27;


let isArtificial = false;
}

def S28_SUB_32_0  : SubRegIndex<32>;
def S28_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S28_FULL_64_0    : SubRegIndex<64>;
def S28_SUB_32_1  : SubRegIndex<32>;
def S28_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S28_FULL_64_1    : SubRegIndex<64>;

def S28 : Register<"S28">
{
let Namespace = "aarch64temp";
let AsmName = "X28";
let AltNames = [ "X28"  ];
let Aliases = [ ];
let SubRegs = [ R28, X28, W28 ];
let SubRegIndices = [ S28_SUB_32_0, S28_FULL_64_0, S28_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 43 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 28;


let isArtificial = false;
}

def S29_SUB_32_0  : SubRegIndex<32>;
def S29_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S29_FULL_64_0    : SubRegIndex<64>;
def S29_SUB_32_1  : SubRegIndex<32>;
def S29_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S29_FULL_64_1    : SubRegIndex<64>;

def S29 : Register<"S29">
{
let Namespace = "aarch64temp";
let AsmName = "A_FP";
let AltNames = [ "A_FP"  ];
let Aliases = [ ];
let SubRegs = [ R29, X29, W29 ];
let SubRegIndices = [ S29_SUB_32_0, S29_FULL_64_0, S29_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 44 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 29;


let isArtificial = false;
}

def S30_SUB_32_0  : SubRegIndex<32>;
def S30_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S30_FULL_64_0    : SubRegIndex<64>;
def S30_SUB_32_1  : SubRegIndex<32>;
def S30_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S30_FULL_64_1    : SubRegIndex<64>;

def S30 : Register<"S30">
{
let Namespace = "aarch64temp";
let AsmName = "A_LR";
let AltNames = [ "A_LR"  ];
let Aliases = [ ];
let SubRegs = [ R30, X30, W30 ];
let SubRegIndices = [ S30_SUB_32_0, S30_FULL_64_0, S30_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 45 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 30;


let isArtificial = false;
}

def S31_SUB_32_0  : SubRegIndex<32>;
def S31_SUB_32_HI_0 : SubRegIndex<32, 32>;
def S31_FULL_64_0    : SubRegIndex<64>;
def S31_SUB_32_1  : SubRegIndex<32>;
def S31_SUB_32_HI_1 : SubRegIndex<32, 32>;
def S31_FULL_64_1    : SubRegIndex<64>;

def S31 : Register<"S31">
{
let Namespace = "aarch64temp";
let AsmName = "A_SP";
let AltNames = [ "A_SP"  ];
let Aliases = [ ];
let SubRegs = [ R31, X31, W31 ];
let SubRegIndices = [ S31_SUB_32_0, S31_FULL_64_0, S31_SUB_32_1 ];
let RegAltNameIndices = [];
let DwarfNumbers = [ 46 ];
list<int> CostPerUse = [0];
let CoveredBySubRegs = 0;

let HWEncoding{4-0} = 31;


let isArtificial = false;
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



def R : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i32 ]
, /* alignment = */ 32
, /* regList   = */
( add R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15, R16, R17, R18, R19, R20, R21, R22, R23, R24, R25, R26, R27, R28, R29, R30, R31 )
> {
}
def X : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i64 ]
, /* alignment = */ 32
, /* regList   = */
( add X0, X1, X2, X3, X4, X5, X6, X7, X8, X9, X10, X11, X12, X13, X14, X15, X16, X17, X18, X19, X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X30, X31 )
> {
}
def W : RegisterClass
< /* namespace = */ "aarch64temp"
, /* regTypes  = */  [  i32 ]
, /* alignment = */ 32
, /* regList   = */
( add W0, W1, W2, W3, W4, W5, W6, W7, W8, W9, W10, W11, W12, W13, W14, W15, W16, W17, W18, W19, W20, W21, W22, W23, W24, W25, W26, W27, W28, W29, W30, W31 )
> {
}