package net.oldev.aDictOnCopy;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DictionaryOnCopyService.SettingsModel mSettings;
    private DictionaryChooser mChooser;


    private void bindModelToUI() {
        mSettings.setOnChangeListener(new DictionaryOnCopyService.SettingsModel.ChangeListener() {
            @Override
            public void onChange(String newPackageName) {
                DictionaryChooser.DictChoiceItem item = mChooser.getInfoOfPackage(newPackageName);
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
                    public void onSelected(DictionaryChooser.DictChoiceItem item) {
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

    private void setDictionaryToUse(DictionaryChooser.DictChoiceItem item) {
        mSettings.setPackageName(item.getPackageName().toString());
    }

    private void autoSetDefaultDictionary(DictionaryChooser chooser) {
        PLog.d("autoSetDefaultDictionary(): auto select a dictionary to use (case initial installation).");
        List<DictionaryChooser.DictChoiceItem> dictChoiceItems = chooser.getAvailableDictionaries();
        if (dictChoiceItems.size() > 0) {
            // Just pick the first one
            DictionaryChooser.DictChoiceItem item = dictChoiceItems.get(0);
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

class DictionaryChooser {

    public static interface OnSelectedListener {
        void onSelected(DictChoiceItem item);
    }

    private final Activity mCtx;
    private final String mAction; // action string to be used to launch a dictionary service

    public DictionaryChooser(@NonNull Activity ctx, @NonNull String action) {
        mCtx = ctx;
        mAction = action;
    }

    public void prompt(final OnSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
        builder.setTitle(R.string.prompt_select_dict_title);

        final List<DictChoiceItem> choices = getAvailableDictionaries();
        builder.setAdapter(new DictChoicesAdapter(choices), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position
                // of the selected item
                ///dbgMsg(choices.get(which).getPackageName() + " is selected");
                PLog.v("DictionaryChooser.prompt(): <%s> is selected.", choices.get(which).getPackageName());
                listener.onSelected(choices.get(which));
            }
        });

        builder.create().show();
    }

    public DictChoiceItem getInfoOfPackage(String packageName) {
        Intent intent = new Intent(mAction);
        intent.setPackage(packageName);
        intent.putExtra(SearchManager.QUERY, "test");

        ResolveInfo ri = mCtx.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return ( (ri != null) ? toDictChoiceItem(mCtx, ri) : null);

    }

    public List<DictChoiceItem> getAvailableDictionaries() {
        Intent intent = new Intent(mAction);
        intent.putExtra(SearchManager.QUERY, "test");
        List<ResolveInfo> lri = mCtx.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        ArrayList<DictChoiceItem> items = new ArrayList<DictChoiceItem>(lri.size());
        for (int i = 0; i < lri.size(); i++) {
            items.add(toDictChoiceItem(mCtx, lri.get(i)));
        }
        return items;
    }

    private static @NonNull DictChoiceItem toDictChoiceItem(@NonNull Context ctx, @NonNull ResolveInfo ri) {
        PackageManager pm = ctx.getPackageManager();
        return new DictChoiceItem(ri.activityInfo.applicationInfo.packageName,
                ri.loadLabel(pm),
                ri.loadIcon(pm));
    }

    public static class DictChoiceItem {
        private CharSequence packageName;
        private CharSequence label;
        private Drawable icon;

        public DictChoiceItem(CharSequence packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }

        public CharSequence getPackageName() {
            return packageName;
        }

        public CharSequence getLabel() {
            return label;
        }

        public Drawable getIcon() {
            return icon;
        }
    }

    private class DictChoicesAdapter extends BaseAdapter {

        private final List<DictChoiceItem> mChoices;

        public DictChoicesAdapter(List<DictChoiceItem> choices) {
            mChoices = choices;
        }

        @Override
        public int getCount() {
            return mChoices.size();
        }

        @Override
        public DictChoiceItem getItem(int position) {
            return mChoices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getPackageName().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = mCtx.getLayoutInflater().inflate(R.layout.choiceitem_dict, container, false);
            }
            ((ImageView) convertView.findViewById(R.id.dictChoiceIcon))
                    .setImageDrawable(getItem(position).getIcon());

            ((TextView) convertView.findViewById(R.id.dictChoiceLabel))
                    .setText(getItem(position).getLabel());
            return convertView;
        }
    }


    private void dbgMsg(String msg) {
        if (BuildConfig.DEBUG) {
            android.widget.Toast.makeText(mCtx.getApplicationContext(), msg,
                    Toast.LENGTH_SHORT).show();
        }
    }
}