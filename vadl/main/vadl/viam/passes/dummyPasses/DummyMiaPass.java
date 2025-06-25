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
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Adds a hardcoded {@link vadl.viam.MicroArchitecture} definition to the VIAM specification.
 * This is deleted as soon as the frontend can handle the translation.
 *
 * <p>If the supplied configuration is a {@link RtlConfiguration} the dummy MiA is selected based
 * on the configuration. Otherwise, the default five stage pipeline is selected.
 */
public class DummyMiaPass extends Pass {

  public DummyMiaPass(GeneralConfiguration configuration) {
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

    var isa = viam.isa().orElse(null);

    if (isa == null) {
      // if there is no mip, we just do nothing
      return null;
    }

    var dummyMia = RtlConfiguration.DummyMia.five;
    if (configuration() instanceof RtlConfiguration rtlConfig) {
      dummyMia = rtlConfig.getDummyMia();
    }

    viam.add(
        switch (dummyMia) {
          case single -> SingleStageDummyMia.mia(isa);
          case three -> ThreeStageDummyMia.mia(viam, isa);
          case five -> FiveStageDummyMia.mia(viam, isa);
        }
    );

    viam.verify();

    return null;
  }

}
