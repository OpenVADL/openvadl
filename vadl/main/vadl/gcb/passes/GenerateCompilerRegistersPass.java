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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.RegisterResource;
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
    var abi = (Abi) viam
        .definitions()
        .filter(x -> x instanceof Abi)
        .findFirst()
        .orElseThrow(() -> Diagnostic.error(
            "Missing ABI definition for compiler generation",
            SourceLocation.INVALID_SOURCE_LOCATION).build()
        );

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
    var regTensorToRegClass = registerClasses.stream().collect(Collectors.toMap(
        k -> k.registerFile(),
        v -> v
    ));

    var parentChildMapping = aliasRegisterClasses
        .stream()
        .collect(Collectors.groupingBy(x -> {
          var artificialResource = (ArtificialResource) x.registerFile();
          var referencesResource = (RegisterTensor) artificialResource.innerResourceRef();
          return regTensorToRegClass.get(referencesResource);
        }));

    for (var parentChildren : parentChildMapping.entrySet()) {
      var parentRegister = parentChildren.getKey();
      var childRegisters = parentChildren.getValue();
      childRegisters.sort(this.compareRegisterClassesBySizeAndOffset());

      for (int i = 0; i < childRegisters.size(); i++) {
        var subRegClass = childRegisters.get(i);

        var superRegIndex = -1;
        for (int j = i - 1; j >= 0; j--) {
          var isSubReg = isPossibleSubregisterOf(subRegClass.registerFile(), 
              childRegisters.get(j).registerFile());
          if (isSubReg) {
            superRegIndex = j;
            break;
          }
        }

        var superRegClass = superRegIndex != -1 
            ? childRegisters.get(superRegIndex)
            : parentRegister;
        this.addAllAsSubRegisters(superRegClass, subRegClass);
      }
    }
  }

  private Comparator<CompilerRegisterClass> compareRegisterClassesBySizeAndOffset() {
    return new Comparator<>() {
      @Override
      public int compare(CompilerRegisterClass o1, CompilerRegisterClass o2) {
        var regFile1 = o1.registerFile();
        var regFile2 = o2.registerFile();
        var typeDiff = regFile2.type().asDataType().bitWidth() 
            - regFile1.type().asDataType().bitWidth();
        if (typeDiff != 0) {
          return typeDiff;
        }

        return getRegisterSliceRange(regFile2).lsb() - getRegisterSliceRange(regFile1).lsb();
      }
    };
  }

  private Constant.BitSlice.Part getRegisterSliceRange(RegisterResource r) {
    if (r instanceof ArtificialResource ar) {
      return Optional.ofNullable(ar.aliasSlice())
            .flatMap(s -> s.parts().findFirst())
            .orElseGet(() -> new Constant.BitSlice.Part(ar.type().asDataType().bitWidth(), 0));
    }

    return new Constant.BitSlice.Part(r.type().asDataType().bitWidth(), 0);
  }

  private boolean isPossibleSubregisterOf(
      RegisterResource subReg, 
      RegisterResource supReg) {
    var sliceSup = this.getRegisterSliceRange(supReg);
    var sliceSub = this.getRegisterSliceRange(subReg);
    return sliceSup.lsb() <= sliceSub.lsb() 
      && sliceSup.msb() >= sliceSub.msb();
  }

  private void addAllAsSubRegisters(
      CompilerRegisterClass superRegisterClass, 
      CompilerRegisterClass subRegisterClass) {
    var superRegisters = superRegisterClass.registers();
    var subRegisters = subRegisterClass.registers();
    var superRegisterFile = superRegisterClass.registerFile();
    var subRegisterFile = subRegisterClass.registerFile();

    var hasEqualType = superRegisterFile.type().asDataType().equals(
        subRegisterFile.type().asDataType());
    var is64Bit = superRegisterFile.type().equals(BitsType.bits(64));
    var subLsb = this.getRegisterSliceRange(subRegisterFile).lsb();

    var subregIndex = hasEqualType && is64Bit 
        ? new CompilerRegister.SubRegIndex(CompilerRegister.SubRegIndexEnum.FULL_64)
        : subLsb == 0
          ? new CompilerRegister.SubRegIndex(CompilerRegister.SubRegIndexEnum.SUB_32)
          : new CompilerRegister.SubRegIndex(CompilerRegister.SubRegIndexEnum.SUB_32_HI);

    for (int j = 0; j < superRegisters.size(); j++) {
      var superReg = superRegisters.get(j);
      var subReg = subRegisters.get(j);
      superReg.addSubReg(subReg, subregIndex);
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
