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

package vadl.iss.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.target.EmitIssInsnTransCIncPass;
import vadl.pass.PassOrders;
import vadl.template.AbstractMultiTemplateRenderingPass;
import vadl.utils.VadlFileUtils;

/**
 * Tests if the generated trans_instr() function (c code) contains the right helper calls
 * for various combinations of direct/indirect jumps, tb-state register writes (static, dynamic),
 * conditional and unconditional.
 *
 * @see vadl.iss.passes.tcg.lowering.TcgOpLoweringPass
 */
public class IssTcgIsJmpTest extends AbstractTest {

  private static final Path WORKING_DIR = Path.of("test/resources/testSource/iss/is-jmp");

  private static final String EPILOG = "ctx->base.is_jmp = DISAS_NORETURN;";
  private static final String EPILOG_HLP = "is_jmp_set";
  private static final String PROLOG = "is_jmp_create_state";
  private static final String DJ = "is_jmp_direct_jmp";
  private static final String IJ = "tcg_gen_lookup_and_goto_ptr";
  private static final String IJ_HLP = "is_jmp_indirect_jmp";
  private static final String SW = "is_jmp_static_tb_state_write";
  private static final String DW = "is_jmp_dynamic_tb_state_write";

  private static final Feature FDJ = Feature.DIRECT_JMP;
  private static final Feature FIJ = Feature.INDIRECT_JMP;
  private static final Feature FSW = Feature.STATIC_WRITE;
  private static final Feature FDW = Feature.DYNAMIC_WRITE;
  private static final Feature FUJ = Feature.UNCONDITIONAL_JMP;

  private String specTemplate;

  private enum Feature {
    DIRECT_JMP,
    INDIRECT_JMP,
    STATIC_WRITE,
    DYNAMIC_WRITE,
    UNCONDITIONAL_JMP,

    DIRECT_AND_INDIRECT_JMP,
  }

  @TestFactory
  Stream<DynamicTest> tests() throws IOException {
    specTemplate = Files.readString(WORKING_DIR.resolve("spec-template.vadl"));
    return Stream.of(
        createTest(List.of(),                        List.of()),
        createTest(List.of(FDJ),                     List.of(EPILOG_HLP, PROLOG, DJ)),
        createTest(List.of(FIJ),                     List.of(EPILOG_HLP, PROLOG,     IJ_HLP)),
        createTest(List.of(FDJ, FIJ),                List.of(EPILOG_HLP, PROLOG, DJ, IJ_HLP)),
        createTest(List.of(FSW),                     List.of(EPILOG_HLP, PROLOG,             SW)),
        createTest(List.of(FDJ, FSW),                List.of(EPILOG_HLP, PROLOG, DJ,         SW)),
        createTest(List.of(FIJ, FSW),                List.of(EPILOG_HLP, PROLOG,     IJ_HLP, SW)),
        createTest(List.of(FDJ, FIJ, FSW),           List.of(EPILOG_HLP, PROLOG, DJ, IJ_HLP, SW)),
        createTest(List.of(FDW),                     List.of(EPILOG_HLP, PROLOG,                 DW)),
        createTest(List.of(FDJ, FDW),                List.of(EPILOG_HLP, PROLOG, DJ,             DW)),
        createTest(List.of(FIJ, FDW),                List.of(EPILOG_HLP, PROLOG,     IJ_HLP,     DW)),
        createTest(List.of(FDJ, FIJ, FDW),           List.of(EPILOG_HLP, PROLOG, DJ, IJ_HLP,     DW)),
        createTest(List.of(FSW, FDW),                List.of(EPILOG_HLP, PROLOG,             SW, DW)),
        createTest(List.of(FDJ, FSW, FDW),           List.of(EPILOG_HLP, PROLOG, DJ,         SW, DW)),
        createTest(List.of(FIJ, FSW, FDW),           List.of(EPILOG_HLP, PROLOG,     IJ_HLP, SW, DW)),
        createTest(List.of(FDJ, FIJ, FSW, FDW),      List.of(EPILOG_HLP, PROLOG, DJ, IJ_HLP, SW, DW)),
        createTest(List.of(FUJ),                     List.of(EPILOG)),
        createTest(List.of(FDJ, FUJ),                List.of(EPILOG,     PROLOG, DJ)),
        createTest(List.of(FIJ, FUJ),                List.of(EPILOG,                 IJ)),
        createTest(List.of(FDJ, FIJ, FUJ),           List.of(EPILOG,     PROLOG, DJ, IJ_HLP)),
        createTest(List.of(FSW, FUJ),                List.of(EPILOG)),
        createTest(List.of(FDJ, FSW, FUJ),           List.of(EPILOG,     PROLOG, DJ,         SW)),
        createTest(List.of(FIJ, FSW, FUJ),           List.of(EPILOG,                 IJ)),
        createTest(List.of(FDJ, FIJ, FSW, FUJ),      List.of(EPILOG,     PROLOG, DJ, IJ_HLP, SW)),
        createTest(List.of(FDW, FUJ),                List.of(EPILOG)),
        createTest(List.of(FDJ, FDW, FUJ),           List.of(EPILOG,     PROLOG, DJ,             DW)),
        createTest(List.of(FIJ, FDW, FUJ),           List.of(EPILOG,                 IJ)),
        createTest(List.of(FDJ, FIJ, FDW, FUJ),      List.of(EPILOG,     PROLOG, DJ, IJ_HLP,     DW)),
        createTest(List.of(FSW, FDW, FUJ),           List.of(EPILOG)),
        createTest(List.of(FDJ, FSW, FDW, FUJ),      List.of(EPILOG,     PROLOG, DJ,         SW, DW)),
        createTest(List.of(FIJ, FSW, FDW, FUJ),      List.of(EPILOG,                 IJ)),
        createTest(List.of(FDJ, FIJ, FSW, FDW, FUJ), List.of(EPILOG,     PROLOG, DJ, IJ_HLP, SW, DW))
    );
  }

  private DynamicTest createTest(List<Feature> features, List<String> present) {
    var name = createName(features);
    return DynamicTest.dynamicTest(name, () -> {
      var spec = createSpec(features);
      System.out.println(spec);
      var specFile = VadlFileUtils.writeToTempFile(spec, name, ".vadl");
      var config = new IssConfiguration(getConfiguration(false));
      config.setSkipClangFormatting(true);
      var passResults = setupPassManagerAndRunSpec(
          specFile.getPath(),
          PassOrders.iss(config).untilFirst(EmitIssInsnTransCIncPass.class)
      ).passManager().getPassResults();
      var insnTransFile = ((AbstractMultiTemplateRenderingPass.Result) passResults
          .lastResultOf(EmitIssInsnTransCIncPass.class)).emittedFiles().getFirst();
      var insnTransStr = FileUtils.readFileToString(insnTransFile.toFile(), "UTF-8");
      assertAll(Stream.concat(
          present.stream().map(str -> () -> {
            System.out.println("Present: " + str);
            assertThat(insnTransStr).contains(str);
          }),
          inv(present).stream().map(str -> () -> {
            System.out.println("Absent: " + str);
            assertThat(insnTransStr).doesNotContain(str);
          })
      ));
    });
  }

  private List<String> all() {
    return List.of(EPILOG, EPILOG_HLP, PROLOG, DJ, IJ, IJ_HLP, SW, DW);
  }

  private List<String> inv(List<String> l) {
    var result = new ArrayList<>(all());
    result.removeAll(l);
    return result;
  }

  private String createSpec(List<Feature> features) {
    var djAndIj = List.of(Feature.DIRECT_JMP, Feature.INDIRECT_JMP);
    if (features.containsAll(djAndIj)) {
      features = new ArrayList<>(features);
      features.removeAll(djAndIj);
      features.add(Feature.DIRECT_AND_INDIRECT_JMP);
    }
    return String.format(
        specTemplate,
        features.stream().map(this::createFeature).collect(Collectors.joining(" "))
    );
  }

  private String createFeature(Feature feature) {
    return switch (feature) {
      case DIRECT_JMP ->    "if R2 = 0 then { PC := 0 }";
      case INDIRECT_JMP ->  "if R2 = 0 then { PC := R1 }";
      case STATIC_WRITE ->  "if R2 = 0 then { ESR1 := 0 }";
      case DYNAMIC_WRITE -> "if R2 = 0 then { ESR2 := R1 as Bits<16> }";
      case UNCONDITIONAL_JMP -> "raise E";
      // must be in if-else, not two ifs, because PC may only be written once
      case DIRECT_AND_INDIRECT_JMP -> "if R2 = 0 then { PC := 0 } else { PC := R1 }";
    };
  }

  private String createName(List<Feature> features) {
    return "is_jmp_" + features.stream().map(f -> switch (f) {
      case DIRECT_JMP -> "dj";
      case INDIRECT_JMP -> "ij";
      case STATIC_WRITE -> "sw";
      case DYNAMIC_WRITE -> "dw";
      case UNCONDITIONAL_JMP -> "uj";
      case DIRECT_AND_INDIRECT_JMP -> null;
    }).collect(Collectors.joining("_"));
  }
}
