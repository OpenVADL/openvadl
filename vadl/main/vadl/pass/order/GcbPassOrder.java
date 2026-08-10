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

package vadl.pass.order;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.passes.DetermineBuiltinAttributesPass;
import vadl.gcb.passes.DetermineRegisterUsesAndDefsPass;
import vadl.gcb.passes.DetermineRelocationTypeForFieldPass;
import vadl.gcb.passes.GenerateCompilerRegistersPass;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.InstructionPatternPruningPass;
import vadl.gcb.passes.PredicateFunctionInlinerPass;
import vadl.gcb.passes.SetMissingConfigurationValuesPass;
import vadl.gcb.passes.assembly.AssemblyConcatBuiltinMergingPass;
import vadl.gcb.passes.assembly.AssemblyFunctionInlinerPass;
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingAndPredicateFunctionsPass;
import vadl.gcb.passes.operands.GenerateInstructionOperandsPass;
import vadl.pass.PassOrder;
import vadl.viam.passes.functionInliner.ArtificialResInlinerPass;
import vadl.viam.passes.functionInliner.FieldAccessInlinerPass;
import vadl.viam.passes.statusBuiltInInlinePass.StatusBuiltInInlinePass;

/**
 * Builds the pass order used for GCB/CPP code generation.
 */
public final class GcbPassOrder {
  private GcbPassOrder() {
  }

  /**
   * Creates the pass order.
   */
  public static PassOrder create(GcbConfiguration configuration) throws IOException {
    var order = ViamPassOrder.create(configuration);
    order.add(new SetMissingConfigurationValuesPass(configuration));
    order.skip(StatusBuiltInInlinePass.class);
    order.skip(ArtificialResInlinerPass.class);

    order.add(new GenerateCompilerRegistersPass(configuration));
    order.skip(FieldAccessInlinerPass.class);
    order.add(new IdentifyFieldUsagePass(configuration));
    order.add(new DetermineRelocationTypeForFieldPass(configuration));
    order.add(new GenerateValueRangeImmediatePass(configuration));
    order.add(new GenerateFieldAccessEncodingAndPredicateFunctionsPass(configuration));
    order.add(new PredicateFunctionInlinerPass(configuration));
    order.add(new AssemblyFunctionInlinerPass(configuration));
    order.add(new AssemblyConcatBuiltinMergingPass(configuration));
    order.add(new DetermineRegisterUsesAndDefsPass(configuration));
    order.add(new GenerateInstructionOperandsPass(configuration));
    order.add(new InstructionPatternPruningPass(configuration));
    order.add(new DetermineBuiltinAttributesPass(configuration));
    order.add(new GenerateGcbIntrinsicsPass(configuration));

    OrderSupport.addHtmlDump(order, configuration, "gcbProcessing",
        "Now the gcb produced all necessary encoding function for field accesses "
            + "and normalized VIAM types to Cpp types.");
    return order;
  }
}
