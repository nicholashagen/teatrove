package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.compiler.Type;

/**
 * Expression that handles the 'between' operator for checking whether a value
 * is beween two other values: <code>x between 2 and 5</code>.
 */
public class BetweenExpression extends Expression implements Logical {
    private static final long serialVersionUID = 1L;

    private Expression mExpr;
    private Expression mLowerBounds;
    private Expression mUpperBounds;
    
    public BetweenExpression(SourceInfo info, Expression expr,
                             Expression lowerBounds, Expression upperBounds) {
        super(info);

        mExpr = expr;
        mLowerBounds = lowerBounds;
        mUpperBounds = upperBounds;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        BetweenExpression expr = (BetweenExpression) super.clone();
        expr.mExpr = (Expression) mExpr.clone();
        expr.mLowerBounds = (Expression) mLowerBounds.clone();
        expr.mUpperBounds = (Expression) mUpperBounds.clone();
        return expr;
    }

    public boolean isExceptionPossible() {
        if (super.isExceptionPossible()) {
            return true;
        }

        if (mExpr != null && isExceptionPossible(mExpr)) { 
            return true;
        }
        
        if (mLowerBounds != null && isExceptionPossible(mLowerBounds)) { 
            return true;
        }
        
        if (mUpperBounds != null && isExceptionPossible(mUpperBounds)) { 
            return true;
        }

        return false;
    }

    protected boolean isExceptionPossible(Expression expr) {
        if (expr.isExceptionPossible()) {
            return true;
        }

        Type type = expr.getType();
        if (type != null && type.isNullable()) {
            return true;
        }
        
        return false;
    }
    
    public Expression getExpression() {
        return mExpr;
    }

    public Expression getLowerBounds() {
        return mLowerBounds;
    }
    
    public Expression getUpperBounds() {
        return mUpperBounds;
    }

    public void setExpression(Expression expr) {
        mExpr = expr;
    }

    public void setLowerBounds(Expression expr) {
        mLowerBounds = expr;
    }
    
    public void setUpperBounds(Expression expr) {
        mUpperBounds = expr;
    }
    
    public void setType(Type type) {
        // always returns a non-null value
        super.setType(type.toNonNull());
    }
}
