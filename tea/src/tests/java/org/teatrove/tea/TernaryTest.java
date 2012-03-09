package org.teatrove.tea;


public class TernaryTest extends AbstractTemplateTest {

    public static void main(String[] args) throws Exception {
        
        // execute template
        execute("ternary");
        
        Object[] a = new Object[3];
        a[0] = "test1";
        a[1] = "test2";
        a[2] = "test3";
        
        Object[] b = { "test", "test4" };
    }
}
