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

package vadl.viam.passes.statusBuiltInInlinePass;

import static vadl.utils.GraphUtils.getUsagesByUnrollingLets;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInStatusOnlyDispatcher;
import vadl.utils.ViamUtils;
import vadl.viam.DefProp;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.TupleGetFieldNode;

/**
 * Removes the status builtin when only the result is used and not the flags.
 * <pre>
 *       instruction ADDS_0_WITH_STATUS_REG: TMP = {
 *          let result, flags = VADL::adds(X(ONE), 0 as Bits<32>) in {
 *             X(ONE) := result as Bits<32>
 *          }
 *       }
 * </pre>
 */
public class RemoveUnusedStatusFlagsFromBuiltinsPass extends Pass {
  public RemoveUnusedStatusFlagsFromBuiltinsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("BuiltinResultInlinerPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream().flatMap(x -> ((DefProp.WithBehavior) x).behaviors().stream())
        .flatMap(behavior -> behavior.getNodes(BuiltInCall.class))
        .filter(builtInCall -> builtInCall.builtIn().isStatusBuiltin())
        .forEach(builtInCall -> {
          var tupleGets = getUsagesByUnrollingLets(builtInCall)
              .filter(TupleGetFieldNode.class::isInstance)
              .map(TupleGetFieldNode.class::cast)
              .toList();

          TupleGetFieldNode resultUser = null;
          for (var node : tupleGets) {
            switch (node.index()) {
              case 0 -> resultUser = node;
              case 1 -> {
                // If the flags are accessed then we can stop iterating because we cannot inline
                // the result.
                return;
              }
              default -> throw new ViamGraphError("User accesses non existing index.")
                  .addContext(node)
                  .addContext("built-in call", builtInCall);
            }
          }

          if (resultUser != null) {
            resultUser.replaceByNothingAndDelete();
            var inliner = new ResultInliner();
            inliner.dispatch(builtInCall, builtInCall.builtIn());
          }
        });

    return null;
  }
}

class ResultInliner implements VadlBuiltInStatusOnlyDispatcher<BuiltInCall> {

  void inlineDefault(BuiltInCall builtInCall, BuiltInTable.BuiltIn equivalent) {
    builtInCall.replaceAndDelete(equivalent.call(builtInCall.arguments()));
  }

  void inlineDefaultCarry(BuiltInCall input, BuiltInTable.BuiltIn equivalent) {
    var newNode = equivalent.call(equivalent.call(input.arguments().get(0),
            input.arguments().get(1)),
        GraphUtils.zeroExtend(input.arguments().get(2),
            input.arguments().get(0).type().asDataType()));
    input.replaceAndDelete(newNode);
  }

  private void throwNotImplemented(BuiltInCall input) {
    throw Diagnostic.error("Built-In Lowering Not Implemented", input)
        .description("OpenVADL does not yet implement the inlining of the %s built-in.",
            input.builtIn().name())
        .build();
  }

  @Override
  public void handleADDS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.ADD);
  }

  @Override
  public void handleADDC(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.ADD);
  }

  @Override
  public void handleSSATADDS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SSATADD);
  }

  @Override
  public void handleUSATADDS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.USATADD);
  }

  @Override
  public void handleSSATADDC(BuiltInCall input) {
    throw new ViamError("not implemented");
  }

  @Override
  public void handleUSATADDC(BuiltInCall input) {
    throw new ViamError("not implemented");
  }

  @Override
  public void handleSUBSC(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SUB);
  }

  @Override
  public void handleSUBSB(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SUB);
  }

  @Override
  public void handleSUBC(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.SUB);
  }

  @Override
  public void handleSUBB(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.SUB);
  }

  @Override
  public void handleSSATSUBS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SSATSUB);
  }

  @Override
  public void handleUSATSUBS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.USATSUB);
  }

  @Override
  public void handleSSATSUBC(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.SSATSUB);
  }

  @Override
  public void handleUSATSUBC(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.USATSUB);
  }

  @Override
  public void handleSSATSUBB(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.SSATSUB);
  }

  @Override
  public void handleUSATSUBB(BuiltInCall input) {
    inlineDefaultCarry(input, BuiltInTable.USATSUB);
  }

  @Override
  public void handleMULS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.MUL);
  }

  @Override
  public void handleSMULLS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SMULL);
  }

  @Override
  public void handleUMULLS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.UMULL);
  }

  @Override
  public void handleSUMULLS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SUMULL);
  }

  @Override
  public void handleSMODS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SMOD);
  }

  @Override
  public void handleUMODS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.UMOD);
  }

  @Override
  public void handleSDIVS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.SDIV);
  }

  @Override
  public void handleUDIVS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.UDIV);
  }

  @Override
  public void handleANDS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.AND);
  }

  @Override
  public void handleXORS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.XOR);
  }

  @Override
  public void handleORS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.OR);
  }

  @Override
  public void handleLSLS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleLSLC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleASRS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleLSRS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleASRC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleLSRC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleROLS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.ROL);
  }

  @Override
  public void handleROLC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleRORS(BuiltInCall input) {
    inlineDefault(input, BuiltInTable.ROR);
  }

  @Override
  public void handleRORC(BuiltInCall input) {
    throwNotImplemented(input);
  }
}

