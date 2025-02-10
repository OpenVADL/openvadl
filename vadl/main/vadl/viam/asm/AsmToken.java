package vadl.viam.asm;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents a token of the asm parser.
 */
public class AsmToken {
  String ruleName;
  @Nullable
  String stringLiteral;

  public AsmToken(String ruleName, @Nullable String stringLiteral) {
    this.ruleName = ruleName;
    this.stringLiteral = stringLiteral;
  }

  public String getRuleName() {
    return ruleName;
  }

  @Nullable
  public String getStringLiteral() {
    return stringLiteral;
  }

  @Override
  public String toString() {
    if (stringLiteral != null) {
      return '"' + stringLiteral + '"';
    }
    return ruleName;
  }

  /**
   * An AsmToken with ruleName=IDENTIFIER and stringLiteral=null
   * is equal to an AsmToken with ruleName=IDENTIFIER and stringLiteral="something"
   * since the parser cannot decide which alternative to choose.
   *
   * @param o the other AsmToken
   * @return whether the two AsmTokens are equal from the AsmParsers viewpoint
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmToken that = (AsmToken) o;

    if (ruleName.equals(that.ruleName)) {
      if (stringLiteral == null || that.stringLiteral == null) {
        return true;
      }
      return stringLiteral.equals(that.stringLiteral);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleName);
  }
}
