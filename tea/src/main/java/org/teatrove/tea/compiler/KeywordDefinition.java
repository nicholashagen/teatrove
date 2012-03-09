package org.teatrove.tea.compiler;

import org.teatrove.tea.annotations.TokenCallback;

public class KeywordDefinition implements Comparable<KeywordDefinition> {

    private int id;
    private String name;
    private String keyword;
    private int priority;

    public KeywordDefinition(int id, String name,
                             String keyword, int priority) {
        this.id = id;
        this.name = name;
        this.keyword = keyword;
        this.priority = priority;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getKeyword() {
        return this.keyword;
    }

    public int getPriority() {
        return this.priority;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) { return true; }
        else if (!(object instanceof KeywordDefinition)) { return false; }
        return this.id == ((KeywordDefinition) object).id;
    }

    @Override
    public String toString() {
        return "Keyword[" + this.name + ':' + this.id + ']';
    }

    @Override
    public int compareTo(KeywordDefinition other) {
        if (other == this) { return 0; }

        if (this.priority >= 0) {
            if (other.priority < 0) { return 1; }
            else if (this.priority < other.priority) { return -1; }
            else if (this.priority > other.priority) { return 1; }
        }
        else if (other.priority >= 0) { return -1; }

        if (this.keyword == null && other.keyword == null) { return 0; }
        else if (this.keyword != null && other.keyword != null) {
            if (this.keyword.length() > other.keyword.length()) {
                return 1;
            }
            else if (this.keyword.length() < other.keyword.length()) {
                return -1;
            }
            else {
                return this.keyword.compareTo(other.keyword);
            }
        }
        else if (this.keyword != null) { return 1; }
        else if (other.keyword != null) { return -1; }

        return 0;
    }
}
