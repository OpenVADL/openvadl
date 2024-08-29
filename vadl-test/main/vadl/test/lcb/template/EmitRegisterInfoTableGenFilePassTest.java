package vadl.test.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoCppFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitRegisterInfoTableGenFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "examples/rv3264im.vadl",
        new PassKey(EmitRegisterInfoTableGenFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitRegisterInfoTableGenFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        def X0 : Register<"X0">
        {
            let Namespace = "processorNameValue";
            let AsmName = "zero";
            let AltNames = [ "zero", "X0" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 0 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 0;
            let isArtificial = 0;
        }
                
                
        def X1 : Register<"X1">
        {
            let Namespace = "processorNameValue";
            let AsmName = "ra";
            let AltNames = [ "ra", "X1" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 1 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 1;
            let isArtificial = 0;
        }
                
                
        def X2 : Register<"X2">
        {
            let Namespace = "processorNameValue";
            let AsmName = "sp";
            let AltNames = [ "sp", "X2" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 2 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 2;
            let isArtificial = 0;
        }
                
                
        def X3 : Register<"X3">
        {
            let Namespace = "processorNameValue";
            let AsmName = "gp";
            let AltNames = [ "gp", "X3" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 3 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 3;
            let isArtificial = 0;
        }
                
                
        def X4 : Register<"X4">
        {
            let Namespace = "processorNameValue";
            let AsmName = "tp";
            let AltNames = [ "tp", "X4" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 4 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 4;
            let isArtificial = 0;
        }
                
                
        def X5 : Register<"X5">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X5" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 5 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 5;
            let isArtificial = 0;
        }
                
                
        def X6 : Register<"X6">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X6" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 6 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 6;
            let isArtificial = 0;
        }
                
                
        def X7 : Register<"X7">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X7" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 7 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 7;
            let isArtificial = 0;
        }
                
                
        def X8 : Register<"X8">
        {
            let Namespace = "processorNameValue";
            let AsmName = "fp";
            let AltNames = [ "fp", "X8" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 8 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 8;
            let isArtificial = 0;
        }
                
                
        def X9 : Register<"X9">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X9" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 9 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 9;
            let isArtificial = 0;
        }
                
                
        def X10 : Register<"X10">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X10" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 10 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 10;
            let isArtificial = 0;
        }
                
                
        def X11 : Register<"X11">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X11" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 11 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 11;
            let isArtificial = 0;
        }
                
                
        def X12 : Register<"X12">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X12" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 12 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 12;
            let isArtificial = 0;
        }
                
                
        def X13 : Register<"X13">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X13" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 13 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 13;
            let isArtificial = 0;
        }
                
                
        def X14 : Register<"X14">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X14" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 14 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 14;
            let isArtificial = 0;
        }
                
                
        def X15 : Register<"X15">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X15" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 15 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 15;
            let isArtificial = 0;
        }
                
                
        def X16 : Register<"X16">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X16" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 16 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 16;
            let isArtificial = 0;
        }
                
                
        def X17 : Register<"X17">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X17" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 17 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 17;
            let isArtificial = 0;
        }
                
                
        def X18 : Register<"X18">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X18" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 18 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 18;
            let isArtificial = 0;
        }
                
                
        def X19 : Register<"X19">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X19" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 19 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 19;
            let isArtificial = 0;
        }
                
                
        def X20 : Register<"X20">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X20" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 20 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 20;
            let isArtificial = 0;
        }
                
                
        def X21 : Register<"X21">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X21" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 21 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 21;
            let isArtificial = 0;
        }
                
                
        def X22 : Register<"X22">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X22" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 22 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 22;
            let isArtificial = 0;
        }
                
                
        def X23 : Register<"X23">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X23" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 23 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 23;
            let isArtificial = 0;
        }
                
                
        def X24 : Register<"X24">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X24" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 24 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 24;
            let isArtificial = 0;
        }
                
                
        def X25 : Register<"X25">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X25" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 25 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 25;
            let isArtificial = 0;
        }
                
                
        def X26 : Register<"X26">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X26" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 26 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 26;
            let isArtificial = 0;
        }
                
                
        def X27 : Register<"X27">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X27" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 27 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 27;
            let isArtificial = 0;
        }
                
                
        def X28 : Register<"X28">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X28" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 28 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 28;
            let isArtificial = 0;
        }
                
                
        def X29 : Register<"X29">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X29" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 29 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 29;
            let isArtificial = 0;
        }
                
                
        def X30 : Register<"X30">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X30" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 30 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 30;
            let isArtificial = 0;
        }
                
                
        def X31 : Register<"X31">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X";
            let AltNames = [ "X31" ];
            let Aliases = [  ];
            let SubRegs = [  ];
            let SubRegIndices = [  ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 31 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = "";
            let HWEncoding{4-0} = 31;
            let isArtificial = 0;
        }
        """.trim().lines(), output);
  }
}
