// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static vadl.TestUtils.findDefinitionByNameIn;
import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.iss.passes.extensions.InstrExecPlan.DirectGvecSupport;
import vadl.iss.passes.extensions.InstrExecPlan.ExecutionPath;
import vadl.iss.passes.extensions.VectorTensorPlan;
import vadl.iss.passes.extensions.VectorTensorPlan.OperandForm;
import vadl.iss.passes.extensions.VectorTensorPlan.OperandKind;
import vadl.iss.passes.extensions.VectorTensorPlan.OverlapPolicy;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorOp;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ParamNode;

public class IssVectorTcgAnalysisPassTest extends AbstractTest {

  private static final IssConfiguration CONFIG =
      new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE));
  private static final Map<String, Specification> SPEC_CACHE = new ConcurrentHashMap<>();

  @Test
  void recognizesRv64vVaddVv()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VV");
    var executionPlan = executionPlan(instr);
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals("forall-write-0", directGvec.region().regionId());
    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_VECTOR, plan.operandForm());
    assertEquals(32, plan.elementBits());
    assertEquals(32, plan.laneCount());
    assertEquals(128, plan.opBytes());
    assertNotNull(plan.shape());
    assertEquals(128, plan.shape().maxszBytes());
    assertTrue(plan.shape().fullRange());
    assertTrue(plan.shape().contiguousLayout());
    assertTrue(plan.shape().paddingPreserved());
    assertEquals(OverlapPolicy.NO_PARTIAL_OVERLAP, plan.overlapPolicy());
    assertNotNull(plan.destination());
    assertEquals(List.of("vd"), bindingParamNames(plan.destination().accessorIndices()));
    assertEquals(2, plan.operands().size());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertNotNull(plan.operands().get(0).registerBinding());
    assertNotNull(plan.operands().get(1).registerBinding());
    assertEquals(List.of("vs2"),
        bindingParamNames(plan.operands().get(0).registerBinding().accessorIndices()));
    assertEquals(List.of("vs1"),
        bindingParamNames(plan.operands().get(1).registerBinding().accessorIndices()));
    assertEquals(ExecutionPath.NORMAL_TCG, instrInfo(instr).executionPath());
  }

  @Test
    void recognizesRv64vVsubVv()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "RV64IMV::VSUB_VV"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.SUB, plan.op());
    assertEquals(OperandForm.VECTOR_VECTOR, plan.operandForm());
    assertEquals(32, plan.elementBits());
    assertEquals(32, plan.laneCount());
  }

  @Test
    void recognizesRv64vVaddVx()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "RV64IMV::VADD_VX"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_SCALAR, plan.operandForm());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertEquals(OperandKind.SCALAR_REGISTER, plan.operands().get(1).kind());
  }

  @Test
    void recognizesRv64vVaddVi()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "RV64IMV::VADD_VI"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_IMMEDIATE, plan.operandForm());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertEquals(OperandKind.IMMEDIATE, plan.operands().get(1).kind());
  }

  @Test
  void recognizesVectorBenchAliasVaddDoVv()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VADD_DO_VV"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_VECTOR, plan.operandForm());
    assertEquals(32, plan.elementBits());
    assertEquals(32, plan.laneCount());
    assertEquals("Z", plan.destination().registerTensor().simpleName());
    assertEquals(List.of("vd"), bindingParamNames(plan.destination().accessorIndices()));
    assertEquals(List.of("vs1"),
        bindingParamNames(plan.operands().get(0).registerBinding().accessorIndices()));
    assertEquals(List.of("vs2"),
        bindingParamNames(plan.operands().get(1).registerBinding().accessorIndices()));
  }

  @Test
  void recognizesVectorBenchVmovVv()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VMOV_VV"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.MOV, plan.op());
    assertEquals(OperandForm.VECTOR_MOVE, plan.operandForm());
    assertEquals(1, plan.operands().size());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().getFirst().kind());
    assertEquals(List.of("vd"), bindingParamNames(plan.destination().accessorIndices()));
    assertEquals(List.of("vs1"),
        bindingParamNames(plan.operands().getFirst().registerBinding().accessorIndices()));
  }

  @Test
  void recognizesVectorBenchVbcastX()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VBCAST_X"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.MOV, plan.op());
    assertEquals(OperandForm.SCALAR_BROADCAST, plan.operandForm());
    assertEquals(1, plan.operands().size());
    assertEquals(OperandKind.SCALAR_REGISTER, plan.operands().getFirst().kind());
    assertEquals(List.of("vd"), bindingParamNames(plan.destination().accessorIndices()));
  }

  @Test
  void recognizesVectorBenchVaddXinc()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VADD_XINC"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_VECTOR, plan.operandForm());
    assertEquals(2, plan.operands().size());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(1).kind());
  }

  @Test
  void recognizesVectorBenchVaddVxInc()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VADD_VX_INC"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_SCALAR, plan.operandForm());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertEquals(OperandKind.SCALAR_REGISTER, plan.operands().get(1).kind());
    assertTrue(containsBuiltInCall(plan.operands().get(1).valueExpression()));
  }

  @Test
  void recognizesVectorBenchVaddVxIncXinc()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var executionPlan = executionPlan(findInstruction(viam, "VectorBench64::VADD_VX_INC_XINC"));
    var directGvec = singleDirectGvecRegion(executionPlan);
    var plan = vectorPlan(directGvec);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(directGvec.isViable(), directGvec::toString);
    assertEquals(VectorOp.ADD, plan.op());
    assertEquals(OperandForm.VECTOR_SCALAR, plan.operandForm());
    assertEquals(OperandKind.VECTOR_REGISTER, plan.operands().get(0).kind());
    assertEquals(OperandKind.SCALAR_REGISTER, plan.operands().get(1).kind());
    assertTrue(containsBuiltInCall(plan.operands().get(1).valueExpression()));
  }

  @Test
  void keepsScalarInstructionOnNormalTcgPathWithoutDirectGvecPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VSETVLI");
    var executionPlan = executionPlan(instr);

    assertEquals(ExecutionPath.NORMAL_TCG, executionPlan.selectedPath());
    assertTrue(executionPlan.directGvecRegions().isEmpty(), executionPlan::toString);
    assertEquals(ExecutionPath.NORMAL_TCG, instrInfo(instr).executionPath());
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    try {
      return SPEC_CACHE.computeIfAbsent(specPath, path -> {
        try {
          return setupPassManagerAndRunSpec(path,
              PassOrders.iss(CONFIG).untilFirst(IssExecStrategyPass.class)
          ).specification();
        } catch (IOException | DuplicatedPassKeyException e) {
          throw new CachedSpecException(e);
        }
      });
    } catch (CachedSpecException e) {
      if (e.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      if (e.getCause() instanceof DuplicatedPassKeyException duplicatedPassKeyException) {
        throw duplicatedPassKeyException;
      }
      throw e;
    }
  }

  private Instruction findInstruction(Specification viam, String name) {
    return findDefinitionByNameIn(name, viam, Instruction.class);
  }

  private boolean containsBuiltInCall(ExpressionNode expression) {
    if (expression instanceof BuiltInCall) {
      return true;
    }
    return expression.inputs()
        .filter(ExpressionNode.class::isInstance)
        .map(ExpressionNode.class::cast)
        .anyMatch(this::containsBuiltInCall);
  }

  private InstrExecPlan executionPlan(Instruction instr) {
    var plan = instrInfo(instr).executionPlan();
    return plan == null ? fail("Expected execution plan for " + instr.simpleName()) : plan;
  }

  private DirectGvecSupport singleDirectGvecRegion(InstrExecPlan executionPlan) {
    var supports = executionPlan.directGvecRegions();
    if (supports.size() != 1) {
      fail("Expected exactly one direct-gvec region but got " + supports.size());
    }
    return supports.getFirst();
  }

  private VectorTensorPlan vectorPlan(DirectGvecSupport directGvec) {
    var plan = directGvec.plan();
    return plan == null ? fail("Expected VectorTensorPlan payload") : plan;
  }

  private List<String> bindingParamNames(List<ExpressionNode> indices) {
    return indices.stream()
        .map(index -> assertInstanceOf(ParamNode.class, index).definition().simpleName())
        .toList();
  }

  private static final class CachedSpecException extends RuntimeException {
    private CachedSpecException(Exception cause) {
      super(cause);
    }
  }
}
