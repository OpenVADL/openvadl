// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.common;

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Analyzes all VIAM behaviors and creates a list of all called float built-ins and
 * for each a set of occurring configurations. The configuration is usually just the constant
 * parameters the builtin has been called with (i.e. the parameters in the angle brackets {@code
 * VADL::builtin<...>()}).
 *
 * <p>For some builtins, additional information is added to the configuration (e.g. for
 * {@link BuiltInTable#FCVTSF} and {@link BuiltInTable#FCVTUF}, the bit-size of the operand
 * is added).
 *
 * <p>The configuration can later be used to only emit helper functions that are actually required.
 */
public class IssFloatBuiltinCollectionPass extends AbstractIssPass {

  public IssFloatBuiltinCollectionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Float Built-in Collection");
  }

  /**
   * Output of the pass.
   * {@code floatBuiltIns} saves all occurring float built-ins and for each a set of all unique
   * constant parameters.
   */
  public record Output(Map<BuiltInTable.BuiltIn, Set<List<Constant>>> floatBuiltIns) {
  }

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    IdentityHashMap<BuiltInTable.BuiltIn, Set<List<Constant>>> floatBuiltIns =
        new IdentityHashMap<>();

    ViamUtils.findAllBehaviors(viam).forEach(g -> g.getNodes(BuiltInCall.class)
        .filter(call -> BuiltInTable.FLOAT_BUILT_INS.contains(call.builtIn()))
        .forEach(call -> handleFloatBuiltin(call, floatBuiltIns)));

    return new Output(floatBuiltIns);
  }

  private void handleFloatBuiltin(BuiltInCall call,
                                  IdentityHashMap<BuiltInTable.BuiltIn, Set<List<Constant>>>
                                      floatBuiltIns) {
    // TODO: here we should check if the constant parameters form a supported float built-in config
    //       e.g. VADL::fcvt<IEEE32, IEEE32>(...) is not valid, but this should be checked in the
    //       frontend. But we may not support 4-bit floats, so VADL::fadd<MINI4>(...) is valid, but
    //       not supported.

    // TODO: currently this treats every float-type declaration as a unique config. but if two use
    //       the same encoding (and relevant settings), we could convert the format to the encoding
    //       and avoid unnecessary helper declarations.

    var builtin = call.builtIn();
    var config = call.constArgs();
    if (builtin == BuiltInTable.FCVTSF || builtin == BuiltInTable.FCVTUF) {
      // Node: For these builtins, the size of the int arguments is inferred by the type-checker.
      //       But the helper emitter needs to know the size, so it gets added to the config.
      var size = ((ExpressionNode) call.inputs().findFirst().get()).type().asDataType().bitWidth();
      ensure(size == 32 || size == 64, "Expected VADL::fcvt[su]f input size to be either 32 or 64");
      // the old config may be an immutable list
      config = Stream.concat(config.stream(), Stream.of(GraphUtils.intU(size, 32))).toList();
      call.setConstArgs(config);
    }
    floatBuiltIns.computeIfAbsent(builtin, b -> new HashSet<>()).add(config);
  }

}
