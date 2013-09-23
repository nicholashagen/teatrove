package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class ApplicationOverrideTest extends AbstractTemplateTest {

    private static final String OUTPUT1 = "TEST";
    private static final Integer OUTPUT2 = Integer.valueOf(1);
    
    public ApplicationOverrideTest() {
        super();
    }
    
    @Before
    public void setup() {
        addContext("CMS1Application", new CMS1(), false);
        addContext("CMS2Application", new CMS2(), true);
    }
    
    @Test
    public void testConflicts() throws Exception {
        String expected = 
            OUTPUT2.toString() + OUTPUT1.toString() + OUTPUT2.toString();
        
        String source =
            "getTest();" + "CMS1Application$getTest();" +
            "CMS2Application.getTest();";
        
        assertEquals(expected, executeSource(source));
    }
    
    public static class CMS1 {
        public String getTest() { return OUTPUT1; }
    }
    
    public static class CMS2 {
        public Integer getTest() { return OUTPUT2; }
    }
}
