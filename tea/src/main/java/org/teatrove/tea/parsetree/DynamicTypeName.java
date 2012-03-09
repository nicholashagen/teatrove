package org.teatrove.tea.parsetree;


public class DynamicTypeName extends TypeName {
    private static final long serialVersionUID = 1L;

    private TypeName mTypeName;

    public DynamicTypeName(TypeName name) {
        super(name.getSourceInfo(), name.getName(), name.getDimensions());
    }

    public TypeName getTypeName() {
        return mTypeName;
    }
}
