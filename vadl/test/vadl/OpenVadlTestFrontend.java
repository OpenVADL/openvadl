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

package vadl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import vadl.ast.Ast;
import vadl.ast.ModelRemover;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.ast.ViamLowering;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticPrinter;
import vadl.viam.Specification;

class OpenVadlTestFrontend implements TestFrontend {

  private Specification specification;
  private String logs = "";

  @Override
  public boolean runSpecification(URI vadlFile) {
    try {
      Ast ast = VadlParser.parse(Path.of(vadlFile));
      {
        // FIXME: These two passes must be part of the VadlParser parse API.
        new Ungrouper().ungroup(ast);
        new ModelRemover().removeModels(ast);
      }
      var typeChecker = new TypeChecker();
      typeChecker.verify(ast);
      var viamGenerator = new ViamLowering();
      specification = viamGenerator.generate(ast);
      return true;
    } catch (Diagnostic e) {
      // FIXME: Proper print to string
      var stringWriter = new StringWriter();
      stringWriter.append(new DiagnosticPrinter(false).toString(e));
      stringWriter.append("\n");
      e.printStackTrace(new PrintWriter(stringWriter));
      logs = e.getMessage() + "\n" + stringWriter;
      return false;
    } catch (Exception e) {
      var stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      logs = e + "\n" + stringWriter;
      return false;
    }
  }

  @Override
  public Specification getViam() {
    return Objects.requireNonNull(specification);
  }

  @Override
  public String getLogAsString() {
    return logs;
  }


  static class Provider extends TestFrontend.Provider {

    public TestFrontend createFrontend() {
      return new OpenVadlTestFrontend();
    }

  }
}
