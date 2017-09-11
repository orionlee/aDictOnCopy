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

import static org.junit.Assert.assertTrue;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class MA_T5TypicalCase_pressYesButton extends BaseTest {
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
    public void t5TypicalCase_pressYesButton() {

        // Test: click dictionary selection and pick one
        // answer *YES* in whether to launch service dialog
        final int IDX_DICT_TO_PICK_IN_T5 = 0;
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T5, true);

        delay(100); // give time for service & activity to complete their action

        assertTrue("The Activity should be finished after the launch service option is clicked in the dialog.",
                   mActivityTestRule.getActivity().isFinishing());

        // Test: confirm the service is launched.
        assertTrue("The dictionary service should have been just launched",
                   DictionaryOnCopyService.isRunning());
    }
}
