package org.teatrove.tea.compiler;

import java.util.LinkedHashMap;

public class DynamicType extends Type {
    private static final long serialVersionUID = 1L;

    private String mPackage = null;
    private LinkedHashMap<String, Type> mParams =
        new LinkedHashMap<String, Type>();

    public DynamicType(String name) {
        this(null, name);
    }

    public DynamicType(String pkg, String name) {
        super(name);
        mPackage = pkg;
    }

    public DynamicType(DynamicType type) {
        super(type);
        mPackage = type.mPackage;
        mParams = type.mParams;
    }

    public String getPackage() {
        return mPackage;
    }

    public String getClassName() {
        if (mPackage == null) {
            return super.getClassName();
        } else {
            return mPackage + '.' + super.getClassName();
        }
    }

    public void addParameter(String name, Type type) {
        if (mParams.containsKey(name)) {
            throw new IllegalArgumentException("param " + name + " exists");
        }

        mParams.put(name, type);
    }

    public boolean hasParameter(String name) {
        return mParams.containsKey(name);
    }

    public Type getParameterType(String name) {
        if (!mParams.containsKey(name)) {
            throw new IllegalArgumentException("invalid param " + name);
        }

        return mParams.get(name);
    }

    public String[] getParameterNames() {
        return mParams.keySet().toArray(new String[mParams.size()]);
    }

    public Type[] getParameterTypes() {
        return mParams.values().toArray(new Type[mParams.size()]);
    }

    public Type toNonNull() {
        if (isNonNull()) {
            return this;
        }
        else {
            return new DynamicType(this) {
                private static final long serialVersionUID = 1L;

                public boolean isNonNull() {
                    return true;
                }

                public boolean isNullable() {
                    return false;
                }

                public Type toNullable() {
                    return DynamicType.this;
                }
            };
        }
    }
}
