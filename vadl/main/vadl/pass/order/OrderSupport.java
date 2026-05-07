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

import static vadl.configuration.DecoderOptions.OptionalStep.OPT_ALL;
import static vadl.configuration.DecoderOptions.OptionalStep.OPT_CONSTRAINT_SYNTHESIS;
import static vadl.configuration.DecoderOptions.OptionalStep.OPT_DECODER_VERIFICATION;
import static vadl.configuration.DecoderOptions.OptionalStep.OPT_ENCODING_VERIFICATION;

import com.google.common.collect.Streams;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.CollectBehaviorDotGraphPass;
import vadl.dump.HtmlDumpPass;
import vadl.pass.PassOrder;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtEncodingConstraintValidationPass;
import vadl.vdt.passes.VdtEncodingSemanticVerificationPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.passes.VdtVerificationPass;
import vadl.viam.passes.verification.ViamVerificationPass;

final class OrderSupport {
  private OrderSupport() {
  }

  static PassOrder addHtmlDump(PassOrder order, GeneralConfiguration config,
                               String phase, String description, Class<?>... exclusions) {
    if (config.doDump()) {
      addDumpBehaviorCollectionPasses(order, config, exclusions);
      var htmlConfig = HtmlDumpPass.Config.from(config, phase, description);
      order.add(new HtmlDumpPass(htmlConfig));
    }
    return order;
  }

  static void addDecodePasses(PassOrder order, GeneralConfiguration config) {
    if (config.getDecoderOptions().isDisabled(OPT_ALL)) {
      return;
    }

    order.add(new VdtEncodingConstraintValidationPass(config))
        .add(new VdtInputPreparationPass(config));

    if (config.getDecoderOptions().isEnabled(OPT_CONSTRAINT_SYNTHESIS)) {
      order.add(new VdtConstraintSynthesisPass(config));
    }

    if (config.getDecoderOptions().isEnabled(OPT_ENCODING_VERIFICATION)) {
      order.add(new VdtEncodingSemanticVerificationPass(config));
    }

    order.add(new VdtLoweringPass(config));

    if (config.getDecoderOptions().isEnabled(OPT_DECODER_VERIFICATION)) {
      order.add(new VdtVerificationPass(config));
    }
  }

  private static PassOrder addDumpBehaviorCollectionPasses(PassOrder order,
                                                           GeneralConfiguration config,
                                                           Class<?>... exceptions) {
    if (!config.doDump()) {
      return order;
    }

    var dontRun = Streams.concat(Stream.of(
        ViamVerificationPass.class,
        AbstractTemplateRenderingPass.class,
        CollectBehaviorDotGraphPass.class
    ), Stream.of(exceptions)).toList();

    order.addBetweenEach((prev, next) -> {
      if (dontRun.stream().anyMatch(a -> a.isInstance(prev))) {
        return Optional.empty();
      }
      if (next.orElse(null) instanceof CollectBehaviorDotGraphPass) {
        return Optional.empty();
      }
      return Optional.of(new CollectBehaviorDotGraphPass(config));
    });
    return order;
  }
}
