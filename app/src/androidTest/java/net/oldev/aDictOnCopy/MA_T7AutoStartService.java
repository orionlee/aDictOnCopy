package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTest;
import net.oldev.aDictOnCopy.MainActivityTestUtils.ServiceSettingsRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.StubPackageManagerRule;
import net.oldev.aDictOnCopy.MainActivityTestUtils.TestEnv;

import org.junit.ClassRule;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

@MediumTest
public class MA_T7AutoStartService extends BaseTest {
    private static final String PKG_NAME_DICT_TO_PICK =
            StubPackageMangerBuilder.RI_LIST_ALL.get(0).activityInfo.packageName;

    // Define test-specific stubs / settings
    @ClassRule
    public static final StubPackageManagerRule mStubPackageManagerRule;
    @ClassRule
    public static final ServiceSettingsRule mServiceSettingsRule;
    static {
        TestEnv testEnv = createTestEnv(2, PKG_NAME_DICT_TO_PICK);
        mStubPackageManagerRule = testEnv.stubPackageManagerRule;
        mServiceSettingsRule = testEnv.serviceSettingsRule;
    }

    @Test
    public void t7AutoStartService() {
        // Test: A valid dictionary package has been specified (in TestEnv)
        // the activity should start the service and finish immediately

        assertTrue("The activity should auto start the service.",
                    DictionaryOnCopyService.isRunning());

        assertTrue("The activity should have finished upon autostarting the service.",
                   mActivityTestRule.getActivity().isFinishing());
    }
}
