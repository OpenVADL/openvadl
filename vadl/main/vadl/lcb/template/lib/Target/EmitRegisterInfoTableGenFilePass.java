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

package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "RegisterInfo.td";
  }

  record WrappedRegisterFile(TableGenRegisterClass registerFile, String allocationSequence)
      implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", Map.of(
              "name", registerFile.name(),
              "namespace", registerFile.namespace().value(),
              "regTypesString", registerFile.regTypesString(),
              "alignment", registerFile.alignment()
          ),
          "allocationSequence", allocationSequence
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
        GenerateTableGenRegistersPass.class));
    var abi = specification.abi().orElseThrow();
    var registerClasses = output.registerClasses();

    if (registerClasses.size() > 1) {
      throw Diagnostic.error("Supporting only one register file", specification.sourceLocation())
          .build();
    }

    var registerClass = ensurePresent(registerClasses.stream().findFirst(), "must be present");


    // The order of registers represents the preferred allocation sequence.
    // Registers are listed in the order caller-save, callee-save, specials.
    var callerSaved = abi.callerSaved().stream().map(Abi.RegisterRef::render).toList();

    // Remove marked regs from callee to mark sure that they are allocated last.
    var exceptions = new HashSet<>(Stream.of(
        Optional.of(abi.returnAddress().render()),
        Optional.of(abi.stackPointer().render()),
        Optional.of(abi.globalPointer().render()),
        Optional.of(abi.framePointer().render()),
        abi.threadPointer().map(Abi.RegisterRef::render)
    ).filter(Optional::isPresent).map(Optional::get).toList());

    var calleeSaved = abi.calleeSaved().stream()
        .map(Abi.RegisterRef::render)
        .filter(render -> !exceptions.contains(render)).toList();

    HashSet<String> both = new HashSet<>();
    both.addAll(callerSaved);
    both.addAll(calleeSaved);
    var specials =
        registerClass.registers().stream().map(
                register -> register.compilerRegister().name()).filter(x -> !both.contains(x))
            .toList();
    var allocationSeq =
        Stream.concat(callerSaved.stream(), Stream.concat(calleeSaved.stream(), specials.stream()))
            .collect(
                Collectors.joining(", "));

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "registers", output.registers(),
        "registerFiles", List.of(
            new WrappedRegisterFile(registerClass, allocationSeq)
        ));
  }
}
