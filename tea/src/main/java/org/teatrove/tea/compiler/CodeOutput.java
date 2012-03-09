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
