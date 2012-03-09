package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;

public class LambdaStatement extends Block {
    private static final long serialVersionUID = 1L;

    private VariableRef[] mVariables;
    private Block mBlock;

    public LambdaStatement(SourceInfo info, VariableRef[] variables,
                           Block block) {
        super(info);
        mVariables = variables;
        mBlock = block;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public VariableRef[] getVariables() {
        return mVariables;
    }

    public void setVariables(VariableRef[] variables) {
        mVariables = variables;
    }

    public Block getBlock() {
        return mBlock;
    }

    public void setBlock(Block block) {
        mBlock = block;
    }
}
