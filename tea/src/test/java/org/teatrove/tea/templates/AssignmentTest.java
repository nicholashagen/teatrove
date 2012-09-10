package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


public class AssignmentTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("AssignmentApplication", new AssignmentContext());
    }
    
    @Test
    public void testAssignment() throws Exception {
        for (String[] test : TEST_SOURCES) {
            String source = test[0];
            String expected = test[1];
            String result = executeSource(source);
            assertEquals("invalid result: " + source, expected, result);
        }
    }

    protected static final String[][] TEST_SOURCES = {
        //{ "user = getUser(); user.firstName = 'test'; user.firstName", "test" },
        //{ "user = test = getUser(); user.firstName = 'test'; test.firstName", "test" },
        //{ "if ((x = getInt(5)) == 5) { 'true' }", "true" }
        { "list = getList('test'); list[0] = 'blah'; list[0]", "blah" }
    };
    
    public static class AssignmentContext {
        private User user = new User();
        
        public int getInt(int value) { return value; }
        public User getUser() { return user; }
        public List<String> getList(String value) { 
            List<String> list = new ArrayList<String>();
            list.add(value);
            return list;
        }
    }
    
    public static class User {
        private String firstName;
        
        public String getFirstName() { return this.firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
    }
}
