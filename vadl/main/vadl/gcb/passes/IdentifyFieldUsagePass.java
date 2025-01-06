package vadl.gcb.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Format.Field;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteResourceNode;

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
   * Helper class for the result of this pass.
   */
  public static class ImmediateDetectionContainer {
    /**
     * fieldUsage tracks how a field is used. This is true for all instructions and formats.
     * Thus, can be stored based on the {@link Format}
     */
    private final IdentityHashMap<Format, IdentityHashMap<Field, FieldUsage>> fieldUsage;
    /**
     * Tracks how a register is used in an {@link Instruction}. This is not true for all
     * {@link Format}. That's why the key is {@link Instruction}.
     */
    private final IdentityHashMap<Instruction, IdentityHashMap<Field, RegisterUsage>>
        registerUsage;

    /**
     * Constructor.
     */
    public ImmediateDetectionContainer() {
      this.fieldUsage = new IdentityHashMap<>();
      this.registerUsage = new IdentityHashMap<>();
    }

    /**
     * Adding a {@link Format} to the result.
     */
    public void addFormat(Format format) {
      if (!fieldUsage.containsKey(format)) {
        fieldUsage.put(format, new IdentityHashMap<>());
      }

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
    public void addField(Format format, Field field, FieldUsage kind) {
      var f = fieldUsage.get(format);
      if (f == null) {
        throw new ViamError("Format must not be null");
      }
      f.put(field, kind);
    }

    /**
     * Adding a {@link RegisterUsage} to the result.
     * If a {@link Field} is already stored then {@code kind} is ignored and
     * {@link RegisterUsage#BOTH} is added.
     */
    public void addField(Instruction instruction, Field field, RegisterUsage kind) {
      var f = registerUsage.get(instruction);
      if (f == null) {
        throw new ViamError("Format must not be null");
      }

      if (f.containsKey(field)) {
        // It already exists therefore, we add `BOTH`.
        f.put(field, RegisterUsage.BOTH);
      } else {
        f.put(field, kind);
      }
    }

    /**
     * Get a result by format.
     */
    public Map<Field, FieldUsage> getFieldUsages(Format format) {
      var obj = fieldUsage.get(format);
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;
    }

    public Map<Format, IdentityHashMap<Field, FieldUsage>> getFieldUsages() {
      return fieldUsage;
    }

    /**
     * Get the usages of registers by instruction.
     */
    public Map<Field, RegisterUsage> getRegisterUsages(Instruction instruction) {
      var obj = registerUsage.get(instruction);
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;
    }

    /**
     * Get the registers which are used as input or both.
     */
    public List<Field> getSourceRegisters(Instruction instruction) {
      var map = getRegisterUsages(instruction);
      return map.entrySet().stream()
          .filter(x -> x.getValue() == RegisterUsage.SOURCE || x.getValue() == RegisterUsage.BOTH)
          .map(Map.Entry::getKey)
          .toList();
    }

    /**
     * Get the usages of fields by instruction.
     */
    public Map<Field, FieldUsage> getImmediateUsages(Instruction instruction) {
      var obj = fieldUsage.get(instruction.format());
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;
    }

    /**
     * Get the immediate fields for the given format.
     */
    public List<Field> getImmediates(Format format) {
      return fieldUsage.getOrDefault(format, new IdentityHashMap<>())
          .entrySet()
          .stream()
          .filter(x -> x.getValue() == FieldUsage.IMMEDIATE)
          .map(Map.Entry::getKey)
          .toList();
    }
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var container = new ImmediateDetectionContainer();

    for (var instruction : viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
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
          container.addFormat(fieldRef.format());
          container.addField(fieldRef.format(), fieldRef,
              FieldUsage.IMMEDIATE);
        });
  }

  private static void handleFields(Instruction instruction, ImmediateDetectionContainer container) {
    instruction.behavior().getNodes(FieldRefNode.class)
        .forEach(fieldRefNode -> {
          var fieldRef = fieldRefNode.formatField();
          var isRegisterRead = fieldRefNode.usages()
              .anyMatch(
                  usage -> usage instanceof ReadRegNode || usage instanceof ReadRegFileNode);
          var isRegisterWrite = fieldRefNode.usages()
              .filter(usage -> usage instanceof WriteResourceNode)
              .anyMatch(usage -> {
                var cast = (WriteResourceNode) usage;
                var nodes = new ArrayList<Node>();
                // The field should be marked as REGISTER when the field is used as a register
                // index. Therefore, we need to check whether the node is in the address tree.
                // We avoid a direct check because it is theoretically possible to do
                // arithmetic with the register file's index. However, this is very unlikely.
                Objects.requireNonNull(cast.address()).collectInputsWithChildren(nodes);
                return cast.hasAddress()
                    && (cast.address() == fieldRefNode || nodes.contains(fieldRefNode));
              });

          container.addFormat(fieldRef.format());

          if (isRegisterRead || isRegisterWrite) {
            container.addField(fieldRef.format(), fieldRef,
                FieldUsage.REGISTER);
            if (isRegisterRead) {
              container.addField(instruction, fieldRef,
                  RegisterUsage.SOURCE);
            } else {
              container.addField(instruction, fieldRef,
                  RegisterUsage.DESTINATION);
            }
          } else {
            throw Diagnostic.error("Register index is not used as write or read.",
                fieldRefNode.sourceLocation()).build();
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
