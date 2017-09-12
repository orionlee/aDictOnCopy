package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@MediumTest
public class MA_T1InitialLaunch_noDictAvailable extends MainActivityTestUtils.BaseTestWithTestEnvAsTestRules {
    public MA_T1InitialLaunch_noDictAvailable() {
        super(0, null); // Define test-specific stubs / settings
    }

    @Test
    public void t1InitialLaunch_noDictAvailable() {
        onViewDictSelectOutputCheckMatches(withText(getString(R.string.dict_selection_label)));
        // start service button is disabled when there is no dictionary
        onView(allOf(not(isEnabled()),
                     withId(R.id.startCtl),
                     withText(getString(R.string.start_service_label)),
                     isDisplayed()));

        onView(allOf(withId(R.id.dictSelectErrOutput),
                     withText(getString(R.string.err_msg_no_dict_available)),
                     isDisplayed()));

    }
}
