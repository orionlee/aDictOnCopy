package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTest;
import net.oldev.aDictOnCopy.MainActivityTestUtils.ServiceSettingsRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.StubPackageManagerRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.TestEnv;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MA_T6Neg_selectedDictNotFound extends BaseTest {
    private static final String PACKAGE_NAME_IN_T6 = "foo.bar.dictPackage.notExist";

    // Define test-specific stubs / settings
    @ClassRule
    public static final StubPackageManagerRule mStubPackageManagerRule;
    @ClassRule
    public static final ServiceSettingsRule mServiceSettingsRule;
    static {
        TestEnv testEnv = createTestEnv(2, PACKAGE_NAME_IN_T6);
        mStubPackageManagerRule = testEnv.stubPackageManagerRule;
        mServiceSettingsRule = testEnv.serviceSettingsRule;
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
