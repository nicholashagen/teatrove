package org.teatrove.tea;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NullSafeTest extends AbstractTemplateTest {
    
    public static void main(String[] args) throws Exception {

        // build params
        List<String> list = Arrays.asList("TEST", null);
        
        int[] array = { 0, 5, 2, 1 };
        
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("test", new Date());
        map.put("test2", null);

        // execute template
        
        execute("nullsafe", "TEST", list, map, array);
    }
}
