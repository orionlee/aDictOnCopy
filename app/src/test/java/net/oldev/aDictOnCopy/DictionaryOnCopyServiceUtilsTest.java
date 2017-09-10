package net.oldev.aDictOnCopy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests DictionaryOnCopyService's internal helper methods
 *
 */
public class DictionaryOnCopyServiceUtilsTest {

    @RunWith(Parameterized.class)
    public static class IsAWordTest {

        @Parameters(name = "{index}: <{0}> expected={1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"hello", true},
                    {"shopping mall", true},
                    {"the shopping mall", true},
                    {" the shopping mall ", true},
                    {"the one shopping mall", false},
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
                    {"http://some.web-site.net/path/query?arg1=v1&arg2=v%202", false},
                    {"some-web-site.net", false},
                    {"some.web-site.net", false},
                    {"https://some-secure-web-site.", false},
                    {"ftp://some.web-site.net/", false},
                    {"ftp://some.web-site.net/path", false},
                    {"file://some-file.", false},
                    {"file:///some-file.", false},
                    {"mailto:some-email@emailsite.", false},
                    {"some-email@emailsite.net", false},
                    {"tel:123456789", false},
                    {"123-456-7890", false},
                    {"123.456.7890", false},
                    {"123 456 7890", false},
                    {"(123)-456-7890", false},
                    {"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", false},
            });
        }

        @SuppressWarnings({"CanBeFinal", "unused"})
        @Parameter
        public CharSequence text;

        @SuppressWarnings({"CanBeFinal", "unused"})
        @Parameter(1)
        public boolean expected;

        @Test
        public void test() throws Exception {
            boolean actual = DictionaryOnCopyService.isAWord(text);
            assertEquals(expected, actual);
        }
    }
}