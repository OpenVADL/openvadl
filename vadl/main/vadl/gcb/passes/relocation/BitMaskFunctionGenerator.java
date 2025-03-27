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

package vadl.gcb.passes.relocation;

import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.utils.SourceLocation;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A relocation is a function where we have an old value, we know the format and have a new value.
 * This class will generate a {@link Function} which changes the old value (=immediate) in the
 * format with the new value.
 */
public class BitMaskFunctionGenerator {
  public static String generateFunctionName(Format format, Format.Field field) {
    return format.identifier.lower() + "_" + field.identifier.simpleName();
  }

  /**
   * Generates a {@link Function} which updates the value with the given {@code field}.
   *
   * @param format which the relocation updates.
   * @param field  which should be updated. It must be a field of the {@code format}.
   * @return a {@link Function} which updates the value.
   */
  public static GcbUpdateFieldRelocationCppFunction generateUpdateFunction(Format format,
                                                                           Format.Field field) {
    var parameterInstWord =
        new Parameter(new Identifier("instWord", SourceLocation.INVALID_SOURCE_LOCATION),
            format.type());
    var parameterNewValue =
        new Parameter(new Identifier("newValue", SourceLocation.INVALID_SOURCE_LOCATION),
            format.type());

    return new GcbUpdateFieldRelocationCppFunction(
        new Identifier(generateFunctionName(format, field), SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[] {parameterInstWord, parameterNewValue},
        format.type(),
        getBehavior(format, field, parameterInstWord, parameterNewValue));
  }

  private static Graph getBehavior(Format format,
                                   Format.Field field,
                                   Parameter parameterInstWord,
                                   Parameter parameterNewValue) {
    var graph = new Graph("updatingValue");
    var ty = format.type();

    var node = new ReturnNode(new CppUpdateBitRangeNode(
        ty,
        new FuncParamNode(parameterInstWord),
        new FuncParamNode(parameterNewValue),
        field
    ));
    graph.addWithInputs(new StartNode(node));
    graph.addWithInputs(node);

    return graph;
  }
}
