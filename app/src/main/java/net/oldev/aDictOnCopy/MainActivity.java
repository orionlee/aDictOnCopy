package net.oldev.aDictOnCopy;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import net.oldev.aDictOnCopy.databinding.ActivityMainBinding;

import java.util.List;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

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


        private @NonNull DictionaryOnCopyService.SettingsModel mRealSettings; // non-final for injection during testing
        private @NonNull DictionaryManager mDictMgr; // final upon init()
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
    @VisibleForTesting DictionaryChooser mChooser;

    @Inject
    PackageManager mPackageManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryOnCopyApp.from(this).getAppComponent().inject(this);

        // SettingsUIModel and DictionaryChooser (actually the underlying DictionaryManager)
        // have circular dependency.
        // Solution for now is to defer SettingsUIModel's bulk of initialization to
        // a separate init() method.
        // It also has the advantage that it allows additional UI logic (indirectly via bindings) to listen to
        // changes during actual initialization (see below)
        mSettings = new SettingsUIModel(new DictionaryOnCopyService.SettingsModel(this.getApplicationContext()));
        mChooser = new DictionaryChooser(MainActivity.this, mPackageManager, mSettings.getAction());

        // Now setup the UI
        final ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setHandler(this);
        binding.setSettings(mSettings);

        // Init *MUST* be done after the settings id bound to data binding.
        mSettings.init(mChooser.getManager(), getString(R.string.dict_selection_label));


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
    public void startServiceAndFinish() {
        DictionaryOnCopyService.startForeground(getApplicationContext());
        MainActivity.this.finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}
