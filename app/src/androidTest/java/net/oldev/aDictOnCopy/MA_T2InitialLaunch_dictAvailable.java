package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTest;
import net.oldev.aDictOnCopy.MainActivityTestUtils.ServiceSettingsRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.StubPackageManagerRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.TestEnv;

import org.junit.ClassRule;
import org.junit.Test;

import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@MediumTest
public class MA_T2InitialLaunch_dictAvailable extends BaseTest {
    // Define test-specific stubs / settings
    @ClassRule
    public static final StubPackageManagerRule mStubPackageManagerRule;
    @ClassRule
    public static final ServiceSettingsRule mServiceSettingsRule;
    static {
        TestEnv testEnv = createTestEnv(2, null);
        mStubPackageManagerRule = testEnv.stubPackageManagerRule;
        mServiceSettingsRule = testEnv.serviceSettingsRule;
    }

    @Test
    public void t2InitialLaunch_dictAvailable() {
        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));
    }
}
