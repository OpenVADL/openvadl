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

package vadl.pass;


/**
 * A {@link Pass} represents one execution in a {@code pipeline}.
 * The consumer adds a {@link PassStep} to the {@link PassManager} which indicates to the
 * manager to run this step.
 *
 * @param key is used to identify the {@link Pass} because it is possible to schedule the
 *            same {@link Pass} multiple times. This key is also used to lookup the result
 *            of the {@link Pass}.
 */
public record PassStep(PassKey key, Pass pass) {

}
