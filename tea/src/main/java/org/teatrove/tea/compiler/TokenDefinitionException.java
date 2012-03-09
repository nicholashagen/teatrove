package org.teatrove.tea.compiler;

import java.lang.reflect.Field;

import org.teatrove.tea.annotations.Token;

public class TokenDefinitionException extends Exception {

    private static final long serialVersionUID = 1L;

    private Token mToken;
    private Field mField;

    public TokenDefinitionException(Token token, Field field, String message) {
        super(generateMessage(token, field, message));
        mToken = token;
        mField = field;
    }

    public Token getToken() {
        return mToken;
    }

    public Field getField() {
        return mField;
    }

    private static String generateMessage(Token token, Field field,
                                          String message) {

        StringBuilder buffer = new StringBuilder(256);
        buffer.append("invalid token definition (token=")
              .append(token.value()).append(", priority=")
              .append(token.priority()).append(", callback=")
              .append(token.callback().getName()).append(", field=")
              .append(field.getDeclaringClass().getName()).append('.')
              .append(field.getName()).append("): ").append(message);

        return buffer.toString();
    }
}
