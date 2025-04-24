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

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Format.Field;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * This pass goes over all instructions and determines
 * how a field is used. It can be used as a register or immediate or other (because it is not
 * relevant). Additionally, it checks whether the register is used as argument or destination.
 */
public class IdentifyFieldUsagePass extends Pass {

  public IdentifyFieldUsagePass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("detectImmediatePass");
  }

  /**
   * Wrapper around {@link RegisterUsage} to also store the register file or register.
   */
  public record RegisterUsageAggregate(RegisterUsage registerUsage,
                                       RegisterEither ref) {

  }

  /**
   * Helper structure to hold a register or register file.
   */
  public record RegisterEither(@Nullable RegisterTensor register,
                               @Nullable RegisterTensor registerFile) {
    public static RegisterEither createRegister(RegisterTensor register) {
      return new RegisterEither(register, null);
    }

    public static RegisterEither createRegisterFile(RegisterTensor registerFile) {
      return new RegisterEither(null, registerFile);
    }
  }

  /**
   * Helper class for the result of this pass.
   */
  public static class ImmediateDetectionContainer {
    /**
     * fieldUsage tracks how a field is used. This is true for all instructions and formats.
     * Thus, can be stored based on the {@link Format}
     */
    private final IdentityHashMap<Instruction, IdentityHashMap<Field, List<FieldUsage>>> fieldUsage;
    /**
     * Tracks how a register is used in an {@link Instruction}. This is not true for all
     * {@link Format}. That's why the key is {@link Instruction}.
     */
    private final IdentityHashMap<Instruction, IdentityHashMap<Field, RegisterUsageAggregate>>
        registerUsage;

    /**
     * Constructor.
     */
    public ImmediateDetectionContainer() {
      this.fieldUsage = new IdentityHashMap<>();
      this.registerUsage = new IdentityHashMap<>();
    }

    /**
     * Adding a {@link Instruction} to the result.
     */
    public void addInstruction(Instruction instruction) {
      if (!registerUsage.containsKey(instruction)) {
        registerUsage.put(instruction, new IdentityHashMap<>());
      }
    }

    /**
     * Adding a {@link FieldUsage} to the result.
     */
    public void addFieldUsage(Instruction instruction, Field field, FieldUsage kind) {
      var f = fieldUsage.computeIfAbsent(instruction, k -> new IdentityHashMap<>());
      f.computeIfAbsent(field, k -> new ArrayList<>()).add(kind);
    }

    private void addRegisterUsage(Instruction instruction,
                                  Field field,
                                  RegisterUsage kind,
                                  RegisterTensor registerTensor) {
      if (registerTensor.isSingleRegister()) {
        addSingleRegisterUsage(instruction, field, kind, registerTensor);
      } else if (registerTensor.isRegisterFile()) {
        addRegisterFileUsage(instruction, field, kind, registerTensor);
      } else {
        throw Diagnostic.error("Not supported register tensor", registerTensor.location())
            .build();
      }
    }

    /**
     * Adding a {@link RegisterUsage} to the result.
     * If a {@link Field} is already stored then {@code kind} is ignored and
     * {@link RegisterUsage#BOTH} is added.
     */
    private void addRegisterFileUsage(Instruction instruction,
                                      Field field,
                                      RegisterUsage kind,
                                      RegisterTensor registerFile) {
      registerFile.ensure(registerFile.isRegisterFile(), "Only a register file is allowed");
      var f = registerUsage.get(instruction);
      if (f == null) {
        throw new ViamError("Format must not be null");
      }

      if (f.containsKey(field)) {
        // It already exists therefore, we add `BOTH`.
        f.put(field,
            new RegisterUsageAggregate(RegisterUsage.BOTH,
                RegisterEither.createRegisterFile(registerFile)));
      } else {
        f.put(field,
            new RegisterUsageAggregate(kind, RegisterEither.createRegisterFile(registerFile)));
      }
    }

    /**
     * Adding a {@link RegisterUsage} to the result.
     * If a {@link Field} is already stored then {@code kind} is ignored and
     * {@link RegisterUsage#BOTH} is added.
     */
    private void addSingleRegisterUsage(Instruction instruction,
                                        Field field,
                                        RegisterUsage kind,
                                        RegisterTensor register) {
      register.ensure(register.isSingleRegister(), "Only a single register is allowed");
      var f = registerUsage.get(instruction);
      if (f == null) {
        throw new ViamError("Format must not be null");
      }

      if (f.containsKey(field)) {
        // It already exists therefore, we add `BOTH`.
        f.put(field,
            new RegisterUsageAggregate(RegisterUsage.BOTH,
                RegisterEither.createRegister(register)));
      } else {
        f.put(field, new RegisterUsageAggregate(kind, RegisterEither.createRegister(register)));
      }
    }

    public Map<Instruction, IdentityHashMap<Field, List<FieldUsage>>> getFieldUsages() {
      return fieldUsage;
    }

    /**
     * Get the usages of fields by instruction.
     */
    public Map<Field, List<FieldUsage>> getFieldUsages(Instruction instruction) {
      var obj = fieldUsage.get(instruction);
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;
    }

    /**
     * Get the usages of registers by instruction.
     */
    public Map<Field, RegisterUsageAggregate> getRegisterUsages(Instruction instruction) {
      var obj = registerUsage.get(instruction);
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;

    }

    /**
     * Get the fields by instruction and usage. Note that if the {@link RegisterUsage#BOTH} is set
     * then it also returned.
     *
     * @return a list of pairs. The left indicates the field and the right whether it is
     *     referencing register or register file.
     *     referencing {@link Register} or {@link RegisterFile}.
     */
    public List<Pair<Field, RegisterEither>> fieldsByRegisterUsage(
        Instruction instruction,
        RegisterUsage usage) {
      var obj = registerUsage.getOrDefault(instruction, new IdentityHashMap<>());

      return obj.entrySet()
          .stream().filter(x -> x.getValue().registerUsage() == usage
              || x.getValue().registerUsage() == RegisterUsage.BOTH)
          .map(x -> Pair.of(x.getKey(), x.getValue().ref))
          .collect(Collectors.toList());
    }

    /**
     * Get the immediate fields for the given format.
     */
    public List<Field> getImmediates(Instruction instruction) {
      return fieldUsage.getOrDefault(instruction, new IdentityHashMap<>())
          .entrySet()
          .stream()
          .filter(x -> x.getValue().contains(FieldUsage.IMMEDIATE))
          .map(Map.Entry::getKey)
          .toList();
    }
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var container = new ImmediateDetectionContainer();

    for (var instruction : viam.isa().stream().flatMap(isa -> isa.ownInstructions().stream())
        .toList()) {
      container.addInstruction(instruction);
      handleFields(instruction, container);
      handleFieldAccessFunctions(instruction, container);
    }

    return container;
  }

  private static void handleFieldAccessFunctions(Instruction instruction,
                                                 ImmediateDetectionContainer container) {
    instruction.behavior().getNodes(FieldAccessRefNode.class)
        .forEach(fieldAccessRefNode -> {
          var fieldRef = fieldAccessRefNode.fieldAccess().fieldRef();
          container.addInstruction(instruction);
          container.addFieldUsage(instruction, fieldRef,
              FieldUsage.IMMEDIATE);
        });
  }

  private static void handleFields(Instruction instruction,
                                   ImmediateDetectionContainer container) {
    instruction.behavior().getNodes(FieldRefNode.class)
        .forEach(fieldRefNode -> {
          var fieldRef = fieldRefNode.formatField();
          var registerRead = fieldRefNode.usages()
              .filter(
                  usage -> usage instanceof ReadRegTensorNode node
                      && node.regTensor().isSingleRegister())
              .map(usage -> ((ReadRegTensorNode) usage).regTensor())
              .findFirst();
          var registerFileRead = fieldRefNode.usages()
              .filter(
                  usage -> usage instanceof ReadRegTensorNode node
                      && node.regTensor().isRegisterFile())
              .map(usage -> ((ReadRegTensorNode) usage).regTensor())
              .findFirst();

          var registerWrite = fieldRefNode.usages()
              .filter(usage -> usage instanceof WriteRegNode)
              .filter(usage -> {
                var cast = (WriteRegNode) usage;
                var nodes = new ArrayList<Node>();
                // The field should be marked as REGISTER when the field is used as a register
                // index. Therefore, we need to check whether the node is in the address tree.
                // We avoid a direct check because it is theoretically possible to do
                // arithmetic with the register file's index. However, this is very unlikely.
                Objects.requireNonNull(cast.address()).collectInputsWithChildren(nodes);
                return cast.hasAddress()
                    && (cast.address() == fieldRefNode || nodes.contains(fieldRefNode));
              })
              .map(usage -> ((WriteRegNode) usage).register())
              .findFirst();

          var registerFileWrite = fieldRefNode.usages()
              .filter(usage -> usage instanceof WriteRegTensorNode node
                  && node.regTensor().isRegisterFile())
              .filter(usage -> {
                var cast = (WriteRegFileNode) usage;
                var nodes = new ArrayList<Node>();
                // The field should be marked as REGISTER when the field is used as a register
                // index. Therefore, we need to check whether the node is in the address tree.
                // We avoid a direct check because it is theoretically possible to do
                // arithmetic with the register file's index. However, this is very unlikely.
                Objects.requireNonNull(cast.address()).collectInputsWithChildren(nodes);
                return cast.hasAddress()
                    && (cast.address() == fieldRefNode || nodes.contains(fieldRefNode));
              })
              .map(usage -> ((WriteRegFileNode) usage).registerFile())
              .findFirst();

          container.addInstruction(instruction);
          if (registerRead.isPresent()
              || registerFileRead.isPresent()
              || registerWrite.isPresent()
              || registerFileWrite.isPresent()) {
            container.addFieldUsage(instruction, fieldRef,
                FieldUsage.REGISTER);
          } else {
            throw Diagnostic.error("Register index is not used as write or read.",
                fieldRefNode.location()).build();
          }

          if (registerRead.isPresent()) {
            container.addRegisterUsage(instruction, fieldRef,
                RegisterUsage.SOURCE, registerRead.get());
          } else if (registerFileRead.isPresent()) {
            container.addRegisterUsage(instruction, fieldRef,
                RegisterUsage.SOURCE, registerFileRead.get());
          } else if (registerWrite.isPresent()) {
            container.addRegisterUsage(instruction, fieldRef,
                RegisterUsage.DESTINATION, registerWrite.get());
          } else {
            container.addRegisterUsage(instruction, fieldRef,
                RegisterUsage.DESTINATION, registerFileWrite.get());
          }
        });
  }

  /**
   * Flags how a field is used.
   */
  public enum FieldUsage {
    REGISTER,
    IMMEDIATE
  }

  /**
   * Flags how a register is used. It can be source register, destination
   * or both.
   */
  public enum RegisterUsage {
    SOURCE,
    DESTINATION,
    BOTH
  }
}
