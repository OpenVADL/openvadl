package vadl.javaannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.SOURCE)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface DispatchFor {
  Class<?> value();
}
