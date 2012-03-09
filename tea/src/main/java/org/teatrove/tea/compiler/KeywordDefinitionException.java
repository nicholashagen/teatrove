package org.teatrove.tea.compiler;

import java.lang.reflect.Field;

import org.teatrove.tea.annotations.Keyword;

public class KeywordDefinitionException extends Exception {

    private static final long serialVersionUID = 1L;

    private Keyword mKeyword;
    private Field mField;

    public KeywordDefinitionException(Keyword keyword, Field field,
                                      String message) {
        super(generateMessage(keyword, field, message));
        mKeyword = keyword;
        mField = field;
    }

    public Keyword getKeyword() {
        return mKeyword;
    }

    public Field getField() {
        return mField;
    }

    private static String generateMessage(Keyword keyword, Field field,
                                          String message) {

        StringBuilder buffer = new StringBuilder(256);
        buffer.append("invalid keyword definition (token=")
              .append(keyword.value()).append(", priority=")
              .append(keyword.priority()).append(", field=")
              .append(field.getDeclaringClass().getName()).append('.')
              .append(field.getName()).append("): ").append(message);

        return buffer.toString();
    }
}
