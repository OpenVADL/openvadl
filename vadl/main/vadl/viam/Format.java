package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FieldRefNode;

import javax.annotation.Nullable;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * The format definition of a VADL specification.
 *
 * <p>Each field has a bit-slice and type.</p>
 */
public class Format extends Definition {

  private final Type type;
  private final List<Field> fields = new ArrayList<>();
  private final List<FieldAccess> fieldAccesses = new ArrayList<>();

  public Format(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  public void addField(Field field) {
    fields.add(field);
  }

  public void addFields(Field... fields) {
    this.fields.addAll(List.of(fields));
  }

  public void addFieldAccess(FieldAccess fieldAccess) {
    fieldAccesses.add(fieldAccess);
  }

  public Stream<Field> fields() {
    return fields.stream();
  }

  public Stream<FieldAccess> fieldAccesses() {
    return fieldAccesses.stream();
  }

  public Stream<FieldAccess> immediates() {
    return fieldAccesses.stream();
  }

  public Type type() {
    return type;
  }

  @Override
  public String toString() {
    return "Format{ " + identifier + ": " + type + "{\n\t"
        + Stream.concat(fields.stream(), fieldAccesses.stream())
        .map(Definition::toString)
        .collect(Collectors.joining("\n\t"))
        + "\n}";
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * A field of a format.
   * Holds information about the type, ranges, and value of the field.
   * This is not an immediate definition field.
   *
   * <p>It generates a function to extract the field from a passed value of the format type.</p>
   */
  public static class Field extends Definition {

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
    public Field(
        Identifier identifier,
        DataType type,
        Constant.BitSlice bitSlice,
        Format parentFormat
    ) {
      super(identifier);

      this.type = type;
      this.bitSlice = bitSlice;
      this.parentFormat = parentFormat;

      verify();
    }

    public Constant.BitSlice bitSlice() {
      return bitSlice;
    }

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
     * Generates a function that extracts the field from the instruction.
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
      var function = new Function(ident, List.of(formatParam), this.type);

      var behavior = function.behavior();
      var funcParamNode = behavior.add(new FuncParamNode(formatParam));
      var sliceNode = behavior.add(
          new SliceNode(funcParamNode, bitSlice, Type.bits(bitSlice.bitSize()))
      );
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
  public static class FieldAccess extends Definition {

    private final Function accessFunction;
    private final Function encoding;
    private final Function predicate;
    private final Field fieldRef;


    /**
     * Constructs a new FieldAccess object with the given identifier, accessFunction function,
     * encoding function, and predicate function.
     *
     * @param identifier     The identifier of the Immediate.
     * @param accessFunction The access function of the FieldAccess.  {@code () -> T}
     * @param encoding       The encoding function of the Immediate.  {@code (var: T) -> R}
     * @param predicate      The predicate function of the Immediate. {@code (var: T) -> Bool}
     */
    public FieldAccess(Identifier identifier, Function accessFunction, Function encoding,
                       Function predicate) {
      super(identifier);

      var decodeFormatRefs = accessFunction.behavior().getNodes(FieldRefNode.class).toList();
      ensure(decodeFormatRefs.size() == 1,
          "Immediate decode function must reference exactly one format field. Got: %s",
          decodeFormatRefs);

      this.fieldRef = decodeFormatRefs.get(0).formatField();
      this.accessFunction = accessFunction;
      this.encoding = encoding;
      this.predicate = predicate;

      encoding.ensure(encoding.returnType() instanceof DataType
              && ((DataType) encoding.returnType()).canBeCastTo(fieldRef.type()),
          "Encoding type mismatch. Couldn't match encoding type %s with field reference type %s",
          encoding.returnType(), fieldRef().type());
    }

    public Function decoding() {
      return accessFunction;
    }

    public Function encoding() {
      return encoding;
    }

    public Function predicate() {
      return predicate;
    }

    public Field fieldRef() {
      return fieldRef;
    }

    public Type type() {
      return accessFunction.returnType();
    }

    @Override
    public String toString() {
      return "FieldAccess{ " + accessFunction.name() + " = " + accessFunction.signature() + " }";
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }
  }

}
