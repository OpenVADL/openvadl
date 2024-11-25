package vadl.javaannotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Handler {
}
