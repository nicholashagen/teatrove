package org.teatrove.tea;


public class RunnableTest {
    // template1:  199775649, 176654255, 178719526, 186328109, 172005186
    // template2:  227897190, 194309326, 179254609, 177549655, 218478561
    // template3:  209840229, 181662871, 214640970, 189692592, 203612855
    
    public static void main(String[] args) {
        int count = 1000;
        long s = System.nanoTime();
        for (int i = 0; i < count; i++) {
            Template3.execute();
        }
        long e = System.nanoTime();
        System.out.println("RESULT: " + (e - s));
    }
    
    public static class Template1 {
        public static void execute() {
            final int a = 5;
            final int b = 6;
            Frame1.execute(
                new Runnable() {
                    public void run() {
                        int c = a + b;
                        String result = String.valueOf(c);
                        System.out.println(result);
                    }
                });
            
        }
    }
    
    public static class Frame1 {
        public static void execute(Runnable runnable) {
            int d = 10;
            runnable.run();
            System.out.println("END: " + d);
        }
    }
    
    public static class Template2 implements Runnable {
        private int a;
        private int b;
        
        public static void execute() {
            Template2 template = new Template2();
            template.a = 5;
            template.b = 6;
            Frame2.execute(template);
        }
        
        public void run() {
            int c = a + b;
            String result = String.valueOf(c);
            System.out.println(result);
        }
    }
    
    public static class Frame2 {
        public static void execute(Runnable runnable) {
            int d = 10;
            runnable.run();
            System.out.println("END: " + d);
        }
    }
    
    public static class Template3 {
        private int a;
        private int b;
        
        public static void execute() {
            Template3 template = new Template3();
            template._execute();
        }
        
        public void _execute() {
            this.a = 5;
            this.b = 6;
            Frame1.execute(
                new Runnable() {
                    public void run() {
                        int c = Template3.this.a + Template3.this.b;
                        String result = String.valueOf(c);
                        System.out.println(result);
                    }
                });
            
        }
    }
    
    public static class Frame3 {
        public static void execute(Runnable runnable) {
            int d = 10;
            runnable.run();
            System.out.println("END: " + d);
        }
    }
}
