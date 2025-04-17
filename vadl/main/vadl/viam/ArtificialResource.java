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

import java.util.List;
import org.jetbrains.annotations.NotNull;
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
public class ArtificialResource extends Resource {

  /**
   * A hint what the artificial resources were created from.
   */
  public enum Kind {
    REG_FILE_ALIAS
  }

  private final Kind kind;
  private final Resource innerResourceRef;

  private final Function readFunction;
  private final Procedure writeProcedure;


  /**
   * Constructs the artificial resource.
   *
   * @param innerResourceRef the actual wrapped resource
   */
  public ArtificialResource(Identifier identifier,
                            Kind kind,
                            Resource innerResourceRef,
                            Function readFunction,
                            Procedure writeProcedure
  ) {
    super(identifier);
    this.kind = kind;
    this.innerResourceRef = innerResourceRef;
    this.readFunction = readFunction;
    this.writeProcedure = writeProcedure;
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

  @Override
  public void verify() {
    super.verify();
    var readParams = readFunction.parameters();
    var writeParams = writeProcedure.parameters();
    ensure(readFunction.returnType().isData(), "Read return type must be a data type");
    ensure(readParams.length <= 1, "Read cannot have more than 1 parameter (address)");
    ensure(writeParams.length == readParams.length + 1,
        "Write must have one more param than read (because the last value is write)");
    var readParam = readFunction.parameters()[0];
    var writeAddrParam = writeProcedure.parameters()[0];
    var writeValParam = writeProcedure.parameters()[1];
    ensure(readParam.type().isData(), "Read type must be a data type");
    ensure(writeAddrParam.type().isData(), "Write address type must be a data type");
    ensure(writeValParam.type().isData(), "Write value type must be a data type");

    ensure(readFunction.returnType().isTrivialCastTo(resultType()),
        "Read return type must match result type");
    ensure(writeValParam.type().isTrivialCastTo(resultType()),
        "Write value type must match result type");
    if (hasAddress()) {
      ensure(readParam.type().isTrivialCastTo(addressType()),
          "Read address type must match address type");
      ensure(writeAddrParam.type().isTrivialCastTo(addressType()),
          "Write address type must match address type");
    }
  }

  @Override
  public boolean hasAddress() {
    return readFunction.parameters().length > 0;
  }

  @Override
  @NotNull
  public DataType addressType() {
    ensure(hasAddress(), "Resource has no address");
    return readFunction.parameters()[0].type().asDataType();
  }

  @Override
  public List<DataType> indexTypes() {
    return List.of(addressType());
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
}