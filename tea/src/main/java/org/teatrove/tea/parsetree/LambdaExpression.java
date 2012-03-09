package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;
import org.teatrove.tea.runtime.Substitution;

public class LambdaExpression extends Expression  {

    private static final long serialVersionUID = 1L;

    private LambdaBlock block;
    
    public LambdaExpression(SourceInfo info, LambdaBlock block) {
        super(info);
        this.block = block;
        setType(new Type(Substitution.class));
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }
    
    public LambdaBlock getBlock() {
        return this.block;
    }
    
    public void setBlock(LambdaBlock block) {
        this.block = block;
    }
}
