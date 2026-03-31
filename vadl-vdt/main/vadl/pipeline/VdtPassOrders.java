// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import static vadl.configuration.DecoderOptions.OptionToSkip.OPT_CONSTRAINT_SYNTHESIS;
import static vadl.configuration.DecoderOptions.OptionToSkip.OPT_DECODER_VERIFICATION;
import static vadl.configuration.DecoderOptions.OptionToSkip.OPT_ENCODING_VERIFICATION;

import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtEncodingConstraintValidationPass;
import vadl.vdt.passes.VdtEncodingSemanticVerificationPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.passes.VdtVerificationPass;

/**
 * Shared VDT pipeline fragments.
 */
public final class VdtPassOrders {
  private VdtPassOrders() {
  }

  public static void addDecodePasses(PassOrder order, GeneralConfiguration config) {
    order
        .add(new VdtEncodingConstraintValidationPass(config))
        .add(new VdtInputPreparationPass(config));

    var skipSynthesis = Stream.of(config.getDecoderOptions().getOptsToSkip())
        .anyMatch(o -> o == OPT_CONSTRAINT_SYNTHESIS);
    if (!skipSynthesis) {
      order.add(new VdtConstraintSynthesisPass(config));
    }

    var skipEncodingVerification = Stream.of(config.getDecoderOptions().getOptsToSkip())
        .anyMatch(o -> o == OPT_ENCODING_VERIFICATION);
    if (!skipEncodingVerification) {
      order.add(new VdtEncodingSemanticVerificationPass(config));
    }

    order.add(new VdtLoweringPass(config));

    var skipDecoderVerification = Stream.of(config.getDecoderOptions().getOptsToSkip())
        .anyMatch(o -> o == OPT_DECODER_VERIFICATION);
    if (!skipDecoderVerification) {
      order.add(new VdtVerificationPass(config));
    }
  }
}
