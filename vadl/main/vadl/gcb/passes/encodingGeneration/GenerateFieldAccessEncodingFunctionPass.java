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

package vadl.gcb.passes.encodingGeneration;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.passes.encodingGeneration.strategies.EncodingGenerationStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ArithmeticImmediateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ShiftedImmediateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.TrivialImmediateStrategy;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * This pass generates the {@link FieldAccess#encoding()} when the {@link Function} is {@code null}.
 * <pre>{@code
 * format Utype : Inst =
 * {     imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = imm as UInt<32>
 * }
 * }</pre>
 * This class should compute the following encoding function automatically:
 * <pre>{@code
 * encode {
 * imm => immU(19..0)
 * }
 * }</pre>
 */
public class GenerateFieldAccessEncodingFunctionPass extends Pass {

  private static final Logger logger = LoggerFactory.getLogger(
      GenerateFieldAccessEncodingFunctionPass.class);

  public static final List<EncodingGenerationStrategy> strategies = List.of(
      new TrivialImmediateStrategy(),
      new ShiftedImmediateStrategy(),
      new ArithmeticImmediateStrategy());

  public GenerateFieldAccessEncodingFunctionPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {
    viam.isa()
        .map(x -> x.ownFormats().stream())
        .orElseGet(Stream::empty)
        .flatMap(x -> x.fieldAccesses().stream())
        .filter(x -> x.encoding() == null)
        .forEach(fieldAccess -> {
          // We need to compute multiple encoding functions based on the field access function.
          // Different field access functions require different heuristics for the encoding.
          for (var strategy : strategies) {
            if (strategy.checkIfApplicable(fieldAccess)) {
              strategy.generateEncoding(fieldAccess);
              break;
            }
          }
        });

    var hasNoEncoding = viam.findAllFormats()
        .flatMap(x -> x.fieldAccesses().stream())
        .filter(x -> x.encoding() == null)
        .toList();

    if (!hasNoEncoding.isEmpty()) {
      for (var format : hasNoEncoding) {
        logger.atError().log("Format {} has no encoding", format.simpleName());
      }
      throw new ViamError("Not all formats have an encoding");
    }


    return null;
  }
}
