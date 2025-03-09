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

package vadl.cppCodeGen.passes.fieldNodeReplacement;

import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.pass.PassName;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Replaces all {@link FieldRefNode} by {@link FuncParamNode} but only in the
 * {@link Format.FieldAccess#accessFunction()}.
 * {@code int decodingFunction(int imm); }
 */
public class FieldNodeReplacementPassForDecoding extends FieldNodeReplacementPass {

  public FieldNodeReplacementPassForDecoding(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(FieldNodeReplacementPassForDecoding.class.getName());
  }

  @Override
  protected Stream<Function> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream())
        .orElseGet(Stream::empty)
        .flatMap(x -> x.fieldAccesses().stream())
        .map(Format.FieldAccess::accessFunction)
        .filter(Objects::nonNull);
  }
}
