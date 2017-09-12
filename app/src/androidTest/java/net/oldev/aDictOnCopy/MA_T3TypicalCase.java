package net.oldev.aDictOnCopy;

import android.app.Activity;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTest;
import net.oldev.aDictOnCopy.MainActivityTestUtils.ServiceSettingsRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.StubPackageManagerRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.TestEnv;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Typical case for specifying a dictionary then starting the service.
 * Serves as a sanity test as well (hence @SmallTest)
 *
 * It requires @FixMethodOrder as the test involves in running the MainActivity twice.
 * (The second invocation is to verify the settings done in the first one).
 */
@SmallTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MA_T3TypicalCase extends BaseTest {
    /*
     * This class inherits from BaseTest, rather than BaseTestWithTestEnvAsTestRules
     * because TestEnv is needed to be applied once only across all test methods of the class.
     *
     * They are not reset across test methods because the test t4 verifies the settings made in t3.
     * So TestEnv should not be applied before running t4.
     */

    // Define test-specific stubs / settings
    @ClassRule
    public static final StubPackageManagerRule sStubPackageManagerRule;
    @ClassRule
    public static final ServiceSettingsRule sServiceSettingsRule;
    static {
        TestEnv testEnv = createTestEnv(2, null);
        sStubPackageManagerRule = testEnv.stubPackageManagerRule;
        sServiceSettingsRule = testEnv.serviceSettingsRule;
    }
    // needed when an activity has to be relaunched between methods.
    // E.g., test t3TypicalCase and test t4TypicalCaseVerifySettingsPersistence
    static final boolean RELAUNCH_ACTIVITY_TRUE = true;

    @Rule
    public final ActivityTestRule<MainActivity> mActivityTestRule =
            new ActivityTestRule<>(MainActivity.class, false, RELAUNCH_ACTIVITY_TRUE);

    @Override
    Activity getActivity() {
        return mActivityTestRule.getActivity();
    }


    private static final int IDX_DICT_TO_PICK_IN_T3 = 1;
    @Test
    public void t3TypicalCase() {
        // Test: service has been shut down
        assertFalse("The activity should shutdown existing service, if any, upon the screen is shown",
                    DictionaryOnCopyService.isRunning());

        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));

        // Test: click dictionary selection and pick one
        // answer *NO* in whether to launch service dialog
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T3, false);

        // Ensure the label reflect the dict picked
        final String labelExpected = StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3)
                                                                         .loadLabel(mActivityTestRule.getActivity().mPackageManager).toString();
        onViewDictSelectOutputCheckMatches(withText(labelExpected));

        //
        // Launch service using the top button
        //

        assertFalse("The Activity should still be running at this point.",
                    mActivityTestRule.getActivity().isFinishing());

        ViewInteraction launchServiceButton = onView(
                allOf(withId(R.id.startCtl), withText(getString(R.string.start_service_label)), isDisplayed()));
        launchServiceButton.perform(click());

        delay(100); // give time for service & activity to complete their action

        assertTrue("The Activity should be finished upon launching the service.",
                   mActivityTestRule.getActivity().isFinishing());

        // Test: confirm the service is launched.
        assertTrue("The dictionary service should have been just launched",
                   DictionaryOnCopyService.isRunning());
    }

    /**
     * Semantically part of t3 test. It is to ensure the dictionary package selection in t3 is persisted.
     * This test relies on the fact that Activity is relaunched per test in ActivityTest rule.
     */
    @Test
    public void t4TypicalCaseVerifySettingsPersistence() {
        final String labelExpected =
                StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3)
                                                    .loadLabel(mActivityTestRule.getActivity().mPackageManager)
                                                    .toString();
        onViewDictSelectOutputCheckMatches(withText(labelExpected));
    }
}
