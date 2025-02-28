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

package vadl.lcb.template;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.io.FilenameUtils;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.RegisterFile;

/**
 * Abstracts the subdir under the output.
 */
public abstract class LcbTemplateRenderingPass extends AbstractTemplateRenderingPass {
  public LcbTemplateRenderingPass(GeneralConfiguration configuration) throws IOException {
    super(configuration, "lcb");
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  protected String renderRegister(RegisterFile registerFile, int addr) {
    return registerFile.identifier.simpleName() + addr;
  }

  @Override
  protected String lineComment() {
    var filename = FilenameUtils.getName(getOutputPath());
    var ending = FilenameUtils.getExtension(filename);
    var hashEndings = Set.of("def");
    if (hashEndings.contains(ending)
        || filename.contains("Makefile")
        || filename.startsWith("CMake")
    ) {
      return "#";
    }

    return super.lineComment();
  }

  protected static <T> Predicate<T> distinctByKey(
      Function<? super T, ?> keyExtractor) {

    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
