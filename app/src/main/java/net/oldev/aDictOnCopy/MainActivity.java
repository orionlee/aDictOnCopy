package net.oldev.aDictOnCopy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DictionaryOnCopyService.SettingsModel mSettings;
    private DictionaryChooser mChooser;


    private void bindModelToUI() {
        mSettings.setOnChangeListener(new DictionaryOnCopyService.SettingsModel.ChangeListener() {
            @Override
            public void onChange(String newPackageName) {
                DictionaryManager.DictChoiceItem item = mChooser.getManager().getInfoOfPackage(newPackageName);
                if (item != null) {
                    final TextView selectDictOutput = (TextView)findViewById(R.id.dictSelectOutput);
                    selectDictOutput.setText(item.getLabel());
                } else {
                    String warnMsg = String.format("MainActivity: Dictionary Package in settings <%s> not found. Perhaps it is uninstalled.",
                            newPackageName);
                    PLog.w(warnMsg);
                    Toast.makeText(MainActivity.this, getString(R.string.err_msgf_selected_dict_not_found, newPackageName), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new DictionaryOnCopyService.SettingsModel(this);
        mChooser = new DictionaryChooser(MainActivity.this, mSettings.getAction());

        // Now setup the UI
        setContentView(R.layout.activity_main);

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

        bindModelToUI();

        // Case initial installation: auto set a dictionary if available
        if (mSettings.getPackageName() == null) {
            autoSetDefaultDictionary(mChooser);
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
        mSettings.setPackageName(item.getPackageName().toString());
    }

    private void autoSetDefaultDictionary(DictionaryChooser chooser) {
        PLog.d("autoSetDefaultDictionary(): auto select a dictionary to use (case initial installation).");
        List<DictionaryManager.DictChoiceItem> dictChoiceItems = chooser.getManager().getAvailableDictionaries();
        if (dictChoiceItems.size() > 0) {
            // Just pick the first one
            DictionaryManager.DictChoiceItem item = dictChoiceItems.get(0);
            setDictionaryToUse(item);

        } else {
            Toast.makeText(this, R.string.err_msg_dict_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}
