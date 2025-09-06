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

package vadl.viam.passes.dummyPasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.passes.AbstractRtlPass;
import vadl.types.BuiltInTable;
import vadl.types.MicroArchitectureType;
import vadl.types.Type;
import vadl.viam.Identifier;
import vadl.viam.Logic;
import vadl.viam.Memory;
import vadl.viam.MicroArchitecture;
import vadl.viam.Processor;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Adds a hardcoded {@link vadl.viam.MicroArchitecture} definition to the VIAM specification.
 * This is deleted as soon as the frontend can handle the translation.
 *
 * <pre>
 * [forwarding]
 * logic bypass
 *
 * [branch predictor]
 * logic predict
 *
 * stage FETCH -> ( fr : FetchResult ) =
 * {
 *   fr := fetchNext
 * }
 *
 * stage DECODE -> ( ir : Instruction ) =
 * {
 *   let instr = decode( FETCH.fr ) in
 *   {
 *     instr.address( @X )
 *     instr.readOrForward( @X, @bypass )
 *     ir := instr
 *   }
 * }
 *
 * stage EXECUTE -> ( ir : Instruction ) =
 * {
 *   let instr = DECODE.ir in
 *   {
 *     instr.read( @PC )
 *     instr.compute
 *     instr.verify
 *     instr.write( @PC )
 *     instr.results( @X, @bypass )
 *     ir := instr
 *   }
 * }
 *
 * stage MEMORY -> ( ir : Instruction ) =
 * {
 *   let instr = EXECUTE.ir in
 *   {
 *     instr.write( @MEM )
 *     instr.read( @MEM )
 *     ir := instr
 *   }
 * }
 *
 * stage WRITE_BACK =
 * {
 *   let instr = MEMORY.ir in
 *   {
 *     instr.write( @X )
 *   }
 * }
 * </pre>
 */
public class DummyMiaPass extends AbstractRtlPass {

  public DummyMiaPass(RtlConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Dummy Micro Architecture");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    if (viam.mia().isPresent()) {
      return null;
    }

    var mip = viam.processor().orElse(null);

    if (mip == null) {
      // if there is no mip, we just do nothing
      return null;
    }

    viam.add(
        switch (configuration().getDummyMia()) {
          case single -> SingleStageDummyMia.mia(mip);
          case three -> ThreeStageDummyMia.mia(viam, mip);
          case five -> FiveStageDummyMia.mia(viam, mip);
        }
    );

    viam.verify();

    return null;
  }

}
