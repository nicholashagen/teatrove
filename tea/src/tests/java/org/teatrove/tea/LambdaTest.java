package org.teatrove.tea;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import org.teatrove.tea.runtime.Substitution;


public class LambdaTest extends AbstractTemplateTest {

    public static class LambdaContext {
        public Runnable createRunnable(final String test) {
            final String blah = "test";
            final int a = test.length();
            return new Runnable() {
                private int note;
                public void run() {
                    int cnt = note + a;
                    String s = test + blah + "done";
                    Class<?> c = this.getClass();
                    Class<?> d = LambdaContext.this.getClass();
                    System.out.println(s + c + d);
                }
            };
        }
        
        public Date createDate(long time) {
            return new Date(time);
        }
        
        public void eachWithIndex(Object[] array, Substitution sub) {
            int idx = 0;
            try {
            for (Object item : array) {
               sub.substitute(idx++, item); 
            }
            } catch (Exception e) { e.printStackTrace(); }
            
            //return 0;
        }
        
        public void name(String name, Substitution sub) {
            String[] names = name.split("\\s+", 2);
            try { sub.substitute(names[0], names[1]); }
            catch (Exception e) { e.printStackTrace(); }
        }
        
        public int sort(Object[] array, final Substitution sub) {
            Arrays.sort(array, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    try {
                        Object result = sub.rsubstitute(o1, o2);
                        return ((Number) result).intValue();
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("invalid", e);
                    }
                }
            });
            
            return 0;
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        // add contexts
        addContext(new LambdaContext());
        
        // execute template
        compile("util.substitute");
        compile("util.frame");
        compile("util.minimum");
        execute("lambda");
    }
}
