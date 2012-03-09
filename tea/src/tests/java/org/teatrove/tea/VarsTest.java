package org.teatrove.tea;



public class VarsTest extends AbstractTemplateTest {
    
    public static void main(String[] args) throws Exception {
        
        // execute template
        compile("util.frame");
        execute("vars", "test");
    }
}
