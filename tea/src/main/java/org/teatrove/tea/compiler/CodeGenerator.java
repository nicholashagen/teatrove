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

package org.teatrove.tea.compiler;

import java.io.IOException;

import org.teatrove.tea.parsetree.Template;

/**
 *
 * @author Brian S O'Neill
 */
public abstract class CodeGenerator {
    private Template mTree;

    public CodeGenerator(Template tree) {
        mTree = tree;
    }

    public Template getParseTree() {
        return mTree;
    }

    public abstract void writeTo(CodeOutput out) throws IOException;
}
