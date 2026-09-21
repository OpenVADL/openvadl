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

package vadl.lcb.passes.llvmLowering;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.GenerateCompilerRegistersPass;
import vadl.gcb.passes.operands.InstructionOperandsCtx;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.FloatEncoding;
import vadl.types.FloatType;
import vadl.viam.ArtificialResource;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.AnyReadRegisterFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Infer register data types from {@link Instruction} behavior
 * and attach them to the {@link RegisterTensor} using {@link RegisterTypesCtx}.
 */
public class InferRegisterTypesPass extends Pass {

  /**
   * Constructor.
   */
  public InferRegisterTypesPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegisterTypesPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults.lastResultOf(
            FunctionInlinerPass.class)).behaviors();
    Objects.requireNonNull(uninlined);

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return null;
    }

    Map<RegisterTensor, Set<ValueType>> registerValueTypes = new HashMap<>();

    isa.ownInstructions().forEach(instruction -> {
      var operands = instruction.expectExtension(InstructionOperandsCtx.class);

      var behavior = ensureNonNull(uninlined.get(instruction),
          () -> Diagnostic.error("Cannot find the uninlined graph of this instruction",
              instruction.location()));

      var matchedFloatBuiltIn =
          TreeMatcher.matches(behavior.getNodes(BuiltInCall.class).map(x -> x),
              new BuiltInMatcher(BuiltInTable.FLOAT_BUILT_INS,
                  List.of(new AnyChildMatcher(new AnyReadRegisterFileMatcher()),
                      new AnyChildMatcher(new AnyReadRegisterFileMatcher()))));

      Stream.concat(operands.inputs().stream(), operands.outputs().stream())
          .filter(GcbInstructionRegisterFileOperand.class::isInstance)
          .map(GcbInstructionRegisterFileOperand.class::cast)
          .map(operand ->
              (operand.registerFile() instanceof RegisterTensor)
                  ?
                  (RegisterTensor) operand.registerFile() :
                  (RegisterTensor) ((ArtificialResource) operand.registerFile()).innerResourceRef()
          )

          .forEach(registerFile ->
              registerValueTypes
                  .computeIfAbsent(registerFile, ignored -> new HashSet<>())
                  .add(
                      matchedFloatBuiltIn.isEmpty()
                          ?
                          ValueType.from(registerFile.resultType()).get() :
                          ValueType.from(new FloatType(
                              FloatEncoding.ieee(registerFile.resultType().bitWidth()))).get())
          );
    });

    var compilerRegisterClasses = ((GenerateCompilerRegistersPass.Output) passResults.lastResultOf(
        GenerateCompilerRegistersPass.class)).registerClasses();

    compilerRegisterClasses.forEach(compilerRegisterClass -> {
      var registerFile = (RegisterTensor) compilerRegisterClass.registerFile();
      var valueTypes = registerValueTypes.getOrDefault(registerFile, Collections.emptySet());

      registerFile.attachExtension(
          new RegisterTypesCtx(
              valueTypes.stream().toList()
          )
      );
    });

    return null;
  }
}
