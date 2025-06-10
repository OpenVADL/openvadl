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

package vadl.rtl.template;

import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlOneHotDecodeNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.viam.Signal;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

public class HdlBehavior {

  public static void create(List<HdlModule> modules) {
    for (HdlModule module : modules) {
      create(module);
    }
  }

  public static void create(HdlModule module) {
    var behavior = module.behavior();
    if (behavior == null) {
      return;
    }

    // create signals and assignments (connections)
    var collector = new SignalCollector(module);
    behavior.getNodes(WriteResourceNode.class).flatMap(Node::inputs)
        .filter(ExpressionNode.class::isInstance).map(ExpressionNode.class::cast)
        .forEach(collector::dispatch);
  }

  @DispatchFor(
      value = ExpressionNode.class,
      returnType = String.class,
      include = { "vadl.viam.asm", " vadl.rtl.ipg.node" }
  )
  static class SignalCollector {

    private final HdlModule module;

    private final HashMap<ExpressionNode, String> cache = new HashMap<>();

    SignalCollector(HdlModule module) {
      this.module = module;
    }

    public String dispatch(ExpressionNode node) {
      return SignalCollectorDispatcher.dispatch(this, node);
    }

    private String exprOrSig(ExpressionNode node, Supplier<String> getExpr) {
      if (cache.containsKey(node)) {
        return cache.get(node);
      }
      var expr = getExpr.get();

      // create signal and assignment if necessary
      if (isSignal(node)) {
        var def = module.definition();
        if (def == null) {
          def = module.context().viam();
        }
        var name = module.context().name(node, module.localNames(), null);
        var id = def.identifier.append(name);
        var signal = new Signal(id, node.type().asDataType());
        module.addResource(signal);

        if (expr != null) {
          module.addConnection(new HdlConnection(
              new HdlConnection.ResourceEndpoint(signal, node),
              new HdlConnection.ExpressionEndpoint(node, expr),
              false
          ));
        }
        expr = signal.simpleName();
      }

      cache.put(node, expr);
      return expr;
    }

    @Handler
    String handle(BuiltInCall node) {
      return exprOrSig(node, () -> {
        var args = node.arguments().stream()
            .map(this::dispatch)
            .collect(Collectors.joining(", "));
        return node.builtIn().name().replace("VADL::", "") + "(" + args + ")";
      });
    }

    @Handler
    String handle(SelectNode node) {
      return exprOrSig(node, () -> "Mux(" + dispatch(node.condition()) + ", " + dispatch(node.trueCase())
          + ", " + dispatch(node.falseCase()) + ")");
    }

    @Handler
    String handle(SignExtendNode node) {
      return exprOrSig(node, () -> dispatch(node.value()) + ".sext(" + node.type().bitWidth() + ".W)");
    }

    @Handler
    String handle(ZeroExtendNode node) {
      return exprOrSig(node, () -> dispatch(node.value()) + ".zext(" + node.type().bitWidth() + ".W)");
    }

    @Handler
    String handle(TruncateNode node) {
      return exprOrSig(node, () -> dispatch(node.value()));
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
            return "\"" + node.constant().asVal().hexadecimal("h") + "\".S("
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
      return "0.U"; // TODO decode input
    }

    @Handler
    String handle(RtlIsInstructionNode node) {
      return node.instructions().stream()
          .map(HdlUtils::getInstructionBitPattern)
          .map(pat -> "BitPat(\"b" + pat + "\").matches(0.U)") // TODO decode input
          .collect(Collectors.joining(" | "));
    }

    @Handler
    String handle(RtlOneHotDecodeNode node) {
      return "OHToUInt(Cat(" + node.values().reversed().stream()
          .map(this::dispatch).collect(Collectors.joining(", ")) + "))";
    }

    @Handler
    String handle(RtlSelectByInstructionNode node) {
      var vals = node.values().stream();
      var sel = node.selection();
      if (sel == null) {
        throw new ViamGraphError("Missing selection in put").addContext(node);
      }
      return "MuxLookup(" + dispatch(sel) + ", 0.U)(Seq("
          + Streams.mapWithIndex(vals, (val, i) -> i + ".U -> " + dispatch(val))
              .collect(Collectors.joining(", ")) + "))";
    }

    @Handler
    String handle(ReadResourceNode node) {
      return exprOrSig(node, () -> {
        var inputs = node.inputs()
            .filter(ExpressionNode.class::isInstance).map(ExpressionNode.class::cast)
            .map(this::dispatch).collect(Collectors.joining(", "));
        return "Seq(" + inputs + ")";
      });
    }
  }

  public static boolean isSignal(ExpressionNode node) {
    if (node instanceof RtlInstructionWordSliceNode
        || node instanceof SliceNode
        || node instanceof ConstantNode) {
      return false;
    }
    return (node.usageCount() > 1
        || node.usages().anyMatch(ReadResourceNode.class::isInstance)
        || node.usages().anyMatch(WriteResourceNode.class::isInstance)
        || node instanceof LetNode
        || node instanceof SelectNode
        || node instanceof RtlIsInstructionNode
        || node instanceof ReadResourceNode);
  }

}
