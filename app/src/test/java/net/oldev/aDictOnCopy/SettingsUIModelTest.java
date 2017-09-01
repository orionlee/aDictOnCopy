package net.oldev.aDictOnCopy;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.databinding.Observable;
import android.databinding.Observable.OnPropertyChangedCallback;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.oldev.aDictOnCopy.MainActivity.SettingsUIModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SettingsUIModelTest {

    @Test
    public void testAutoSetDefault_noDict() {

        // - default selection, case no dictionary available,
        final SettingsUIModel uiModel = createSettingsUIModelUnderTest(0);

        final MockOnPropertyChangedCallback onPropertyChangedCallback = new MockOnPropertyChangedCallback();
        uiModel.addOnPropertyChangedCallback(onPropertyChangedCallback);

        // calling the non-simple uiModel.getPackageDisplayName()
        // would trigger all logic in validating packageName, error handling, etc.
        final CharSequence packageDisplayName = uiModel.getPackageDisplayName();
        assertEquals("Dictionary Package Display Name should be generic the selection label when there is no dictionary available",
                     DICT_SELECTION_LABEL, packageDisplayName);
        assertEquals("Error code should indicate no dictionary is available",
                     SettingsUIModel.ERR_NO_DICT_AVAILABLE, uiModel.getErrorCode());

        // BR.displayName has not been really changed as the initial one is the generic dict selection label
        verifyPropertyChanged(onPropertyChangedCallback, BR.errorCode, BR.inError);

    }

    @Test
    public void testAutoSetDefault_someDictAvailable() {
        // - default selection, case some available
        final SettingsUIModel uiModel = createSettingsUIModelUnderTest(2);
        assertNotEquals("Dictionary Package Display Name should refer to a package provided by stub package manager",
                        DICT_SELECTION_LABEL, uiModel.getPackageDisplayName());
        assertTrue("There should be no error", uiModel.getErrorCode() < 0);

    }

    @Test
    public void testAveragePackageChange() {
        // - average case, verify package property changes
        // @see https://medium.com/@hiBrianLee/writing-testable-android-mvvm-app-part-4-e2f83fc21d71
        // Difference: replace generic Mockito mocks with custom one. Test time reduced from ~900ms to < 10ms
        final SettingsUIModel uiModel = createSettingsUIModelUnderTest(2);

        final MockOnPropertyChangedCallback onPropertyChangedCallback = new MockOnPropertyChangedCallback();
        uiModel.addOnPropertyChangedCallback(onPropertyChangedCallback);

        final ResolveInfo riToChangeTo =  StubPackageMangerBuilder.RI_LIST_ALL.get(1);


        uiModel.setPackageName(riToChangeTo.activityInfo.packageName);

        verifyPropertyChanged(onPropertyChangedCallback, BR.packageDisplayName);

        assertEquals("Dictionary Package Display Name should be the same as what stub PackageManager provides",
                     riToChangeTo.loadLabel(null), uiModel.getPackageDisplayName());

        assertTrue("There should be no error", !uiModel.isInError());

    }

    @Test
    public void testNegSelectedDictNotFound() {
        final SettingsUIModel uiModel = createSettingsUIModelUnderTest(0);
        uiModel.setPackageName("foo.bar.nonExistentDictPkg");
        // Getting display name for a non-existent package should result in error, as specified in the model under test in the beginning.
        final CharSequence packageDisplayName = uiModel.getPackageDisplayName();
        assertEquals("Error code should indicate selected dictionary (in backend settings) cannot be found.",
                     SettingsUIModel.ERR_SELECTED_DICT_NOT_FOUND, uiModel.getErrorCode());
    }


    private static class MockOnPropertyChangedCallback extends OnPropertyChangedCallback {
        public final List<Integer> propertyIds = new ArrayList<Integer>();

        public MockOnPropertyChangedCallback() {
            super();
        }

        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            propertyIds.add(propertyId);
        }
    }

    /**
     * @see MockOnPropertyChangedCallback
     * @param onPropertyChangedCallback
     * @param propertyIdsExpected
     */
    protected final void verifyPropertyChanged(MockOnPropertyChangedCallback onPropertyChangedCallback, int... propertyIdsExpected) {
        assertEquals("Ensure OnPropertyChangedCallback is called correctly - incorrect number of invocations",
                     propertyIdsExpected.length, onPropertyChangedCallback.propertyIds.size());
        for (int i = 0; i < propertyIdsExpected.length; i++) {
            assertEquals("Ensure OnPropertyChangedCallback is called correctly - incorrect propertyId at invocation <" +
                                 (i+1) + "> ; propertyIdsActual=" + onPropertyChangedCallback.propertyIds,
                         propertyIdsExpected[i], onPropertyChangedCallback.propertyIds.get(i).intValue());
        }
    }


    private static final String DICT_SELECTION_LABEL = "Select dictionary (unit test)...";

    private static class StubIntent extends Intent {
        // The stub needs to hold states, using mock would be more complex

        private String mAction;

        private String mPackage;

        public StubIntent(String action) {
            mAction = action;
        }

        @Override
        public String getAction() {
            return mAction;
        }

        @Override
        public String getPackage() {
            return mPackage;
        }

        @Override
        public Intent setPackage(String packageName) {
            this.mPackage = packageName;
            return this;
        }
    }

    private SettingsUIModel createSettingsUIModelUnderTest(int numDictAvailable) {
        DictionaryManager.IntentFactory stubIntentFactory = new DictionaryManager.IntentFactory() {
            @NonNull
            @Override
            public Intent withAction(String action) {
                return new StubIntent(action);
            }
        };

        final SettingsUIModel uiModel = new SettingsUIModel(stubRealSettings());

        PackageManager stubPkgMgr = new StubPackageMangerBuilder(numDictAvailable).build();
        final DictionaryManager dictMgr = new DictionaryManager(stubPkgMgr, stubIntentFactory, uiModel.getAction());

        uiModel.init(dictMgr, DICT_SELECTION_LABEL);

        return uiModel;
    }


    private static class StubDictSettingsModel extends DictionaryOnCopyService.SettingsModel {
        // The stub needs to hold states, using mock would be more complex

        private String packageName;

        public StubDictSettingsModel() {
            super(null); // stub doesn't care for context
        }

        @Nullable
        @Override
        public String getPackageName() {
            return packageName;
        }

        @Override
        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }
    private static DictionaryOnCopyService.SettingsModel stubRealSettings() {
        return new StubDictSettingsModel();
    }

}
