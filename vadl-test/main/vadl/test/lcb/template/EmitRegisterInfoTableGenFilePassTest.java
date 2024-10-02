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
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
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
        def PC : Register<"PC">
        {
            let Namespace = "processorNameValue";
            let AsmName = "PC";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 0 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
           \s
            let HWEncoding = 0;
           \s
            let isArtificial = 0;
        }
        def X0 : Register<"X0">
        {
            let Namespace = "processorNameValue";
            let AsmName = "zero";
            let AltNames = [ "zero", "X0"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 1 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 0;
           \s
           \s
            let isArtificial = 0;
        }
        def X1 : Register<"X1">
        {
            let Namespace = "processorNameValue";
            let AsmName = "ra";
            let AltNames = [ "ra", "X1"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 2 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 1;
           \s
           \s
            let isArtificial = 0;
        }
        def X2 : Register<"X2">
        {
            let Namespace = "processorNameValue";
            let AsmName = "sp";
            let AltNames = [ "sp", "X2"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 3 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 2;
           \s
           \s
            let isArtificial = 0;
        }
        def X3 : Register<"X3">
        {
            let Namespace = "processorNameValue";
            let AsmName = "gp";
            let AltNames = [ "gp", "X3"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 4 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 3;
           \s
           \s
            let isArtificial = 0;
        }
        def X4 : Register<"X4">
        {
            let Namespace = "processorNameValue";
            let AsmName = "tp";
            let AltNames = [ "tp", "X4"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 5 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 4;
           \s
           \s
            let isArtificial = 0;
        }
        def X5 : Register<"X5">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X5";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 6 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 5;
           \s
           \s
            let isArtificial = 0;
        }
        def X6 : Register<"X6">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X6";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 7 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 6;
           \s
           \s
            let isArtificial = 0;
        }
        def X7 : Register<"X7">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X7";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 8 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 7;
           \s
           \s
            let isArtificial = 0;
        }
        def X8 : Register<"X8">
        {
            let Namespace = "processorNameValue";
            let AsmName = "fp";
            let AltNames = [ "fp", "X8"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 9 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 8;
           \s
           \s
            let isArtificial = 0;
        }
        def X9 : Register<"X9">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X9";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 10 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 9;
           \s
           \s
            let isArtificial = 0;
        }
        def X10 : Register<"X10">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X10";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 11 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 10;
           \s
           \s
            let isArtificial = 0;
        }
        def X11 : Register<"X11">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X11";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 12 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 11;
           \s
           \s
            let isArtificial = 0;
        }
        def X12 : Register<"X12">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X12";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 13 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 12;
           \s
           \s
            let isArtificial = 0;
        }
        def X13 : Register<"X13">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X13";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 14 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 13;
           \s
           \s
            let isArtificial = 0;
        }
        def X14 : Register<"X14">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X14";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 15 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 14;
           \s
           \s
            let isArtificial = 0;
        }
        def X15 : Register<"X15">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X15";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 16 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 15;
           \s
           \s
            let isArtificial = 0;
        }
        def X16 : Register<"X16">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X16";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 17 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 16;
           \s
           \s
            let isArtificial = 0;
        }
        def X17 : Register<"X17">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X17";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 18 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 17;
           \s
           \s
            let isArtificial = 0;
        }
        def X18 : Register<"X18">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X18";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 19 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 18;
           \s
           \s
            let isArtificial = 0;
        }
        def X19 : Register<"X19">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X19";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 20 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 19;
           \s
           \s
            let isArtificial = 0;
        }
        def X20 : Register<"X20">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X20";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 21 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 20;
           \s
           \s
            let isArtificial = 0;
        }
        def X21 : Register<"X21">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X21";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 22 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 21;
           \s
           \s
            let isArtificial = 0;
        }
        def X22 : Register<"X22">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X22";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 23 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 22;
           \s
           \s
            let isArtificial = 0;
        }
        def X23 : Register<"X23">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X23";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 24 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 23;
           \s
           \s
            let isArtificial = 0;
        }
        def X24 : Register<"X24">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X24";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 25 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 24;
           \s
           \s
            let isArtificial = 0;
        }
        def X25 : Register<"X25">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X25";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 26 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 25;
           \s
           \s
            let isArtificial = 0;
        }
        def X26 : Register<"X26">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X26";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 27 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 26;
           \s
           \s
            let isArtificial = 0;
        }
        def X27 : Register<"X27">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X27";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 28 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 27;
           \s
           \s
            let isArtificial = 0;
        }
        def X28 : Register<"X28">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X28";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 29 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 28;
           \s
           \s
            let isArtificial = 0;
        }
        def X29 : Register<"X29">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X29";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 30 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 29;
           \s
           \s
            let isArtificial = 0;
        }
        def X30 : Register<"X30">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X30";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 31 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 30;
           \s
           \s
            let isArtificial = 0;
        }
        def X31 : Register<"X31">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X31";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 32 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 31;
           \s
           \s
            let isArtificial = 0;
        }
        def X0 : Register<"X0">
        {
            let Namespace = "processorNameValue";
            let AsmName = "zero";
            let AltNames = [ "zero", "X0"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 33 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 0;
           \s
           \s
            let isArtificial = 0;
        }
        def X1 : Register<"X1">
        {
            let Namespace = "processorNameValue";
            let AsmName = "ra";
            let AltNames = [ "ra", "X1"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 34 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 1;
           \s
           \s
            let isArtificial = 0;
        }
        def X2 : Register<"X2">
        {
            let Namespace = "processorNameValue";
            let AsmName = "sp";
            let AltNames = [ "sp", "X2"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 35 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 2;
           \s
           \s
            let isArtificial = 0;
        }
        def X3 : Register<"X3">
        {
            let Namespace = "processorNameValue";
            let AsmName = "gp";
            let AltNames = [ "gp", "X3"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 36 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 3;
           \s
           \s
            let isArtificial = 0;
        }
        def X4 : Register<"X4">
        {
            let Namespace = "processorNameValue";
            let AsmName = "tp";
            let AltNames = [ "tp", "X4"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 37 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 4;
           \s
           \s
            let isArtificial = 0;
        }
        def X5 : Register<"X5">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X5";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 38 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 5;
           \s
           \s
            let isArtificial = 0;
        }
        def X6 : Register<"X6">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X6";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 39 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 6;
           \s
           \s
            let isArtificial = 0;
        }
        def X7 : Register<"X7">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X7";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 40 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 7;
           \s
           \s
            let isArtificial = 0;
        }
        def X8 : Register<"X8">
        {
            let Namespace = "processorNameValue";
            let AsmName = "fp";
            let AltNames = [ "fp", "X8"  ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 41 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 8;
           \s
           \s
            let isArtificial = 0;
        }
        def X9 : Register<"X9">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X9";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 42 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 9;
           \s
           \s
            let isArtificial = 0;
        }
        def X10 : Register<"X10">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X10";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 43 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 10;
           \s
           \s
            let isArtificial = 0;
        }
        def X11 : Register<"X11">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X11";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 44 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 11;
           \s
           \s
            let isArtificial = 0;
        }
        def X12 : Register<"X12">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X12";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 45 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 12;
           \s
           \s
            let isArtificial = 0;
        }
        def X13 : Register<"X13">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X13";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 46 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 13;
           \s
           \s
            let isArtificial = 0;
        }
        def X14 : Register<"X14">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X14";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 47 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 14;
           \s
           \s
            let isArtificial = 0;
        }
        def X15 : Register<"X15">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X15";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 48 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 15;
           \s
           \s
            let isArtificial = 0;
        }
        def X16 : Register<"X16">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X16";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 49 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 16;
           \s
           \s
            let isArtificial = 0;
        }
        def X17 : Register<"X17">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X17";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 50 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 17;
           \s
           \s
            let isArtificial = 0;
        }
        def X18 : Register<"X18">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X18";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 51 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 18;
           \s
           \s
            let isArtificial = 0;
        }
        def X19 : Register<"X19">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X19";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 52 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 19;
           \s
           \s
            let isArtificial = 0;
        }
        def X20 : Register<"X20">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X20";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 53 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 20;
           \s
           \s
            let isArtificial = 0;
        }
        def X21 : Register<"X21">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X21";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 54 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 21;
           \s
           \s
            let isArtificial = 0;
        }
        def X22 : Register<"X22">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X22";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 55 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 22;
           \s
           \s
            let isArtificial = 0;
        }
        def X23 : Register<"X23">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X23";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 56 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 23;
           \s
           \s
            let isArtificial = 0;
        }
        def X24 : Register<"X24">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X24";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 57 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 24;
           \s
           \s
            let isArtificial = 0;
        }
        def X25 : Register<"X25">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X25";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 58 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 25;
           \s
           \s
            let isArtificial = 0;
        }
        def X26 : Register<"X26">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X26";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 59 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 26;
           \s
           \s
            let isArtificial = 0;
        }
        def X27 : Register<"X27">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X27";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 60 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 27;
           \s
           \s
            let isArtificial = 0;
        }
        def X28 : Register<"X28">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X28";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 61 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 28;
           \s
           \s
            let isArtificial = 0;
        }
        def X29 : Register<"X29">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X29";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 62 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 29;
           \s
           \s
            let isArtificial = 0;
        }
        def X30 : Register<"X30">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X30";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 63 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 30;
           \s
           \s
            let isArtificial = 0;
        }
        def X31 : Register<"X31">
        {
            let Namespace = "processorNameValue";
            let AsmName = "X31";
            let AltNames = [   ];
            let Aliases = [ ];
            let SubRegs = [ ];
            let SubRegIndices = [ ];
            let RegAltNameIndices = [];
            let DwarfNumbers = [ 64 ];
            list<int> CostPerUse = [0];
            let CoveredBySubRegs = 0;
           \s
            let HWEncoding{4-0} = 31;
           \s
           \s
            let isArtificial = 0;
        }
                
                
                
        def PCClass : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add PC )
        >;
        def X : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X0, X1, X2, X3, X4, X5, X6, X7, X8, X9, X10, X11, X12, X13, X14, X15, X16, X17, X18, X19, X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X30, X31 )
        >;
        def X0Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X0 )
        >;
        def X1Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X1 )
        >;
        def X2Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X2 )
        >;
        def X3Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X3 )
        >;
        def X4Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X4 )
        >;
        def X5Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X5 )
        >;
        def X6Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X6 )
        >;
        def X7Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X7 )
        >;
        def X8Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X8 )
        >;
        def X9Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X9 )
        >;
        def X10Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X10 )
        >;
        def X11Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X11 )
        >;
        def X12Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X12 )
        >;
        def X13Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X13 )
        >;
        def X14Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X14 )
        >;
        def X15Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X15 )
        >;
        def X16Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X16 )
        >;
        def X17Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X17 )
        >;
        def X18Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X18 )
        >;
        def X19Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X19 )
        >;
        def X20Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X20 )
        >;
        def X21Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X21 )
        >;
        def X22Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X22 )
        >;
        def X23Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X23 )
        >;
        def X24Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X24 )
        >;
        def X25Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X25 )
        >;
        def X26Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X26 )
        >;
        def X27Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X27 )
        >;
        def X28Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X28 )
        >;
        def X29Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X29 )
        >;
        def X30Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X30 )
        >;
        def X31Class : RegisterClass
        < /* namespace = */ "processorNameValue"
        , /* regTypes  = */  [  i64 ];
        , /* alignment = */ 32
        , /* regList   = */
          ( add X31 )
        >;
        """.trim().lines(), output);
  }
}
