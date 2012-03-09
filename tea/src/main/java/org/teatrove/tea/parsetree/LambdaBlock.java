package org.teatrove.tea.parsetree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;

public class LambdaBlock extends Block implements Returnable {

    private static final long serialVersionUID = 1L;

    private Type mType;
    private Type mReturnType;
    private Variable[] mPromotedVariables;
    private VariableRef[] mOutOfScopeVariables;
    
    public LambdaBlock(SourceInfo info, Statement[] statements) {
        super(info, statements);
    }

    public LambdaBlock(SourceInfo info) {
        super(info);
    }

    public LambdaBlock(Statement stmt) {
        super(stmt);
        if (stmt instanceof LambdaBlock) {
            LambdaBlock block = (LambdaBlock) stmt;
            this.mReturnType = block.mReturnType;
            this.mPromotedVariables = block.mPromotedVariables;
            this.mOutOfScopeVariables = block.mOutOfScopeVariables;
        }
    }

    public Object clone() {
        LambdaBlock b = (LambdaBlock) super.clone();
        b.mReturnType = mReturnType;
        if (mPromotedVariables != null) {
            b.mPromotedVariables = (Variable[]) mPromotedVariables.clone();
        }
        if (mOutOfScopeVariables != null) {
            b.mOutOfScopeVariables = 
                (VariableRef[]) mOutOfScopeVariables.clone();
        }
        return b;
    }
    
    public Type getType() {
        return mType;
    }
    
    public void setType(Type type) {
        mType = type;
    }
    
    public Type getReturnType() {
        return mReturnType;
    }
    
    public void setReturnType(Type returnType) {
        mReturnType = returnType;
    }

    // TODO: what is purpose of this again?
    public boolean hasSharedVariables() {
        // check if either promoted or out of scope variables
        return getPromotedVariables().length > 0 ||
               getOutOfScopeVariables().length > 0;
    }
    
    // TODO: is this the right name?
    public boolean hasFinalVariables() {
        // check if any promoted variable assigned more than once
        for (Variable var : getPromotedVariables()) {
            if (!var.isFinal()) {
                return false;
            }
        }
        
        // check if any out of scope variable assigned more than once
        for (VariableRef ref : getOutOfScopeVariables()) {
            Variable var = ref.getVariable();
            if (var == null || !var.isFinal()) {
                return false;
            }
        }
        
        // all variables, if any, final
        return true;
    }
    
    public Variable[] getPromotedVariables() {
        Set<Variable> vars = new HashSet<Variable>();
        findPromoted(vars, this);
        return vars.toArray(new Variable[vars.size()]);
    }
    
    public void setPromotedVariables(Variable[] vars) {
        this.mPromotedVariables = vars;
    }
    
    protected void findPromoted(Set<Variable> vars, Block block) {
        // add vars if substitution block
        if (block instanceof LambdaBlock) {
            LambdaBlock sub = (LambdaBlock) block;
            if (sub.mPromotedVariables != null) {
                Collections.addAll(vars, sub.mPromotedVariables);
            }
        }
        
        // parse sub-statements
        Statement[] stmts = block.getStatements();
        for (int i = 0; i < stmts.length; i++) {
            if (stmts[i] instanceof Block) {
                findPromoted(vars, (Block) stmts[i]);
            }
        }
    }

    public VariableRef[] getOutOfScopeVariables() {
        Set<VariableRef> refs = new HashSet<VariableRef>();
        findOutOfScope(refs, this);
        return refs.toArray(new VariableRef[refs.size()]);
    }

    public void setOutOfScopeVariables(VariableRef[] vars) {
        this.mOutOfScopeVariables = vars;
    }

    protected void findOutOfScope(Set<VariableRef> refs, Block block) {
        // add vars if substitution block
        if (block instanceof LambdaBlock) {
            LambdaBlock sub = (LambdaBlock) block;
            if (sub.mOutOfScopeVariables != null) {
                Collections.addAll(refs, sub.mOutOfScopeVariables);
            }
        }
        
        // parse sub-statements
        Statement[] stmts = block.getStatements();
        for (int i = 0; i < stmts.length; i++) {
            if (stmts[i] instanceof Block) {
                findOutOfScope(refs, (Block) stmts[i]);
            }
        }
    }
}
