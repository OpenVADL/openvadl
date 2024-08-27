/*
 * Node representing the frame index.
 * The method CPUDAGToDAGISel::SelectAddrFI is used to determine
 * if the specific register is a frame pointer.
 */
def AddrFI : ComplexPattern<iPtr, 1, "SelectAddrFI", [frameindex], []>;

def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, i32>, SDTCisVT<1, i32>]>;
def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, i32>, SDTCisVT<1, i32>]>;

[# th:each="imm : ${immediates}" ]
[(${imm})]
[/]

[# th:each="instruction : ${instructions}" ]
[(${instruction})]
[/]