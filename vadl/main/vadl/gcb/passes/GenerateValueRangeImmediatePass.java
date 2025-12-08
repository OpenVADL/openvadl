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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.passes.SnapshotInstructionBehaviorPass;

/**
 * This pass generates the ranges for immediates.
 */
public class GenerateValueRangeImmediatePass extends Pass {
  public GenerateValueRangeImmediatePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateValueRangeImmediatePass");
  }

  /**
   * Get the lowest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static long lowestPossibleValue(BitsType rawType, boolean isSigned) {
    return isSigned
        ? (long) (-1 * Math.pow(2, (double) rawType.bitWidth() - 1))
        : 0;
  }

  /**
   * Get the lowest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static long lowestPossibleValue(BitsType rawType) {
    return lowestPossibleValue(rawType, rawType.isSigned());
  }

  /**
   * Get the highest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static long highestPossibleValue(BitsType rawType) {
    return highestPossibleValue(rawType, rawType.isSigned());
  }


  /**
   * Get the highest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static long highestPossibleValue(BitsType rawType, boolean isSigned) {
    return
        (long) (isSigned
            ? Math.pow(2, (double) rawType.bitWidth() - 1)
            : Math.pow(2, rawType.bitWidth())) - 1;
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var snapshots =
        (Map<Instruction, Graph>) passResults.lastResultOf(SnapshotInstructionBehaviorPass.class);
    var fieldResult =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults
            .lastResultOf(IdentifyFieldUsagePass.class);

    viam.isa().stream().flatMap(x -> x.ownInstructions().stream())
        .forEach(instruction -> {
          var fields = fieldResult.getImmediateFields(instruction);
          var ctx = new ValueRangeCtx();
          var snapshot = ensureNonNull(snapshots.get(instruction),
              () -> Diagnostic.error("Cannot find snapshot for instruction",
                  instruction.location()));

          // Iterate over all the instruction's fields and calculate which values
          // are possible for this field.
          fields.forEach(field -> {
            var isSigned = isSigned(instruction, snapshot, field);
            var lowest = lowestPossibleValue(field.type().toBitsType(), isSigned);
            var highest = highestPossibleValue(field.type().toBitsType(), isSigned);
            var range = new ValueRange(lowest, highest);
            ctx.add(field, range);
          });

          instruction.attachExtension(ctx);
        });

    return null;
  }

  /**
   * The fields always are unsigned. To know whether the immediate is going to be
   * unsigned or signed, we have to check whether there exist a {@link FieldAccess}.
   * If it does, then take its type. If not then just use the field's type.
   */
  private boolean isSigned(Instruction instruction, Graph snapshot, Format.Field field) {
    var fieldAccesses = snapshot.getNodes(FieldAccessRefNode.class).toList();
    for (var fieldAccess : fieldAccesses) {
      for (var fieldRef : fieldAccess.fieldAccess().fieldRefs()) {
        if (fieldRef.equals(field)) {
          return fieldAccess.fieldAccess().type().asDataType().isSigned();
        }
      }
    }

    throw Diagnostic.error("Cannot get type", instruction.location()).build();
  }
}
