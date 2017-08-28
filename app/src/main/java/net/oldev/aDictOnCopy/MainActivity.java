package net.oldev.aDictOnCopy;

import android.app.Application;
import android.content.DialogInterface;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import net.oldev.aDictOnCopy.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * Semantically decorates DictionaryOnCopyService.SettingsModel,
     * and provides additional UI-specific functionalities:
     * - provides user-facing package display name
     * - interfaces with Android Data Binding
     *
     */
    public static class SettingsUIModel extends BaseObservable {

        public static interface PackageDisplayNameErrorListener {
            public static final int ERR_NO_DICT_AVAILABLE = 1;
            public static final int ERR_SELECTED_DICT_NOT_FOUND = 2;

            void onError(int errorCode, SettingsUIModel settings);
        }

        private final @NonNull DictionaryOnCopyService.SettingsModel mRealSettings;
        private @NonNull DictionaryManager mDictMgr; // final upon init()
        private @NonNull String mDictSelectionLabel; // final upon init()

        /**
         * Hook to allow UI to peform anything extra (e.g., error toasts) when
         * package display name to be displayed is in an error condition.
         */
        private @NonNull PackageDisplayNameErrorListener mPackageDisplayNameErrorListener; // final upon init()

        //
        // Methods that wrap around DictionaryOnCopyService.SettingsModel
        // are private scope because they are only needed within parent MainActivity
        //
        // The public methods are those exposed (to layouts) via Android Data Binding.
        //
        private SettingsUIModel(@NonNull Application app) {
            mRealSettings = new DictionaryOnCopyService.SettingsModel(app.getApplicationContext());
        }

        public void init(@NonNull DictionaryManager dictMgr,
                         @NonNull String dictSelectionLabel,
                         @NonNull PackageDisplayNameErrorListener packageDisplayNameErrorListener) {
            mDictMgr = dictMgr;
            mDictSelectionLabel = dictSelectionLabel;
            mPackageDisplayNameErrorListener = packageDisplayNameErrorListener;

            // Case initial installation: auto set a dictionary if available
            if (getPackageName() == null) {
                autoSetDefaultDictionary();
            }
        }


        @NonNull
        private String getAction() {
            return mRealSettings.getAction();
        }

        @Nullable
        private String getPackageName() {
            return mRealSettings.getPackageName();
        }

        private void setPackageName(String packageName) {
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
                mPackageDisplayNameErrorListener.onError(PackageDisplayNameErrorListener.ERR_NO_DICT_AVAILABLE, this);
                return mDictSelectionLabel;
            }

            DictionaryManager.DictChoiceItem item =
                    mDictMgr.getInfoOfPackage(newPackageName);
            if (item != null) {
                return item.getLabel();
            } else {
                String warnMsg = String.format("MainActivity: Dictionary Package in settings <%s> not found. Perhaps it is uninstalled.",
                        newPackageName);
                PLog.w(warnMsg);
                mPackageDisplayNameErrorListener.onError(PackageDisplayNameErrorListener.ERR_SELECTED_DICT_NOT_FOUND, this);
                return mDictSelectionLabel;
            }
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SettingsUIModel and DictionaryChooser (actually the underlying DictionaryManager)
        // have circular dependency.
        // Solution for now is to defer SettingsUIModel's bulk of initialization to
        // a separate init() method.
        // It also has the advantage that it allows additional UI logic to listen to
        // changes during actual initialization (see below)
        mSettings = new SettingsUIModel(this.getApplication());
        mChooser = new DictionaryChooser(MainActivity.this, mSettings.getAction());

        // Now setup the UI
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setSettings(mSettings);

        final String dictSelectionLabel = getString(R.string.dict_selection_label);


        mSettings.init(mChooser.getManager(), dictSelectionLabel, new SettingsUIModel.PackageDisplayNameErrorListener() {
            @Override
            public void onError(int errorCode, SettingsUIModel settings) {
                switch (errorCode) {
                    case SettingsUIModel.PackageDisplayNameErrorListener.ERR_NO_DICT_AVAILABLE:
                        // case no dictionary is available at all (even after init)
                        Toast.makeText(MainActivity.this, R.string.err_msg_dict_not_found, Toast.LENGTH_LONG).show();
                        break;
                    case SettingsUIModel.PackageDisplayNameErrorListener.ERR_SELECTED_DICT_NOT_FOUND:
                        Toast.makeText(MainActivity.this,
                                       getString(R.string.err_msgf_selected_dict_not_found, settings.getPackageName()),
                                       Toast.LENGTH_LONG)
                             .show();
                        break;
                    default:
                        PLog.w("Unexpected error codes from PackageDisplayNameErrorListener#onError(): " + errorCode);
                }
            }
        });

        View startCtl = findViewById(R.id.startCtl);
        startCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServiceAndFinish();
            }
        });

        final View selectDictCtl = findViewById(R.id.dictSelectCtl);
        selectDictCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChooser.prompt(new DictionaryChooser.OnSelectedListener() {
                    @Override
                    public void onSelected(DictionaryManager.DictChoiceItem item) {
                        setDictionaryToUse(item);
                        promptUserToStartService();
                    }
                });
            }
        });


        // Let the main activity acts as a convenient shortcut to stop the service as well
        if (DictionaryOnCopyService.isRunning()) {
            DictionaryOnCopyService.stopForeground(getApplicationContext());
        }

    }

    private void startServiceAndFinish() {
        DictionaryOnCopyService.startForeground(getApplicationContext());
        MainActivity.this.finish();
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
