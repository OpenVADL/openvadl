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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.DataLayoutProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterResource;
import vadl.viam.Resource;
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

  record WrappedRegisterClass(TableGenRegisterClass registerFile, String allocationSequence)
      implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", Map.of(
              "name", registerFile.name(),
              "namespace", registerFile.namespace().value(),
              "regSize", registerFile.regTypes().stream().findFirst().orElseThrow().size(),
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
    final var output = ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
        GenerateTableGenRegistersPass.class));
    var abi = specification.abi().orElseThrow();

    // The order of registers represents the preferred allocation sequence.
    // Registers are listed in the order caller-save, callee-save, specials.
    var callerSaved = abi.callerSaved().stream().map(Abi.RegisterRef::render).toList();
    verifyAllTheSameRegisterFile(abi.callerSaved());
    verifyAllTheSameRegisterFile(abi.calleeSaved());
    verifyBothTheSame(abi.calleeSaved(), abi.callerSaved());

    // Remove marked regs from callee to mark sure that they are allocated last.
    var exceptions = new HashSet<>(Stream.of(
        Optional.of(abi.returnAddress().render()),
        Optional.of(abi.stackPointer().render()),
        abi.globalPointer().map(Abi.RegisterRef::render),
        Optional.of(abi.framePointer().render()),
        abi.threadPointer().map(Abi.RegisterRef::render)
    ).filter(Optional::isPresent).map(Optional::get).toList());

    var calleeSaved = abi.calleeSaved().stream()
        .map(Abi.RegisterRef::render)
        .filter(render -> !exceptions.contains(render))
        .toList();

    // Because of the checks, we can safely conclude...
    var calleeSavedRegisterFile = abi.calleeSaved().getFirst().registerFile();
    var callerSavedRegisterFile = abi.callerSaved().getFirst().registerFile();

    var registerClasses = output.registerClasses();
    var outputRegisterClasses = new ArrayList<WrappedRegisterClass>();
    var outputAliasRegisterClasses = new ArrayList<WrappedRegisterClass>();
    for (var registerClass : registerClasses) {
      List<String> calleeAndCallerSavedSequence = new ArrayList<>();

      if (registerClass.registerFileRef().equals(callerSavedRegisterFile)) {
        calleeAndCallerSavedSequence.addAll(callerSaved);
      }

      if (registerClass.registerFileRef().equals(calleeSavedRegisterFile)) {
        calleeAndCallerSavedSequence.addAll(calleeSaved);
      }

      HashSet<String> calleeAndCallerSaved = new HashSet<>(calleeAndCallerSavedSequence);
      var specials =
          registerClass.registers().stream()
              .map(register -> register.compilerRegister().name())
              .filter(x -> !calleeAndCallerSaved.contains(x))
              .toList();

      var allocationSeq =
          Stream.concat(calleeAndCallerSavedSequence.stream(), specials.stream())
              .collect(Collectors.joining(", "));

      outputRegisterClasses.add(new WrappedRegisterClass(registerClass, allocationSeq));
    }

    for (var registerClass : output.aliasRegisterClasses()) {
      var allocationSeq = IntStream.range(0, registerClass.registers().size()).mapToObj(
          x -> registerClass.name() + x).collect(Collectors.joining(", "));
      outputAliasRegisterClasses.add(new WrappedRegisterClass(registerClass, allocationSeq));
    }

    var sub32 = new ArrayList<String>();
    var sub32Hi = new ArrayList<String>();
    var full64 = new ArrayList<String>();

    for (var register : output.registers()) {
      var seen = new HashSet<String>();
      for (var subRegIndex : register.subRegIndices()) {
        if (seen.contains(subRegIndex.name())) {
          subRegIndex.incrementVersion();
        }

        var name = subRegIndex.name();
        seen.add(name);
        switch (subRegIndex.inner()) {
          case SUB_32 -> sub32.add(name);
          case FULL_64 -> full64.add(name);
          case SUB_32_HI -> sub32Hi.add(name);
        }
      }
    }

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "pointerAlignment", DataLayoutProvider.pointerAlignment(abi),
        "registers", output.registers(),
        "sub32", sub32.stream().distinct().toList(),
        "sub32Hi", sub32Hi.stream().distinct().toList(),
        "full64", full64.stream().distinct().toList(),
        "aliasRegisters", output.aliasRegisters(),
        "registerFiles", outputRegisterClasses,
        "aliasRegisterFiles", outputAliasRegisterClasses
    );
  }

  private void verifyBothTheSame(List<Abi.RegisterRef> calleeSaved,
                                 List<Abi.RegisterRef> callerSaved) {
    if (!callerSaved.isEmpty() && !calleeSaved.isEmpty()) {
      var callerRegFile = getRegisterFile(callerSaved.getFirst().registerFile());
      var calleeRegFile = getRegisterFile(calleeSaved.getFirst().registerFile());

      if (!calleeRegFile.equals(callerRegFile)) {
        throw Diagnostic.error("Both register files must match",
            calleeRegFile.location().join(callerRegFile.location())).build();
      }
    }
  }

  /**
   * If the given parameter is an {@link ArtificialResource} then return the underlying
   * register file.
   */
  private Resource getRegisterFile(RegisterResource registerResource) {
    if (registerResource instanceof ArtificialResource artificialResource) {
      return artificialResource.innerResourceRef();
    }

    return registerResource;
  }

  private void verifyAllTheSameRegisterFile(List<Abi.RegisterRef> registerRefs) {
    var set = registerRefs.stream().map(ref -> getRegisterFile(ref.registerFile()))
        .collect(Collectors.toSet());

    if (set.size() > 1) {
      throw Diagnostic.error("All register must have the same register file.",
          registerRefs.stream().map(Abi.RegisterRef::location)
              .reduce(registerRefs.get(0).location(),
                  SourceLocation::join).location()).build();
    }
  }
}
