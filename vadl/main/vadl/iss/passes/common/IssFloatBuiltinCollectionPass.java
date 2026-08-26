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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.ViamUtils;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * Analyzes all VIAM behaviors and creates a list of all called float built-ins and
 * for each a set of occurring configurations. The configuration is just a list of types, made
 * up of the type parameters and relevant argument types of the call.
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
   * type parameters (including types of arguments).
   */
  public record Output(Map<BuiltInTable.BuiltIn, Set<List<Type>>> floatBuiltIns) {
  }

  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    IdentityHashMap<BuiltInTable.BuiltIn, Set<List<Type>>> floatBuiltIns =
        new IdentityHashMap<>();

    ViamUtils.findAllBehaviors(viam).forEach(g -> g.getNodes(BuiltInCall.class)
        .filter(call -> BuiltInTable.FLOAT_BUILT_INS.contains(call.builtIn()))
        .forEach(call -> handleFloatBuiltin(call, floatBuiltIns)));

    return new Output(floatBuiltIns);
  }

  private void handleFloatBuiltin(BuiltInCall call,
                                  IdentityHashMap<BuiltInTable.BuiltIn, Set<List<Type>>>
                                      floatBuiltIns) {
    var builtin = call.builtIn();
    var config = new ArrayList<>(call.typeParams());
    // Note: we only add the type of the first argument to the config, since all float built-ins
    //       take at least one argument, and all argument types are the same (ignoring frm)
    config.addFirst(call.originalArgTypes().getFirst());
    floatBuiltIns.computeIfAbsent(builtin, b -> new HashSet<>()).add(config);
  }

}
