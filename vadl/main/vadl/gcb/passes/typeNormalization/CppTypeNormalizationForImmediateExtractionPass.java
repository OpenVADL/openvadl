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

package vadl.gcb.passes.typeNormalization;

import java.util.stream.Stream;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.pass.PassName;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts a bit mask to ensure that the code generation works for immediate
 * extraction.
 */
public class CppTypeNormalizationForImmediateExtractionPass extends CppTypeNormalizationPass {

  public CppTypeNormalizationForImmediateExtractionPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName(CppTypeNormalizationForImmediateExtractionPass.class.getName());
  }

  @Override
  protected Stream<Pair<Format.FieldAccess, Function>> getApplicable(Specification viam) {
    return viam.isa()
        .map(x -> x.ownFormats().stream())
        .orElse(Stream.empty())
        .flatMap(x -> x.fieldAccesses().stream())
        .map(fieldAccess -> new Pair<>(fieldAccess, fieldAccess.fieldRef().extractFunction()))
        .distinct();
  }

  @Override
  protected GcbFieldAccessCppFunction liftFunction(Format.FieldAccess fieldAccess) {
    return createGcbFieldAccessCppFunction(fieldAccess.accessFunction(), fieldAccess);
  }
}
