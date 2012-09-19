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
    
    // TODO user.firstName = 5 // fail
    // TODO list[0] = 'string' // fail
    // TODO map[10] = null // fail
    // TODO set[0] = 5 // fail
    // TODO string[3] = 'c' // fail
    // TODO obj.missing = 3 // fail
    // TODO user*.firstName = 'test' // fail
    
    protected static final String[][] TEST_SOURCES = {
        { "user = getUser(); user.firstName = 'test'; user.firstName", "test" },
        { "user = test = getUser(); user.firstName = 'test'; test.firstName", "test" },
        { "if ((x = getInt(5)) == 5) { 'true' }", "true" },
        //TODO{ "list = getList('test'); list[0] = 'blah'; list[0]", "blah" },
        { "a = b = 5 as Double; a + b", "10.0" },
        { "a = 6 as Long; a", "6" },
        { "a = getUser(); a.class.simpleName", "User" },
        //TODO{ "map = getMap(); map['test'] = 'string'; map['test'];", "string" },
        //TODO{ "map = getMap(); list = getList('a'); map['test'] = list[0] = cnt = '5'; map['test'] & list[0] & cnt", "555" },
        { "user = getUser(); user.firstName = user.lastName = 'test'; user.firstName & ' ' & user.lastName", "test test" },
        //TODO{ "array = getArray(2, 3); array[0] = 3; array[1] = 5; array[0] + array[1];", "8" }
        //TODO{ "user = getNullUser(); user?.firstName = 'blah'; user?.firstName", "null" },
        { "c = 5.2; c", "5.2" },
        { "a = b = 5.3; a + b", "10.6" },
        { "a = getInt(5); a = a + 7; a", "12" },
        { "a = getInt(6); a = getInt(7); a", "7" },
        { "u = getList('test'); foreach (i in u) { if (i isa String) { i } }", "test" }
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
        private String lastName;
        
        public String getFirstName() { return this.firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return this.lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
