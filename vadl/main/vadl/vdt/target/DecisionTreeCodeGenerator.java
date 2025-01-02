package vadl.vdt.target;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.impl.theiling.LeafNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.Bit;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;

/**
 * Generate C/C++ code for a decision tree from an in-memory representation of the decision tree.
 */
public class DecisionTreeCodeGenerator implements Visitor<Void>, Closeable, AutoCloseable {

  private final Writer writer;
  private int indent = 0;

  public DecisionTreeCodeGenerator(OutputStream out) {
    this.writer = new PrintWriter(out, true, StandardCharsets.UTF_8);
  }

  public void generate(Node tree) throws IOException {

    indent = 0;

    writer.append("#include <cstdint>\n\n");
    writer.append("bool decode(uint32_t insn) {\n");

    indent += 2;
    tree.accept(this);

    writer.append(" ".repeat(indent)).append("return false;\n");
    indent -= 2;

    writer.append("}\n");
  }

  @Override
  public Void visit(InnerNode node) {

    if (!(node instanceof InnerNodeImpl n)) {
      throw new IllegalArgumentException("Node type not supported: " + node.getClass());
    }

    final BigInteger mask = n.getMask().toValue();
    final Map<BitPattern, Node> children = n.getChildren();

    try {
      writer.append(" ".repeat(indent)).append("switch (insn & 0x").append(mask.toString(16))
          .append(") {\n");
      indent += 2;

      for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
        final BigInteger caseValue = toBitVector(entry.getKey()).toValue();
        writer.append(" ".repeat(indent)).append("case 0x").append(caseValue.toString(16))
            .append(":\n");
        indent += 2;
        entry.getValue().accept(this);
        indent -= 2;
      }

      if (n.getFallback() != null) {
        writer.append(" ".repeat(indent)).append("default:\n");
        indent += 2;
        n.getFallback().accept(this);
        indent -= 2;
      }

      indent -= 2;
      writer.append(" ".repeat(indent)).append("}\n");

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  @Override
  public Void visit(LeafNode node) {

    if (!(node instanceof LeafNodeImpl lf)) {
      throw new IllegalArgumentException("Leaf node type not supported: " + node.getClass());
    }

    final Instruction insn = lf.instruction();

    try {
      writer.append(" ".repeat(indent)).append("match_")
          .append(insn.source().identifier.simpleName())
          .append("();\n");
      writer.append(" ".repeat(indent)).append("return true;\n");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

  private BitVector toBitVector(BitPattern pattern) {
    Bit[] bits = new Bit[pattern.width()];
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new Bit(pattern.get(i).getValue() == PBit.Value.ONE);
    }
    return new BitVector(bits);
  }
}
