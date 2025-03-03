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

package vadl.iss.template;

import vadl.configuration.IssConfiguration;

/**
 * An ISS template rendering pass that takes the pass to an template and renders it
 * with the default variables set by the {@link IssTemplateRenderingPass}.
 * This reduces the number of required rendering passes and makes the pass order more
 * readable, especially when using the {@link #issDefault(String, IssConfiguration)}
 * constructor.
 *
 * @see vadl.pass.PassOrders#iss(IssConfiguration)
 */
public class IssDefaultRenderingPass extends IssTemplateRenderingPass {

  private final String issTemplatePath;

  public IssDefaultRenderingPass(String issTemplatePath, IssConfiguration configuration) {
    super(configuration);
    this.issTemplatePath = issTemplatePath;
  }

  @Override
  protected String issTemplatePath() {
    return issTemplatePath;
  }

  public static IssDefaultRenderingPass issDefault(String issTemplatePath,
                                                   IssConfiguration config) {
    return new IssDefaultRenderingPass(issTemplatePath, config);
  }
}
