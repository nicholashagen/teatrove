package org.teatrove.tea.compiler;


public class ParserDefinitionException extends Exception {

    private static final long serialVersionUID = 1L;

    private Class<?> mClass;

    public ParserDefinitionException(Class<?> clazz, String message) {
        super(generateMessage(clazz, message));
        mClass = clazz;
    }

    public Class<?> getType() {
        return mClass;
    }

    private static String generateMessage(Class<?> clazz, String message) {

        StringBuilder buffer = new StringBuilder(256);
        buffer.append("invalid parser definition (class=")
              .append(clazz.getName()).append("): ").append(message);

        return buffer.toString();
    }
}
