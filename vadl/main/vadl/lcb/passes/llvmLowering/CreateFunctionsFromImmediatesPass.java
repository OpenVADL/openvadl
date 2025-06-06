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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.common.GcbAccessOrExtractionFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbCppFunctionBodyLess;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.Specification;

public class CreateFunctionsFromImmediatesPass extends Pass {
  public CreateFunctionsFromImmediatesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("CreateFunctionsFromImmediatesPass");
  }

  public record Output(Map<TableGenImmediateRecord, GcbCppFunctionWithBody> encodings,
                       Map<TableGenImmediateRecord, GcbCppFunctionWithBody> decodings,
                       Map<TableGenImmediateRecord, GcbCppFunctionWithBody> predicates) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var encodings = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var decodings = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var predicates = new IdentityHashMap<TableGenImmediateRecord, GcbCppFunctionWithBody>();
    var immediates = (List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class);

    for (var immediate : immediates) {
      encodings.put(immediate, encoding(immediate));
      decodings.put(immediate, decoding(immediate));
      predicates.put(immediate, predicate(immediate));
    }

    return new Output(encodings, decodings, predicates);
  }

  @Nonnull
  private GcbCppFunctionWithBody encoding(TableGenImmediateRecord immediate) {
    var encodingBodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.encoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(
            Objects.requireNonNull(immediate.fieldAccessRef().encoding()).targetField().type()),
        immediate.fieldAccessRef().encoding().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(encodingBodyLessFunction,
        generateCode(immediate.encoderMethod(),
            immediate.fieldAccessRef(),
            encodingBodyLessFunction));
  }

  @Nonnull
  private GcbCppFunctionWithBody decoding(TableGenImmediateRecord immediate) {
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.decoderMethod(),
        new Parameter[] {},
        CppTypeMap.upcast(immediate.fieldAccessRef().accessFunction().returnType()),
        immediate.fieldAccessRef().accessFunction().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(bodyLessFunction,
        generateCode(immediate.decoderMethod(),
            immediate.fieldAccessRef(),
            bodyLessFunction));
  }

  @Nonnull
  private GcbCppFunctionWithBody predicate(TableGenImmediateRecord immediate) {
    var bodyLessFunction = new GcbCppFunctionBodyLess(
        immediate.predicateMethod(),
        new Parameter[] {},
        Type.bool(),
        immediate.fieldAccessRef().predicate().behavior(),
        immediate.fieldAccessRef());
    return new GcbCppFunctionWithBody(bodyLessFunction,
        generateCode(immediate.encoderMethod(),
            immediate.fieldAccessRef(),
            bodyLessFunction));
  }

  private String generateCode(Identifier identifier,
                              Format.FieldAccess fieldAccess,
                              GcbCppFunctionBodyLess header) {
    return new GcbAccessOrExtractionFunctionCodeGenerator(header,
        fieldAccess,
        identifier.lower()).genFunctionDefinition();
  }
}
