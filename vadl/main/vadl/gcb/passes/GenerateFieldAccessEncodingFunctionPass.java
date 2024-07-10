package vadl.gcb.passes;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

public class GenerateFieldAccessEncodingFunctionPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam) {
    viam.isas()
        .flatMap(x -> x.formats().stream())
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() == null)
        .forEach(fieldAccess -> {
          var behavior = fieldAccess.accessFunction().behavior();

          if (trivialBehavior(behavior)) {
            var ident = fieldAccess.identifier.append("encoding");
            var identParam = ident.append(fieldAccess.name());
            var param = new Parameter(identParam, fieldAccess.accessFunction().returnType());
            var function =
                new Function(ident, new Parameter[] {param}, fieldAccess.fieldRef().type());

            fieldAccess.setEncoding(function);
            generateTrivialEncodingFunction(param, fieldAccess.fieldRef(), Objects.requireNonNull(
                fieldAccess.encoding()).behavior());
          } else if (hasOnlyShifts(behavior)) {

          }
        });

    var haveEncoding = viam.isas()
        .flatMap(x -> x.formats().stream())
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .allMatch(x -> x.encoding() != null);

    if (!haveEncoding) {
      throw new ViamError("Not all formats have an encoding");
    }


    return null;
  }

  /**
   * Generate a simple encoding for {@code fieldAccess} and adds nodes to
   * {@code behavior} graph.
   */
  private void generateTrivialEncodingFunction(Parameter parameter,
                                               Format.Field fieldRef,
                                               Graph behavior) {
    // The field takes up a certain slice.
    // But we need to take a slice of the immediate of the same size.
    var fieldAccessBitSlice = fieldRef.bitSlice();
    var invertedSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        Constant.BitSlice.Part.of(fieldAccessBitSlice.bitSize() - 1, 0)});
    var invertedSliceNode = new SliceNode(new FuncParamNode(
        parameter),
        invertedSlice,
        fieldRef.type());
    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
  }

  /**
   * Checks whether the behavior only contains (logical or arithmetic) left or right shifts.
   */
  private boolean hasOnlyShifts(Graph behavior) {
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.LSL
              || cast.builtIn() == BuiltInTable.LSR) {
            return true;
          }

          return false;
        });
  }

  /**
   * Checks whether the behavior does not contain any {@link BuiltIn} or {@link SliceNode}.
   */
  private boolean trivialBehavior(Graph behavior) {
    return behavior.getNodes()
        .filter(x -> x instanceof BuiltInCall || x instanceof SliceNode)
        .findAny().isEmpty();
  }
}
