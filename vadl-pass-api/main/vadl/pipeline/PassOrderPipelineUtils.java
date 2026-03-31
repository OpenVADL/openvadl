// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

package vadl.pipeline;

import com.google.common.collect.Streams;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.CollectBehaviorDotGraphPass;
import vadl.dump.HtmlDumpPass;
import vadl.pass.PassOrder;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * Shared helpers for wiring dump-related passes into pipeline definitions.
 */
public final class PassOrderPipelineUtils {
  private PassOrderPipelineUtils() {
  }

  /**
   * Appends the standard HTML dump passes for a pipeline phase when dumping is enabled.
   */
  public static PassOrder addHtmlDump(PassOrder order,
                                      GeneralConfiguration config,
                                      String phase,
                                      String description,
                                      Class<?>... exclusions) {
    if (config.doDump()) {
      addDumpBehaviorCollectionPasses(order, config, exclusions);
      var htmlConfig = HtmlDumpPass.Config.from(config, phase, description);
      order.add(new HtmlDumpPass(htmlConfig));
    }

    return order;
  }

  /**
   * Inserts behavior collection passes between pipeline steps when dumping is enabled.
   */
  public static PassOrder addDumpBehaviorCollectionPasses(PassOrder order,
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
