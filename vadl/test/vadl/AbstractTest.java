package vadl;

import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

public abstract class AbstractTest {
  protected static Identifier createIdentifier(String name) {
    return new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION);
  }

  protected static Specification createSpecification(String name) {
    return new Specification(createIdentifier(name));
  }

  protected static Format createFormat(String name, BitsType ty) {
    return new Format(createIdentifier(name), ty);
  }

  protected static Format.FieldAccess createFieldAccess(String name, Function accessFunction) {
    return new Format.FieldAccess(createIdentifier(name), accessFunction, null, null);
  }

  protected static Function createFunction(String name, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {}, retTy);
  }

  protected static Function createFunction(String name, Parameter param, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {param}, retTy);
  }

  protected static Function createFunctionWithoutParam(String name, DataType retTy) {
    return new Function(createIdentifier(name), new Parameter[] {}, retTy);
  }

  protected static Parameter createParameter(String name, DataType ty) {
    return new Parameter(createIdentifier(name), ty);
  }

  protected static Format.Field createFieldWithParent(String name, DataType ty,
                                                      Constant.BitSlice slice, int bitWidthFormat) {
    var parent = new Format(createIdentifier(name + ".format"), BitsType.bits(bitWidthFormat));
    return createField(name, ty, slice, parent);
  }

  protected static Format.Field createField(String name, DataType ty, Constant.BitSlice slice,
                                            Format parent) {
    return new Format.Field(createIdentifier(name), ty, slice, parent);
  }

  protected static Assembly createAssembly(String name) {
    return new Assembly(createIdentifier(name), new Function(createIdentifier(name + ".assembly"),
        new Parameter[] {}, DataType.string()));
  }

  protected static Instruction createInstruction(String name, BitsType ty) {
    var format = createFormat(name + ".format", ty);
    return createInstruction(name, format);
  }

  protected static Instruction createInstruction(String name, Format format) {
    return new Instruction(createIdentifier(name), new Graph(name + ".graph"),
        createAssembly(name + ".assembly"),
        new Encoding(createIdentifier(name + ".encoding"),
            format,
            new Encoding.Field[] {}));
  }
}
