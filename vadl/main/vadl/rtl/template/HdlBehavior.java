// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.rtl.template;

import static java.util.Objects.requireNonNull;
import static vadl.viam.Constant.Value.fromInteger;

import com.google.common.collect.Streams;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlDebugPrintNode;
import vadl.rtl.ipg.nodes.RtlDecodeTreeNode;
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlInvalidInstructionNode;
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlOneHotDecodeNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlResetSignalNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.ipg.nodes.RtlValidSignalNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.vdt.target.rtl.RtlTableDecoderGenerator;
import vadl.vdt.target.rtl.RtlVdtDecoderGenerator;
import vadl.viam.Constant;
import vadl.viam.Definition;
import vadl.viam.Resource;
import vadl.viam.Signal;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Create HDL signals, ports and connections from HDL module behaviors.
 */
public class HdlBehavior {

  /**
   * Create HDL signals and ports from HDL module behaviors.
   *
   * @param modules list of HDL modules
   */
  public static void create(List<HdlModule> modules) {
    for (HdlModule module : modules) {
      create(module);
    }
  }

  /**
   * Create HDL signals and ports from an HDL module behavior.
   *
   * @param module HDL module
   */
  public static void create(HdlModule module) {
    var behavior = module.behavior();
    if (behavior == null) {
      return;
    }

    // create signals and assignments (connections)
    var collector = new SignalCollector(module);

    // Prepare & cache the signals for is-instruction and select-by-instruction nodes by lowering
    // the decode tree node first (if present).
    Optional.ofNullable(module.behavior())
        .flatMap(g -> g.getNodes(RtlDecodeTreeNode.class).findAny())
        .ifPresent(collector::dispatch);

    behavior.getNodes(WriteResourceNode.class).forEach(collector::handle);
    behavior.getNodes(RtlDebugPrintNode.class).forEach(collector::handle);
  }

  @DispatchFor(
      value = ExpressionNode.class,
      returnType = String.class,
      include = {"vadl.viam.asm", " vadl.rtl.ipg.node"}
  )
  static class SignalCollector {

    private final HdlModule module;

    private final HashMap<Node, String> cacheExprOrSig = new HashMap<>();

    private final HashMap<Node, String> cachePortOrRes = new HashMap<>();

    SignalCollector(HdlModule module) {
      this.module = module;
    }

    public String dispatch(ExpressionNode node) {
      return exprOrSig(node);
    }

    private String exprOrSig(ExpressionNode node) {
      if (cacheExprOrSig.containsKey(node)) {
        return cacheExprOrSig.get(node);
      }
      var expr = SignalCollectorDispatcher.dispatch(this, node);

      // create signal and assignment if necessary
      if (isSignal(node)) {
        var def = module.definition();
        if (def == null) {
          def = module.context().viam();
        }
        var name = module.context().name(node, module.localNames(), fallbackName(node));
        var id = def.identifier.append(name);
        var signal = new Signal(id, node.type().asDataType());
        module.addResource(signal);

        module.addConnection(new HdlConnection(
            new HdlConnection.ResourceEndpoint(signal, node),
            new HdlConnection.ExpressionEndpoint(node, expr),
            false, null
        ));
        expr = signal.simpleName();
      }

      cacheExprOrSig.put(node, expr);
      return expr;
    }

    private String portOrResource(Node node, Resource resource) {
      if (cachePortOrRes.containsKey(node)) {
        return cachePortOrRes.get(node);
      }
      String expr;

      if (module.resources().contains(resource)) {
        expr = new HdlConnection.ResourceEndpoint(resource, null).rtlName();
      } else {
        var name = module.context().name(node, module.portNames(), fallbackName(node));
        var port = new HdlPort(name, resource, (node instanceof ReadResourceNode),
            (node instanceof WriteResourceNode), node);
        module.addPort(port);
        expr = "io." + port.hdlName();
      }

      cachePortOrRes.put(node, expr);
      return expr;
    }

    @Nullable
    private String fallbackName(Node node) {
      if (node instanceof RtlIsInstructionNode n) {
        return "is_" + n.instructions().stream()
            .map(Definition::simpleName).collect(Collectors.joining(""));
      }
      if (node instanceof RtlInvalidInstructionNode) {
        return "invalid_insn";
      }
      return null;
    }

    @Handler
    String handle(BuiltInCall node) {
      var args = node.arguments().stream()
          .map(this::dispatch)
          .collect(Collectors.joining(", "));
      return node.builtIn().name().replace("VADL::", "") + "(" + args + ")";
    }

    @Handler
    String handle(SelectNode node) {
      return "Mux((" + dispatch(node.condition()) + ").asBool, " + dispatch(node.trueCase())
          + ", " + dispatch(node.falseCase()) + ")";
    }

    @Handler
    String handle(SignExtendNode node) {
      return dispatch(node.value()) + ".sext(" + node.type().bitWidth() + ".W)";
    }

    @Handler
    String handle(ZeroExtendNode node) {
      return dispatch(node.value()) + ".zext.asUInt";
    }

    @Handler
    String handle(TruncateNode node) {
      return dispatch(node.value()) + ".trunc(" + node.type().bitWidth() + ".W)";
    }

    @Handler
    String handle(ConstantNode node) {
      if (node.type() instanceof DataType type) {
        switch (type) {
          case BoolType t -> {
            return node.constant().asVal().bool() + ".B";
          }
          case UIntType t -> {
            return "\"" + node.constant().asVal().hexadecimal("h") + "\".U("
                + type.bitWidth() + ".W)";
          }
          case SIntType t -> {
            return "\"" + node.constant().asVal().hexadecimal("h") + "\".U("
                + type.bitWidth() + ".W)";
          }
          case BitsType t -> {
            return "\"" + node.constant().asVal().hexadecimal("h") + "\".U("
                + type.bitWidth() + ".W)";
          }
          default -> {
          }
        }
      }
      throw new ViamGraphError("Type can not be translated to HDL")
          .addContext(node);
    }

    @Handler
    String handle(LetNode node) {
      return dispatch(node.expression());
    }

    @Handler
    String handle(SliceNode node) {
      var slices = node.bitSlice().parts()
          .map(p -> dispatch(node.value()) + "(" + p.msb() + ", " + p.lsb() + ")").toList();
      if (slices.size() > 1) {
        return "Cat(" + String.join(", ", slices) + ")";
      }
      return slices.getFirst();
    }

    @Handler
    String handle(RtlInstructionWordSliceNode node) {
      var ins = node.instruction();
      if (ins == null) {
        throw new ViamGraphError("Missing instruction input").addContext(node);
      }
      var slices = node.slice().parts()
          .map(p -> dispatch(ins) + "(" + p.msb() + ", " + p.lsb() + ")").toList();
      if (slices.size() > 1) {
        return "Cat(" + String.join(", ", slices) + ")";
      }
      return slices.getFirst();
    }

    @Handler
    String handle(RtlDecodeTreeNode decodeTreeNode) {

      final var def = Optional.ofNullable(module.definition())
          .orElse(module.context().viam());

      final Map<vadl.viam.Instruction, Map<Signal, ConstantNode>> decisionMap =
          new LinkedHashMap<>();
      final Set<Signal> signals = new LinkedHashSet<>();
      Signal invalidInsn = null;

      for (Node usage : decodeTreeNode.usages().toList()) {

        // The name is prefixed with 'dec_' by now to indicate that the signal will be assigned
        // by the decode tree.
        var name = module.context().name(usage, module.localNames(), fallbackName(usage));
        var id = def.identifier.append(name);
        var signal = new Signal(id, ((ExpressionNode) usage).type().asDataType());
        signals.add(signal);

        switch (usage) {
          case RtlIsInstructionNode n -> {

            final var value = new ConstantNode(Constant.Value.fromBoolean(true));
            n.instructions()
                .forEach(i -> decisionMap.computeIfAbsent(i, x -> new HashMap<>())
                    .put(signal, value));

          }

          case RtlInvalidInstructionNode x -> invalidInsn = signal;

          case RtlOneHotDecodeNode n -> {

            for (int i = 0; i < n.instructions().size(); i++) {

              final var value = new ConstantNode(
                  fromInteger(BigInteger.valueOf(i), n.type().asDataType()));

              n.instructions().get(i)
                  .forEach(insn -> decisionMap
                      .computeIfAbsent(insn, x -> new HashMap<>())
                      .put(signal, value));
            }

          }
          default -> throw new ViamGraphError("Unsupported usage of decode tree: %s", usage);
        }

        module.addResource(signal);
        cacheExprOrSig.put(usage, signal.simpleName());
      }

      final var isa = module.context().viam().isa().orElse(null);
      ViamError.ensureNonNull(isa, "The ISA must not be null.");

      // Include ISA instructions without signals so they're not decoded as 'invalid' insns.
      isa.ownInstructions()
          .forEach(i -> decisionMap.computeIfAbsent(i, x -> new HashMap<>()));

      ViamError.ensureNonNull(decodeTreeNode.instructionWord(),
          "The instruction word of the RtlDecodeTreeNode must not be null.");

      final String insnWord = dispatch(decodeTreeNode.instructionWord());

      ViamError.ensureNonNull(invalidInsn,
          "Expected an RtlInvalidInstruction node to exist.");

      final String result;

      final var vdt = module.context().vdt();
      if (vdt != null) {
        result = new RtlVdtDecoderGenerator(insnWord, decisionMap, signals, invalidInsn)
            .generate(vdt);
      } else {
        result = new RtlTableDecoderGenerator(insnWord, decisionMap, signals, invalidInsn)
            .generate();
      }

      // Create the HDL statement representing the decode tree
      module.addConnection(new HdlConnection(
          null,
          new HdlConnection.ExpressionEndpoint(decodeTreeNode, result),
          false, null
      ));

      // The return value isn't really used anywhere, but we have to return something.
      return "";
    }

    @Handler
    String handle(RtlIsInstructionNode node) {
      var signalName = cacheExprOrSig.get(node);
      if (signalName == null) {
        throw new IllegalStateException("Expected signal for is-insn node to exist");
      }
      return signalName;
    }

    @Handler
    String handle(RtlInvalidInstructionNode node) {
      var signalName = cacheExprOrSig.get(node);
      if (signalName == null) {
        throw new IllegalStateException("Expected signal for invalid-insn node to exist");
      }
      return signalName;
    }

    @Handler
    String handle(RtlOneHotDecodeNode node) {
      var signalName = cacheExprOrSig.get(node);
      if (signalName == null) {
        throw new IllegalStateException("Expected signal for is-insn node to exist");
      }
      return signalName;
    }

    @Handler
    String handle(RtlSelectByInstructionNode node) {
      var vals = node.values().stream();
      var sel = node.selection();
      if (sel == null) {
        throw new ViamGraphError("Missing selection input").addContext(node);
      }
      return "MuxLookup[Bits](" + dispatch(sel) + ", 0.U)(Seq("
          + Streams.mapWithIndex(vals, (val, i) -> {
            requireNonNull(val);
            return i + ".U -> " + dispatch(val);
          }
          )
          .collect(Collectors.joining(", ")) + "))";
    }

    @Handler
    String handle(RtlResetSignalNode node) {
      return "reset.asBool";
    }

    @Handler
    String handle(RtlValidSignalNode node) {
      if (node.validNode() instanceof RtlReadMemNode read) {
        var expr = portOrResource(read, read.resourceDefinition());
        return expr + ".valid";
      }
      if (node.validNode() instanceof RtlWriteMemNode write) {
        var expr = portOrResource(write, write.resourceDefinition());
        return expr + ".valid";
      }
      throw new ViamGraphError("No valid signal for node")
          .addContext(node.validNode().asNode());
    }

    @Handler
    String handle(ReadResourceNode node) {
      var expr = portOrResource(node, node.resourceDefinition());
      var port = module.ports().stream().filter(p -> p.nodes().contains(node)).findFirst();
      var res = module.resources().stream().filter(r -> node.resourceDefinition().equals(r))
          .findFirst();
      if (port.isPresent() && node.hasAddress()) {
        if (node instanceof RtlConditionalReadNode read) {
          var cond = read.nullableCondition();
          if (cond != null) {
            var condEnd = new HdlConnection.ExpressionEndpoint(
                cond,
                dispatch(cond)
            );
            module.connections().add(new HdlConnection(
                new HdlConnection.ExpressionEndpoint(node, expr + ".enable"),
                condEnd,
                false, null
            ));
          }
        }
        var addrEnd = new HdlConnection.ExpressionEndpoint(
            node.address(),
            dispatch(node.address())
        );
        module.connections().add(new HdlConnection(
            new HdlConnection.ExpressionEndpoint(node, expr + ".address"),
            addrEnd,
            false, null
        ));
        if (node instanceof RtlReadMemNode read) {
          var wordsEnd = new HdlConnection.ExpressionEndpoint(
              read.words(),
              dispatch(read.words())
          );
          module.connections().add(new HdlConnection(
              new HdlConnection.ExpressionEndpoint(node, expr + ".words"),
              wordsEnd,
              false, null
          ));
        }
      }
      if (res.isPresent() || node instanceof ReadSignalNode) {
        return expr;
      }
      return expr + ".data.asUInt";
    }

    // not a handler because we generate dispatch only for expression nodes
    void handle(WriteResourceNode node) {
      var res = module.resources().stream().filter(r -> node.resourceDefinition().equals(r))
          .findFirst();
      var cond = node.nullableCondition();
      HdlConnection.ExpressionEndpoint condEnd;
      if (cond != null) {
        if (cond instanceof RtlResetSignalNode && node instanceof WriteRegTensorNode write) {
          if (write.registerTensor().isSingleRegister()) {
            module.addRegisterReset(write.registerTensor(), dispatch(node.value()));
          }
          return;
        }
        condEnd = new HdlConnection.ExpressionEndpoint(
            cond,
            dispatch(cond)
        );
      } else {
        condEnd = null;
      }
      if (res.isPresent()) {
        res.ifPresent(resource -> module.addConnection(new HdlConnection(
            new HdlConnection.ResourceEndpoint(resource, null),
            new HdlConnection.ExpressionEndpoint(node, dispatch(node.value())), // one input
            false, condEnd
        )));
        return;
      }
      var expr = portOrResource(node, node.resourceDefinition());
      if (node instanceof WriteSignalNode) {
        var valueEnd = new HdlConnection.ExpressionEndpoint(
            node.value(),
            dispatch(node.value())
        );
        module.connections().add(new HdlConnection(
            new HdlConnection.ExpressionEndpoint(node, expr),
            valueEnd,
            false, null
        ));
      } else {
        if (condEnd != null) {
          module.connections().add(new HdlConnection(
              new HdlConnection.ExpressionEndpoint(node, expr + ".enable"),
              condEnd,
              false, null
          ));
        }
        if (node.hasAddress()) {
          var addrEnd = new HdlConnection.ExpressionEndpoint(
              node.address(),
              dispatch(node.address())
          );
          module.connections().add(new HdlConnection(
              new HdlConnection.ExpressionEndpoint(node, expr + ".address"),
              addrEnd,
              false, null
          ));
        }
        if (node instanceof RtlWriteMemNode write) {
          var wordsEnd = new HdlConnection.ExpressionEndpoint(
              write.words(),
              dispatch(write.words())
          );
          module.connections().add(new HdlConnection(
              new HdlConnection.ExpressionEndpoint(node, expr + ".words"),
              wordsEnd,
              false, null
          ));
        }
        var valueEnd = new HdlConnection.ExpressionEndpoint(
            node.value(),
            dispatch(node.value()) + ".asTypeOf(" + expr + ".data)"
        );
        module.connections().add(new HdlConnection(
            new HdlConnection.ExpressionEndpoint(node, expr + ".data"),
            valueEnd,
            false, null
        ));
      }
    }

    // not a handler because we generate dispatch only for expression nodes
    void handle(RtlDebugPrintNode node) {
      var str = node.render(
          (placeholder, value) -> "${" + dispatch(value) + "}" + placeholder);
      var print = "printf(cf\"%T " + str + "\\n\")";
      HdlConnection.ExpressionEndpoint cond = null;
      if (node.nullableCondition() != null) {
        cond = new HdlConnection.ExpressionEndpoint(node.condition(), dispatch(node.condition()));
      }
      module.connections().add(new HdlConnection(
          null,
          new HdlConnection.ExpressionEndpoint(node, print),
          false,
          cond
      ));
    }

  }

  /**
   * Determines if a node value needs to be emitted as a signal in HDL.
   *
   * @param node expression node
   * @return True, if expression node needs to be a signal.
   */
  public static boolean isSignal(ExpressionNode node) {
    if (node instanceof RtlInstructionWordSliceNode
        || node instanceof SliceNode
        || node instanceof ConstantNode
        || node instanceof UnaryNode
        || node instanceof RtlValidSignalNode
        || node instanceof ReadResourceNode
        || node instanceof RtlDecodeTreeNode
        || node instanceof RtlResetSignalNode
        || (node instanceof BuiltInCall builtIn && builtIn.arguments().size() < 2)) {
      return false;
    }
    return (node.usageCount() > 1
        || node instanceof SelectNode
        || node instanceof RtlIsInstructionNode
        || node instanceof RtlInvalidInstructionNode);
  }

}
