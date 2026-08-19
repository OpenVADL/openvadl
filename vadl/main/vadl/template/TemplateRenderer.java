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

package vadl.template;

import java.io.Writer;
import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/** Renders classpath templates with the project's standard Thymeleaf configuration. */
public final class TemplateRenderer {
  private static final TemplateEngine templateEngine = templateEngine();

  private TemplateRenderer() {
  }

  /**
   * Render a template with the provided variables.
   *
   * @param templatePath path relative to {@code resources/templates}
   * @param variables variables available to the template
   * @param writer output destination
   */
  public static void render(String templatePath, Map<String, Object> variables, Writer writer) {
    var context = new Context();
    variables.forEach(context::setVariable);
    templateEngine.process(templatePath, context, writer);
  }

  private static TemplateEngine templateEngine() {
    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(templateResolver());
    return templateEngine;
  }

  private static ITemplateResolver templateResolver() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setTemplateMode(TemplateMode.TEXT);
    templateResolver.setCharacterEncoding("UTF8");
    templateResolver.setCheckExistence(true);
    templateResolver.setCacheable(true);
    return templateResolver;
  }
}
