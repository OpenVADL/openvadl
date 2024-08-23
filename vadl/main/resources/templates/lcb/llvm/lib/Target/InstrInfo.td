/*
 * Node representing the frame index.
 * The method CPUDAGToDAGISel::SelectAddrFI is used to determine
 * if the specific register is a frame pointer.
 */
def AddrFI : ComplexPattern<iPtr, 1, "SelectAddrFI", [frameindex], []>;

[# th:each="imm : ${immediates}" ]
[(${imm})]
[/]

[# th:each="instruction : ${instructions}" ]
[(${instruction})]
[/]