package vadl.javaannotations.viam;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;


public class DoCollectData extends BugChecker implements Matcher<Tree> {
  @Override
  public boolean matches(Tree tree, VisitorState state) {
    return false;
  }
}
