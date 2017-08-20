package net.oldev.aDictOnCopy;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
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
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A more reliable and complete test
 * It uses mock PackageManger to avoid external dictionary app dependency.
 * However, the mocks noticably slow down the test.
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

    private static class StubPackageMangerBuilder {

        static final List<ResolveInfo> RI_LIST_ALL = buildRiListAll();
        private final int mNumDictAvailable;

        public StubPackageMangerBuilder(int numDictAvailable) {
            if (numDictAvailable > RI_LIST_ALL.size()) {
                throw new IllegalArgumentException(String.format("numDictAvailable <%s> is larger than max <s>",
                        numDictAvailable, RI_LIST_ALL.size()));
            }
            mNumDictAvailable = numDictAvailable;
        }

        public PackageManager build() {
            PackageManager mockPkgMgr = mock(PackageManager.class);

            final List<ResolveInfo> riList = new ArrayList<ResolveInfo>();
            for(int i = 0; i < mNumDictAvailable; i++) {
                final ResolveInfo ri = RI_LIST_ALL.get(i);
                riList.add(ri);

                when(mockPkgMgr.resolveActivity(argThat(new ArgumentMatcher<Intent>() {
                    @Override
                    public boolean matches(Intent intent) {
                        return ( intent != null &&
                                ri.activityInfo.packageName.equals(intent.getPackage()) &&
                                isDictionaryAction(intent) );
                    }
                }), eq(PackageManager.MATCH_DEFAULT_ONLY)))
                        .thenReturn(ri);
            }

            when(mockPkgMgr.queryIntentActivities(argThat(new ArgumentMatcher<Intent>() {
                @Override
                public boolean matches(Intent intent) {
                    return isDictionaryAction(intent);
                }
            }), eq(PackageManager.MATCH_DEFAULT_ONLY)))
                    .thenReturn(riList);

            return mockPkgMgr;
        }

        private static boolean isDictionaryAction(Intent intent) {
            if (intent == null) {
                return false;
            }

            switch(intent.getAction()) {
                case "colordict.intent.action.SEARCH":
                case Intent.ACTION_SEARCH:
                    return true;
                default:
                    return false;
            }
        }

        private static List<ResolveInfo> buildRiListAll() {
            List<ResolveInfo> riListAll = new ArrayList<ResolveInfo>();

            riListAll.add(mockResolveInfo("livio.pack.lang.en_US",
                    "English (Mock)",
                    net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_livio));

            riListAll.add(mockResolveInfo("com.socialnmobile.colordict",
                    "ColorDict (Mock)",
                    net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_colordict));

            return Collections.unmodifiableList(riListAll);
        }

        private static ResolveInfo mockResolveInfo(String packageName, String label, int iconIdIfAvailable) {
            ResolveInfo ri = mock(ResolveInfo.class);

            ActivityInfo ai = new ActivityInfo();
            ai.packageName = packageName;

            ri.activityInfo = ai;

            when(ri.loadLabel(any(PackageManager.class)))
                    .thenReturn(label);

            Drawable drawable = null;
            if (iconIdIfAvailable > 0) {
                drawable = InstrumentationRegistry.getContext().getResources().getDrawable(iconIdIfAvailable, null);
            }
            when(ri.loadIcon(any(PackageManager.class)))
                    .thenReturn(drawable);

            return ri;
        }
    }

    private void stubDictionariesAvailable(int numDictAvailable) {
        MainActivity activity = mActivityTestRule.getActivity();

        PackageManager stubPkgMgr = new StubPackageMangerBuilder(numDictAvailable).build();

        activity.mChooser.mDictMgr.mPkgMgr = stubPkgMgr;
    }

    @Test
    public void t1InitialLaunchCaseNoDictAvailable() throws Throwable {
        stubDictionariesAvailable(0);

        // Manually call initial setup logic again now that the package manager is stubbed
        // (I cannot intercept activity's life cycle so that the Activity instance under test
        //  is to use stub PackageManager for its setup (mostly in onCreate())
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().autoSetDefaultDictionary();
            }
        });

        // Ensure the label indicates no dictionary is picked.
        //
        delay(100); // some delay to ensure uiThread finishes its work
        onViewDictSelectOutputCheckMatches(withText(getString(R.string.dict_selection_label)));

    }

    @Test
    public void t2InitialLaunchCaseDictAvailable() throws Throwable {
        stubDictionariesAvailable(2);

        // Manually call initial setup logic again now that the package manager is stubbed
        // (I cannot intercept activity's life cycle so that the Activity instance under test
        //  is to use stub PackageManager for its setup (mostly in onCreate())
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().autoSetDefaultDictionary();
            }
        });

        // Ensure the label indicates some dictionary is picked
        //
        delay(100); // some delay to ensure uiThread finishes its work
        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));

    }


    @Test
    public void t3TypicalCase() {
        // Test: service has been shut down
        assertFalse("The activity should shutdown existing service, if any, upon the screen is shown",
                DictionaryOnCopyService.isRunning());

        stubDictionariesAvailable(2); // 2 dictionaries available

        onViewDictSelectOutputCheckMatches(not(withText(getString(R.string.dict_selection_label))));

        // Test: click dictionary selection and pick one
        // answer *NO* in whether to launch service dialog
        final int IDX_DICT_TO_PICK_IN_T3 = 1;
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T3, R.string.no_btn_label);

        // Ensure the label reflect the dict picked
        final String labelExpected = StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3)
                .loadLabel(mActivityTestRule.getActivity().mChooser.getManager().mPkgMgr).toString();
        onViewDictSelectOutputCheckMatches(withText(labelExpected));

        // Verify the persistence in backend directly
        //
        // OPEN:
        //   The original plan is to  verify at UI layer (rather than going to backend model),
        //   in the next test t4 (after activity is relaunched).
        //   But it cannot be done easily, as the UI output in the test case would rely on
        //   system's real PackageManager, rather than the stub we use here.
        final String packageNameExpected = StubPackageMangerBuilder.RI_LIST_ALL.get(IDX_DICT_TO_PICK_IN_T3).activityInfo.packageName;
        assertPackageNameInSettingsEquals(packageNameExpected);

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

    @Test
    public void t5TypicalCaseCasePressYesButton() {
        stubDictionariesAvailable(2); // 2 dictionaries available

        // Test: click dictionary selection and pick one
        // answer *YES* in whether to launch service dialog
        final int IDX_DICT_TO_PICK_IN_T4 = 0;
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T4, R.string.yes_btn_label);

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
