package net.oldev.aDictOnCopy;

import android.content.pm.PackageManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.oldev.aDictOnCopy.di.AppModule;
import net.oldev.aDictOnCopy.di.DaggerTestAppComponent;
import net.oldev.aDictOnCopy.di.StubSystemModule;
import net.oldev.aDictOnCopy.di.SystemModule;
import net.oldev.aDictOnCopy.di.TestAppComponent;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

/**
 * Helpers for MainActivity Test classes.
 *
 * Note: the test classes were intended to be static inner classes,
 * but doing so will result in ClassNotFoundException when AndroidJUnitRunner tries to load
 * the test class. Hence, they are all top level classes with a prefix <code>MA_</code>
 *
 * @see https://issuetracker.google.com/issues/65560251 AndroidJUnitRunner bug on inner static test class
 */
public class MainActivityTestUtils {

    static class ServiceSettingsRule implements TestRule {
        private final String mPackageName4Test;
        ServiceSettingsRule(String packageName4Test) {
            mPackageName4Test = packageName4Test;
        }

        private class ServiceSettingsRuleStatement extends Statement {
            private final Statement mBase;
            private String mDictPkgOriginal = null;

            ServiceSettingsRuleStatement(final Statement base) {
                super();
                mBase = base;
            }

            @Override
            public void evaluate() throws Throwable {
                // Pre-test setup
                mDictPkgOriginal = getSettingsModel().getPackageName();
                getSettingsModel().setPackageName(mPackageName4Test);

                try {
                    // test body
                    mBase.evaluate();
                } finally {
                    // Post-test teardown: restore original package name
                    getSettingsModel().setPackageName(mDictPkgOriginal);
                }
            }
        }

        private static DictionaryOnCopyService.SettingsModel getSettingsModel() {
            DictionaryOnCopyService.SettingsModel settings =
                    new DictionaryOnCopyService.SettingsModel(InstrumentationRegistry.getTargetContext());
            return settings;
        }


        @Override
        public Statement apply(final Statement base, final Description description) {
            return new ServiceSettingsRuleStatement(base);
        }
    }

    static class StubPackageManagerRule implements TestRule {

        private final int mNumDictAvailable;

        StubPackageManagerRule(int numDictAvailable) {
            mNumDictAvailable = numDictAvailable;
        }

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // Pre-test setup
                    useStubPackageManagerWithDictionaries(mNumDictAvailable);

                    base.evaluate();

                    // Post-test teardown: N/A
                }
            };
        }

        private static void useStubPackageManagerWithDictionaries(int numDictAvailable) {
            // Dependency Injection setup for test environment
            PackageManager stubPkgMgr = new InstrumentedStubPackageMangerBuilder(numDictAvailable).build();
            DictionaryOnCopyApp app = DictionaryOnCopyApp.from(InstrumentationRegistry.getTargetContext());
            TestAppComponent testAppComponent =
                    DaggerTestAppComponent.builder()
                                          .appModule(new AppModule(app))
                                          .stubSystemModule(
                                                  new StubSystemModule(stubPkgMgr,
                                                                       SystemModule.INTENT_LAUNCHER))
                                          .build();
            app.setAppComponent(testAppComponent);

        }
    }


    static class TestEnv {
        final StubPackageManagerRule stubPackageManagerRule;
        final ServiceSettingsRule serviceSettingsRule;

        TestEnv(StubPackageManagerRule stubPackageManagerRule, ServiceSettingsRule serviceSettingsRule) {
            this.stubPackageManagerRule = stubPackageManagerRule;
            this.serviceSettingsRule = serviceSettingsRule;
        }
    }

    /**
     * Skeleton for MainActivity Test: mainly a set of common helper methods.
     * Implementation needs to supply a MainActivityTestRule for their specific case
     *
     * @see #createTestEnv(int, String) Typically a subclass will need to use createTestEnv to
     * setup the environment for the specific test
     */
    @RunWith(AndroidJUnit4.class)
    @Ignore
    static abstract class BaseTest {

        /**
         *
         * @return the activity under test
         */
        abstract @NonNull MainActivity getActivity();

        //
        // Helpers for specific tests
        //

        /**
         *
         * @return Junit Rules needed for the specific MainActivity test class
         */
        static @NonNull TestEnv createTestEnv(int numDictAvailable, @Nullable String packageName4Test) {
            return new TestEnv(new StubPackageManagerRule(numDictAvailable),
                               new ServiceSettingsRule(packageName4Test));

        }

        @NonNull
        String getString(@StringRes int resId) {
            return getActivity().getString(resId);
        }


        static void delay(@SuppressWarnings("SameParameterValue") long delayMillis) {
            // Added a sleep statement to match the app's execution delay.
            // The recommended way to handle such scenarios is to use Espresso idling resources:
            // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        /**
         * Syntactic sugar to check the TextView @+id/dictSelectOutput
         * @param matcher the condition to check on the view, beyond the basics that it exists and is displayed.
         */
        static ViewInteraction onViewDictSelectOutputCheckMatches(org.hamcrest.Matcher<? super android.view.View> matcher) {
            return onView(allOf(withId(R.id.dictSelectOutput),
                                withParent(withId(R.id.dictSelectCtl)),
                                isDisplayed()))
                    .check(matches(matcher));
        }

        /**
         * Perform the following :
         * - Click dictionary selection control
         * - Pick a dictionary among the choices provided, as specified in dictChoiceIdx (0-based)
         * - Click a button in the subsequent launch service dialog, as specified in isYesInLaunchServiceDialog
         *
         * @param dictChoiceIdx
         * @param isYesInLaunchServiceDialog
         */
        void clickDictSelectCtlAndSelectChoice(int dictChoiceIdx, boolean isYesInLaunchServiceDialog) {
            ViewInteraction dictSelectCtl = onView(
                    allOf(withId(R.id.dictSelectCtl), isDisplayed()));
            dictSelectCtl.perform(click());

            // Pick one in the multiple choices in the dialog
            ViewInteraction linearLayout2 = onView(
                    allOf(childAtPosition(
                            allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                                  withParent(withClassName(is("android.widget.LinearLayout")))),
                            dictChoiceIdx),
                          isDisplayed()));
            linearLayout2.perform(click());

            @IdRes int btnId;
            @StringRes int launchServiceDialogBtnLabel;
            if (isYesInLaunchServiceDialog) { // Obtained btnIds via espresso test recording
                btnId = android.R.id.button1;
                launchServiceDialogBtnLabel = R.string.yes_btn_label;
            } else {
                btnId = android.R.id.button2;
                launchServiceDialogBtnLabel = R.string.no_btn_label;
            }
            // A dialog box asking whether to launch the service.
            // click the specified button
            ViewInteraction launchServiceDialogOptionButton = onView(
                    allOf(withId(btnId),
                          withText(getString(launchServiceDialogBtnLabel)),
                          isDisplayed()));
            launchServiceDialogOptionButton.perform(click());

        }

        private static Matcher<View> childAtPosition(
                final Matcher<View> parentMatcher, @SuppressWarnings("SameParameterValue") final int position) {

            return new TypeSafeMatcher<View>() {
                @Override
                public void describeTo(org.hamcrest.Description description) {
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
    }

    /**
     * Abstract MainActivity test class where TestEnv is applied for each test method.
     * In other words, TestEnv is instantiated as JUnit Test Rules.
     */
    static abstract class BaseTestWithTestEnvAsTestRules extends BaseTest {
        // Define test-specific stubs / settings
        @Rule
        public final StubPackageManagerRule mStubPackageManagerRule;
        @Rule
        public final ServiceSettingsRule mServiceSettingsRule;

        @Rule
        public final ActivityTestRule<MainActivity> mActivityTestRule; // activity under test

        BaseTestWithTestEnvAsTestRules(int numDictAvailable, @Nullable String packageName4Test) {
            super();
            TestEnv testEnv = createTestEnv(numDictAvailable, packageName4Test);
            mStubPackageManagerRule = testEnv.stubPackageManagerRule;
            mServiceSettingsRule = testEnv.serviceSettingsRule;

            mActivityTestRule = new ActivityTestRule<>(MainActivity.class);
        }

        @Override
        MainActivity getActivity() {
            return mActivityTestRule.getActivity();
        }
    }

}
