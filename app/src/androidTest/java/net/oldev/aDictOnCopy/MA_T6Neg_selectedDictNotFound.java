package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTestWithTestEnvAsTestRules;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@MediumTest
public class MA_T6Neg_selectedDictNotFound extends BaseTestWithTestEnvAsTestRules {
    private static final String PACKAGE_NAME_IN_T6 = "foo.bar.dictPackage.notExist";

    public MA_T6Neg_selectedDictNotFound() {
        super(2, PACKAGE_NAME_IN_T6); // Define test-specific stubs / settings
    }

    @Test
    public void t6Neg_selectedDictNotFound() {
        onViewDictSelectOutputCheckMatches(withText(getString(R.string.dict_selection_label)));
        // start service button is disabled when there is no dictionary
        onView(allOf(not(isEnabled()),
                     withId(R.id.startCtl),
                     withText(getString(R.string.start_service_label)),
                     isDisplayed()));

        String errMsgExpected = String.format(getString(R.string.err_msgf_selected_dict_not_found),
                                              PACKAGE_NAME_IN_T6);
        onView(allOf(withId(R.id.dictSelectErrOutput),
                     withText(errMsgExpected),
                     isDisplayed()));
    }
}
