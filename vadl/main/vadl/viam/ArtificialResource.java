package vadl.viam;

import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public class ArtificialResource extends Resource {

  private final DataType addressType;
  private final DataType resultType;

  private final Function readFunction;
  private final Procedure writeProcedure;


  public ArtificialResource(Identifier identifier,
                            Function readFunction,
                            Procedure writeProcedure
  ) {
    super(identifier);
    this.readFunction = readFunction;
    this.writeProcedure = writeProcedure;
    this.addressType = (DataType) readFunction.parameters()[0].type();
    this.resultType = (DataType) readFunction.returnType();
  }


  public Function readFunction() {
    return readFunction;
  }

  public Procedure writeProcedure() {
    return writeProcedure;
  }

  @Override
  public void verify() {
    super.verify();
    ensure(readFunction.returnType().isData(), "Read return type must be a data type");
    ensure(readFunction.parameters().length == 1, "Read function must have exactly one parameter");
    ensure(writeProcedure.parameters().length == 2,
        "Write function must have exactly one parameter");
    var readParam = readFunction.parameters()[0];
    var writeAddrParam = writeProcedure.parameters()[0];
    var writeValParam = writeProcedure.parameters()[1];
    ensure(readParam.type().isData(), "Read type must be a data type");
    ensure(writeAddrParam.type().isData(), "Write address type must be a data type");
    ensure(writeValParam.type().isData(), "Write value type must be a data type");

    ensure(readFunction.returnType().isTrivialCastTo(resultType),
        "Read return type must match result type");
    ensure(readParam.type().isTrivialCastTo(addressType),
        "Read address type must match address type");
    ensure(writeAddrParam.type().isTrivialCastTo(addressType),
        "Write address type must match address type");
    ensure(writeValParam.type().isTrivialCastTo(resultType),
        "Write value type must match result type");
  }

  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
  public DataType addressType() {
    return addressType;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(addressType, resultType);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
