package org.teatrove.tea;


public class ObjectsTest extends AbstractTemplateTest {

    public static void main(String[] args) throws Exception {
        
        // execute template
        compile("util.object", "util.subobject");
        execute("objects", "John Doe", 30, true);
    }
}
