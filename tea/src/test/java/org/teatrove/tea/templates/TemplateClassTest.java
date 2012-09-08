/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TemplateClassTest extends AbstractTemplateTest {

    @Test
    public void testSimpleTemplateClass() throws Exception {
        compileSource("user", "class", 
                      "String first, String last, Integer age");
        
        assertEquals("John = 10", executeSource(TEST_SOURCE_1));
    }

    @Test
    public void testAnonymousClass() throws Exception {
        assertEquals("John = 10", executeSource(TEST_SOURCE_2));
    }
    
    @Test
    public void testAnonymousReturn() throws Exception {
        compileSource("getUserInfo", "template", "Integer age", TEST_SOURCE_3a);
        
        assertEquals("John = 14", executeSource(TEST_SOURCE_3b));
    }
    
    protected static final String TEST_SOURCE_1 =
        "user = ##user{ 'first' : 'John', 'last' : 'Doe', 'age' : 5 }; " +
        "user.first ' = ' (user.age * 2)";
    
    protected static final String TEST_SOURCE_2 = 
        "first = 'John'; last = 'Doe'; age = 5; " +
        "anon = ##{ 'first' : first, 'last' : last, 'age' : age }; " +
        "anon.first ' = ' (anon.age * 2)";
    
    protected static final String TEST_SOURCE_3a = 
        "first = 'John'; last = 'Doe'; " +
        "anon = ##{ 'first' : first, 'last' : last, 'age' : age }; " +
        "anon";
    
    protected static final String TEST_SOURCE_3b = 
        "anon = call getUserInfo(7); " +
        "anon.first ' = ' (anon.age * 2)";
}
