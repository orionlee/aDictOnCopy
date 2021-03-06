package net.oldev.aDictOnCopy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.oldev.aDictOnCopy.databinding.ActivityMainBinding;

import java.util.List;

import javax.inject.Inject;

public class MainActivity extends Activity {

    /**
     * Semantically decorates DictionaryOnCopyService.SettingsModel,
     * and provides additional UI-specific functionalities:
     * - provides user-facing package display name
     * - interfaces with Android Data Binding
     *
     */
    public static class SettingsUIModel extends BaseObservable {

        public static final int ERR_NO_DICT_AVAILABLE = 1;
        public static final int ERR_SELECTED_DICT_NOT_FOUND = 2;


        private @NonNull final DictionaryOnCopyService.SettingsModel mRealSettings; // non-final for injection during testing
        @SuppressWarnings("NullableProblems")
        private @NonNull DictionaryManager mDictMgr; // final upon init()
        @SuppressWarnings("NullableProblems")
        private @NonNull String mDictSelectionLabel; // final upon init()
        private int mErrorCode = -1;

        //
        // Methods that wrap around DictionaryOnCopyService.SettingsModel
        // are private scope because they are only needed within parent MainActivity
        //
        // The public methods are those exposed (to layouts) via Android Data Binding.
        //
        SettingsUIModel(@NonNull DictionaryOnCopyService.SettingsModel realSettings) {
            mRealSettings = realSettings;
        }

        public void init(@NonNull DictionaryManager dictMgr,
                         @NonNull String dictSelectionLabel) {
            mDictMgr = dictMgr;
            mDictSelectionLabel = dictSelectionLabel;

            // Case initial installation: auto set a dictionary if available
            if (getPackageName() == null) {
                autoSetDefaultDictionary();
            }
        }


        @NonNull
        String getAction() {
            return mRealSettings.getAction();
        }

        @Nullable
        String getPackageName() {
            return mRealSettings.getPackageName();
        }

        void setPackageName(String packageName) {
            mRealSettings.setPackageName(packageName);
            notifyPropertyChanged(BR.packageDisplayName);
        }

        /**
         * Encapsulates conversion logic from internal packageName to user-facing one.
         *
         * Functionally, it is similar to Android Data Binding's @BindingAdapter static methods,
         * except this conversion is done specific for packageName (textual conversion),
         * rather than generic attributes in Android Data Binding (primarily for type conversion).
         *
         * @return user-facing name for packageName
         */
        @Bindable
        public @NonNull CharSequence getPackageDisplayName() {

            final String newPackageName = getPackageName();
            if (newPackageName == null) {
                // case auto set default does not yield any possible dictionary
                // use the default selection label
                setErrorCode(ERR_NO_DICT_AVAILABLE);
                return mDictSelectionLabel;
            }

            DictionaryManager.DictChoiceItem item =
                    mDictMgr.getInfoOfPackage(newPackageName);
            if (item != null) {
                setErrorCode(-1); // in case it was in error previously.
                return item.getLabel();
            } else {
                String warnMsg = String.format("MainActivity: Dictionary Package in settings <%s> not found. Perhaps it is uninstalled.",
                        newPackageName);
                PLog.w(warnMsg);
                setErrorCode(ERR_SELECTED_DICT_NOT_FOUND);
                return mDictSelectionLabel;
            }
        }

        @Bindable
        public int getErrorCode() {
            return mErrorCode;
        }

        @Bindable
        public boolean isInError() {
            return getErrorCode() >= 0;
        }

        private void setErrorCode(int errorCode) {
            mErrorCode = errorCode;
            notifyPropertyChanged(BR.errorCode);
            notifyPropertyChanged(BR.inError);
        }

        private void autoSetDefaultDictionary() {
            PLog.d("autoSetDefaultDictionary(): auto select a dictionary to use (case initial installation).");
            List<DictionaryManager.DictChoiceItem> dictChoiceItems = mDictMgr.getAvailableDictionaries();
            if (dictChoiceItems.size() > 0) {
                // Just pick the first one
                DictionaryManager.DictChoiceItem item = dictChoiceItems.get(0);
                setPackageName(item.getPackageName().toString());

            } else {
                setPackageName(null);
            }
        }

    }

    private SettingsUIModel mSettings;
    private DictionaryChooser mChooser;

    /*
     * Allows externally-injected PackageManager, rather than relying on
     * <code>getApplicationContext().getPackageManager</code>, so that it can be stubbed.
     */
    @SuppressWarnings("CanBeFinal")
    @Inject
    PackageManager mPackageManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryOnCopyApp.from(this).getAppComponent().inject(this);

        // Non-UI initialization
        // - the bare minimum initialization needed for the code path of
        //   starting DictionaryOnCopyService without launching the full UI
        //   This path reduces the memory usage
        final DictionaryOnCopyService.SettingsModel serviceSettings =
                new DictionaryOnCopyService.SettingsModel(this.getApplicationContext());

        final DictionaryManager dictMgr = new DictionaryManager(mPackageManager,
                                                          DictionaryManager.INTENT_FACTORY_DEFAULT,
                                                          serviceSettings.getAction());
        if (hasValidDictionaryPackageSpecifiedInModel(serviceSettings, dictMgr) &&
                !DictionaryOnCopyService.isRunning()) {
            PLog.d("Starting the service and exit right away without showing the UI, minimizing memory usage in average case.");
            startServiceAndFinish();
            return;
        }
        // else continue to start the UI. For cases:
        //   1. first launch after install
        //   2. the dictionary specified is not valid anymore (edge case)
        //   3. the service is already running. Show the UI to allow users to change settings.

        // Finally, UI initialization
        //

        // SettingsUIModel and DictionaryChooser (actually the underlying DictionaryManager)
        // have circular dependency.
        // Solution for now is to defer SettingsUIModel's bulk of initialization to
        // a separate init() method.
        // It also has the advantage that it allows additional UI logic (indirectly via bindings) to listen to
        // changes during actual initialization (see below)
        mSettings = new SettingsUIModel(serviceSettings);
        mChooser = new DictionaryChooser(this, dictMgr);

        // Now setup the UI
        final ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setHandler(this);
        binding.setSettings(mSettings);

        // Init *MUST* be done after the settings id bound to data binding.
        mSettings.init(dictMgr, getString(R.string.dict_selection_label));


        // Let the main activity acts as a convenient shortcut to stop the service as well
        if (DictionaryOnCopyService.isRunning()) {
            DictionaryOnCopyService.stopForeground(getApplicationContext());
        }

    }

    // Used by binding
    public @NonNull String modelErrorCodeToMessage(SettingsUIModel settings) {
        final int errorCode = settings.getErrorCode();

        if (errorCode < 0 ) {
            return ""; // no error at all
        }
        switch (errorCode) {
            case SettingsUIModel.ERR_NO_DICT_AVAILABLE:
                // case no dictionary is available at all (even after init)
                return getString(R.string.err_msg_no_dict_available);
            case SettingsUIModel.ERR_SELECTED_DICT_NOT_FOUND:
                return  getString(R.string.err_msgf_selected_dict_not_found, settings.getPackageName());
            default:
                PLog.w("Unexpected error codes from PackageDisplayNameErrorListener#onError(): " + errorCode);
                return "";
        }
    }

    // Used by binding
    @SuppressWarnings("WeakerAccess")
    public void startServiceAndFinish() {
        DictionaryOnCopyService.startForeground(getApplicationContext());
        finish();
    }

    // Used by binding
    public void promptUserToChooseDictionary() {
        mChooser.prompt(new DictionaryChooser.OnSelectedListener() {
            @Override
            public void onSelected(DictionaryManager.DictChoiceItem item) {
                setDictionaryToUse(item);
                promptUserToStartService();
            }
        });
    }

    private void promptUserToStartService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.prompt_start_service_title)
                .setMessage(R.string.prompt_start_service_msg)
                .setPositiveButton(R.string.yes_btn_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startServiceAndFinish();
                    }
                })
                .setNegativeButton(R.string.no_btn_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
        builder.create().show();
    }

    private void setDictionaryToUse(DictionaryManager.DictChoiceItem item) {
        final String packageName = ( item != null ? item.getPackageName().toString() : null );
        mSettings.setPackageName(packageName);
    }

    /**
     * Non-UI helper to check if there is a valid (installed) dictionary package
     * specified in the backend model.
     */
    private static boolean hasValidDictionaryPackageSpecifiedInModel(
            DictionaryOnCopyService.SettingsModel serviceSettings,
            DictionaryManager dictMgr) {
        final String pkgName = serviceSettings.getPackageName();
        return pkgName!= null &&
                dictMgr.getInfoOfPackage(pkgName) != null;
    }

}
