// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;

/**
 * A resource that accesses another resource under the hood but may contain logic when
 * doing so.
 * For example take the following VADL definition
 * <pre>{@code
 * [zero: X(31)]                       // X31 is zero register ZR
 * alias register file X = S           // general purpose register file, X31 ZR
 * }</pre>
 * When we access X, we are actually accessing S. However, we need to add logic as we need
 * to check for 0 when accessing X. This is done using the readFunction and writeProcedure.
 * So in this case X would be turned into an artificial resource.
 */
public class ArtificialResource extends RegisterResource {

  /**
   * A hint what the artificial resources were created from.
   */
  public enum Kind {
    REGISTER
  }

  private final Kind kind;
  private final Resource innerResourceRef;

  private final Function readFunction;
  private final Procedure writeProcedure;
  @Nullable
  private final Constant.BitSlice aliasSlice;
  private final Semantics semantics;

  private final List<RegisterTensor.Dimension> dimensions;

  public enum OverwriteMode {
    MERGE,
    ZERO,
    SIGN
  }

  public record ZeroConstraint(List<Constant.Value> indices) {
  }

  public record Semantics(
      RegisterTensor baseTensor,
      List<Constant.Value> fixedIndices,
      List<RegisterTensor.Dimension> dynamicDimensions,
      @Nullable Constant.BitSlice aliasSlice,
      OverwriteMode overwriteMode,
      @Nullable ZeroConstraint zeroConstraint
  ) {
    public int totalIndexCount() {
      return fixedIndices.size() + dynamicDimensions.size();
    }
  }

  /**
   * Constructs the artificial resource.
   *
   * @param innerResourceRef the actual wrapped resource
   */
  public ArtificialResource(Identifier identifier,
                            Kind kind,
                            Resource innerResourceRef,
                            Function readFunction,
                            Procedure writeProcedure,
                            List<RegisterTensor.Dimension> dimensions,
                            @Nullable Constant.BitSlice aliasSlice,
                            Semantics semantics
  ) {
    super(identifier);
    this.kind = kind;
    this.innerResourceRef = innerResourceRef;
    this.readFunction = readFunction;
    this.writeProcedure = writeProcedure;
    this.dimensions = dimensions;
    this.aliasSlice = aliasSlice;
    this.semantics = semantics;
  }

  public Kind kind() {
    return kind;
  }

  public Resource innerResourceRef() {
    return innerResourceRef;
  }

  public Function readFunction() {
    return readFunction;
  }

  public Procedure writeProcedure() {
    return writeProcedure;
  }

  public @Nullable Constant.BitSlice aliasSlice() {
    return aliasSlice;
  }

  public Semantics semantics() {
    return semantics;
  }

  @Override
  public Identifier identifier() {
    return identifier;
  }

  @Override
  public List<RegisterTensor.Dimension> dimensions() {
    return dimensions;
  }

  public int dimCount() {
    return dimensions().size();
  }

  public int maxNumberOfAccessIndices() {
    return dimCount();
  }

  /**
   * The {@link ArtificialResource} is an alias for a register file.
   */
  @Override
  public boolean isRegisterFile() {
    return readFunction.parameters().length == 1;
  }

  /**
   * The {@link ArtificialResource} is an alias for a concrete register.
   */
  public boolean isRegister() {
    return readFunction.parameters().length == 0;
  }

  @Override
  public void verify() {
    super.verify();
    var readParams = readFunction.parameters();
    var writeParams = writeProcedure.parameters();
    ensure(readFunction.returnType().isDataType(), "Read return type must be a data type");
    ensure(writeParams.length == readParams.length + 1,
        "Write must have one more param than read (because the last value is write)");
    for (int i = 0; i < readParams.length; i++) {
      var readParam = readFunction.parameters()[i];
      var writeAddrParam = writeProcedure.parameters()[i];
      ensure(readParam.type().isDataType(), "Read type must be a data type");
      ensure(writeAddrParam.type().isDataType(), "Write address type must be a data type");
      ensure(readParam.type().isTrivialCastTo(writeAddrParam.type()),
          "Write address type must be a data type");
    }

    var writeValParam = writeParams[writeParams.length - 1];
    ensure(writeValParam.type().isDataType(), "Write value type must be a data type");

    ensure(readFunction.returnType().isTrivialCastTo(resultType()),
        "Read return type must match result type");
    ensure(writeValParam.type().isTrivialCastTo(resultType()),
        "Write value type must match result type");
  }

  @Override
  public boolean hasAddress() {
    return readFunction.parameters().length > 0;
  }

  @Override
  @Nonnull
  public DataType addressType() {
    ensure(hasAddress(), "Resource has no address");
    return readFunction.parameters()[0].type().asDataType();
  }

  @Override
  public List<DataType> indexTypes() {
    return dimensions().stream().limit(maxNumberOfAccessIndices())
        .map(RegisterTensor.Dimension::indexType).toList();
  }

  @Override
  public DataType resultType() {
    return readFunction.returnType().asDataType();
  }

  @Override
  public DataType resultType(int providedDimensions) {
    return resultType();
  }

  @Override
  public ConcreteRelationType relationType() {
    return readFunction.signature();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "alias " + kind().name().toLowerCase() + " " + identifier.simpleName() + ": " + type();
  }
}
