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

package vadl.configuration;

import vadl.gcb.valuetypes.ProcessorName;

/**
 * This record defines some lcb specific LCB configuration.
 */
public class LcbConfiguration extends GcbConfiguration {
  private final ProcessorName processorName;

  public LcbConfiguration(GeneralConfiguration gcbConfiguration, ProcessorName processorName) {
    super(gcbConfiguration);
    this.processorName = processorName;
  }

  public static LcbConfiguration from(GcbConfiguration gcbConfiguration,
                                      ProcessorName processorName) {
    return new LcbConfiguration(gcbConfiguration, processorName);
  }

  public ProcessorName processorName() {
    return processorName;
  }
}
