package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    @Test
    public void testFailures() throws Exception {
        for (String source : TEST_ERRORS) {
            adddMockListener(1, 0);
            compileSource(source);
        }
    }

    protected static final String[][] TEST_SOURCES = {
        /*
        { "a = 5; a", "5" },
        { "user = getUser(); user.firstName = 'test'; user.firstName", "test" },
        { "user = test = getUser(); user.firstName = 'test'; test.firstName", "test" },
        { "if ((x = getInt(5)) == 5) { 'true' }", "true" },
        { "list = getList('test'); list[0] = 'blah'; list[0]", "blah" },
        { "a = b = 5 as Double; a + b", "10.0" },
        { "a = 6 as Long; a", "6" },
        { "a = getUser(); a.class.simpleName", "User" },
        { "a = getUser(); a.age = 5; a.age * 2;", "10" },
        { "map = getMap(); map['test'] = 'string'; map['test'];", "string" },
        { "map = getIntMap(); c = map['test'] = 5; map['test'] * c;", "25" },
        { "map = getMap(); list = getList('a'); map['test'] = list[0] = cnt = '5'; map['test'] & list[0] & cnt", "555" },
        { "user = getUser(); user.firstName = user.lastName = 'test'; user.firstName & ' ' & user.lastName", "test test" },
        { "array = getArray(2, 3); array[0] = 3; array[1] = 5; array[0] + array[1];", "8" },
        { "user = getNullUser(); user?.firstName = 'blah'; user?.firstName", "null" },
        { "user = getNullUser(); user?.result = 5.3; user?.result", "0.0" },
        { "user = getUser(); user?.missing?.firstName = 'blah'; user?.missing?.firstName", "null" },
        { "user = getUser(); user.firstName = test = user.parent.lastName = 'blah'; user.firstName & test & user.parent.lastName", "blahblahblah" },
        { "user = getUser(); user.result = test = user.parent.result = 5.23; user.result & test & user.parent.result", "5.235.235.23" },
        { "c = 5.2; c", "5.2" },
        { "a = b = 5.3; a + b", "10.6" },
        { "a = b = c = d = 8; a + b + c + d", "32" },
        { "a = getInt(5); a = a + 7; a", "12" },
        { "a = getInt(6); a = getInt(7); a", "7" },
        { "u = getList('test'); foreach (i in u) { if (i isa String) { i } }", "test" },
        { "ref = checkTest(getString('5') ?: '9');", "" },
        { "list = getList('test'); foreach (i in list) { i }", "test" },
        { "a = getInt(5); a = a + 5; a", "10" },
        */
        { "list = getList('test'); total = 0; foreach (i in list) { total = total + 1 }; total", "1" }
    };

    protected static final String[] TEST_ERRORS = {
        "user = getUser(); user.age = 'test';",
        "list = getIntList(5); list[0] = 'test';",
        "list = getList('test'); list['test'] = 'test';",
        //TODO: "map = getOtherMap(); map['test'] = 'test';",
        "map = getIntMap(); map['test'] = 'test';",
        "set = getSet(); set[0] = 'test';",
        "value = 'test'; value[2] = 'a';",
        "user = getUser(); user.invalid = 'test';",
        "user = getUser(); user*.firstName = 'test';"
    };
    
    public static class AssignmentContext {
        private User user = new User();
        
        public int getInt(int value) { return value; }
        public User getUser() { return user; }
        public User getNullUser() { return null; }
        public List<String> getList(String value) { 
            List<String> list = new ArrayList<String>();
            list.add(value);
            return list;
        }
        
        public List<Integer> getIntList(int x) {
            return Collections.singletonList(Integer.valueOf(x));
        }
        
        public Set<String> getSet() { return new HashSet<String>(); }
        
        public Map<String, String> getMap() {
            return new HashMap<String, String>();
        }
        
        public Map<String, Integer> getIntMap() {
            return new HashMap<String, Integer>();
        }
        
        public Map<Integer, String> getOtherMap() {
            return new HashMap<Integer, String>();
        }
        
        public int[] getArray(int... a) { return a; }
        
        public String getString(String value) { return value; }
        public Integer checkTest(String s) {
            return Integer.valueOf(s, 10);
        }
        
        public void numberFormat(String format) { }
    }
    
    public static class User {
        private String firstName;
        private String lastName;
        private double result;
        private Integer age;
        
        public User getParent() { return this; }
        public User getMissing() { return null; }
        
        public String getFirstName() { return this.firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return this.lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public double getResult() { return this.result; }
        public void setResult(double result) { this.result = result; }
        
        public Integer getAge() { return this.age; }
        public void setAge(Integer age) { this.age = age; }
    }
}
