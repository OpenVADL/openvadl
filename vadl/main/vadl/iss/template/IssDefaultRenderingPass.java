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

package vadl.iss.template;

import vadl.configuration.IssConfiguration;

/**
 * An ISS template rendering pass that takes the pass to an template and renders it
 * with the default variables set by the {@link IssTemplateRenderingPass}.
 * This reduces the number of required rendering passes and makes the pass order more
 * readable, especially when using the {@link #issDefault(String, IssConfiguration)}
 * constructor.
 *
 * @see vadl.pipeline.PassOrders#iss(IssConfiguration)
 */
public class IssDefaultRenderingPass extends IssTemplateRenderingPass {

  private final String issTemplatePath;

  private final boolean skipThymeleaf;

  /**
   * Constructs an ISS default rendering pass object for rendering a specified ISS template.
   */
  public IssDefaultRenderingPass(String issTemplatePath, boolean skipThymeleaf,
                                 IssConfiguration configuration) {
    super(configuration);
    this.issTemplatePath = issTemplatePath;
    this.skipThymeleaf = skipThymeleaf;
  }

  @Override
  protected String issTemplatePath() {
    return issTemplatePath;
  }

  @Override
  protected boolean skipThymeleaf() {
    return skipThymeleaf;
  }

  @Override
  protected boolean enableCopyright() {
    // if we do a plain copy, it is not auto-generated
    return !skipThymeleaf;
  }

  public static IssDefaultRenderingPass issDefault(String issTemplatePath,
                                                   IssConfiguration config) {
    return issDefault(issTemplatePath, false, config);
  }

  public static IssDefaultRenderingPass issDefault(String issTemplatePath, boolean skipThymeleaf,
                                                   IssConfiguration config) {
    return new IssDefaultRenderingPass(issTemplatePath, skipThymeleaf, config);
  }
}
