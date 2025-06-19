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
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.VadlBuiltInStatusOnlyDispatcher;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * The {@link RemoveRegisterWritesPass} removed any register writes and reads.
 * Therefore, status builtins don't write the status registers anymore. We can try to replace
 * status builtins to the non status builtins.
 */
public class ReplaceStatusBuiltinsByNonStatusBuiltinsPass extends Pass {
  public ReplaceStatusBuiltinsByNonStatusBuiltinsPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ReplaceStatusBuiltinsByNonStatusBuiltinsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {


    return null;
  }
}

class BuiltinReplacer implements VadlBuiltInStatusOnlyDispatcher<BuiltInCall> {

  @Override
  public void handleADDS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ADD, input.arguments(), input.type()));
  }

  @Override
  public void handleADDC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ADD, input.arguments(), input.type()));
  }

  @Override
  public void handleSSATADDS(BuiltInCall input) {

  }

  @Override
  public void handleUSATADDS(BuiltInCall input) {

  }

  @Override
  public void handleSSATADDC(BuiltInCall input) {

  }

  @Override
  public void handleUSATADDC(BuiltInCall input) {

  }

  @Override
  public void handleSUBSC(BuiltInCall input) {

  }

  @Override
  public void handleSUBSB(BuiltInCall input) {

  }

  @Override
  public void handleSUBC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.SUB, input.arguments(), input.type()));
  }

  @Override
  public void handleSUBB(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.SUB, input.arguments(), input.type()));
  }

  @Override
  public void handleSSATSUBS(BuiltInCall input) {

  }

  @Override
  public void handleUSATSUBS(BuiltInCall input) {

  }

  @Override
  public void handleSSATSUBC(BuiltInCall input) {

  }

  @Override
  public void handleUSATSUBC(BuiltInCall input) {

  }

  @Override
  public void handleSSATSUBB(BuiltInCall input) {

  }

  @Override
  public void handleUSATSUBB(BuiltInCall input) {

  }

  @Override
  public void handleMULS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.MUL, input.arguments(), input.type()));
  }

  @Override
  public void handleSMULLS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.SMULL, input.arguments(), input.type()));
  }

  @Override
  public void handleUMULLS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.UMULL, input.arguments(), input.type()));
  }

  @Override
  public void handleSUMULLS(BuiltInCall input) {

  }

  @Override
  public void handleSMODS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.SMOD, input.arguments(), input.type()));
  }

  @Override
  public void handleUMODS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.UMOD, input.arguments(), input.type()));
  }

  @Override
  public void handleSDIVS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.SDIV, input.arguments(), input.type()));
  }

  @Override
  public void handleUDIVS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.UDIV, input.arguments(), input.type()));
  }

  @Override
  public void handleANDS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.AND, input.arguments(), input.type()));
  }

  @Override
  public void handleXORS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.XOR, input.arguments(), input.type()));
  }

  @Override
  public void handleORS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.OR, input.arguments(), input.type()));
  }

  @Override
  public void handleLSLS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.LSL, input.arguments(), input.type()));
  }

  @Override
  public void handleLSLC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.LSL, input.arguments(), input.type()));
  }

  @Override
  public void handleASRS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ASR, input.arguments(), input.type()));
  }

  @Override
  public void handleLSRS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.LSR, input.arguments(), input.type()));
  }

  @Override
  public void handleASRC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ASR, input.arguments(), input.type()));
  }

  @Override
  public void handleLSRC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.LSR, input.arguments(), input.type()));
  }

  @Override
  public void handleROLS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ROL, input.arguments(), input.type()));
  }

  @Override
  public void handleROLC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ROL, input.arguments(), input.type()));
  }

  @Override
  public void handleRORS(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ROR, input.arguments(), input.type()));
  }

  @Override
  public void handleRORC(BuiltInCall input) {
    input.replaceAndDelete(new BuiltInCall(BuiltInTable.ROR, input.arguments(), input.type()));
  }
}
