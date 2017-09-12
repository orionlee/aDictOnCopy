package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTestWithTestEnvAsTestRules;

import org.junit.Test;

import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@MediumTest
public class MA_T2InitialLaunch_dictAvailable extends BaseTestWithTestEnvAsTestRules {
    public MA_T2InitialLaunch_dictAvailable() {
        super(2, null); // Define test-specific stubs / settings
    }

    @Test
    public void t2InitialLaunch_dictAvailable() {
        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));
    }
}
