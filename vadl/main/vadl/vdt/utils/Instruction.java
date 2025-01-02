package vadl.vdt.utils;

public interface Instruction {

  vadl.viam.Instruction source();

  int width();

  BitPattern pattern();

}
