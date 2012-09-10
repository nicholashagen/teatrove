/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.tea.parsetree;

import org.teatrove.tea.compiler.SourceInfo;

/**
 * AssignmentStatements act on {@link Assignable} expressions on the left
 * assigning a given {@link Expression} on the right.  Typically, the left
 * expressions are variables, lookups or indexed properties (arrays, maps, etc).
 *
 * @author Brian S O'Neill
 */
public class AssignmentExpression extends Expression {
    private static final long serialVersionUID = 1L;

    private Expression mLvalue;
    private Expression mRvalue;

    public AssignmentExpression(SourceInfo info,
                                Expression lvalue, Expression rvalue) {
        super(info);
        if (!(lvalue instanceof Assignable)) {
            throw new IllegalStateException("lvalue must be assignable");
        }
        
        mLvalue = lvalue;
        mRvalue = rvalue;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }

    public Object clone() {
        AssignmentExpression as = (AssignmentExpression) super.clone();
        as.mLvalue = (Expression)mLvalue.clone();
        as.mRvalue = (Expression)mRvalue.clone();
        return as;
    }

    public Expression getLValue() {
        return mLvalue;
    }
    
    public void setLValue(Expression lvalue) {
        if (!(lvalue instanceof Assignable)) {
            throw new IllegalStateException("lvalue must be assignable");
        }
        
        mLvalue = lvalue;
    }

    public Expression getRValue() {
        return mRvalue;
    }

    public void setRValue(Expression rvalue) {
        mRvalue = rvalue;
    }
}
