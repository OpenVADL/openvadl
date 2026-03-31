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

package vadl.gcb.passes.assembly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * It is easier to generate a parser when the {@link BuiltInCall} with
 * {@link vadl.types.BuiltInTable#CONCATENATE_STRINGS} are merged into a
 * single builtin.
 */
public class AssemblyConcatBuiltinMergingPass extends Pass {

  public AssemblyConcatBuiltinMergingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("assemblyConcatBuiltinMergingPass");
  }


  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    // It is a candidate when it is a builtin with concatenate string
    // and all users are not concatenate string (so it is a "root")
    // and one argument is also a builtin with concatenate string.
    var candidates = viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElseGet(Stream::empty)
        .flatMap(
            instruction -> {
              var nodes = new ArrayList<BuiltInCall>();
              var returnNode =
                  instruction.assembly().function().behavior().getNodes(ReturnNode.class)
                      .findFirst().orElseThrow();
              returnNode.collectInputsWithChildren(nodes, BuiltInCall.class);
              return nodes.stream();
            })
        .filter(builtin -> builtin.builtIn() == BuiltInTable.CONCATENATE_STRINGS)
        // only find concat strings that are not used by other concat strings.
        // we will recursively resolve them, so we want only the root objects.
        .filter(builtIn -> builtIn.usages().noneMatch(
            user -> user instanceof BuiltInCall userBuiltIn
                && userBuiltIn.builtIn() == BuiltInTable.CONCATENATE_STRINGS))
        // we skip those that don't have a string concat built-in as an argument.
        .filter(built -> built.arguments().stream().anyMatch(
            arg -> arg instanceof BuiltInCall argNode
                && argNode.builtIn() == BuiltInTable.CONCATENATE_STRINGS))
        .toList();

    // Insert the child's arguments into parent's argument
    for (var candidate : candidates) {
      mergeStringConcatAndChildren(candidate);
    }

    return null;
  }

  /*
  Traverses the string concat built-ins in a deep-first fashion and replaces a string concat
  that uses a string-concat as an argument by merging all string arguments.
  As this is recursive, there are no more nested string concats.
   */
  private static void mergeStringConcatAndChildren(BuiltInCall builtInCall) {
    if (builtInCall.builtIn() != BuiltInTable.CONCATENATE_STRINGS) {
      return;
    }

    var hadStringConcatArg = false;
    for (var arg : builtInCall.arguments()) {
      if (arg instanceof BuiltInCall argNode
          && argNode.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
        // if arg is also string concat, we merge them
        mergeStringConcatAndChildren(argNode);
        hadStringConcatArg = true;
      }
    }

    if (!hadStringConcatArg) {
      // there is nothing to be merged
      return;
    }

    var mergedArgs = new ArrayList<ExpressionNode>();
    for (var arg : builtInCall.arguments()) {
      // add all args to the mergedArgs

      if (arg instanceof BuiltInCall argNode
          && argNode.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
        // if arg is a string concat, we add all its arguments
        mergedArgs.addAll(argNode.arguments());
      } else {
        // otherwise we just add the arg
        mergedArgs.add(arg);
      }
    }

    var newStringConcat = BuiltInCall.of(
        BuiltInTable.CONCATENATE_STRINGS,
        mergedArgs
    );

    // replace new merged string concat
    builtInCall.replaceAndDelete(newStringConcat);
  }
}
