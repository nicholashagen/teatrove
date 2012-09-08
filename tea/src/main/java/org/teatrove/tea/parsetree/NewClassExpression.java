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

import org.teatrove.tea.compiler.CompilationUnit;
import org.teatrove.tea.compiler.SourceInfo;

public class NewClassExpression extends NewArrayExpression {
    private static final long serialVersionUID = 1L;

    private Name mTarget;
    private boolean mAnonymous;
    private CompilationUnit mUnit;

    public NewClassExpression(SourceInfo info,
                              ExpressionList list) {
        super(info, list, true);
        this.mTarget = null;
        this.mAnonymous = true;
}

    public NewClassExpression(SourceInfo info,
                              Name target,
                              ExpressionList list,
                              boolean associative) {
        super(info, list, associative);
        this.mTarget = target;
        this.mAnonymous = false;
    }

    public boolean isAnonymous() {
        return mAnonymous;
    }

    public Name getTarget() {
        return mTarget;
    }

    public void setTarget(Name target) {
        mTarget = target;
    }

    public CompilationUnit getCalledTemplate() {
        return mUnit;
    }

    public void setCalledTemplate(CompilationUnit unit) {
        mUnit = unit;
    }

    public Object clone() {
        NewClassExpression nae = (NewClassExpression)super.clone();
        nae.mTarget = (Name) mTarget.clone();
        return nae;
    }

    public Object accept(NodeVisitor visitor) {
        return visitor.visit(this);
    }
}
