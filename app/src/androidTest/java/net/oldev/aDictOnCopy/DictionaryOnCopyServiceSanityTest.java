package net.oldev.aDictOnCopy;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;

import net.oldev.aDictOnCopy.di.AppModule;
import net.oldev.aDictOnCopy.di.DaggerTestAppComponent;
import net.oldev.aDictOnCopy.di.StubSystemModule;
import net.oldev.aDictOnCopy.di.TestAppComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DictionaryOnCopyServiceSanityTest {

    private static void delay(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class ClipboardHelper {

        /*
         * mUiThreadRule is needed to access ClipboardManager (which cannot be accessed otherwise)
         * The larger test involves start/stop service that cannot be run in UIThread
         *
         */
        @Rule
        public final UiThreadTestRule mUiThreadRule = new UiThreadTestRule();

        public void setText(CharSequence text) {
            ClipData clipData = ClipData.newPlainText("test clip plain text",
                                                      text);
            setPrimaryClip(clipData);

        }


        /* OPEN: requires TargetApi(16)
        public void setHtml(CharSequence text, String htmlText) {
            ClipData clipData = ClipData.newHtmlText("test clip HTML text",
                                                     text,
                                                     htmlText);
            setPrimaryClip(clipData);

        }
        */

        private void setPrimaryClip(final ClipData clipData) {
            try {
                mUiThreadRule.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    ClipboardManager clipbMgr =
                            (ClipboardManager) InstrumentationRegistry.getTargetContext().getSystemService(CLIPBOARD_SERVICE);
                    clipbMgr.setPrimaryClip(clipData);
                    }
                });
                // ensure clipboard listener has time to react to the changes
                // so that subsequent calls can verify what the listener has done.
                delay(50);
            } catch (Throwable t) {
                fail("Unexpected error in setting clipboard. " + t.getMessage());
            }
        }
    }

    private static class DictionaryOnCopyServiceHelper {
        // defines the delay added post service start/stop/etc., to ensure the service state is as intended.
        private static final int POST_SERVICE_START_DELAY_MS = 100;

        public void startForeground() {
            DictionaryOnCopyService.startForeground(InstrumentationRegistry.getTargetContext());
            delay(POST_SERVICE_START_DELAY_MS);
        }

        public void stopForeground() {
            DictionaryOnCopyService.stopForeground(InstrumentationRegistry.getTargetContext());
            delay(POST_SERVICE_START_DELAY_MS);
        }

        public void pause() {
            startServiceWithAction(DictionaryOnCopyService.ACTION_PAUSE);
        }

        public void resume() {
            startServiceWithAction(DictionaryOnCopyService.ACTION_RESUME);
        }

        private void startServiceWithAction(String action) {
            final Intent intent = new Intent(InstrumentationRegistry.getTargetContext(),
                                             DictionaryOnCopyService.class)
                    .setAction(action);
            InstrumentationRegistry.getTargetContext().getApplicationContext().startService(intent);
            delay(POST_SERVICE_START_DELAY_MS);

        }
    }

    private static String sDictPackageNameOrig = null; // used by setUp / tearDown
    private static String DICT_PACKAGE_NAME_FOR_TEST; // final once inited by setUp
    private static String DICT_ACTION_FOR_TEST; // final once inited by setUp

    /**
     * It is a mock in the sense that it records the invocation (that can be verified)
     */
    private static class MockIntentLauncher implements DictionaryOnCopyService.IntentLauncher {
        // record the intents received for verification
        public final List<Intent> intents = new ArrayList<Intent>();

        @Override
        public void start(Context ctx, @NonNull Intent intent) {
            intents.add(intent);
        }
    }

    /**
     * Assert that the launcher is launched with the expected intent, then reset the launcher to clear out the recordings.
     *
     * @param launcher the launcher to be asserted
     * @param queryWordExpected the word expected in the intent extra, null if no intent is expected.
     */
    private static void assertAndReset(MockIntentLauncher launcher, String queryWordExpected) {
        if (queryWordExpected == null) {
            assertEquals("launcher is expected to have not been invoked. Unexpected Intents received: " + launcher.intents,
                         0, launcher.intents.size());
        } else {
            assertEquals("launcher is expected to have invoked.",
                         1, launcher.intents.size());

            // Now verify the intent is what we expected.
            final Intent actual = launcher.intents.get(0);
            assertEquals("launcher intent assertion: getExtra() failed",
                         queryWordExpected,
                         actual.getStringExtra(SearchManager.QUERY));
            assertEquals("launcher intent assertion: getPackage() failed",
                         DICT_PACKAGE_NAME_FOR_TEST,
                         actual.getPackage());
            assertEquals("launcher intent assertion: getAction() failed",
                         DICT_ACTION_FOR_TEST,
                         actual.getAction());
            assertEquals("launcher intent assertion: getAction() failed",
                         Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION,
                         actual.getFlags());
        }

        // assertions done, now, resets the intents recording
        launcher.intents.clear();
    }

    private static MockIntentLauncher sMockDictionaryLauncher = new MockIntentLauncher();

    @Before
    public void setUpTestDependency() {
        // Note: app setup with testAppComponent must be done with @Before, and not @BeforeClass

        // Dependency Injection setup for test environment
        PackageManager stubPkgMgr = new InstrumentedStubPackageMangerBuilder(2).build();
        DictionaryOnCopyApp app = DictionaryOnCopyApp.from(InstrumentationRegistry.getTargetContext());
        TestAppComponent testAppComponent = DaggerTestAppComponent.builder()
                                                                  .appModule(new AppModule(app))
                                                                  .stubSystemModule(new StubSystemModule(stubPkgMgr))
                                                                  .build();
        app.setAppComponent(testAppComponent);


        // - set the appropriate dictionary package to be used under test
        DICT_PACKAGE_NAME_FOR_TEST = InstrumentedStubPackageMangerBuilder.RI_LIST_ALL.get(1).activityInfo.packageName;
        DictionaryOnCopyService.SettingsModel settings =
                new DictionaryOnCopyService.SettingsModel(InstrumentationRegistry.getTargetContext());
        sDictPackageNameOrig = settings.getPackageName();
        settings.setPackageName(DICT_PACKAGE_NAME_FOR_TEST);
        DICT_ACTION_FOR_TEST = settings.getAction();

        // - intercept dictionary activity to be launched (to verify it)
        DictionaryOnCopyService.sIntentLauncherForTest = sMockDictionaryLauncher;
    }

    @After
    public void tearDownTestDependency() {
        DictionaryOnCopyService.SettingsModel settings =
                new DictionaryOnCopyService.SettingsModel(InstrumentationRegistry.getTargetContext());
        settings.setPackageName(sDictPackageNameOrig);

        DictionaryOnCopyService.sIntentLauncherForTest = null;
    }

    private final ClipboardHelper mClipboardHelper = new ClipboardHelper();
    private final DictionaryOnCopyServiceHelper mServiceHelper = new DictionaryOnCopyServiceHelper();

    @Test
    public void testAverageCaseWithLifeCycles() throws Throwable {

        assertEquals(false, DictionaryOnCopyService.isRunning());

        // Test: service starts, ready to launch upon listening to clipboard
        //
        mServiceHelper.startForeground();
        assertEquals(true, DictionaryOnCopyService.isRunning()); // verify isRunning helper is working

        mClipboardHelper.setText("orange");
        assertAndReset(sMockDictionaryLauncher, "orange");

        // Test: service pauses
        //
        mServiceHelper.pause();
        mClipboardHelper.setText("banana");
        assertAndReset(sMockDictionaryLauncher, null);

        // Test: service resumes
        //
        mServiceHelper.resume();
        mClipboardHelper.setText("melon");
        assertAndReset(sMockDictionaryLauncher, "melon");

        // Test: service stops
        //
        mServiceHelper.stopForeground();
        mClipboardHelper.setText("grape");
        assertAndReset(sMockDictionaryLauncher, null);

        assertEquals(false, DictionaryOnCopyService.isRunning()); // verify isRunning helper is working

    }

}
