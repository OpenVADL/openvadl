package vadl.viam.passes.htmlDump;

import static vadl.viam.helper.TestGraphUtils.binaryOp;
import static vadl.viam.helper.TestGraphUtils.bits;
import static vadl.viam.helper.TestGraphUtils.intU;

import java.io.IOException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import vadl.dump.HtmlDumpPass;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Assembly;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;

public class HtmlDumpTest {

  @Test
  void dumpTest() throws IOException {

    var spec = new Specification(Identifier.noLocation("Hello world"));
    var graph = new Graph("Hello Graph");
    var end = graph.addWithInputs(new ReturnNode(
        binaryOp(BuiltInTable.ADD, bits(0b11111, 10), bits(0b11110, 10))
    ));
    graph.add(new StartNode(end));
    var func =
        new Function(Identifier.noLocation("Dummy"),
            new Parameter[] {},
            Type.bits(10),
            graph);

    spec.add(func);


    new HtmlDumpPass(new HtmlDumpPass.Config("demoPhase", "build/testdump"))
        .execute(PassResults.empty(), spec);

  }

}
