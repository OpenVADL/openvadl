package vadl.viam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

public sealed abstract class Register extends Definition
    permits Register.Cell, Register.File, Register.Slice, Register.Sub {

  private final ConcreteRelationType type;

  public Register(Identifier identifier, ConcreteRelationType type) {
    super(identifier);
    this.type = type;
  }

  public DataType resultType() {
    return (DataType) type.resultType();
  }

  public boolean hasAddress() {
    return !type.argTypes().isEmpty();
  }

  public @Nullable DataType addressType() {
    if (type.argTypes().isEmpty()) {
      return null;
    }
    return (DataType) type.argTypes().get(0);
  }

  public ConcreteRelationType relationType() {
    return type;
  }

  public File asFile() {
    ensure(this instanceof File, "Not a register file");
    return (File) this;
  }

  public Cell asCell() {
    ensure(this instanceof Cell, "Not a register cell");
    return (Cell) this;
  }

  public Sub asSubRegister() {
    ensure(this instanceof Sub, "Not a sub register");
    return (Sub) this;
  }

  @Override
  public void verify() {
    ensure(type.resultType() instanceof DataType,
        "Invalid register type. Must result in DataType, was: %s", type);
    ensure(type.argTypes().size() <= 1, "Type must have at most one argument, was: %s", type);
    if (hasAddress()) {
      ensure(type.argTypes().get(0) instanceof DataType,
          "Address type must be a DataType, was: %s", type.argTypes().get(0));
    }
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  public enum AccessKind {
    FULL,
    PARTIAL
  }

  public static final class Cell extends Register {

    private final List<Sub> subRegisters;
    private final AccessKind readAccess;
    private final AccessKind writeAccess;

    public Cell(Identifier identifier, DataType resultType, List<Sub> subRegisters,
                AccessKind readAccess, AccessKind writeAccess) {
      super(identifier, Type.concreteRelation(resultType));
      this.subRegisters = subRegisters;
      this.readAccess = readAccess;
      this.writeAccess = writeAccess;
    }

    public Cell(Identifier identifier, DataType resultType, AccessKind readAccess,
                AccessKind writeAccess) {
      this(identifier, resultType, List.of(), writeAccess, readAccess);
    }

    public Stream<Sub> subRegisters() {
      return subRegisters.stream();
    }

    public AccessKind readAccess() {
      return readAccess;
    }

    public AccessKind writeAccess() {
      return writeAccess;
    }

    @Override
    public void verify() {
      super.verify();
      subRegisters.forEach(e ->
          e.ensure(e.parent == this, "SubRegister has invalid parent. %s instead of %s",
              e.parent,
              this)
      );
    }


    @Override
    public String toString() {
      return "register " + identifier + ": " + resultType();
    }
  }

  public static final class Sub extends Register {

    private final Format.Field fieldRef;
    private final Cell parent;

    public Sub(Identifier identifier, Format.Field fieldRef, Cell parent) {
      super(identifier, Type.concreteRelation(fieldRef.type()));

      this.fieldRef = fieldRef;
      this.parent = parent;
    }

    public Format.Field formatField() {
      return fieldRef;
    }

    public Cell parent() {
      return parent;
    }

    public AccessKind readAccess() {
      return parent.readAccess();
    }

    public AccessKind writeAccess() {
      return parent.writeAccess();
    }

    @Override
    public String toString() {
      return "register " + identifier + ": " + resultType();
    }
  }

  public static final class Slice extends Register {

    private final Format.Field fieldRef;
    private final Cell parent;

    public Slice(Identifier identifier, Format.Field fieldRef, Cell parent) {
      super(identifier, Type.concreteRelation(fieldRef.type()));

      this.fieldRef = fieldRef;
      this.parent = parent;
    }
  }


  public static final class File extends Register {

    private final List<Constraint> constraints;

    public File(Identifier identifier,
                DataType addressType,
                DataType resultType
    ) {
      super(identifier, Type.concreteRelation(addressType, resultType));
      constraints = new ArrayList<>();
    }

    public Stream<Constraint> constraints() {
      return constraints.stream();
    }

    public void addConstraint(Constraint constraint) {
      constraints.add(constraint);
    }

    @Override
    public DataType addressType() {
      return (DataType) relationType().argTypes().get(0);
    }

    @Override
    public String toString() {
      return "register file " + identifier + ": " + relationType();
    }

    public record Constraint(
        File parent,
        Constant.Value address,
        Constant.Value value
    ) {

      public Constraint(File parent,
                        Constant.Value address,
                        Constant.Value value) {
        this.parent = parent;
        this.address = address;
        this.value = value;

        parent.ensure(address.type().canBeCastTo(parent.addressType()),
            "Address value of register constraint is of wrong type. Constraint: %s", this);

        parent.ensure(value.type().canBeCastTo(parent.resultType()),
            "Value value of register constraint is of wrong type. Constraint: %s", this);
      }

      @Override
      public String toString() {
        return parent.identifier.name() + "(" + address + ") = " + value;
      }
    }
  }

}
