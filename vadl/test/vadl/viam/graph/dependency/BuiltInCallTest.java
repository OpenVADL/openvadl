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

package vadl.viam.graph.dependency;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static vadl.types.BuiltInTable.COMMUTATIVE;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;

class BuiltInCallTest {

  private static final SIntType SIGNED_INT = Type.signedInt(32);
  private static final ConstantNode constantNode = new ConstantNode(Constant.Value.of(
      1, SIGNED_INT)
  );


  private static Stream<Arguments> getCanonicalizableBuiltin() {
    return COMMUTATIVE.stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("getCanonicalizableBuiltin")
  void canonicalize_shouldSortConstantLast(BuiltInTable.BuiltIn builtin) {
    var node = new BuiltInCall(builtin, new NodeList<>(
        new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32))),
        new FieldRefNode(null, DataType.unsignedInt(32))
    ), Type.unsignedInt(32));

    node = (BuiltInCall) node.canonical();

    assertThat(node.arguments().size()).isEqualTo(2);
    assertThat(node.arguments().get(0).getClass()).isEqualTo(FieldRefNode.class);
    assertThat(node.arguments().get(1).getClass()).isEqualTo(ConstantNode.class);
  }

  @Test
  void verifyState_shouldThrowException_whenNotEnoughArguments() {
    var operation = new BuiltInCall(BuiltInTable.ADD,
        new NodeList<>(List.of(
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(31)))
        )),
        DataType.unsignedInt(32));

    var throwable = assertThrows(ViamGraphError.class, operation::verifyState);
    assertEquals("Number of arguments must match, 2 vs 1", throwable.getContextlessMessage());
  }

  @Test
  void verifyState_shouldThrowException_whenResultDoesNotMatch() {
    var operation = new BuiltInCall(BuiltInTable.ADD,
        new NodeList<>(List.of(
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(31))),
            new ConstantNode(Constant.Value.of(1, DataType.unsignedInt(32)))
        )),
        DataType.unsignedInt(32));

    var throwable = assertThrows(ViamGraphError.class, operation::verifyState);
    assertThat(throwable.getContextlessMessage()).contains(
        "Arguments' types do not match with the type of the builtin");
  }

}