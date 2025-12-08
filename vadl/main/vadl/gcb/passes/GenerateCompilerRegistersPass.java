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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegisterClass;
import vadl.gcb.valuetypes.GeneralCompilerRegister;
import vadl.gcb.valuetypes.IndexedCompilerRegister;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * The VIAM gives us register files, but we need separate registers.
 * The RISC-V specification has the register file {@code X}. This pass creates
 * the registers {@code X0} to {@code X31} from this register file.
 */
public class GenerateCompilerRegistersPass extends Pass {
  public GenerateCompilerRegistersPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegistersPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(List<CompilerRegister> generalRegisters,
                       List<CompilerRegister> aliasRegisters,
                       List<CompilerRegisterClass> registerClasses,
                       List<CompilerRegisterClass> aliasRegisterClasses) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var generalRegisters =
        generalRegisters(viam.registerTensors().filter(RegisterTensor::isSingleRegister).toList());
    var aliasRegisters =
        aliasRegisters(viam.artificialResources().filter(ArtificialResource::isRegister).toList(),
            generalRegisters.size());
    int dwarfOffset = aliasRegisters.right();
    var registerClasses =
        registerClasses(viam.registerTensors().filter(RegisterTensor::isRegisterFile).toList(), abi,
            dwarfOffset);
    var aliasRegisterFiles = aliasRegisterFiles(
        viam.artificialResources().filter(ArtificialResource::isRegisterFile).toList(), abi,
        registerClasses.right());

    createSubRegisters(aliasRegisterFiles.left(), registerClasses.left());

    return new Output(generalRegisters,
        aliasRegisters.left(),
        registerClasses.left(),
        aliasRegisterFiles.left());
  }

  private void createSubRegisters(List<CompilerRegisterClass> aliasRegisterClasses,
                                  List<CompilerRegisterClass> registerClasses) {

    for (var registerClass : registerClasses) {
      for (var aliasRegisterClass : aliasRegisterClasses) {
        var artificialResource = (ArtificialResource) aliasRegisterClass.registerFile();
        var referencesResource = (RegisterTensor) artificialResource.innerResourceRef();

        // If yes then the alias points to real register file.
        if (registerClass.registerFile().equals(referencesResource)) {
          var hasEqualType = artificialResource.type().asDataType()
              .equals(referencesResource.resultType().asDataType());

          for (int i = 0; i < registerClass.registers().size(); i++) {
            var aliasRegister = aliasRegisterClass.registers().get(i);
            var realRegister = registerClass.registers().get(i);

            if (hasEqualType && artificialResource.type().equals(BitsType.bits(64))) {
              realRegister.addSubReg(aliasRegister, new CompilerRegister.SubRegIndex(
                  CompilerRegister.SubRegIndexEnum.FULL_64));
            } else {
              realRegister.addSubReg(aliasRegister, new CompilerRegister.SubRegIndex(
                  CompilerRegister.SubRegIndexEnum.SUB_32));
            }
          }
        }
      }
    }
  }

  private List<CompilerRegister> generalRegisters(List<RegisterTensor> registers) {
    var compilerRegisters = new ArrayList<CompilerRegister>();
    int dwarfOffset = 0;

    for (var register : registers) {
      // The alias should be the same as the register name.
      var alias = GeneralCompilerRegister.generateName(register);

      var compilerRegister =
          new GeneralCompilerRegister(register, alias, Collections.emptyList(), dwarfOffset++);
      compilerRegisters.add(compilerRegister);
    }

    return compilerRegisters;
  }

  private Pair<List<CompilerRegister>, Integer> aliasRegisters(List<ArtificialResource> registers,
                                                               int dwarfOffset) {
    var compilerRegisters = new ArrayList<CompilerRegister>();
    for (var register : registers) {
      var address = register.readFunction().behavior().getNodes(ConstantNode.class).findFirst()
          .map(x -> x.constant().asVal().intValue());

      // The alias should be the same as the register name.
      var alias = GeneralCompilerRegister.generateName(register);

      var compilerRegister = address.isEmpty()
          ? new GeneralCompilerRegister(register, alias, Collections.emptyList(), dwarfOffset++) :
          new GeneralCompilerRegister(register, address.get(), alias, Collections.emptyList(),
              dwarfOffset++);
      compilerRegisters.add(compilerRegister);
    }

    return Pair.of(compilerRegisters, dwarfOffset);
  }

  private Pair<List<CompilerRegisterClass>, Integer> registerClasses(
      List<RegisterTensor> registerFiles, Abi abi, int dwarfOffset) {
    var result = new ArrayList<CompilerRegisterClass>();

    for (var registerFile : registerFiles) {
      var registers =
          IndexedCompilerRegister.fromRegisterFile(registerFile, abi, dwarfOffset, false);
      dwarfOffset += registers.size();

      var alignment = ensureNonNull(abi.registerFileAlignment().get(registerFile),
          () -> Diagnostic.error("There is not alignment for the register file defined",
              registerFile.location().join(abi.location())));

      result.add(new CompilerRegisterClass(registerFile, registers, alignment));
    }

    return Pair.of(result, dwarfOffset);
  }

  private Pair<List<CompilerRegisterClass>, Integer> aliasRegisterFiles(
      List<ArtificialResource> artificialResources, Abi abi, int dwarfOffset) {
    var result = new ArrayList<CompilerRegisterClass>();

    for (var artificialResource : artificialResources) {
      // If it is not a register file then continue.
      if (artificialResource.innerResourceRef() instanceof RegisterTensor registerTensor
          && registerTensor.isRegisterFile()) {
        var registers =
            IndexedCompilerRegister.fromRegisterFile(artificialResource, abi, dwarfOffset, false);
        dwarfOffset += registers.size();

        var alignment = ensureNonNull(abi.registerFileAlignment().get(registerTensor),
            () -> Diagnostic.error("There is not alignment for the register file defined",
                artificialResource.location().join(abi.location())));

        result.add(new CompilerRegisterClass(artificialResource, registers, alignment));
      }
    }

    return Pair.of(result, dwarfOffset);
  }
}