package vadl.utils;

import vadl.types.BuiltInTable;

/**
 * A dispatcher that handles all {@code VADL::*} built-ins.
 */
public interface VadlBuiltInDispatcher<T> extends VadlBuiltInNoStatusDispatcher<T> {

  @Override
  default boolean dispatch(T input, BuiltInTable.BuiltIn builtIn) {
    if (VadlBuiltInNoStatusDispatcher.super.dispatch(input, builtIn)) {
      return true;
    } else if (builtIn == BuiltInTable.ADDS) {
      handleADDS(input);
    } else if (builtIn == BuiltInTable.ADDC) {
      handleADDC(input);
    } else if (builtIn == BuiltInTable.SSATADDS) {
      handleSSATADDS(input);
    } else if (builtIn == BuiltInTable.USATADDS) {
      handleUSATADDS(input);
    } else if (builtIn == BuiltInTable.SSATADDC) {
      handleSSATADDC(input);
    } else if (builtIn == BuiltInTable.USATADDC) {
      handleUSATADDC(input);
    } else if (builtIn == BuiltInTable.SUBSC) {
      handleSUBSC(input);
    } else if (builtIn == BuiltInTable.SUBSB) {
      handleSUBSB(input);
    } else if (builtIn == BuiltInTable.SUBC) {
      handleSUBC(input);
    } else if (builtIn == BuiltInTable.SUBB) {
      handleSUBB(input);
    } else if (builtIn == BuiltInTable.SSATSUBS) {
      handleSSATSUBS(input);
    } else if (builtIn == BuiltInTable.USATSUBS) {
      handleUSATSUBS(input);
    } else if (builtIn == BuiltInTable.SSATSUBC) {
      handleSSATSUBC(input);
    } else if (builtIn == BuiltInTable.USATSUBC) {
      handleUSATSUBC(input);
    } else if (builtIn == BuiltInTable.SSATSUBB) {
      handleSSATSUBB(input);
    } else if (builtIn == BuiltInTable.USATSUBB) {
      handleUSATSUBB(input);
    } else if (builtIn == BuiltInTable.MULS) {
      handleMULS(input);
    } else if (builtIn == BuiltInTable.SMULLS) {
      handleSMULLS(input);
    } else if (builtIn == BuiltInTable.UMULLS) {
      handleUMULLS(input);
    } else if (builtIn == BuiltInTable.SUMULLS) {
      handleSUMULLS(input);
    } else if (builtIn == BuiltInTable.SMODS) {
      handleSMODS(input);
    } else if (builtIn == BuiltInTable.UMODS) {
      handleUMODS(input);
    } else if (builtIn == BuiltInTable.SDIVS) {
      handleSDIVS(input);
    } else if (builtIn == BuiltInTable.UDIVS) {
      handleUDIVS(input);
    } else if (builtIn == BuiltInTable.ANDS) {
      handleANDS(input);
    } else if (builtIn == BuiltInTable.XORS) {
      handleXORS(input);
    } else if (builtIn == BuiltInTable.ORS) {
      handleORS(input);
    } else if (builtIn == BuiltInTable.LSLS) {
      handleLSLS(input);
    } else if (builtIn == BuiltInTable.LSLC) {
      handleLSLC(input);
    } else if (builtIn == BuiltInTable.ASRS) {
      handleASRS(input);
    } else if (builtIn == BuiltInTable.LSRS) {
      handleLSRS(input);
    } else if (builtIn == BuiltInTable.ASRC) {
      handleASRC(input);
    } else if (builtIn == BuiltInTable.LSRC) {
      handleLSRC(input);
    } else if (builtIn == BuiltInTable.ROLS) {
      handleROLS(input);
    } else if (builtIn == BuiltInTable.ROLC) {
      handleROLC(input);
    } else if (builtIn == BuiltInTable.RORS) {
      handleRORS(input);
    } else if (builtIn == BuiltInTable.RORC) {
      handleRORC(input);
    } else {
      return false;
    }
    return true;
  }

  void handleADDS(T input);

  void handleADDC(T input);

  void handleSSATADDS(T input);

  void handleUSATADDS(T input);

  void handleSSATADDC(T input);

  void handleUSATADDC(T input);

  void handleSUBSC(T input);

  void handleSUBSB(T input);

  void handleSUBC(T input);

  void handleSUBB(T input);

  void handleSSATSUBS(T input);

  void handleUSATSUBS(T input);

  void handleSSATSUBC(T input);

  void handleUSATSUBC(T input);

  void handleSSATSUBB(T input);

  void handleUSATSUBB(T input);

  void handleMULS(T input);

  void handleSMULLS(T input);

  void handleUMULLS(T input);

  void handleSUMULLS(T input);

  void handleSMODS(T input);

  void handleUMODS(T input);

  void handleSDIVS(T input);

  void handleUDIVS(T input);

  void handleANDS(T input);

  void handleXORS(T input);

  void handleORS(T input);

  void handleLSLS(T input);

  void handleLSLC(T input);

  void handleASRS(T input);

  void handleLSRS(T input);

  void handleASRC(T input);

  void handleLSRC(T input);

  void handleROLS(T input);

  void handleROLC(T input);

  void handleRORS(T input);

  void handleRORC(T input);


}
