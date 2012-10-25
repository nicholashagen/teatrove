package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value=Parameterized.class)
public class EnumsTest extends AbstractTemplateTest {

    private String source;
    private String expected;

    public EnumsTest(String source, String expected) {
        this.source = source;
        this.expected = expected;
    }
    
    @Before
    public void setup() {
        addContext("EnumsApplication", new EnumsContext());
    }
    
    @Test
    public void testValues() throws Exception {
        assertEquals(this.expected, executeSource(this.source));
    }
    
    @Parameters
    public static List<Object[]> data() {
      return Arrays.asList(
          new Object[] { "org.teatrove.tea.templates.EnumsTest$TestEnum.VALID.ordinal()", "0" },
          new Object[] { "org.teatrove.tea.templates.EnumsTest$TestEnum.INVALID.name()", "INVALID" },
          new Object[] { "org.teatrove.tea.templates.EnumsTest$TestEnum.values()[0].name()", "VALID" },
          new Object[] { "org.teatrove.tea.templates.EnumsTest$TestEnum.valueOf('VALID').name()", "VALID" },
          new Object[] { "a = getTestEnum(0); if (a == org.teatrove.tea.templates.EnumsTest$TestEnum.VALID) { 'true' }", "true" },
          new Object[] { "a = getTestEnum(1); if (a == org.teatrove.tea.templates.EnumsTest$TestEnum.INVALID) { 'true' }", "true" },
          new Object[] { "org.teatrove.tea.templates.EnumsTest$TestEnum.VALID.test()", "blah" }
      );
    }

    // TODO: if both left and right is enum, only do ==

    public static enum TestEnum {
        VALID,
        INVALID;
        
        public String test() { return "blah"; }
    }
    
    public static class EnumsContext {
       public TestEnum getTestEnum(int value) {
           return TestEnum.values()[value];
       }
    }
}
