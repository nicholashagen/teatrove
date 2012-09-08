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
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public abstract class CodeOutput {
    private Set<String> innerClasses = new HashSet<String>();
    
    public CodeOutput() {
        super();
    }
    
    // TODO: change CodeOutput getInnerClass to return another CodeOutput
    // and make it the job of the caller to reset all
    
    protected Set<String> getInnerClasses() {
        return this.innerClasses;
    }
    
    protected void addInnerClass(String innerClass) {
        this.innerClasses.add(innerClass);
    }
    
    public abstract OutputStream getOutputStream()
        throws IOException;

    public abstract OutputStream getOutputStream(String innerClass)
        throws IOException;

    public abstract void resetOutputStream();

    public abstract void resetOutputStream(String innerClass);

    public abstract void resetOutputStreams();
}
