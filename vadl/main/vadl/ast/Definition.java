// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;
import vadl.viam.PseudoInstruction;
import vadl.viam.asm.AsmToken;

/**
 * The Definition nodes inside the AST.
 * A definition defines part of the architecture, but has no side effects and doesn't evaluate to
 * anything.
 */
abstract class Definition extends Node {
  Annotations annotations = new Annotations();

  @LazyInit
  String viamId;

  Definition withAnnotations(Annotations annotations) {
    this.annotations = annotations;
    return this;
  }

  abstract <R> R accept(DefinitionVisitor<R> visitor);
}

interface DefinitionVisitor<R> {
  R visit(AbiSequenceDefinition definition);

  R visit(AliasDefinition definition);

  R visit(ApplicationBinaryInterfaceDefinition definition);

  R visit(AsmDescriptionDefinition definition);

  R visit(AsmDirectiveDefinition definition);

  R visit(AsmGrammarAlternativesDefinition definition);

  R visit(AsmGrammarElementDefinition definition);

  R visit(AsmGrammarLiteralDefinition definition);

  R visit(AsmGrammarLocalVarDefinition definition);

  R visit(AsmGrammarRuleDefinition definition);

  R visit(AsmGrammarTypeDefinition definition);

  R visit(AsmModifierDefinition definition);

  R visit(AssemblyDefinition definition);

  R visit(CacheDefinition definition);

  R visit(ConstantDefinition definition);

  R visit(CounterDefinition definition);

  R visit(CpuFunctionDefinition definition);

  R visit(CpuProcessDefinition definition);

  R visit(DefinitionList definition);

  R visit(EncodingDefinition definition);

  R visit(EnumerationDefinition definition);

  R visit(ExceptionDefinition definition);

  R visit(FormatDefinition definition);

  R visit(FunctionDefinition definition);

  R visit(GroupDefinition definition);

  R visit(ImportDefinition definition);

  R visit(InstructionDefinition definition);

  R visit(InstructionSetDefinition definition);

  R visit(LogicDefinition definition);

  R visit(MacroInstanceDefinition definition);

  R visit(MacroInstructionDefinition definition);

  R visit(MacroMatchDefinition definition);

  R visit(MemoryDefinition definition);

  R visit(MicroArchitectureDefinition definition);

  R visit(MicroProcessorDefinition definition);

  R visit(ModelDefinition definition);

  R visit(ModelTypeDefinition definition);

  R visit(OperationDefinition definition);

  R visit(PatchDefinition definition);

  R visit(PipelineDefinition definition);

  R visit(PlaceholderDefinition definition);

  R visit(PortBehaviorDefinition definition);

  R visit(ProcessDefinition definition);

  R visit(PseudoInstructionDefinition definition);

  R visit(RecordTypeDefinition definition);

  R visit(RegisterDefinition definition);

  R visit(RegisterFileDefinition definition);

  R visit(RelocationDefinition definition);

  R visit(SignalDefinition definition);

  R visit(SourceDefinition definition);

  R visit(SpecialPurposeRegisterDefinition definition);

  R visit(StageDefinition definition);

  R visit(UsingDefinition definition);

  R visit(AbiPseudoInstructionDefinition abiPseudoInstructionDefinition);
}

/**
 * A common parameter type that corresponds to the {@code parameter} grammar rule.
 *
 * <p>name The declared name of this parameter.
 * type The declared type of this parameter.
 */
class Parameter extends Node implements IdentifiableNode, TypedNode {
  Identifier name;
  TypeLiteral typeLiteral;

  public Parameter(Identifier name, TypeLiteral typeLiteral) {
    this.name = name;
    this.typeLiteral = typeLiteral;
  }

  @Override
  public Identifier identifier() {
    return name;
  }

  @Override
  SourceLocation location() {
    return name.location().join(typeLiteral.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    name.prettyPrint(indent, builder);
    builder.append(" : ");
    typeLiteral.prettyPrint(indent, builder);
  }

  static void prettyPrintMultiple(int indent, List<Parameter> parameters, StringBuilder builder) {
    if (parameters.isEmpty()) {
      return;
    }

    builder.append("(");
    for (int i = 0; i < parameters.size(); i++) {
      if (i != 0) {
        builder.append(", ");
      }
      parameters.get(i).prettyPrint(indent, builder);
    }
    builder.append(")");

  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Parameter parameter = (Parameter) o;
    return name.equals(parameter.name) && typeLiteral.equals(parameter.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public Type type() {
    return typeLiteral.type();
  }
}

class ConstantDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;

  @Nullable
  TypeLiteral typeLiteral;

  Expr value;
  SourceLocation loc;

  ConstantDefinition(IdentifierOrPlaceholder identifier, @Nullable TypeLiteral typeLiteral,
                     Expr value,
                     SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.value = value;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("constant %s".formatted(identifier().name));
    if (typeLiteral != null) {
      builder.append(": ");
      typeLiteral.prettyPrint(indent, builder);
    }
    if (isBlockLayout(value)) {
      builder.append(" =\n");
    } else {
      builder.append(" = ");
    }
    value.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s".formatted(this.getClass().getSimpleName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantDefinition that = (ConstantDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(typeLiteral, that.typeLiteral)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeLiteral);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }

  @Override
  public Type type() {
    return value.type();
  }
}

class FormatDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  TypeLiteral typeLiteral;
  List<FormatField> fields;
  List<AuxiliaryField> auxiliaryFields;
  SourceLocation loc;

  @Override
  public Type type() {
    return new FormatType(this);
  }

  record BitRange(int from, int to) {
  }

  interface FormatField extends WithSourceLocation {
    Identifier identifier();

    void prettyPrint(int indent, StringBuilder builder);
  }


  static class RangeFormatField extends Node implements FormatField {
    Identifier identifier;
    List<Expr> ranges;
    @Nullable
    TypeLiteral typeLiteral;

    @Nullable
    Type type;

    // While the ranges are expressions in diffrent forms, once computed they are stored here to
    // make them easier to process.
    @Nullable
    List<BitRange> computedRanges;

    public RangeFormatField(Identifier identifier, List<Expr> ranges,
                            @Nullable TypeLiteral typeLiteral) {
      this.identifier = identifier;
      this.ranges = ranges;
      this.typeLiteral = typeLiteral;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(ranges.get(ranges.size() - 1).location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append("\t [");
      ranges.get(0).prettyPrint(indent, builder);
      for (int i = 1; i < ranges.size(); i++) {
        builder.append(", ");
        ranges.get(i).prettyPrint(indent, builder);
      }
      builder.append("]");
      if (typeLiteral != null) {
        builder.append(" : ");
        typeLiteral.prettyPrint(0, builder);
      }
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      RangeFormatField that = (RangeFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(ranges, that.ranges);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(ranges);
      return result;
    }
  }

  static class TypedFormatField extends Node implements FormatField {
    final Identifier identifier;
    final TypeLiteral typeLiteral;

    // The range this field occupies in its parent format.
    @Nullable
    BitRange range;

    public TypedFormatField(Identifier identifier, TypeLiteral typeLiteral) {
      this.identifier = identifier;
      this.typeLiteral = typeLiteral;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier().location().join(typeLiteral.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append(" : ");
      typeLiteral.prettyPrint(indent, builder);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TypedFormatField that = (TypedFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(typeLiteral, that.typeLiteral);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(typeLiteral);
      result = 31 * result + Objects.hashCode(symbolTable);
      return result;
    }
  }

  /**
   * A (pseudo) field derived from another. In the VIAM this is called FieldAccess.
   *
   * <p><pre>
   * {@code
   * format ABC : Bits<8> =
   *   { A  : Bits<2>
   *   , B  : Bits<6>
   *   , C = A as Bits<4>    // This is a derived format field
   *   }
   * }
   * </pre>
   */
  static class DerivedFormatField extends Node implements FormatField {
    Identifier identifier;
    Expr expr;

    /**
     * Since the predicate doesn't have to follow the derived format field, the parser cannot
     * connect link them together and instead the typechecker does this by understanding the
     * semantic.
     */
    @Nullable
    Expr predicate;

    //    /**
    //     * Since the predicate doesn't have to follow the derived format field, the parser cannot
    //     * connect link them together and instead the typechecker does this by understanding the
    //     * semantic.
    //     */
    //    @Nullable
    //    Expr encoding;

    public DerivedFormatField(Identifier identifier, Expr expr) {
      this.identifier = identifier;
      this.expr = expr;
    }

    @Override
    public Identifier identifier() {
      return identifier;
    }

    @Override
    SourceLocation location() {
      return identifier.location().join(expr.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      identifier.prettyPrint(indent, builder);
      builder.append(" = ");
      expr.prettyPrint(indent, builder);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DerivedFormatField that = (DerivedFormatField) o;
      return Objects.equals(identifier, that.identifier)
          && Objects.equals(expr, that.expr);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(identifier);
      result = 31 * result + Objects.hashCode(expr);
      return result;
    }
  }

  /**
   * The predicate or encoding of a derived format field.
   *
   * <p><pre>
   * {@code
   * format ABC : Bits<8> =
   *   { A  : Bits<2>
   *   , B  : Bits<6>
   *   , C = A as Bits<4>
   *   : encode {               // This is an auxiliary field
   *     HI => VAR as Bits<2>
   *     }
   *   : predicate {            // This is an auxiliary field
   *     VAR => VAR = 0
   *     }
   *   }
   * }
   * </pre>
   */
  static class AuxiliaryField extends Node {
    private final AuxiliaryFieldKind kind;
    private final List<AuxiliaryFieldEntry> entries;

    AuxiliaryField(AuxiliaryFieldKind kind, List<AuxiliaryFieldEntry> entries) {
      this.kind = kind;
      this.entries = entries;
    }

    @Override
    SourceLocation location() {
      return entries.get(0).id.location().join(entries.get(entries.size() - 1).expr.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      builder.append(prettyIndentString(indent));
      builder.append(
          switch (kind) {
            case PREDICATE -> ": predicate\n";
            case ENCODE -> ": encode\n";
          });
      var isFirst = true;
      for (var entry : entries) {
        if (isFirst) {
          builder.append(prettyIndentString(indent + 1)).append("{ ");
          isFirst = false;
        } else {
          builder.append(prettyIndentString(indent + 1)).append(", ");
        }
        entry.id.prettyPrint(0, builder);
        builder.append(" => ");
        entry.expr.prettyPrint(0, builder);
        builder.append("\n");
      }
      builder.append(prettyIndentString(indent + 1)).append("}\n");
    }

    public AuxiliaryFieldKind kind() {
      return kind;
    }

    public List<AuxiliaryFieldEntry> entries() {
      return entries;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (AuxiliaryField) obj;
      return Objects.equals(this.kind, that.kind)
          && Objects.equals(this.entries, that.entries);
    }

    @Override
    public int hashCode() {
      return Objects.hash(kind, entries);
    }
  }

  static final class AuxiliaryFieldEntry {
    Identifier id;
    Expr expr;

    AuxiliaryFieldEntry(Identifier id, Expr expr) {
      this.id = id;
      this.expr = expr;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (AuxiliaryFieldEntry) obj;
      return Objects.equals(this.id, that.id)
          && Objects.equals(this.expr, that.expr);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, expr);
    }

    @Override
    public String toString() {
      return "AuxiliaryFieldEntry["
          + "id=" + id + ", "
          + "expr=" + expr + ']';
    }

  }

  enum AuxiliaryFieldKind {
    PREDICATE, ENCODE
  }

  public FormatDefinition(IdentifierOrPlaceholder identifier, TypeLiteral typeLiteral,
                          List<FormatField> fields, List<AuxiliaryField> auxiliaryFields,
                          SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.fields = fields;
    this.auxiliaryFields = auxiliaryFields;
    this.loc = location;
  }

  FormatField getField(String name) {
    return fields.stream().filter(f -> f.identifier().name.equals(name)).findFirst().orElseThrow();
  }

  @Nullable
  Type getFieldType(String name) {
    var field = getField(name);

    if (field instanceof TypedFormatField typedField) {
      return typedField.typeLiteral.type;
    } else if (field instanceof RangeFormatField rangeField) {
      return rangeField.type;
    } else if (field instanceof DerivedFormatField derivedField) {
      return derivedField.expr.type;
    } else {
      throw new IllegalStateException("Unknown field type: " + field.getClass().getSimpleName());
    }
  }

  @Nullable
  BitRange getFieldRange(String name) {
    var field = getField(name);

    if (field instanceof TypedFormatField typedField) {
      return typedField.range;
    } else if (field instanceof RangeFormatField rangeField) {
      // FIXME: propper merge them
      if (rangeField.ranges.size() > 1) {
        throw new IllegalStateException(
            "Not implemented: Too many ranges: " + rangeField.ranges.size());
      }
      if (rangeField.computedRanges == null) {
        return null;
      }
      return rangeField.computedRanges.get(0);
    } else if (field instanceof DerivedFormatField derivedField) {
      throw new IllegalStateException(
          "Cannot compute range of derived field: " + field.getClass().getSimpleName());
    } else {
      throw new IllegalStateException("Unknown field type: " + field.getClass().getSimpleName());
    }
  }


  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("format ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);

    if (fields.isEmpty()) {
      builder.append("\n");
      return;
    }

    builder.append(" =\n");

    builder.append(prettyIndentString(indent));
    builder.append("{ ");

    fields.get(0).prettyPrint(indent, builder);
    builder.append("\n");

    for (int i = 1; i < fields.size(); i++) {
      builder.append(prettyIndentString(indent));
      builder.append(", ");
      fields.get(i).prettyPrint(indent, builder);
      builder.append("\n");

    }

    for (var auxiliaryField : auxiliaryFields) {
      auxiliaryField.prettyPrint(indent, builder);
    }

    builder.append(prettyIndentString(indent));
    builder.append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FormatDefinition that = (FormatDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral)
        && fields.equals(that.fields)
        && auxiliaryFields.equals(that.auxiliaryFields);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    result = 31 * result + fields.hashCode();
    result = 31 * result + auxiliaryFields.hashCode();
    return result;
  }
}

class InstructionSetDefinition extends Definition implements IdentifiableNode {
  Identifier identifier;
  @Nullable
  Identifier extending;
  List<Definition> definitions;
  SourceLocation loc;

  @Nullable
  InstructionSetDefinition extendingNode;

  InstructionSetDefinition(Identifier identifier, @Nullable Identifier extending,
                           List<Definition> statements, SourceLocation location) {
    this.identifier = identifier;
    this.extending = extending;
    this.definitions = statements;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("instruction set architecture ").append(identifier.name);
    if (extending != null) {
      builder.append(" extending ").append(extending.name);
    }
    builder.append(" = {\n");
    Definition previousDefinition = null;
    for (Definition definition : definitions) {
      if (previousDefinition != null
          && !definition.getClass().equals(previousDefinition.getClass())) {
        builder.append("\n");
      }
      definition.prettyPrint(indent + 1, builder);
      previousDefinition = definition;
    }
    builder.append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (InstructionSetDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(extending, that.extending)
        && Objects.equals(extendingNode, that.extendingNode)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(extending);
    result = 31 * result + Objects.hashCode(extendingNode);
    result = 31 * result + Objects.hashCode(definitions);
    return result;
  }
}

class CounterDefinition extends Definition implements IdentifiableNode, TypedNode {
  CounterKind kind;
  IdentifierOrPlaceholder identifier;
  TypeLiteral typeLiteral;
  SourceLocation loc;

  @Override
  public Type type() {
    return typeLiteral.type();
  }

  enum CounterKind {
    PROGRAM,
    GROUP
  }

  public CounterDefinition(CounterKind kind, IdentifierOrPlaceholder identifier,
                           TypeLiteral typeLiteral,
                           SourceLocation location) {
    this.kind = kind;
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("%s counter ".formatted(kind.toString().toLowerCase(Locale.ENGLISH)));
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s kind: %s".formatted(this.getClass().getSimpleName(), kind.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CounterDefinition that = (CounterDefinition) o;
    return annotations.equals(that.annotations)
        && kind == that.kind
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + kind.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }
}

class MemoryDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  TypeLiteral addressTypeLiteral;
  TypeLiteral dataTypeLiteral;
  SourceLocation loc;

  @Nullable
  ConcreteRelationType type;

  public MemoryDefinition(IdentifierOrPlaceholder identifier, TypeLiteral addressTypeLiteral,
                          TypeLiteral dataTypeLiteral, SourceLocation loc) {
    this.identifier = identifier;
    this.addressTypeLiteral = addressTypeLiteral;
    this.dataTypeLiteral = dataTypeLiteral;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("memory ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    addressTypeLiteral.prettyPrint(indent, builder);
    builder.append(" -> ");
    dataTypeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MemoryDefinition that = (MemoryDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && addressTypeLiteral.equals(that.addressTypeLiteral)
        && dataTypeLiteral.equals(that.dataTypeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + addressTypeLiteral.hashCode();
    result = 31 * result + dataTypeLiteral.hashCode();
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return Objects.requireNonNull(type);
  }
}

class RegisterDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  TypeLiteral typeLiteral;
  SourceLocation loc;

  @Nullable
  DataType type;

  public RegisterDefinition(IdentifierOrPlaceholder identifier, TypeLiteral typeLiteral,
                            SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("register ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterDefinition that = (RegisterDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public Type type() {
    return typeLiteral.type();
  }
}

class RegisterFileDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  RelationTypeLiteral typeLiteral;
  SourceLocation loc;

  @Nullable
  ConcreteRelationType type = null;

  public RegisterFileDefinition(IdentifierOrPlaceholder identifier, RelationTypeLiteral typeLiteral,
                                SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("register file ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    var isFirst = true;
    for (TypeLiteral argType : typeLiteral.argTypes) {
      if (!isFirst) {
        builder.append(" * ");
      }
      isFirst = false;
      argType.prettyPrint(0, builder);
    }
    builder.append(" -> ");
    typeLiteral.resultType.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RegisterFileDefinition that = (RegisterFileDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return Objects.requireNonNull(type);
  }

  record RelationTypeLiteral(List<TypeLiteral> argTypes, TypeLiteral resultType) {
  }
}

class InstructionDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder identifier;
  IdentifierOrPlaceholder typeIdentifier;
  Statement behavior;
  SourceLocation loc;

  @Nullable
  FormatDefinition formatNode;

  @Nullable
  EncodingDefinition encodingDefinition;

  @Nullable
  AssemblyDefinition assemblyDefinition;

  InstructionDefinition(IdentifierOrPlaceholder identifier, IdentifierOrPlaceholder typeIdentifier,
                        Statement behavior, SourceLocation location) {
    this.identifier = identifier;
    this.typeIdentifier = typeIdentifier;
    this.behavior = behavior;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  Identifier typeIdentifier() {
    return (Identifier) typeIdentifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("instruction ");
    identifier.prettyPrint(indent, builder);
    builder.append(" : ");
    typeIdentifier.prettyPrint(indent, builder);
    builder.append(" = ");
    behavior.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (InstructionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(typeIdentifier, that.typeIdentifier)
        && Objects.equals(behavior, that.behavior);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeIdentifier);
    result = 31 * result + Objects.hashCode(behavior);
    return result;
  }
}

class PseudoInstructionDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder identifier;
  PseudoInstrKind kind;
  List<Parameter> params;
  List<InstructionCallStatement> statements;
  SourceLocation loc;

  /**
   * The matching assembly definition.
   * Set by the symboltable.
   */
  @Nullable
  AssemblyDefinition assemblyDefinition;

  PseudoInstructionDefinition(IdentifierOrPlaceholder identifier, PseudoInstrKind kind,
                              List<Parameter> params, List<InstructionCallStatement> statements,
                              SourceLocation loc) {
    this.identifier = identifier;
    this.kind = kind;
    this.params = params;
    this.statements = statements;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    var kindStr = switch (kind) {
      case PSEUDO -> "pseudo";
      case COMPILER -> "compiler";
    };
    builder.append(kindStr).append(" instruction ");
    identifier.prettyPrint(indent, builder);
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" = {\n");
    for (InstructionCallStatement statement : statements) {
      statement.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (PseudoInstructionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(kind, that.kind)
        && Objects.equals(params, that.params)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }

  enum PseudoInstrKind {
    PSEUDO, COMPILER
  }
}

class RelocationDefinition extends Definition implements IdentifiableNode, TypedNode {
  Identifier identifier;
  List<Parameter> params;
  TypeLiteral resultTypeLiteral;
  Expr expr;
  SourceLocation loc;

  @Nullable
  ConcreteRelationType type;

  RelocationDefinition(Identifier identifier, List<Parameter> params, TypeLiteral resultTypeLiteral,
                       Expr expr, SourceLocation loc) {
    this.identifier = identifier;
    this.params = params;
    this.resultTypeLiteral = resultTypeLiteral;
    this.expr = expr;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return identifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("relocation ");
    identifier.prettyPrint(indent, builder);
    builder.append(" ");
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" -> ");
    resultTypeLiteral.prettyPrint(0, builder);
    if (isBlockLayout(expr)) {
      builder.append(" =\n");
      expr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" = ");
      expr.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (RelocationDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(params, that.params)
        && Objects.equals(resultTypeLiteral, that.resultTypeLiteral)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(resultTypeLiteral);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return Objects.requireNonNull(type);
  }
}

sealed interface IsEncs permits EncodingDefinition.EncsNode,
    EncodingDefinition.EncodingField, PlaceholderNode, MacroInstanceNode, MacroMatchNode {
  SourceLocation location();

  void prettyPrint(int indent, StringBuilder builder);
}

class EncodingDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder instrIdentifier;
  EncsNode encodings;
  SourceLocation loc;

  @Nullable
  FormatDefinition formatNode;

  EncodingDefinition(IdentifierOrPlaceholder instrIdentifier, EncsNode encodings,
                     SourceLocation location) {
    this.instrIdentifier = instrIdentifier;
    this.encodings = encodings;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) instrIdentifier;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("encoding ");
    instrIdentifier.prettyPrint(0, builder);
    builder.append(" =\n");
    builder.append(prettyIndentString(indent)).append("{ ");
    encodings.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (EncodingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(instrIdentifier, that.instrIdentifier)
        && Objects.equals(encodings, that.encodings);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(instrIdentifier);
    result = 31 * result + Objects.hashCode(encodings);
    return result;
  }

  static final class EncsNode extends Node implements IsEncs {
    List<IsEncs> items;
    SourceLocation loc;

    EncsNode(List<IsEncs> items, SourceLocation loc) {
      this.items = items;
      this.loc = loc;
    }

    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.ENCS;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      boolean first = true;
      for (var entry : items) {
        if (!first) {
          builder.append(prettyIndentString(indent)).append(", ");
        }
        entry.prettyPrint(0, builder);
        builder.append("\n");
        first = false;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EncsNode that = (EncsNode) o;
      return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(items);
    }
  }

  static final class EncodingField implements IsEncs {
    final Identifier field;
    Expr value;

    EncodingField(Identifier field, Expr value) {
      this.field = field;
      this.value = value;
    }

    @Override
    public SourceLocation location() {
      return field.location().join(value.location());
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      field.prettyPrint(0, builder);
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (EncodingField) obj;
      return Objects.equals(this.field, that.field)
          && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(field, value);
    }

    @Override
    public String toString() {
      return "EncodingField["
          + "field=" + field + ", "
          + "value=" + value + ']';
    }

  }
}

class AssemblyDefinition extends Definition {
  List<IdentifierOrPlaceholder> identifiers;
  Expr expr;
  SourceLocation loc;

  // Can hold InstructionDefinition or PseudoInstructionDefinition
  List<Definition> instructionNodes = new ArrayList<>();

  AssemblyDefinition(List<IdentifierOrPlaceholder> identifiers, Expr expr,
                     SourceLocation location) {
    this.identifiers = identifiers;
    this.expr = expr;
    this.loc = location;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("assembly ");
    var isFirst = true;
    for (IdentifierOrPlaceholder identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(0, builder);
    }
    builder.append(" = ");
    expr.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (AssemblyDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifiers, that.identifiers)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifiers);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }
}

class UsingDefinition extends Definition implements IdentifiableNode {
  final IdentifierOrPlaceholder id;
  final TypeLiteral typeLiteral;
  final SourceLocation loc;

  UsingDefinition(IdentifierOrPlaceholder id, TypeLiteral typeLiteral, SourceLocation location) {
    this.id = id;
    this.typeLiteral = typeLiteral;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }


  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("using ");
    id.prettyPrint(indent, builder);
    builder.append(" = ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (UsingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(typeLiteral, that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(typeLiteral);
    return result;
  }
}

class FunctionDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder name;
  List<Parameter> params;
  TypeLiteral retType;
  Expr expr;
  SourceLocation loc;

  @Nullable
  ConcreteRelationType type;

  FunctionDefinition(IdentifierOrPlaceholder name, List<Parameter> params, TypeLiteral retType,
                     Expr expr, SourceLocation location) {
    this.name = name;
    this.params = params;
    this.retType = retType;
    this.expr = expr;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("function ");
    name.prettyPrint(indent, builder);
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" -> ");
    retType.prettyPrint(0, builder);
    if (!isBlockLayout(expr)) {
      builder.append(" = ");
    } else {
      builder.append(" =\n");
    }
    expr.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (FunctionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(name, that.name)
        && Objects.equals(params, that.params)
        && Objects.equals(retType, that.retType)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(name);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(retType);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return Objects.requireNonNull(type);
  }
}

class AliasDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  AliasKind kind;
  @Nullable
  TypeLiteral aliasType;
  @Nullable
  TypeLiteral targetType;
  Expr value;
  SourceLocation loc;

  AliasDefinition(IdentifierOrPlaceholder id, AliasKind kind,
                  @Nullable TypeLiteral aliasType, @Nullable TypeLiteral targetType, Expr value,
                  SourceLocation location) {
    this.id = id;
    this.kind = kind;
    this.aliasType = aliasType;
    this.targetType = targetType;
    this.value = value;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("alias ");
    switch (kind) {
      case REGISTER -> builder.append("register ");
      case REGISTER_FILE -> builder.append("register file ");
      case PROGRAM_COUNTER -> builder.append("program counter ");
      default -> {
      }
    }
    id.prettyPrint(0, builder);
    if (aliasType != null) {
      builder.append(" : ");
      aliasType.prettyPrint(0, builder);
      if (targetType != null) {
        builder.append(" -> ");
        targetType.prettyPrint(0, builder);
      }
    }
    builder.append(" = ");
    value.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (AliasDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(kind, that.kind)
        && Objects.equals(aliasType, that.aliasType)
        && Objects.equals(targetType, that.targetType)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(aliasType);
    result = 31 * result + Objects.hashCode(targetType);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }

  enum AliasKind {
    REGISTER, REGISTER_FILE, PROGRAM_COUNTER
  }
}

final class EnumerationDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  @Nullable
  TypeLiteral enumType;
  List<Entry> entries;
  SourceLocation loc;

  EnumerationDefinition(IdentifierOrPlaceholder id, @Nullable TypeLiteral enumType,
                        List<Entry> entries, SourceLocation location) {
    this.id = id;
    this.enumType = enumType;
    this.entries = entries;
    this.loc = location;
  }

  Entry getEntry(String name) {
    return entries.stream().filter(e -> e.name.name.equals(name)).findFirst().orElseThrow();
  }

  Expr getEntryValue(String name) {
    return Objects.requireNonNull(getEntry(name).value);
  }

  Type getEntryType(String name) {
    return Objects.requireNonNull(getEntry(name).value).type();
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("enumeration ");
    id.prettyPrint(0, builder);
    if (enumType != null) {
      builder.append(" : ");
      enumType.prettyPrint(0, builder);
    }
    builder.append(" =\n");
    builder.append(prettyIndentString(indent + 1)).append("{ ");
    var isFirst = true;
    for (var entry : entries) {
      if (!isFirst) {
        builder.append(prettyIndentString(indent + 1)).append(", ");
      }
      isFirst = false;
      entry.name.prettyPrint(0, builder);
      if (entry.value != null) {
        builder.append(" = ");
        entry.value.prettyPrint(0, builder);
      }
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (EnumerationDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(enumType, that.enumType)
        && Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(enumType);
    result = 31 * result + Objects.hashCode(entries);
    return result;
  }

  static class Entry extends Node implements WithSourceLocation {

    Identifier name;
    @Nullable
    Expr value;


    public Entry(Identifier name, @Nullable Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    SourceLocation location() {
      var loc = name.sourceLocation();
      if (value != null) {
        loc = loc.join(value.sourceLocation());
      }
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      name.prettyPrint(indent, builder);
      builder.append(" = ");
      if (value != null) {
        value.prettyPrint(indent, builder);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Entry entry = (Entry) o;
      return name.equals(entry.name) && Objects.equals(value, entry.value);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + Objects.hashCode(value);
      return result;
    }
  }
}

final class ExceptionDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  Statement statement;
  SourceLocation loc;

  ExceptionDefinition(IdentifierOrPlaceholder id, Statement statement, SourceLocation location) {
    this.id = id;
    this.statement = statement;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("exception ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    statement.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (ExceptionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(statement);
    return result;
  }
}

final class PlaceholderDefinition extends Definition {

  List<String> segments;
  SyntaxType type;
  SourceLocation loc;

  PlaceholderDefinition(List<String> segments, SyntaxType type, SourceLocation loc) {
    this.segments = segments;
    this.type = type;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("$");
    builder.append(String.join(".", segments));
    builder.append("\n");
  }
}

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
final class MacroInstanceDefinition extends Definition implements IsMacroInstance {
  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceDefinition(MacroOrPlaceholder macro, List<Node> arguments,
                                 SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("$");
    if (macro instanceof Macro m) {
      builder.append(m.name().name);
    } else if (macro instanceof MacroPlaceholder mp) {
      builder.append(String.join(".", mp.segments()));
    }
    builder.append("(");
    var isFirst = true;
    for (var arg : arguments) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      arg.prettyPrint(0, builder);
    }
    builder.append(")\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroInstanceDefinition that = (MacroInstanceDefinition) o;
    return macro.equals(that.macro)
        && arguments.equals(that.arguments);
  }

  @Override
  public int hashCode() {
    int result = macro.hashCode();
    result = 31 * result + arguments.hashCode();
    return result;
  }

  @Override
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }
}

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
final class MacroMatchDefinition extends Definition implements IsMacroMatch {
  MacroMatch macroMatch;

  MacroMatchDefinition(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return macroMatch.sourceLocation();
  }

  @Override
  SyntaxType syntaxType() {
    return macroMatch.resultType();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    macroMatch.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchDefinition that = (MacroMatchDefinition) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }
}

class ImportDefinition extends Definition {

  Ast moduleAst;
  List<List<Identifier>> importedSymbols;
  @Nullable
  Identifier fileId;
  @Nullable
  StringLiteral filePath;
  List<StringLiteral> args;
  SourceLocation loc;

  ImportDefinition(Ast moduleAst, List<List<Identifier>> importedSymbols,
                   @Nullable Identifier fileId, @Nullable StringLiteral filePath,
                   List<StringLiteral> args, SourceLocation loc) {
    Objects.requireNonNullElse(fileId, filePath);
    this.moduleAst = moduleAst;
    this.importedSymbols = importedSymbols;
    this.fileId = fileId;
    this.filePath = filePath;
    this.args = args;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("import ");
    if (fileId != null) {
      fileId.prettyPrint(0, builder);
    } else if (filePath != null) {
      filePath.prettyPrint(0, builder);
    }
    if (!importedSymbols.isEmpty()) {
      builder.append("::{");
      var isFirst = true;
      for (List<Identifier> importedSymbol : importedSymbols) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        var isFirstSegment = true;
        for (Identifier segment : importedSymbol) {
          if (!isFirstSegment) {
            builder.append("::");
          }
          isFirstSegment = false;
          segment.prettyPrint(0, builder);
        }
      }
      builder.append("}");
    }
    if (!args.isEmpty()) {
      builder.append(" with (");
      var isFirst = true;
      for (StringLiteral arg : args) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        arg.prettyPrint(0, builder);
      }
      builder.append(")");
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportDefinition that = (ImportDefinition) o;
    return Objects.equals(moduleAst, that.moduleAst)
        && Objects.equals(importedSymbols, that.importedSymbols)
        && Objects.equals(fileId, that.fileId)
        && Objects.equals(filePath, that.filePath)
        && Objects.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleAst, importedSymbols, fileId, filePath, args);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}

record Annotations(List<Annotation> annotations) {
  Annotations() {
    this(new ArrayList<>());
  }

  void prettyPrint(int indent, StringBuilder builder) {
    annotations.forEach(annotation -> annotation.prettyPrint(indent, builder));
  }
}

final class Annotation {
  Expr expr;
  @Nullable
  TypeLiteral type;
  @Nullable
  IdentifierOrPlaceholder property;

  Annotation(Expr expr, @Nullable TypeLiteral type,
             @Nullable IdentifierOrPlaceholder property) {
    this.expr = expr;
    this.type = type;
    this.property = property;
  }

  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(Node.prettyIndentString(indent));
    builder.append('[');
    expr.prettyPrint(indent, builder);
    if (type != null) {
      builder.append(" : ");
      type.prettyPrint(indent, builder);
    }
    if (property != null) {
      builder.append(' ');
      property.prettyPrint(indent, builder);
    }
    builder.append(" ]\n");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Annotation) obj;
    return Objects.equals(this.expr, that.expr)
        && Objects.equals(this.type, that.type)
        && Objects.equals(this.property, that.property);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expr, type, property);
  }

  @Override
  public String toString() {
    return "Annotation["
        + "expr=" + expr + ", "
        + "type=" + type + ", "
        + "property=" + property + ']';
  }
}

class DefinitionList extends Definition {

  List<Definition> items;
  SyntaxType syntaxType;
  SourceLocation location;

  DefinitionList(List<Definition> items, SyntaxType syntaxType, SourceLocation location) {
    this.items = items;
    this.location = location;
    this.syntaxType = syntaxType;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return syntaxType;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    items.forEach(item -> item.prettyPrint(indent, builder));
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }
}

/**
 * An internal temporary definition of a model.
 * This node should never leave the parser.
 */
final class ModelDefinition extends Definition implements IdentifiableNode {
  /**
   * Either a concrete identifier for this model or, if it is a model-in-model, a placeholder ID.
   */
  IdentifierOrPlaceholder id;

  /**
   * The list of formal parameters for the represented macro.
   */
  List<MacroParam> params;

  /**
   * The actual macro body to be templated.
   */
  Node body;

  /**
   * A macro return type - should only be a {@link BasicSyntaxType}.
   */
  SyntaxType returnType;

  /**
   * In a model-in-model situation, the parent model's arguments can be referenced in the inner
   * model. To preserve their value during macro expansion, the bound arguments field is used.
   *
   * @see MacroExpander#visit(ModelDefinition)
   * @see MacroExpander#collectMacroParameters(Macro, List, SourceLocation)
   */
  Map<String, Node> boundArguments = new HashMap<>();
  SourceLocation loc;

  ModelDefinition(IdentifierOrPlaceholder id, List<MacroParam> params, Node body,
                  SyntaxType returnType, SourceLocation loc) {
    this.id = id;
    this.params = params;
    this.body = body;
    this.returnType = returnType;
    this.loc = loc;
  }

  Macro toMacro() {
    return new Macro(new Identifier(id.pathToString(), id.location()), params, body, returnType,
        boundArguments);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent)).append("model ");
    id.prettyPrint(0, builder);
    builder.append("(");
    var isFirst = true;
    for (var param : params) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      param.name().prettyPrint(0, builder);
      builder.append(" : ").append(param.type().print());
    }
    builder.append(") : ");
    builder.append(returnType.print());
    builder.append(" = {\n");
    body.prettyPrint(indent + 1, builder);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelDefinition that = (ModelDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(params, that.params)
        && Objects.equals(body, that.body)
        && Objects.equals(returnType, that.returnType)
        && Objects.equals(boundArguments, that.boundArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, params, body, returnType, boundArguments);
  }
}

/**
 * An internal temporary placeholder of record type.
 * This node should never leave the parser.
 */
final class RecordTypeDefinition extends Definition implements IdentifiableNode {
  Identifier name;
  RecordType recordType;
  SourceLocation loc;

  RecordTypeDefinition(Identifier name, RecordType recordType, SourceLocation loc) {
    this.name = name;
    this.recordType = recordType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("record ");
    builder.append(recordType.print());
    builder.append(recordType.entries.stream()
        .map(entry -> entry.name() + " : " + entry.type().print())
        .collect(Collectors.joining(",", "(", ")")));
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordTypeDefinition that = (RecordTypeDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(recordType, that.recordType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, recordType);
  }

  @Override
  public Identifier identifier() {
    return name;
  }
}

/**
 * An internal temporary placeholder of model type.
 * This node should never leave the parser.
 */
final class ModelTypeDefinition extends Definition implements IdentifiableNode {
  Identifier name;
  ProjectionType projectionType;
  SourceLocation loc;

  ModelTypeDefinition(Identifier name, ProjectionType projectionType, SourceLocation loc) {
    this.name = name;
    this.projectionType = projectionType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("model-type ");
    name.prettyPrint(0, builder);
    builder.append(" = ");
    builder.append(projectionType.print());
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelTypeDefinition that = (ModelTypeDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(projectionType, that.projectionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, projectionType);
  }

  @Override
  public Identifier identifier() {
    return name;
  }
}

class TemplateParam extends Node implements IdentifiableNode {
  Identifier name;
  TypeLiteral type;

  @Nullable
  Expr value;

  public TemplateParam(Identifier name, TypeLiteral type, @Nullable Expr value) {
    this.name = name;
    this.type = type;
    this.value = value;
  }

  @Override
  public Identifier identifier() {
    return name;
  }

  @Override
  SourceLocation location() {
    if (value != null) {
      return name.location().join(value.location());
    }
    return name.location().join(type.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    name.prettyPrint(0, builder);
    builder.append(": ");
    type.prettyPrint(0, builder);
    if (value != null) {
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TemplateParam that = (TemplateParam) o;
    return name.equals(that.name) && type.equals(that.type)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}

class ProcessDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder name;
  List<TemplateParam> templateParams;
  List<Parameter> inputs;
  List<Parameter> outputs;
  Statement statement;
  SourceLocation loc;

  ProcessDefinition(IdentifierOrPlaceholder name, List<TemplateParam> templateParams,
                    List<Parameter> inputs, List<Parameter> outputs, Statement statement,
                    SourceLocation loc) {
    this.name = name;
    this.templateParams = templateParams;
    this.inputs = inputs;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("process ");
    name.prettyPrint(indent, builder);
    if (!templateParams.isEmpty()) {
      builder.append("<");
      var isFirst = true;
      for (TemplateParam templateParam : templateParams) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        templateParam.prettyPrint(indent, builder);
      }
      builder.append("> ");
    }
    Parameter.prettyPrintMultiple(indent, inputs, builder);
    if (!outputs.isEmpty()) {
      builder.append(" -> ");
      Parameter.prettyPrintMultiple(indent, outputs, builder);
    }
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " " + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProcessDefinition that = (ProcessDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(templateParams, that.templateParams)
        && Objects.equals(inputs, that.inputs)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, templateParams, inputs, outputs, statement);
  }


}

class OperationDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder name;
  List<IsId> resources;
  SourceLocation loc;

  OperationDefinition(IdentifierOrPlaceholder name, List<IsId> resources, SourceLocation loc) {
    this.name = name;
    this.resources = resources;
    this.loc = loc;
  }


  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("operation ");
    name.prettyPrint(indent, builder);
    builder.append(" =");
    if (resources.isEmpty()) {
      builder.append(" {}\n");
    } else {
      builder.append("\n");
      var isFirst = true;
      for (IsId resource : resources) {
        builder.append(prettyIndentString(indent));
        builder.append(isFirst ? "{ " : ", ");
        isFirst = false;
        resource.prettyPrint(0, builder);
        builder.append("\n");
      }
      builder.append(prettyIndentString(indent)).append("}\n");
    }
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " " + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationDefinition that = (OperationDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, resources);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }
}

class GroupDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder name;
  @Nullable
  TypeLiteral type;
  Group.Sequence groupSequence;
  SourceLocation loc;

  GroupDefinition(IdentifierOrPlaceholder name, @Nullable TypeLiteral type,
                  Group.Sequence groupSequence, SourceLocation loc) {
    this.name = name;
    this.type = type;
    this.groupSequence = groupSequence;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("group ");
    name.prettyPrint(indent, builder);
    if (type != null) {
      builder.append(" : ");
      type.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    groupSequence.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " " + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupDefinition that = (GroupDefinition) o;
    return Objects.equals(name, that.name) && Objects.equals(type, that.type)
        && Objects.equals(groupSequence, that.groupSequence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, groupSequence);
  }
}

class ApplicationBinaryInterfaceDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  IsId isa;
  List<Definition> definitions;
  SourceLocation loc;

  @Nullable
  InstructionSetDefinition isaNode;

  ApplicationBinaryInterfaceDefinition(Identifier id, IsId isa, List<Definition> definitions,
                                       SourceLocation loc) {
    this.id = id;
    this.isa = isa;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("application binary interface ");
    id.prettyPrint(indent, builder);
    builder.append(" for ");
    isa.prettyPrint(indent, builder);
    builder.append(" = {\n");
    for (Definition definition : definitions) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationBinaryInterfaceDefinition that = (ApplicationBinaryInterfaceDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(isa, that.isa)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, isa, definitions);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

/**
 * The compiler generator requires a few {@link PseudoInstruction}. Those need to be defined
 * in the ABI. They are distinguised with the {@link AbiPseudoInstructionDefinition#kind}
 * property.
 */
class AbiPseudoInstructionDefinition extends Definition {

  Kind kind;
  IdentifierOrPlaceholder target;
  SourceLocation loc;

  AbiPseudoInstructionDefinition(Kind kind, IdentifierOrPlaceholder target, SourceLocation loc) {
    this.kind = kind;
    this.target = target;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    builder.append(" = ");
    builder.append(target).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbiPseudoInstructionDefinition that = (AbiPseudoInstructionDefinition) o;
    return kind == that.kind && target.equals(that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, target);
  }

  enum Kind {
    RETURN("return"),
    CALL("call"),
    LOCAL_ADDRESS_LOAD("local address load");

    private final String keyword;

    Kind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class AbiSequenceDefinition extends Definition {

  SeqKind kind;
  List<Parameter> params;
  List<InstructionCallStatement> statements;
  SourceLocation loc;

  AbiSequenceDefinition(SeqKind kind, List<Parameter> params,
                        List<InstructionCallStatement> statements, SourceLocation loc) {
    this.kind = kind;
    this.params = params;
    this.statements = statements;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    builder.append(" sequence ");
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" = {\n");
    for (InstructionCallStatement statement : statements) {
      statement.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbiSequenceDefinition that = (AbiSequenceDefinition) o;
    return kind == that.kind && Objects.equals(params, that.params)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, params, statements);
  }

  enum SeqKind {
    ADDRESS("address"), CALL("call"), CONSTANT("constant"), NOP("nop"), RETURN("return");

    private final String keyword;

    SeqKind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class SpecialPurposeRegisterDefinition extends Definition {

  Purpose purpose;
  List<SequenceCallExpr> exprs;
  SourceLocation loc;

  SpecialPurposeRegisterDefinition(Purpose purpose,
                                   List<SequenceCallExpr> sequence,
                                   SourceLocation loc) {
    this.purpose = purpose;
    this.exprs = sequence;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(purpose.keywords);
    builder.append(" = ");
    for (var i : exprs) {
      i.prettyPrint(indent + 1, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpecialPurposeRegisterDefinition that = (SpecialPurposeRegisterDefinition) o;
    return purpose == that.purpose && Objects.equals(exprs, that.exprs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose, exprs);
  }

  enum Purpose {
    RETURN_ADDRESS("return address"),
    RETURN_VALUE("return value"),
    STACK_POINTER("stack pointer"),
    GLOBAL_POINTER("global pointer"),
    FRAME_POINTER("frame pointer"),
    THREAD_POINTER("thread pointer"),
    FUNCTION_ARGUMENT("function argument"),
    CALLER_SAVED("caller saved"),
    CALLEE_SAVED("callee saved");

    private final String keywords;

    Purpose(String keywords) {
      this.keywords = keywords;
    }
  }
}

class MicroProcessorDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  List<IsId> implementedIsas;
  @Nullable
  IsId abi;
  List<Definition> definitions;
  SourceLocation loc;

  List<InstructionSetDefinition> implementedIsaNodes = new ArrayList<>();
  @Nullable
  ApplicationBinaryInterfaceDefinition abiNode;

  MicroProcessorDefinition(Identifier id, List<IsId> implementedIsas, @Nullable IsId abi,
                           List<Definition> definitions, SourceLocation loc) {
    this.id = id;
    this.implementedIsas = implementedIsas;
    this.abi = abi;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("micro processor ");
    id.prettyPrint(0, builder);
    builder.append(" implements ");
    var isFirst = true;
    for (IsId implementedIsa : implementedIsas) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      implementedIsa.prettyPrint(0, builder);
    }
    builder.append(" with ");
    if (abi != null) {
      abi.prettyPrint(0, builder);
    }
    builder.append(" = {\n");
    for (Definition definition : definitions) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MicroProcessorDefinition that = (MicroProcessorDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(implementedIsas, that.implementedIsas)
        && Objects.equals(abi, that.abi)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, implementedIsas, abi, definitions);
  }


}

// TODO: This should probably be a IdentifiableDefinition, but I don't understand how..
class PatchDefinition extends Definition {
  Identifier generator;
  Identifier handle;
  @Nullable
  IsId reference;
  @Nullable
  String source;
  SourceLocation loc;

  PatchDefinition(Identifier generator, Identifier handle, @Nullable IsId reference,
                  @Nullable String source, SourceLocation loc) {
    this.generator = generator;
    this.handle = handle;
    this.reference = reference;
    this.source = source;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }


  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("patch ");
    generator.prettyPrint(0, builder);
    builder.append(" ");
    handle.prettyPrint(0, builder);
    builder.append(" = ");
    if (reference != null) {
      reference.prettyPrint(0, builder);
    }
    if (source != null) {
      builder.append("-<{").append(source).append("}>-");
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PatchDefinition that = (PatchDefinition) o;
    return Objects.equals(generator, that.generator)
        && Objects.equals(handle, that.handle)
        && Objects.equals(reference, that.reference)
        && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generator, handle, reference, source);
  }
}

class SourceDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  String source;
  SourceLocation loc;

  SourceDefinition(Identifier id, String source, SourceLocation loc) {
    this.id = id;
    this.source = source;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("source ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    builder.append("-<{").append(source).append("}>-\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceDefinition that = (SourceDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source);
  }


}

class CpuFunctionDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  BehaviorKind kind;
  @Nullable
  IsId stopWithReference;
  Expr expr;
  SourceLocation loc;

  CpuFunctionDefinition(Identifier id, BehaviorKind kind, @Nullable IsId stopWithReference,
                        Expr expr,
                        SourceLocation loc) {
    this.id = id;
    this.kind = kind;
    this.stopWithReference = stopWithReference;
    this.expr = expr;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    if (stopWithReference != null) {
      builder.append(" with @");
      stopWithReference.prettyPrint(0, builder);
    }
    if (isBlockLayout(expr)) {
      builder.append(" =\n");
      expr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" = ");
      expr.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CpuFunctionDefinition that = (CpuFunctionDefinition) o;
    return kind == that.kind && Objects.equals(stopWithReference, that.stopWithReference)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, stopWithReference, expr);
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  enum BehaviorKind {
    START("start"), STOP("stop");

    private final String keyword;

    BehaviorKind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class CpuProcessDefinition extends Definition {
  ProcessKind kind;
  List<Parameter> startupOutputs;
  Statement statement;
  SourceLocation loc;

  CpuProcessDefinition(ProcessKind kind, List<Parameter> startupOutputs, Statement stmt,
                       SourceLocation loc) {
    this.kind = kind;
    this.startupOutputs = startupOutputs;
    this.statement = stmt;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    if (kind == ProcessKind.STARTUP) {
      builder.append(" -> ");
      Parameter.prettyPrintMultiple(indent, startupOutputs, builder);
    }
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CpuProcessDefinition that = (CpuProcessDefinition) o;
    return kind == that.kind && Objects.equals(startupOutputs, that.startupOutputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, startupOutputs, statement);
  }

  enum ProcessKind {
    FIRMWARE("firmware"), STARTUP("startup");

    private final String keyword;

    ProcessKind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class MicroArchitectureDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  IsId processor;
  List<Definition> definitions;
  SourceLocation loc;

  MicroArchitectureDefinition(Identifier id, IsId processor, List<Definition> definitions,
                              SourceLocation loc) {
    this.id = id;
    this.processor = processor;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("micro architecture ");
    id.prettyPrint(0, builder);
    builder.append(" implements ");
    processor.prettyPrint(0, builder);
    builder.append(" = {\n");
    for (Definition definition : definitions) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MicroArchitectureDefinition that = (MicroArchitectureDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(processor, that.processor)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, processor, definitions);
  }

}

class MacroInstructionDefinition extends Definition {
  MacroBehaviorKind kind;
  List<Parameter> inputs;
  List<Parameter> outputs;
  Statement statement;
  SourceLocation loc;

  MacroInstructionDefinition(MacroBehaviorKind kind, List<Parameter> inputs,
                             List<Parameter> outputs, Statement statement, SourceLocation loc) {
    this.kind = kind;
    this.inputs = inputs;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    Parameter.prettyPrintMultiple(indent, inputs, builder);
    builder.append(" -> ");
    Parameter.prettyPrintMultiple(indent, outputs, builder);
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MacroInstructionDefinition that = (MacroInstructionDefinition) o;
    return kind == that.kind && Objects.equals(inputs, that.inputs)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, inputs, outputs, statement);
  }

  enum MacroBehaviorKind {
    TRANSLATION("translation"), PREDICTION("prediction"), FETCH("fetch"), DECODER("decoder"),
    STARTUP("startup");

    private final String keyword;

    MacroBehaviorKind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class PortBehaviorDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  PortKind kind;
  List<Parameter> inputs;
  List<Parameter> outputs;
  Statement statement;
  SourceLocation loc;

  PortBehaviorDefinition(Identifier id, PortKind kind, List<Parameter> inputs,
                         List<Parameter> outputs, Statement statement, SourceLocation loc) {
    this.id = id;
    this.kind = kind;
    this.inputs = inputs;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    id.prettyPrint(0, builder);
    Parameter.prettyPrintMultiple(indent, inputs, builder);
    builder.append(" -> ");
    Parameter.prettyPrintMultiple(indent, outputs, builder);
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PortBehaviorDefinition that = (PortBehaviorDefinition) o;
    return Objects.equals(id, that.id) && kind == that.kind
        && Objects.equals(inputs, that.inputs)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, kind, inputs, outputs, statement);
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  enum PortKind {
    READ("read"), WRITE("write"), HIT("hit"), MISS("miss");

    private final String keyword;

    PortKind(String keyword) {
      this.keyword = keyword;
    }
  }
}

class PipelineDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  List<Parameter> outputs;
  Statement statement;
  SourceLocation loc;

  PipelineDefinition(Identifier id, List<Parameter> outputs, Statement statement,
                     SourceLocation loc) {
    this.id = id;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("pipeline");
    id.prettyPrint(0, builder);
    if (!outputs.isEmpty()) {
      builder.append(" -> ");
      Parameter.prettyPrintMultiple(indent, outputs, builder);
    }
    builder.append(" = ");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PipelineDefinition that = (PipelineDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, outputs, statement);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

class StageDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  List<Parameter> outputs;
  Statement statement;
  SourceLocation loc;

  StageDefinition(Identifier id, List<Parameter> outputs, Statement statement,
                  SourceLocation loc) {
    this.id = id;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("stage ");
    id.prettyPrint(0, builder);
    if (!outputs.isEmpty()) {
      Parameter.prettyPrintMultiple(indent, outputs, builder);
    }
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StageDefinition that = (StageDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, outputs, statement);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

class CacheDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  TypeLiteral sourceType;
  TypeLiteral targetType;
  SourceLocation loc;

  CacheDefinition(Identifier id, TypeLiteral sourceType, TypeLiteral targetType,
                  SourceLocation loc) {
    this.id = id;
    this.sourceType = sourceType;
    this.targetType = targetType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("cache ");
    id.prettyPrint(0, builder);
    builder.append(" : ");
    sourceType.prettyPrint(0, builder);
    builder.append(" -> ");
    targetType.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CacheDefinition that = (CacheDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(sourceType, that.sourceType)
        && Objects.equals(targetType, that.targetType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sourceType, targetType);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

class LogicDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  SourceLocation loc;

  LogicDefinition(Identifier id, SourceLocation loc) {
    this.id = id;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("logic ");
    id.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogicDefinition that = (LogicDefinition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

class SignalDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  TypeLiteral type;
  SourceLocation loc;

  SignalDefinition(Identifier id, TypeLiteral type, SourceLocation loc) {
    this.id = id;
    this.type = type;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("signal ");
    id.prettyPrint(0, builder);
    builder.append(" : ");
    type.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignalDefinition that = (SignalDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

/**
 * Represents the <code>assembly description</code> definition of a vadl specification.
 * It contains definitions for modifiers, directives and grammar rules of an assembly language.
 * <p>
 * Further, it can also contain constant, function and using definitions.
 * </p>
 */
class AsmDescriptionDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  Identifier abi;
  List<AsmModifierDefinition> modifiers;
  List<AsmDirectiveDefinition> directives;
  List<AsmGrammarRuleDefinition> rules;
  List<Definition> commonDefinitions;
  SourceLocation loc;

  public AsmDescriptionDefinition(Identifier id, Identifier abi,
                                  List<AsmModifierDefinition> modifiers,
                                  List<AsmDirectiveDefinition> directives,
                                  List<AsmGrammarRuleDefinition> rules,
                                  List<Definition> commonDefinitions,
                                  SourceLocation loc) {
    this.id = id;
    this.abi = abi;
    this.modifiers = modifiers;
    this.directives = directives;
    this.rules = rules;
    this.commonDefinitions = commonDefinitions;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    annotations.prettyPrint(indent, builder);

    builder.append(prettyIndentString(indent));
    builder.append("assembly description ");
    id.prettyPrint(indent, builder);
    builder.append(" for ");
    abi.prettyPrint(indent, builder);
    builder.append(" = {\n");
    indent++;

    if (!commonDefinitions.isEmpty()) {
      for (var def : commonDefinitions) {
        def.prettyPrint(indent, builder);
      }
    }

    if (!modifiers.isEmpty()) {
      builder.append(prettyIndentString(indent)).append("modifiers = {\n");
      indent++;
      for (var mod : modifiers) {
        mod.prettyPrint(indent, builder);
        if (!Objects.equals(modifiers.get(modifiers.size() - 1), mod)) {
          builder.append(',');
        }
        builder.append("\n");
      }
      builder.append(prettyIndentString(--indent)).append("}\n");
    }

    if (!directives.isEmpty()) {
      builder.append(prettyIndentString(indent)).append("directives = {\n");
      indent++;
      for (var dir : directives) {
        dir.prettyPrint(indent, builder);
        if (!Objects.equals(directives.get(directives.size() - 1), dir)) {
          builder.append(',');
        }
        builder.append("\n");
      }
      builder.append(prettyIndentString(--indent)).append("}\n");
    }

    builder.append(prettyIndentString(indent)).append("grammar = {\n");
    indent++;
    for (var rule : rules) {
      builder.append(prettyIndentString(indent));
      rule.prettyPrint(indent, builder);
      if (!Objects.equals(rules.get(rules.size() - 1), rule)) {
        builder.append("\n");
      }
    }
    builder.append(prettyIndentString(--indent)).append("}\n");

    builder.append(prettyIndentString(--indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmDescriptionDefinition that = (AsmDescriptionDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(abi, that.abi)
        && Objects.equals(rules, that.rules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, abi, rules);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

/**
 * Represents a modifier in the specified assembly language.
 * A modifier is a mapping from a string to a defined relocation.
 * e.g. <code>"hi" −> RV32I::HI`</code>
 */
class AsmModifierDefinition extends Definition {
  Expr stringLiteral;
  Identifier isa;
  Identifier relocation;
  SourceLocation loc;

  public AsmModifierDefinition(Expr stringLiteral, Identifier isa, Identifier relocation,
                               SourceLocation loc) {
    this.stringLiteral = stringLiteral;
    this.isa = isa;
    this.relocation = relocation;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    stringLiteral.prettyPrint(0, builder);
    builder.append(" -> ");
    isa.prettyPrint(0, builder);
    builder.append("::");
    relocation.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmModifierDefinition that = (AsmModifierDefinition) o;
    return Objects.equals(stringLiteral, that.stringLiteral) && Objects.equals(isa, that.isa)
        && Objects.equals(relocation, that.relocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringLiteral, isa, relocation);
  }
}

/**
 * Represents a directive in the specified assembly language.
 * A directive is a mapping from a string to a predefined builtin directive.
 * e.g. <code>".word"  -> Byte4</code>
 */
class AsmDirectiveDefinition extends Definition {
  Expr stringLiteral;
  Identifier builtinDirective;
  SourceLocation loc;

  public AsmDirectiveDefinition(Expr stringLiteral, Identifier builtinDirective,
                                SourceLocation loc) {
    this.stringLiteral = stringLiteral;
    this.builtinDirective = builtinDirective;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    stringLiteral.prettyPrint(0, builder);
    builder.append(" -> ");
    builtinDirective.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmDirectiveDefinition that = (AsmDirectiveDefinition) o;
    return Objects.equals(stringLiteral, that.stringLiteral)
        && Objects.equals(builtinDirective, that.builtinDirective);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringLiteral, builtinDirective);
  }
}

interface AsmGrammarEntityVisitor<R> {
  R visit(AsmGrammarRuleDefinition entity);

  R visit(AsmGrammarAlternativesDefinition entity);

  R visit(AsmGrammarElementDefinition entity);

  R visit(AsmGrammarLocalVarDefinition entity);

  R visit(AsmGrammarLiteralDefinition entity);
}

/**
 * Represents a rule(non-terminal) in the assembly language grammar.
 * The body of the rule is represented by a list of alternatives.
 *
 * @see AsmGrammarAlternativesDefinition
 */
class AsmGrammarRuleDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  @Nullable
  AsmGrammarTypeDefinition asmTypeDefinition;
  AsmGrammarAlternativesDefinition alternatives;
  SourceLocation loc;

  boolean isTerminalRule = false;
  boolean isBuiltinRule = false;
  @Nullable
  AsmType asmType;

  public AsmGrammarRuleDefinition(Identifier id,
                                  @Nullable AsmGrammarTypeDefinition asmTypeDefinition,
                                  AsmGrammarAlternativesDefinition alternatives,
                                  SourceLocation loc) {
    this.id = id;
    this.asmTypeDefinition = asmTypeDefinition;
    this.alternatives = alternatives;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    id.prettyPrint(indent, builder);
    if (asmTypeDefinition != null) {
      asmTypeDefinition.prettyPrint(indent, builder);
    }
    builder.append(" : ");
    builder.append("\n");

    indent++;
    alternatives.prettyPrint(indent, builder);
    indent--;

    builder.append("\n").append(prettyIndentString(indent)).append(";\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarRuleDefinition that = (AsmGrammarRuleDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(asmTypeDefinition, that.asmTypeDefinition)
        && Objects.equals(alternatives, that.alternatives);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, asmTypeDefinition, alternatives);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

/**
 * Represents alternatives in an assembly grammar rule.
 * Contains a list of alternatives, where each alternative is a list of assembly grammar elements.
 *
 * @see AsmGrammarElementDefinition
 */
class AsmGrammarAlternativesDefinition extends Definition {
  List<List<AsmGrammarElementDefinition>> alternatives;
  SourceLocation loc;

  @Nullable
  AsmType asmType;
  @Nullable
  List<Set<AsmToken>> alternativesFirstTokens;
  @Nullable
  Set<AsmToken> enclosingBlockFirstTokens;

  public AsmGrammarAlternativesDefinition(List<List<AsmGrammarElementDefinition>> alternatives,
                                          SourceLocation loc) {
    this.alternatives = alternatives;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    var elementIndent = indent + 1;
    for (int i = 0; i < alternatives.size(); i++) {
      var alternative = alternatives.get(i);
      if (i != 0) {
        builder.append(prettyIndentString(indent));
        builder.append("|\n");
      }
      for (int j = 0; j < alternative.size(); j++) {
        var element = alternative.get(j);
        element.prettyPrint(elementIndent, builder);
        if (j != alternative.size() - 1) {
          builder.append("\n");
        }
      }
      if (i != alternatives.size() - 1) {
        builder.append("\n");
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarAlternativesDefinition that = (AsmGrammarAlternativesDefinition) o;

    boolean equal = true;
    for (int i = 0; i < alternatives.size(); i++) {
      var curAlternative = alternatives.get(i);
      for (int j = 0; j < curAlternative.size(); j++) {
        try {
          equal &= Objects.equals(curAlternative.get(j), that.alternatives.get(i).get(j));
        } catch (IndexOutOfBoundsException e) {
          return false;
        }
      }
    }
    return equal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(alternatives);
  }
}

/**
 * Represents an element in an assembly grammar rule.
 * An element is the basic building block of which rules are made of.
 * <p>
 * An element can be any of:
 * <li>local variable definition</li>
 * <li>rule invocation</li>
 * <li>vadl function invocation</li>
 * <li>sequence of elements</li>
 * <li>optional block</li>
 * <li>repetition block</li>
 * <li>semantic predicate</li>
 * <li>string literal</li>
 * </p>
 *
 * @see AsmGrammarLiteralDefinition
 */
class AsmGrammarElementDefinition extends Definition {
  @Nullable
  AsmGrammarLocalVarDefinition localVar;
  @Nullable
  Identifier attribute;
  Boolean isPlusEqualsAttributeAssign;
  Boolean isAttributeLocalVar = false;
  @Nullable
  AsmGrammarLiteralDefinition asmLiteral;
  @Nullable
  AsmGrammarAlternativesDefinition groupAlternatives;
  @Nullable
  AsmGrammarAlternativesDefinition optionAlternatives;
  @Nullable
  AsmGrammarAlternativesDefinition repetitionAlternatives;
  @Nullable
  Expr semanticPredicate;
  @Nullable
  AsmGrammarTypeDefinition groupAsmTypeDefinition;
  SourceLocation loc;

  @Nullable
  AsmType asmType;
  Boolean isWithinRepetitionBlock = false;

  public AsmGrammarElementDefinition(@Nullable AsmGrammarLocalVarDefinition localVar,
                                     @Nullable Identifier attribute,
                                     Boolean isPlusEqualsAttributeAssign,
                                     @Nullable AsmGrammarLiteralDefinition asmLiteral,
                                     @Nullable AsmGrammarAlternativesDefinition groupAlternatives,
                                     @Nullable AsmGrammarAlternativesDefinition optionAlternatives,
                                     @Nullable
                                     AsmGrammarAlternativesDefinition repetitionAlternatives,
                                     @Nullable Expr semanticPredicate,
                                     @Nullable AsmGrammarTypeDefinition groupAsmType,
                                     SourceLocation loc) {
    this.localVar = localVar;
    this.attribute = attribute;
    this.isPlusEqualsAttributeAssign = isPlusEqualsAttributeAssign;
    this.asmLiteral = asmLiteral;
    this.groupAlternatives = groupAlternatives;
    this.optionAlternatives = optionAlternatives;
    this.repetitionAlternatives = repetitionAlternatives;
    this.semanticPredicate = semanticPredicate;
    this.groupAsmTypeDefinition = groupAsmType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  String symbol() {
    return isPlusEqualsAttributeAssign ? "+=" : "=";
  }

  private void prettyPrintAlternatives(int indent, StringBuilder builder,
                                       @Nullable AsmGrammarAlternativesDefinition alternatives,
                                       char blockStartSymbol, char blockEndSymbol) {
    if (alternatives != null) {
      builder.append(blockStartSymbol).append("\n");
      alternatives.prettyPrint(++indent, builder);
      builder.append(prettyIndentString(--indent));
      builder.append("\n").append(prettyIndentString(indent)).append(blockEndSymbol);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));

    if (localVar != null) {
      localVar.prettyPrint(indent, builder);
    }
    if (attribute != null) {
      attribute.prettyPrint(indent, builder);
      builder.append(" ").append(symbol()).append(" ");
    }
    if (asmLiteral != null) {
      asmLiteral.prettyPrint(0, builder);
    }

    prettyPrintAlternatives(indent, builder, groupAlternatives, '(', ')');
    prettyPrintAlternatives(indent, builder, optionAlternatives, '[', ']');
    prettyPrintAlternatives(indent, builder, repetitionAlternatives, '{', '}');

    if (semanticPredicate != null) {
      builder.append("?( ");
      semanticPredicate.prettyPrint(0, builder);
      builder.append(" )");
    }
    if (groupAsmTypeDefinition != null) {
      groupAsmTypeDefinition.prettyPrint(0, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarElementDefinition that = (AsmGrammarElementDefinition) o;
    return Objects.equals(localVar, that.localVar) && Objects.equals(attribute, that.attribute)
        && Objects.equals(isPlusEqualsAttributeAssign, that.isPlusEqualsAttributeAssign)
        && Objects.equals(asmLiteral, that.asmLiteral)
        && Objects.equals(groupAlternatives, that.groupAlternatives)
        && Objects.equals(groupAsmTypeDefinition, that.groupAsmTypeDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localVar, attribute, isPlusEqualsAttributeAssign, asmLiteral,
        groupAlternatives, groupAsmTypeDefinition);
  }
}

/**
 * Represents the definition of a local variable in the assembly grammar.
 * A variable defines an identifier and assigns an assembly grammar literal to it.
 *
 * @see AsmGrammarLiteralDefinition
 */
class AsmGrammarLocalVarDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  AsmGrammarLiteralDefinition asmLiteral;
  SourceLocation loc;

  @Nullable
  AsmType asmType;

  public AsmGrammarLocalVarDefinition(Identifier id, AsmGrammarLiteralDefinition asmLiteral,
                                      SourceLocation loc) {
    this.id = id;
    this.asmLiteral = asmLiteral;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("var ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    asmLiteral.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarLocalVarDefinition that = (AsmGrammarLocalVarDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(asmLiteral, that.asmLiteral);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, asmLiteral);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}

/**
 * Represents a literal in the assembly grammar.
 * Literals have types and can be cast to another type.
 * <p>
 * A literal is any of:
 * <li>string literal</li>
 * <li>rule invocation</li>
 * <li>vadl function invocation</li>
 * <li>local variable usage</li>
 * </p>
 */
class AsmGrammarLiteralDefinition extends Definition {
  @Nullable
  Identifier id;
  List<AsmGrammarLiteralDefinition> parameters;
  @Nullable
  Expr stringLiteral;
  @Nullable
  AsmGrammarTypeDefinition asmTypeDefinition;
  SourceLocation loc;

  @Nullable
  AsmType asmType;

  public AsmGrammarLiteralDefinition(@Nullable Identifier id,
                                     List<AsmGrammarLiteralDefinition> parameters,
                                     @Nullable Expr stringLiteral, @Nullable
                                     AsmGrammarTypeDefinition asmTypeDefinition,
                                     SourceLocation loc) {
    this.id = id;
    this.parameters = parameters;
    this.stringLiteral = stringLiteral;
    this.asmTypeDefinition = asmTypeDefinition;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    if (id != null) {
      id.prettyPrint(0, builder);
      if (!parameters.isEmpty()) {
        builder.append('<');
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).prettyPrint(indent, builder);
          if (i != parameters.size() - 1) {
            builder.append(", ");
          }
        }
        builder.append('>');
      }
    }
    if (stringLiteral != null) {
      stringLiteral.prettyPrint(0, builder);
    }
    if (asmTypeDefinition != null) {
      asmTypeDefinition.prettyPrint(0, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarLiteralDefinition that = (AsmGrammarLiteralDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(stringLiteral, that.stringLiteral)
        && Objects.equals(asmTypeDefinition, that.asmTypeDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, stringLiteral, asmTypeDefinition);
  }
}

/**
 * Represents a type cast in an assembly grammar rule.
 * Contains the identifier of the type to be cast to.
 */
class AsmGrammarTypeDefinition extends Definition {
  Identifier id;
  SourceLocation loc;

  public AsmGrammarTypeDefinition(Identifier id, SourceLocation loc) {
    this.id = id;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" @");
    id.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarTypeDefinition that = (AsmGrammarTypeDefinition) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
