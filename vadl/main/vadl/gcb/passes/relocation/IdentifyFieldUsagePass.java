package vadl.gcb.passes.relocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Format.Field;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
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
    private final IdentityHashMap<Format, IdentityHashMap<Field, FieldUsage>> fieldUsage;
    private final IdentityHashMap<Format, IdentityHashMap<Field, RegisterUsage>>
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
      if (!registerUsage.containsKey(format)) {
        registerUsage.put(format, new IdentityHashMap<>());
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
    public void addField(Format format, Field field, RegisterUsage kind) {
      var f = registerUsage.get(format);
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
    public Map<Field, FieldUsage> getFieldUsage(Format format) {
      var obj = fieldUsage.get(format);
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

    public Map<Format, IdentityHashMap<Field, FieldUsage>> getFieldUsages() {
      return fieldUsage;
    }
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var container = new ImmediateDetectionContainer();

    viam.isa()
        .map(isa -> isa.ownInstructions().stream())
        .orElse(Stream.empty())
        .flatMap(instruction -> instruction.behavior().getNodes(FieldRefNode.class))
        .forEach(fieldRefNode -> {
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
                return cast.address() == fieldRefNode || nodes.contains(fieldRefNode);
              });

          container.addFormat(fieldRefNode.formatField().format());

          if (isRegisterRead || isRegisterWrite) {
            container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                FieldUsage.REGISTER);
            if (isRegisterRead) {
              container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                  RegisterUsage.SOURCE);
            } else {
              container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                  RegisterUsage.DESTINATION);
            }
          } else {
            container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                FieldUsage.IMMEDIATE);
          }

          // There is no other option because any other field like opcode will never be referenced
          // the viam.
        });


    return container;
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
