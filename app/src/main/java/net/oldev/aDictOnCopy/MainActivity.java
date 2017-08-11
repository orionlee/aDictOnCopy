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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Now setup the UI
        setContentView(R.layout.activity_main);

        View startCtl = findViewById(R.id.startCtl);
        startCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DictionaryOnCopyService.startForeground(getApplicationContext());
                MainActivity.this.finish();
            }
        });

        final TextView selectDictCtl = (TextView)findViewById(R.id.dictSelectCtl);
        selectDictCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DictionaryChooser(MainActivity.this).prompt(new DictionaryChooser.OnSelectedListener() {
                    @Override
                    public void onSelected(DictionaryChooser.DictChoiceItem item) {
                        selectDictCtl.setText(item.getLabel());
                        // TODO: store in the backend
                    }
                });
            }
        });

        // Let the main activity acts as a convenient shortcut to stop the service as well
        if (DictionaryOnCopyService.isRunning()) {
            DictionaryOnCopyService.stopForeground(getApplicationContext());
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

    public DictionaryChooser(Activity ctx) {
        mCtx = ctx;
    }

    public void prompt(final OnSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
        builder.setTitle("Select a Dictionary");

        final List<DictChoiceItem> choices = getAvailableDictionaries(mCtx);
        builder.setAdapter(new DictChoicesAdapter(choices), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position
                // of the selected item
                dbgMsg(choices.get(which).getPackageName() + " is selected");
                listener.onSelected(choices.get(which));
            }
        });

        builder.create().show();
    }

    private static List<DictChoiceItem> getAvailableDictionaries(Context ctx) {
        Intent intent = new Intent("colordict.intent.action.SEARCH"); // use color dict limits the packages to a manageable level.
        intent.putExtra(SearchManager.QUERY, "test");
        List<ResolveInfo> lri = ctx.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        ArrayList<DictChoiceItem> items = new ArrayList<DictChoiceItem>(lri.size());
        for (int i = 0; i < lri.size(); i++) {
            items.add(toDictChoiceItem(ctx, lri.get(i)));
        }
        return items;
    }

    private static DictChoiceItem toDictChoiceItem(Context ctx, ResolveInfo ri) {
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