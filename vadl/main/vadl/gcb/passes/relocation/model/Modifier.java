package vadl.gcb.passes.relocation.model;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.Map;
import java.util.Optional;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.RelocationCtx;
import vadl.lcb.passes.isaMatching.RelocationFunctionLabel;
import vadl.template.Renderable;
import vadl.viam.Format;
import vadl.viam.Relocation;

/**
 * Represents the transformation functions(?) in the assembler during fixups.
 */
public record Modifier(String value,
                       CompilerRelocation.Kind kind,
                       Format.Field field,
                       Optional<RelocationFunctionLabel> relocationFunctionLabel)
    implements Renderable {
  public static Modifier from(Relocation relocation, Format.Field field) {
    var name = relocation.identifier.lower() + "_" + field.identifier.tail().lower();
    var kind = relocation.isAbsolute() ? CompilerRelocation.Kind.ABSOLUTE
        : CompilerRelocation.Kind.RELATIVE;

    // Check the label for the relocation
    var ctx = ensureNonNull(relocation.extension(RelocationCtx.class),
        () -> Diagnostic.error("Expected a relocation label", relocation.sourceLocation()));

    return new Modifier("MO_" + name, kind, field, Optional.of(ctx.label()));
  }

  public static Modifier absolute(Format.Field imm) {
    return new Modifier("MO_ABS_" + imm.identifier.lower(), CompilerRelocation.Kind.ABSOLUTE, imm,
        Optional.empty());
  }

  public static Modifier relative(Format.Field imm) {
    return new Modifier("MO_REL_" + imm.identifier.lower(), CompilerRelocation.Kind.RELATIVE, imm,
        Optional.empty());
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of("value", value);
  }
}
