package org.teatrove.tea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericsTest extends AbstractTemplateTest {

    public static void main(String[] args) throws Exception {
        
        // setup params
        List<List<Integer[]>> numbers = new ArrayList<List<Integer[]>>();
        numbers.add(Collections.singletonList(new Integer[] { 5, 3, 1 }));
        
        Map<String, List<String>> states = new HashMap<String, List<String>>();
        states.put("test", Arrays.asList("going", "going", "gone"));
        
        // execute template
        execute("generics", "Test", numbers, states);
    }
}
