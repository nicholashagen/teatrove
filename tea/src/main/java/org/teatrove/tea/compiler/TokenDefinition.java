package org.teatrove.tea.compiler;

import org.teatrove.tea.annotations.TokenCallback;

public class TokenDefinition implements Comparable<TokenDefinition> {

    private int id;
    private String name;
    private String token;
    private int priority;
    private TokenCallback callback;
    private Class<? extends TokenCallback> callbackClass;

    public TokenDefinition(int id, String name, String token, int priority) {
        this.id = id;
        this.name = name;
        this.token = token;
        this.priority = priority;
        this.callbackClass = TokenCallback.class;
    }

    public TokenDefinition(int id, String name,
                           Class<? extends TokenCallback> callbackClass,
                           int priority) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.callbackClass = callbackClass;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getToken() {
        return this.token;
    }

    public int getPriority() {
        return this.priority;
    }

    public TokenCallback getCallback() {
        if (TokenCallback.class.equals(this.callbackClass)) { return null; }

        if (this.callback == null) {
            try { this.callback = this.callbackClass.newInstance(); }
            catch (Exception e) {
                throw new IllegalStateException("invalid callback", e);
            }
        }

        return this.callback;
    }

    public Class<? extends TokenCallback> getCallbackClass() {
        return this.callbackClass;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) { return true; }
        else if (!(object instanceof TokenDefinition)) { return false; }
        return this.id == ((TokenDefinition) object).id;
    }

    @Override
    public String toString() {
        return "Token[" + this.name + ':' + this.id + ']';
    }

    @Override
    public int compareTo(TokenDefinition other) {
        if (other == this) { return 0; }

        if (this.priority >= 0) {
            if (other.priority < 0) { return 1; }
            else if (this.priority < other.priority) { return -1; }
            else if (this.priority > other.priority) { return 1; }
        }
        else if (other.priority >= 0) { return -1; }

        if (this.callback != null && other.callback == null) { return 1; }
        if (this.callback == null && other.callback != null) { return -1; }

        if (this.token == null && other.token == null) { return 0; }
        else if (this.token != null && other.token != null) {
            if (this.token.length() > other.token.length()) {
                return 1;
            }
            else if (this.token.length() < other.token.length()) {
                return -1;
            }
            else {
                return this.token.compareTo(other.token);
            }
        }
        else if (this.token != null) { return 1; }
        else if (other.token != null) { return -1; }

        return 0;
    }
}
