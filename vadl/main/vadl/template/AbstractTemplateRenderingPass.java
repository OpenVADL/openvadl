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

package vadl.template;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This is an abstract pass that emits a single file from a given template.
 * It is a subtype of the more generic {@link AbstractMultiTemplateRenderingPass} which
 * allows rendering of multiple files from the same template.
 *
 * @see Result
 * @see AbstractMultiTemplateRenderingPass
 */
public abstract class AbstractTemplateRenderingPass extends AbstractMultiTemplateRenderingPass {

  /**
   * The result of a rendering pass.
   * It contains the path to the emitted file.
   * Custom rendering passes may extend this class and construct their own result by overriding
   * the {@link AbstractTemplateRenderingPass#constructResult(Path)}.
   */
  public static class Result extends AbstractMultiTemplateRenderingPass.Result {

    /**
     * Constructs the result.
     *
     * @param emittedFiles the paths to the files that were rendered/emitted
     */
    protected Result(Path emittedFiles) {
      super(List.of(emittedFiles));
    }

    public Path emittedFile() {
      return emittedFiles().get(0);
    }
  }

  /**
   * Construct the template rendering pass.
   *
   * @param configuration of the generator. Contains the general output location.
   * @param subDir        the subdirectory of the output location (e.g., iss).
   */
  public AbstractTemplateRenderingPass(GeneralConfiguration configuration,
                                       String subDir) {
    super(configuration, subDir);
  }

  /**
   * Get the path where the file will be written to.
   */
  protected abstract String getOutputPath();


  @Override
  public PassName getName() {
    return new PassName("EmitFile " + getOutputPath());
  }

  /**
   * The map with the variables for the template. This method has access to the {@code passResults}
   * from the passes which have run before.
   */
  protected abstract Map<String, Object> createVariables(final PassResults passResults,
                                                         Specification specification);


  @Override
  protected final List<RenderInput> createRenderInputs(PassResults passResults,
                                                       Specification specification) {
    // construct the render input for the multi template rendering pass
    return List.of(
        new RenderInput(
            getOutputPath(),
            createVariables(passResults, specification)
        )
    );
  }


  /**
   * Customize the pass result.
   *
   * @param emittedFile the file emitted during rendering.
   * @return the customized result, which must be a subtype of {@link Result}.
   */
  protected Result constructResult(Path emittedFile) {
    return new Result(emittedFile);
  }

  /**
   * Allows subtypes of this pass to construct their own result.
   * Subtypes of this class should override {@link #constructResult(Path)} to customize
   * the pass result.
   */
  @Override
  protected final AbstractMultiTemplateRenderingPass.Result constructResult(
      List<Path> emittedFiles) {
    return constructResult(emittedFiles.get(0));
  }


}
