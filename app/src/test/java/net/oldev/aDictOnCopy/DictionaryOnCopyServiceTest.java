package net.oldev.aDictOnCopy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by Sam Lee on 8/14/2017.
 */
public class DictionaryOnCopyServiceTest {

    @RunWith(Parameterized.class)
    public static class IsAWordTest {

        @Parameters(name = "{index}: <{0}> expected={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {"hello", true},
                    {"shopping mall", true},
                    {"", false},
                    {"This is a test for a rejecting a sentence.", false},
                    {"123", false},
                    {"$123", false},
                    {"97.45", false},
                    {"10.1%", false},
                    {"765,432", false},
                    {"7/21", false},
                    {"7 / 21", false},
                    {"http://some-web-site.", false},
                    {"https://some-secure-web-site.", false},
                    {"file://some-file.", false},
                    {"mailto:some-email@emailsite.", false},
                    {"tel:123456789", false},
                    {"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", false},
            });
        }

        @Parameter
        public CharSequence text;

        @Parameter(1)
        public boolean expected;

        @Test
        public void test() throws Exception {
            boolean actual = DictionaryOnCopyService.isAWord(text);
            assertEquals(expected, actual);
        }
    }
}