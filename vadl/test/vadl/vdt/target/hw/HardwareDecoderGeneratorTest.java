package vadl.vdt.target.hw;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.DecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassManager;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtEncodingConstraintValidationPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.dump.TextGraphGenerator;

public class HardwareDecoderGeneratorTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(HardwareDecoderGeneratorTest.class);

  @Test
  void test_generate_rv32i_vdt() throws Exception {

    /* GIVEN */

    var config = new GeneralConfiguration(Path.of("build/test-output"), false);
    config.getDecoderOptions().setGenerator(DecoderOptions.Generator.REGULAR);

    var spec = runAndGetViamSpecification("sys/risc-v/rv32i.vadl");

    var manager = new PassManager();
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtLoweringPass(config));

    manager.run(spec);

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    assertNotNull(decodeTree);

    log.info("VDT:\n{}", new TextGraphGenerator(decodeTree).generate());

    /* WHEN */
    final CharSequence result = new HardwareIrregularDecoderGenerator().generate(decodeTree);

    /* THEN */
    System.out.println(result);
  }

  @Test
  void test_generate_rv32i_table() throws Exception {

    /* GIVEN */
    var config = new GeneralConfiguration(Path.of("build/test-output"), false);
    config.getDecoderOptions().setGenerator(DecoderOptions.Generator.REGULAR);

    var spec = runAndGetViamSpecification("sys/risc-v/rv32i.vadl");

    var manager = new PassManager();
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtLoweringPass(config));

    manager.run(spec);

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    assertNotNull(decodeTree);

    log.info("VDT:\n{}", new TextGraphGenerator(decodeTree).generate());

    /* WHEN */
    final CharSequence result = new HardwareRegularDecoderGenerator().generate(decodeTree);

    /* THEN */
    System.out.println(result);
  }

}
