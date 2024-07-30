╔═ astDumpTests/resources/dumps/mini.vadl.dump ═╗
InstructionSetDefinition
. Identifier name: "RV3264I"
. ConstantDefinition
. : Identifier name: "Arch32"
. : IntegerLiteral literal: 33 (33)
. ConstantDefinition
. : Identifier name: "MLen"
. : IdentifierChain
. : ' Identifier name: "Arch32"
. RegisterFileDefinition
. : Identifier name: "X"
. : TypeLiteral
. : ' Identifier name: "Bits"
. : ' IntegerLiteral literal: 5 (5)
. : TypeLiteral
. : ' Identifier name: "Bits"
. : ' IdentifierChain
. : ' | Identifier name: "MLen"
. FormatDefinition
. : TypedFormatField
. : ' Identifier name: "funct7"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 7 (7)
. : TypedFormatField
. : ' Identifier name: "rs2"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "rs1"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "funct3"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 3 (3)
. : TypedFormatField
. : ' Identifier name: "rd"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "opcode"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 7 (7)
. InstructionDefinition
. : Identifier name: "ADD"
. : Identifier name: "Rtype"
. : BlockStatement
. : ' AssignmentStatement
. : ' | Target:
. : ' | CallExpr
. : ' | . Identifier name: "X"
. : ' | . IdentifierChain
. : ' | . : Identifier name: "rd"
. : ' | Value:. : ' | BinaryExpr operator: +
. : ' | . CallExpr
. : ' | . : Identifier name: "X"
. : ' | . : IdentifierChain
. : ' | . : ' Identifier name: "rs1"
. : ' | . CallExpr
. : ' | . : Identifier name: "X"
. : ' | . : IdentifierChain
. : ' | . : ' Identifier name: "rs2"

╔═ astDumpTests/resources/dumps/mini2.vadl.dump ═╗
InstructionSetDefinition
. Identifier name: "RV3264I"
. ConstantDefinition
. : Identifier name: "Arch32"
. : IntegerLiteral literal: 33 (33)
. ConstantDefinition
. : Identifier name: "MLen"
. : IdentifierChain
. : ' Identifier name: "Arch32"
. RegisterFileDefinition
. : Identifier name: "X"
. : TypeLiteral
. : ' Identifier name: "Bits"
. : ' IntegerLiteral literal: 5 (5)
. : TypeLiteral
. : ' Identifier name: "Bits"
. : ' IdentifierChain
. : ' | Identifier name: "MLen"
. FormatDefinition
. : TypedFormatField
. : ' Identifier name: "funct7"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 7 (7)
. : TypedFormatField
. : ' Identifier name: "rs2"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "rs1"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "funct3"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 3 (3)
. : TypedFormatField
. : ' Identifier name: "rd"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 5 (5)
. : TypedFormatField
. : ' Identifier name: "opcode"
. : ' TypeLiteral
. : ' | Identifier name: "Bits"
. : ' | IntegerLiteral literal: 7 (7)
. InstructionDefinition
. : Identifier name: "ADD"
. : Identifier name: "Rtype"
. : BlockStatement
. : ' AssignmentStatement
. : ' | Target:
. : ' | CallExpr
. : ' | . Identifier name: "X"
. : ' | . IdentifierChain
. : ' | . : Identifier name: "rd"
. : ' | Value:. : ' | BinaryExpr operator: +
. : ' | . CallExpr
. : ' | . : Identifier name: "X"
. : ' | . : IdentifierChain
. : ' | . : ' Identifier name: "rs1"
. : ' | . CallExpr
. : ' | . : Identifier name: "X"
. : ' | . : IdentifierChain
. : ' | . : ' Identifier name: "rs2"

╔═ [end of file] ═╗
