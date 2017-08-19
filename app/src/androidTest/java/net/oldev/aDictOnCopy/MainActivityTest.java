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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private static class MockPackageMangerBuilder {

        private static final List<ResolveInfo> RI_LIST_ALL = buildRiListAll();
        private final int mNumDictAvailable;

        public MockPackageMangerBuilder(int numDictAvailable) {
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

    private void mockDictionariesAvailable(int numDictAvailable) {
        MainActivity activity = mActivityTestRule.getActivity();

        PackageManager mockPkgMgr = new MockPackageMangerBuilder(numDictAvailable).build();

        activity.mChooser.mDictMgr.mPkgMgr = mockPkgMgr;
    }

    // TODO: add various test configuration
    // - no dictionary available
    // TODO: test more thoroughly
    // - test default selection (on initial installation)
    // - ensure selected is persisted

    @Test
    public void mainActivityTest() {
        // Test: service has been shut down
        assertFalse("The activity should shutdown existing service, if any, upon the screen is shown",
                DictionaryOnCopyService.isRunning());

        mockDictionariesAvailable(2); // 2 dictionaries available

        onView(allOf(withId(R.id.dictSelectOutput),
                withParent(withId(R.id.dictSelectCtl)),
                isDisplayed()))
                .check(matches(not(withText((getString(R.string.dict_selection_label))))));

        // Test: click dictionary selection and pick one
        ViewInteraction dictSelectCtl = onView(
                allOf(withId(R.id.dictSelectCtl), isDisplayed()));
        dictSelectCtl.perform(click());

        ViewInteraction linearLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.select_dialog_listview),
                                withParent(withId(R.id.contentPanel))),
                        0),
                        isDisplayed()));
        linearLayout2.perform(click());

        // A dialog box asking whether to launch the service. answer no.
        ViewInteraction launchServiceDialogNoButton = onView(
                allOf(withId(android.R.id.button2), withText(getString(R.string.no_btn_label))));
        launchServiceDialogNoButton.perform(scrollTo(), click());


        //
        // Launch service manually
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
