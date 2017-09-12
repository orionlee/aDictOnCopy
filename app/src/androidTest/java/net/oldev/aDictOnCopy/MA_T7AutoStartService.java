package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import net.oldev.aDictOnCopy.MainActivityTestUtils.BaseTestWithTestEnvAsTestRules;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

@MediumTest
public class MA_T7AutoStartService extends BaseTestWithTestEnvAsTestRules {
    private static final String PKG_NAME_DICT_TO_PICK =
            StubPackageMangerBuilder.RI_LIST_ALL.get(0).activityInfo.packageName;

    public MA_T7AutoStartService() {
        super(2, PKG_NAME_DICT_TO_PICK); // Define test-specific stubs / settings
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
