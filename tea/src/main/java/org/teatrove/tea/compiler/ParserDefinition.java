package org.teatrove.tea.compiler;

import java.lang.reflect.Method;

public class ParserDefinition
    implements Comparable<ParserDefinition> {

    private final Object mInstance;
    private final Method mMethod;

    private int mPriority;
    private Class<?>[] mDependencies;

    public ParserDefinition(Object instance, Method method) {
        mInstance = instance;
        mMethod = method;
    }

    public Object getInstance() {
        return mInstance;
    }

    public Method getMethod() {
        return mMethod;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        mPriority = priority;
    }

    public Class<?>[] getDependencies() {
        return mDependencies;
    }

    public void setDependencies(Class<?>[] dependencies) {
        mDependencies = dependencies;
    }

    public Object invoke(Chain chain, Parser parser) {
        Object instance = chain.create(mInstance);
        try { return mMethod.invoke(instance, chain, parser); }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public Object invoke(Object[] params) {
        try { return mMethod.invoke(mInstance, params); }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode += mInstance.getClass().hashCode() * 11;
        hashCode += mMethod.hashCode() * 17;
        return hashCode;
    }

    public boolean equals(Object object) {
        if (object == this) { return true; }
        else if (!(object instanceof ParserDefinition)) { return false; }

        ParserDefinition other = (ParserDefinition) object;
        return other.mInstance.getClass().equals(this.mInstance.getClass()) &&
               other.mMethod.equals(this.mMethod);
    }

    public int compareTo(ParserDefinition other) {
        if (other == this) { return 0; }

        // check dependencies
        if (this.mDependencies != null) {
            for (Class<?> dependency : this.mDependencies) {
                if (dependency.isInstance(other.mInstance)) {
                    return 1;
                }
            }
        }

        // check dependencies
        if (other.mDependencies != null) {
            for (Class<?> dependency : other.mDependencies) {
                if (dependency.isInstance(this.mInstance)) {
                    return -1;
                }
            }
        }

        // check priority
        if (this.mPriority >= 0) {
            if (other.mPriority < 0) { return 1; }
            else if (this.mPriority < other.mPriority) { return -1; }
            else if (this.mPriority > other.mPriority) { return 1; }
        }
        else if (other.mPriority >= 0) { return -1; }

        // equivalent (just compare method names)
        return this.mMethod.getName().compareTo(other.mMethod.getName());
    }
}
