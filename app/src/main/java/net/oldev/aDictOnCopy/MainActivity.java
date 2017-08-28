package net.oldev.aDictOnCopy;

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
    public class SettingsUIModel extends BaseObservable {
        private final DictionaryOnCopyService.SettingsModel mRealSettings;

        //
        // Methods that wrap around DictionaryOnCopyService.SettingsModel
        // are private scope because they are only needed within parent MainActivity
        //
        // The public methods are those exposed (to layouts) via Android Data Binding.
        //
        private SettingsUIModel() {
            mRealSettings = new DictionaryOnCopyService.SettingsModel(MainActivity.this);
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
            // Conversion relies on parent instance's mChooser member.

            final String newPackageName = getPackageName();
            DictionaryManager.DictChoiceItem item =
                    MainActivity.this.mChooser.getManager().getInfoOfPackage(newPackageName);
            if (item != null) {
                return item.getLabel();
            } else {
                String warnMsg = String.format("MainActivity: Dictionary Package in settings <%s> not found. Perhaps it is uninstalled.",
                        newPackageName);
                PLog.w(warnMsg);
                Toast.makeText(MainActivity.this, getString(R.string.err_msgf_selected_dict_not_found, newPackageName), Toast.LENGTH_LONG).show();
                return MainActivity.this.getString(R.string.dict_selection_label);
            }
        }
    }

    private SettingsUIModel mSettings;
    @VisibleForTesting DictionaryChooser mChooser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new SettingsUIModel();
        mChooser = new DictionaryChooser(MainActivity.this, mSettings.getAction());

        // Now setup the UI
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setSettings(mSettings);

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

        // Case initial installation: auto set a dictionary if available
        if (mSettings.getPackageName() == null) {
            autoSetDefaultDictionary();
        }

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

    @VisibleForTesting
    void autoSetDefaultDictionary() {
        PLog.d("autoSetDefaultDictionary(): auto select a dictionary to use (case initial installation).");
        List<DictionaryManager.DictChoiceItem> dictChoiceItems = mChooser.getManager().getAvailableDictionaries();
        if (dictChoiceItems.size() > 0) {
            // Just pick the first one
            DictionaryManager.DictChoiceItem item = dictChoiceItems.get(0);
            setDictionaryToUse(item);

        } else {
            setDictionaryToUse(null);
            Toast.makeText(this, R.string.err_msg_dict_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}
