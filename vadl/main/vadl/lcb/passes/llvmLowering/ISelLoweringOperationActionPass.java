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

package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.llvmLowering.domain.MachineValueType;
import vadl.lcb.passes.llvmLowering.domain.SelectionDagToISDNameMapper;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBSwapSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmRotlSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmRotrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShlPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSraPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSrlPartsSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUMulLoHiSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.DependencyNode;

/**
 * An {@link InstructionSetArchitecture} will not provide all {@link Instruction} which is
 * required for LLVM. LLVM supports default implementations to replace selection dag nodes by
 * other nodes. This passes computes the nodes which need to be expanded.
 */
public class ISelLoweringOperationActionPass extends Pass {
  public static final Set<Class<?>> expandableSelectionDagNodes = new HashSet<>();

  static {
    expandableSelectionDagNodes.add(LlvmRotlSD.class);
    expandableSelectionDagNodes.add(LlvmRotrSD.class);
    expandableSelectionDagNodes.add(LlvmShlPartsSD.class);
    expandableSelectionDagNodes.add(LlvmSrlPartsSD.class);
    expandableSelectionDagNodes.add(LlvmSraPartsSD.class);
    expandableSelectionDagNodes.add(LlvmBSwapSD.class);
    expandableSelectionDagNodes.add(LlvmUMulLoHiSD.class);
  }

  public ISelLoweringOperationActionPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ISelLoweringOperationActionPass");
  }

  record Coverage(Instruction instruction, DependencyNode node) {

  }

  /**
   * Container for selection dag nodes which need to be expanded.
   */
  public record NoCoverage(Class<DependencyNode> node,
                           String llvmDagName, /* ISD Name in LLVM */
                           MachineValueType mvt) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("isdName", llvmDagName,
          "mvt", mvt);
    }
  }

  /**
   * Output record for this pass. It contains the nodes which should be covered by the patterns.
   * And also the classes of the nodes which need to be expanded.
   */
  public record CoverageSummary(List<Coverage> coveredSelectionDagNodes,
                                List<NoCoverage> notCoveredSelectionDagNodes) {
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = viam.abi().orElseThrow();
    var tableGenMachineInstructions = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

    // First, we collect the root nodes of the patterns.
    // Then we check which selection dag nodes need to be expanded.
    var covered = coverage(tableGenMachineInstructions);
    var notCovered = noCoverage(abi, covered);

    return new CoverageSummary(covered, notCovered);
  }

  private List<NoCoverage> noCoverage(Abi abi, List<Coverage> covered) {
    var result = new ArrayList<NoCoverage>();
    var mapped =
        covered.stream().map(x -> x.node.getClass()).collect(Collectors.toSet());

    for (var needle : expandableSelectionDagNodes) {
      if (!mapped.contains(needle)) {
        result.add(new NoCoverage((Class<DependencyNode>) needle, SelectionDagToISDNameMapper.map(
            (Class<LlvmNodeLowerable>) needle),
            MachineValueType.from(abi.stackPointer().registerFile().resultType())));
      }
    }

    return result;
  }

  private List<Coverage> coverage(
      List<TableGenMachineInstruction> tableGenMachineInstructions) {
    return tableGenMachineInstructions
        .stream()
        .flatMap(
            tableGenMachineInstruction -> tableGenMachineInstruction.llvmLoweringRecord().patterns()
                .stream().flatMap(
                    coverageCandidate(tableGenMachineInstruction)))
        .toList();
  }

  private static Function<TableGenPattern, Stream<? extends Coverage>> coverageCandidate(
      TableGenMachineInstruction tableGenMachineInstruction) {
    return tableGenPattern -> tableGenPattern.selector().getDataflowRoots().stream()
        .map(rootNode -> new Coverage(tableGenMachineInstruction.instruction(),
            rootNode));
  }
}
