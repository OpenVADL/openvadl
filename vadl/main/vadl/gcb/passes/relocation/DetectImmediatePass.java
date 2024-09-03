package vadl.gcb.passes.relocation;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * This pass goes over all instructions and determines
 * how a field is used. It can be used as a register or immediate or other (because it is not
 * relevant).
 */
public class DetectImmediatePass extends Pass {

  public DetectImmediatePass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("detectImmediatePass");
  }

  public static class ImmediateDetectionContainer {
    private final IdentityHashMap<Format, IdentityHashMap<Format.Field, FieldUsage>> value;

    public ImmediateDetectionContainer() {
      this.value = new IdentityHashMap<>();
    }

    public void addFormat(Format format) {
      if (!value.containsKey(format)) {
        value.put(format, new IdentityHashMap<>());
      }
    }

    public void addField(Format format, Format.Field field, FieldUsage kind) {
      var f = value.get(format);
      if (f == null) {
        throw new ViamError("Format must not be null");
      }
      f.put(field, kind);
    }

    public Map<Format.Field, FieldUsage> get(Format format) {
      var obj = value.get(format);
      if (obj == null) {
        throw new ViamError("Hashmap must not be null");
      }
      return obj;
    }
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var container = new ImmediateDetectionContainer();

    viam.isas()
        .flatMap(isa -> isa.ownInstructions().stream())
        .flatMap(instruction -> instruction.behavior().getNodes(FieldRefNode.class))
        .forEach(fieldRefNode -> {
          var isRegister = fieldRefNode.usages()
              .anyMatch(
                  usage -> usage instanceof ReadRegNode || usage instanceof ReadRegFileNode);
          container.addFormat(fieldRefNode.formatField().format());

          if (isRegister) {
            container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                FieldUsage.REGISTER);
          } else {
            container.addField(fieldRefNode.formatField().format(), fieldRefNode.formatField(),
                FieldUsage.IMMEDIATE);
          }

          // There is no other option because any other field like opcode will be never referenced
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
}
