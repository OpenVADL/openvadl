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

package vadl.dump;

import java.io.IOException;
import java.util.List;
import vadl.configuration.DumpMode;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.pass.Pass;
import vadl.pass.PassFailureHandler;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.pass.PassStep;
import vadl.viam.Specification;

/**
 * Creates emergency dumps when a pass fails.
 */
public class PassFailureDumpHandler implements PassFailureHandler {
  @Override
  public void onPassFailure(List<PassStep> pipeline,
                            PassResults passResults,
                            Pass failedPass,
                            Specification viam,
                            Exception exception) throws IOException {
    var config = pipeline.isEmpty() ? failedPass.configuration() : pipeline.get(0).pass()
        .configuration();

    if (config.dumpMode() == DumpMode.NONE) {
      return;
    }

    if (config.dumpMode() == DumpMode.ON_FAILURE
        && (exception instanceof Diagnostic || exception instanceof DiagnosticList)) {
      return;
    }

    var passClassName = failedPass.getClass().getSimpleName();
    var graphCollectPass = new CollectBehaviorDotGraphPass(config);
    var graphCollectResult = graphCollectPass.execute(passResults, viam);
    passResults.add(PassKey.of("BehaviorCollectionOnException"), graphCollectPass, 0,
        graphCollectResult);

    var htmlDumpPass = new HtmlDumpPass(HtmlDumpPass.Config
        .from(config, "Exception During " + passClassName,
            "This is a dump after exception occurred during the %s pass."
                .formatted(passClassName)));
    htmlDumpPass.execute(passResults, viam);
  }
}
