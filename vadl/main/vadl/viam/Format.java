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

package vadl.viam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * The format definition of a VADL specification.
 *
 * <p>Each field has a bit-slice and type.</p>
 */
public class Format extends Definition implements DefProp.WithType {

  private final BitsType type;
  private Field[] fields;
  private List<FieldAccess> fieldAccesses;

  /**
   * Constructs a new instance of a VADL format.
   *
   * @param identifier The identifier of the format.
   * @param type       The type of the format.
   */
  public Format(Identifier identifier, BitsType type) {
    super(identifier);
    this.type = type;
    this.fields = new Field[] {};
    this.fieldAccesses = new ArrayList<>();
  }

  public Field[] fields() {
    return fields;
  }

  /**
   * Used by VIAM builder only.
   */
  public void setFields(Field[] fields) {
    this.fields = Stream.of(fields).sorted(Comparator.comparingInt(a -> -a.bitSlice.msb()))
        .toArray(Field[]::new);
  }

  public List<FieldAccess> fieldAccesses() {
    return fieldAccesses;
  }

  public void addFieldAccess(FieldAccess fieldAccess) {
    fieldAccesses.add(fieldAccess);
  }

  /**
   * Used by VIAM builder only.
   */
  public void setFieldAccesses(FieldAccess[] fieldAccesses) {
    this.fieldAccesses = new ArrayList<>(Arrays.stream(fieldAccesses).toList());
  }

  @Override
  public BitsType type() {
    return type;
  }

  @Override
  public String toString() {
    return "Format{ " + identifier + ": " + type + "{\n\t"
        + Stream.concat(Stream.of(fields), fieldAccesses.stream()).map(Definition::toString)
        .collect(Collectors.joining("\n\t")) + "\n}";
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Format format = (Format) o;
    return Objects.equals(type, format.type) && Arrays.equals(fields, format.fields)
        && fieldAccesses.equals(format.fieldAccesses);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(type);
    result = 31 * result + Arrays.hashCode(fields);
    result = 31 * result + fieldAccesses.hashCode();
    return result;
  }

  /**
   * A field of a format.
   * Holds information about the type, ranges, and value of the field.
   * This is not an immediate definition field.
   *
   * <p>It generates a function to extract the field from a passed value of the format type.</p>
   */
  public static class Field extends Definition implements DefProp.WithType {

    private final DataType type;
    private final Constant.BitSlice bitSlice;

    // nullable because lazily initialized
    @Nullable
    private Function extractFunction;

    private final Format parentFormat;
    @Nullable
    private Format refFormat;

    /**
     * Constructs a Field object with the given identifier, type, ranges, and encoding.
     *
     * @param identifier   the identifier of the field
     * @param type         the type of the field
     * @param bitSlice     the constant bitslice of the instruction for this field
     * @param parentFormat the parent format of the field
     */
    public Field(Identifier identifier, DataType type, Constant.BitSlice bitSlice,
                 Format parentFormat) {
      super(identifier);

      this.type = type;
      this.bitSlice = bitSlice;
      this.parentFormat = parentFormat;
    }

    public Constant.BitSlice bitSlice() {
      return bitSlice;
    }

    @Override
    public DataType type() {
      return type;
    }

    public Format format() {
      return parentFormat;
    }

    public int size() {
      return bitSlice.bitSize();
    }

    /**
     * Returns a function that extracts the field from the instruction.
     * It takes one argument of the format type and returns a value of the field type.
     */
    public Function extractFunction() {
      if (extractFunction == null) {
        this.extractFunction = createExtractFunction();
      }
      return extractFunction;
    }

    /**
     * Returns the reference format of the field.
     */
    @Nullable
    public Format refFormat() {
      return refFormat;
    }

    /**
     * Adds a format reference to the field.
     */
    public void setRefFormat(Format refFormat) {
      ensure(this.refFormat == null, "Field reference format already set");
      this.refFormat = refFormat;
    }

    @Override
    public void verify() {
      super.verify();
      ensure(bitSlice.bitSize() == type.bitWidth(),
          "Field type width of %s is different to slice size of %s", type.bitWidth(),
          bitSlice.bitSize());
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public String toString() {
      var ref = refFormat != null ? " -> " + refFormat.identifier : "";
      return "Field{ " + identifier + " " + bitSlice + ": " + type + ref + " }";
    }

    /**
     * Generates a function that extracts the field from the instruction.
     * It takes one argument of the format type and returns a value of the field type.
     */
    private Function createExtractFunction() {
      var ident = identifier.extendSimpleName("_extract");
      var paramIdent = ident.append("format");
      var formatParam = new Parameter(paramIdent, parentFormat.type());
      var function = new Function(ident, new Parameter[] {formatParam}, this.type);
      formatParam.setParent(function);

      var behavior = function.behavior();
      var funcParamNode = behavior.add(new FuncParamNode(formatParam));
      var sliceNode =
          behavior.add(new SliceNode(funcParamNode, bitSlice, Type.bits(bitSlice.bitSize())));
      var returnNode = behavior.add(new ReturnNode(sliceNode));
      // add start node
      behavior.add(new StartNode(returnNode));

      // verify that produced graph is correct
      behavior.verify();

      return function;
    }
  }


  /**
   * Represents a field access in a VADL specification.
   *
   * <p>An immediate contains a decode function, an encoding function (to encode the
   * format field/fieldRef from the immediate content) and a predicate function (to
   * test if an immediate is valid).
   */
  public static class FieldAccess extends Definition implements DefProp.WithType {

    private final Function accessFunction;
    @Nullable
    private Function encoding;
    private final Function predicate;
    private final List<Field> fieldRefs;


    /**
     * Constructs a new FieldAccess object with the given identifier, accessFunction function,
     * encoding function, and predicate function.
     *
     * @param identifier     The identifier of the Immediate.
     * @param accessFunction The access function of the FieldAccess.  {@code () -> T}
     * @param encoding       The encoding function of the Immediate.  {@code (var: T) -> R}
     * @param predicate      The predicate function of the Immediate. {@code (var: T) -> Bool}
     */
    public FieldAccess(Identifier identifier, Function accessFunction, @Nullable Function encoding,
                       Function predicate) {
      super(identifier);

      this.accessFunction = accessFunction;

      var decodeFormatFields = accessFunction.behavior().getNodes(FieldRefNode.class)
          .map(FieldRefNode::formatField).toList();
      ensure(!decodeFormatFields.isEmpty(),
          "Immediate decode function must reference at least one format field. Got: %s",
          decodeFormatFields);

      this.fieldRefs = decodeFormatFields;
      this.encoding = encoding;
      this.predicate = predicate;
    }

    public Function accessFunction() {
      return accessFunction;
    }

    /**
     * If the encoding function is null, it wasn't yet inferred nor specified by the user.
     */
    @Nullable
    public Function encoding() {
      return encoding;
    }

    /**
     * Sets the encoding to the given function. This must be called
     * by the pass that infers the encoding function. It will fail
     * if the encoding was already set before.
     */
    public void setEncoding(Function encoding) {
      ViamError.ensure(this.encoding == null, () -> Diagnostic.error(
          "Cannot regenerate an encoding for a field access which already has an encoding.",
          encoding.location()));
      this.encoding = encoding;
      verify();
    }

    public Function predicate() {
      return predicate;
    }

    /**
     * Returns the first format field the field access refers to.
     *
     * @deprecated As there can be multiple format fields.
     */
    @Deprecated
    public Field fieldRef() {
      ensure(fieldRefs.size() == 1, "Only one field reference expected, but found: %s", fieldRefs);
      return fieldRefs().getFirst();
    }

    public List<Field> fieldRefs() {
      return fieldRefs;
    }

    @Override
    public Type type() {
      return accessFunction.returnType();
    }

    @Override
    public void verify() {
      super.verify();
      // TODO: We must specify how we treat encoding of multiple fields from one access function
      // if (encoding != null) {
      // ensure(encoding.returnType() instanceof DataType
      //         && encoding.returnType().isTrivialCastTo(fieldRef.type()),
      //     "Encoding type mismatch. Couldn't match encoding type %s with field reference type %s",
      //     encoding.returnType(), fieldRef().type());
      // }
    }

    @Override
    public String toString() {
      return "FieldAccess{ " + simpleName() + " = "
          + accessFunction.signature()
          + " }";
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FieldAccess that = (FieldAccess) o;
      return Objects.equals(accessFunction, that.accessFunction)
          && Objects.equals(encoding, that.encoding) && Objects.equals(predicate, that.predicate)
          && Objects.equals(fieldRefs, that.fieldRefs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(accessFunction, encoding, predicate, fieldRefs);
    }
  }
}
