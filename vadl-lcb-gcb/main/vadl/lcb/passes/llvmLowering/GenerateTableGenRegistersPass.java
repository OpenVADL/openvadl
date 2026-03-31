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

import static vadl.error.Diagnostic.ensure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.GenerateCompilerRegistersPass;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegisterClass;
import vadl.gcb.valuetypes.IndexedCompilerRegister;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenAliasRegisterClass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegister;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterResource;
import vadl.viam.Specification;

/**
 * Generate registers, register classes from {@link CompilerRegister} and
 * {@link CompilerRegisterClass} which were generated in {@link GenerateCompilerRegistersPass}.
 */
public class GenerateTableGenRegistersPass extends Pass {

  public GenerateTableGenRegistersPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegisterClassesPass");
  }

  /**
   * Represents a {@link RegisterResource.Constraint} in LLVM.
   */
  public record LlvmConstraint(ValueType type, int value, TableGenRegister register) {

  }

  /**
   * Contains the output of the pass.
   */
  public record Output(List<TableGenRegisterClass> registerClasses,
                       List<TableGenAliasRegisterClass> aliasRegisterClasses,
                       List<TableGenRegister> registers,
                       List<TableGenRegisterAlias> aliasRegisters,
                       List<LlvmConstraint> constraints) {
    /* `registers` do not belong to any register class. */
  }

  @Nullable
  @Override
  public Output execute(PassResults passResults, Specification viam) throws IOException {
    var configuration = (LcbConfiguration) configuration();
    var output = (GenerateCompilerRegistersPass.Output) passResults.lastResultOf(
        GenerateCompilerRegistersPass.class);
    var compilerRegisterClasses = output.registerClasses();

    final var registerClasses = new ArrayList<TableGenRegisterClass>();
    final var aliasRegisterClasses = new ArrayList<TableGenAliasRegisterClass>();
    final var registers = new ArrayList<TableGenRegister>();
    final var aliasRegisters = new ArrayList<TableGenRegisterAlias>();

    for (var compilerRegister : output.generalRegisters()) {
      var register = new TableGenRegister(
          configuration.targetName(),
          compilerRegister,
          compilerRegister.subRegs(),
          compilerRegister.subRegIndices(),
          compilerRegister.hwEncodingValue(),
          Optional.empty(),
          compilerRegister.isArtificial()
      );
      registers.add(register);
    }

    for (var compilerRegister : output.aliasRegisters()) {
      var register = new TableGenRegisterAlias(
          configuration.targetName(),
          compilerRegister,
          Optional.of(compilerRegister.hwEncodingValue()),
          compilerRegister.isArtificial()
      );
      aliasRegisters.add(register);
    }

    for (var compilerRegisterClass : compilerRegisterClasses) {
      var classRegisters = new ArrayList<TableGenRegister>();
      for (var compilerRegister : compilerRegisterClass.registers()) {
        var register = new TableGenRegister(
            configuration.targetName(),
            compilerRegister,
            compilerRegister.subRegs(),
            compilerRegister.subRegIndices(),
            Objects.requireNonNull(compilerRegisterClass.registerFile().addressType()).bitWidth()
                - 1,
            Optional.of(compilerRegister.hwEncodingValue()),
            compilerRegister.isArtificial()
        );
        registers.add(register);
        classRegisters.add(register);
      }

      var type = ValueType.from(compilerRegisterClass.registerFile().resultType()).get();
      registerClasses.add(
          new TableGenRegisterClass(
              configuration.targetName(),
              compilerRegisterClass.name(),
              compilerRegisterClass.alignment().bitAlignment(),
              List.of(type),
              classRegisters,
              compilerRegisterClass.registerFile())
      );
    }

    for (var compilerRegisterClass : output.aliasRegisterClasses()) {
      var classRegisters = new ArrayList<TableGenRegister>();
      for (var compilerRegister : compilerRegisterClass.registers()) {
        var register = new TableGenRegister(
            configuration.targetName(),
            compilerRegister,
            compilerRegister.subRegs(),
            compilerRegister.subRegIndices(),
            Objects.requireNonNull(compilerRegisterClass.registerFile().addressType()).bitWidth()
                - 1,
            Optional.of(compilerRegister.hwEncodingValue()),
            compilerRegister.isArtificial()
        );
        registers.add(register);
        classRegisters.add(register);
      }

      var type = ValueType.from(compilerRegisterClass.registerFile().resultType()).get();

      ensure(compilerRegisterClass.registerFile() instanceof ArtificialResource,
          () -> Diagnostic.error("This must be an alias.",
              compilerRegisterClass.registerFile().location()));

      aliasRegisterClasses.add(
          new TableGenAliasRegisterClass(
              configuration.targetName(),
              compilerRegisterClass.name(),
              compilerRegisterClass.alignment().bitAlignment(),
              List.of(type),
              classRegisters,
              (ArtificialResource) compilerRegisterClass.registerFile())
      );
    }

    var constraints = getConstraints(registerClasses);
    var orderedRegisters = sortRegisters(registers);

    nameSubRegisterIndices(orderedRegisters);

    return new Output(registerClasses, aliasRegisterClasses, orderedRegisters, aliasRegisters,
        constraints);
  }

  /**
   * This is a special problem for LLVM. We cannot reuse multiple register indices. We need for
   * every sub register index a separate name in TableGen. That's why we rename the indices when
   * the names "collide".
   */
  private static void nameSubRegisterIndices(List<TableGenRegister> orderedRegisters) {
    for (var register : orderedRegisters) {
      var seen = new HashSet<String>();
      for (var subRegIndex : register.subRegIndices()) {
        if (seen.contains(subRegIndex.name())) {
          subRegIndex.incrementVersion();
        }

        var name = subRegIndex.name();
        seen.add(name);
      }
    }
  }

  private List<TableGenRegister> sortRegisters(List<TableGenRegister> registers) {
    var result = new ArrayList<TableGenRegister>();
    var ready = new HashSet<CompilerRegister>();

    while (result.size() != registers.size()) {
      for (var register : registers) {
        var allSubRegisters = ready.containsAll(register.subRegs());
        if (allSubRegisters && !ready.contains(register.compilerRegister())) {
          ready.add(register.compilerRegister());
          result.add(register);
        }
      }
    }

    return result;
  }

  private List<LlvmConstraint> getConstraints(List<TableGenRegisterClass> mainRegisterClasses) {
    var constraints = new ArrayList<LlvmConstraint>();

    for (var rc : mainRegisterClasses) {
      var registerFile = rc.registerFileRef();
      for (var constraint : registerFile.constraints()) {
        var addr = constraint.indices().getFirst().intValue();
        var value = constraint.value().intValue();

        rc.registers().stream().filter(
                r -> r.compilerRegister() instanceof IndexedCompilerRegister reg
                    && reg.index() == addr)
            .findFirst()
            .ifPresent(register -> constraints.add(
                new LlvmConstraint(ValueType.from(registerFile.resultType()).get(),
                    value,
                    register)));

      }
    }
    return constraints;
  }
}