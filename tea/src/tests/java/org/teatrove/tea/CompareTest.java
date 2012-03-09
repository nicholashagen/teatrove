package org.teatrove.tea;



public class CompareTest extends AbstractTemplateTest {
    
    public static class Test implements Comparable<Test> {
        private String value;
        public Test(String value) { this.value = value; }
        public String toString() {
            return value;
        }
        
        @Override
        public int compareTo(Test o) {
            return 1;
        }
    }
    
    public static class CompareContext {
        public String doSomething(String test, Object... blah) {
            return "1";
        }
        
        public String doSomething(String test, String... blah) {
            return "2";
        }
        
        public String doSomething(String test, Integer... blah) {
            return "3";
        }
        
        public String doSomething(String test, double... blah) {
            return "4";
        }

        public String doSomething(Object test) {
            return "5";
        }

        public String doSomethingElse(Object value) {
            return "6";
        }
        
        public String doSomethingElse(String... values) {
            return "7";
        }
        
        public CompareContext getContext() {
            return this;
        }
    }
    
    public static void main(String[] args) throws Exception {

        // System.out.println("TEST: " + new CompareContext().doSomething("test"));
        // add contexts
        addContext(new CompareContext());
        
        // execute template
        execute("compare", new Test("a"), new Test("b"));
    }
}
