package org.teatrove.tea.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Token {
    String value();
    int priority() default -1;
    TokenType type() default TokenType.TOKEN;
    Class<? extends TokenCallback> callback() default TokenCallback.class;
}
