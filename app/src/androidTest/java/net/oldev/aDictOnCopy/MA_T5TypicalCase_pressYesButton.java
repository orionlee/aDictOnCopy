package net.oldev.aDictOnCopy;

import android.support.test.filters.MediumTest;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

@MediumTest
public class MA_T5TypicalCase_pressYesButton extends MainActivityTestUtils.BaseTestWithTestEnvAsTestRules {
    public MA_T5TypicalCase_pressYesButton() {
        super(2, null); // Define test-specific stubs / settings
    }

    @Test
    public void t5TypicalCase_pressYesButton() {

        // Test: click dictionary selection and pick one
        // answer *YES* in whether to launch service dialog
        final int IDX_DICT_TO_PICK_IN_T5 = 0;
        clickDictSelectCtlAndSelectChoice(IDX_DICT_TO_PICK_IN_T5, true);

        delay(100); // give time for service & activity to complete their action

        assertTrue("The Activity should be finished after the launch service option is clicked in the dialog.",
                   getActivity().isFinishing());

        // Test: confirm the service is launched.
        assertTrue("The dictionary service should have been just launched",
                   DictionaryOnCopyService.isRunning());
    }
}
