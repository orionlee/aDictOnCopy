package net.oldev.aDictOnCopy;


import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A more reliable and complete test
 * It uses mock PackageManger to avoid external dictionary app dependency.
 * However, the mocks noticably slow down the test.
 *
 * Test orders are important
 *
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainActivityTest {

    private static final boolean RELAUNCH_ACTIVITY_TRUE = true;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule =
            new ActivityTestRule<>(MainActivity.class, false, RELAUNCH_ACTIVITY_TRUE);

    /**
     * A test setup for test t1
     * It is masqueraded as test because it has a t1-specific parameter (0 dictionary)
     * that cannot be expressed using @Before.
     * It works because the tests are executed in order.
     * The dollar sign after t1 in method name is to ensure the method is called
     * before the real t1 test method.
     */
    @Test
    public void t1$setUp() {
        // Change the PackageManger use to a stub for test *before* Activity is created
        InstrumentedStubPackageMangerBuilder.stubDictionariesAvailableInDictionaryManager(0);
    }

    @Test
    public void t1InitialLaunchCaseNoDictAvailable() throws Throwable {
        onViewDictSelectOutputCheckMatches(withText(getString(R.string.dict_selection_label)));
    }

    /**
     * A test setup for test t2 (applied to subsequent tests too)
     * It is masqueraded as test because it has a t2-specific parameter (2 dictionaries)
     * that cannot be expressed using @Before
     * It works because the tests are executed in order.
     */
    @Test
    public void t2$setUp4RemainingTests() {
        // Change the PackageManger use to a stub for test *before* Activity is created
        InstrumentedStubPackageMangerBuilder.stubDictionariesAvailableInDictionaryManager(2);
    }

    @Test
    public void t2InitialLaunchCaseDictAvailable() throws Throwable {
        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));
    }


    private final int IDX_DICT_TO_PICK_IN_T3 = 1;
    @Test
    public void t3TypicalCase() {
        // Test: service has been shut down
        assertFalse("The activity should shutdown existing service, if any, upon the screen is shown",
                DictionaryOnCopyService.isRunning());

        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));

        // Test: click dictionary selection and pick one
        // answer *NO* in whether to launch service dialog
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T3, R.string.no_btn_label);

        // Ensure the label reflect the dict picked
        final String labelExpected = StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3)
                .loadLabel(mActivityTestRule.getActivity().mChooser.getManager().mPkgMgr).toString();
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
        final String labelExpected = StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3)
                .loadLabel(mActivityTestRule.getActivity().mChooser.getManager().mPkgMgr).toString();
        onViewDictSelectOutputCheckMatches(withText(labelExpected));
    }

    @Test
    public void t5TypicalCaseCasePressYesButton() {

        // Test: click dictionary selection and pick one
        // answer *YES* in whether to launch service dialog
        final int IDX_DICT_TO_PICK_IN_T5 = 0;
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T5, R.string.yes_btn_label);

        delay(100); // give time for service & activity to complete their action

        assertTrue("The Activity should be finished after the launch service option is clicked in the dialog.",
                mActivityTestRule.getActivity().isFinishing());

        // Test: confirm the service is launched.
        assertTrue("The dictionary service should have been just launched",
                DictionaryOnCopyService.isRunning());
    }

    /**
     * Perform the following :
     * - Click dictionary selection control
     * - Pick a dictionary among the choices provided, as specified in dictChoiceIdx (0-based)
     * - Click a button in the subsequent launch service dialog, as specified in launchServiceDialogBtnLabel
     *
     * @param dictChoiceIdx
     * @param launchServiceDialogBtnLabel
     */
    private void clickDictSelectCtlAndSelectChoice(int dictChoiceIdx, @StringRes int launchServiceDialogBtnLabel) {
        ViewInteraction dictSelectCtl = onView(
                allOf(withId(R.id.dictSelectCtl), isDisplayed()));
        dictSelectCtl.perform(click());

        // Pick one in the multiple choices in the dialog
        ViewInteraction linearLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.select_dialog_listview),
                                withParent(withId(R.id.contentPanel))),
                        dictChoiceIdx),
                        isDisplayed()));
        linearLayout2.perform(click());

        // A dialog box asking whether to launch the service.
        // click the specified button
        ViewInteraction launchServiceDialogOptionButton = onView(
                allOf(withText(getString(launchServiceDialogBtnLabel)),
                        withParent(withParent(withId(R.id.buttonPanel)))));
        launchServiceDialogOptionButton.perform(scrollTo(), click());

    }


    private void assertPackageNameInSettingsEquals(String packageNameExpected) {
        DictionaryOnCopyService.SettingsModel settings =
                new DictionaryOnCopyService.SettingsModel(mActivityTestRule.getActivity());
        assertEquals("Dictionary package picked in t3 should still be here, i.e., persisted",
                packageNameExpected, settings.getPackageName());
    }


    /**
     * Syntactic sugar to check the TextView @+id/dictSelectOutput
     * @param matcher the condition to check on the view, beyond the basics that it exists and is displayed.
     */
    private ViewInteraction onViewDictSelectOutputCheckMatches(org.hamcrest.Matcher<? super android.view.View> matcher) {
        return onView(allOf(withId(R.id.dictSelectOutput),
                withParent(withId(R.id.dictSelectCtl)),
                isDisplayed()))
                .check(matches(matcher));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    @NonNull
    private String getString(@StringRes int resId) {
        return mActivityTestRule.getActivity().getString(resId);
    }

    private static void delay(long delayMillis) {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
