package org.teatrove.tea;



public class RegistryTest extends AbstractTemplateTest {
    
    public static void main(String[] args) throws Exception {

        compile("opts", "model.services", "layouts.main", "render", "views.services");
        
        // execute template
        execute("services", "TEST");
    }
}
