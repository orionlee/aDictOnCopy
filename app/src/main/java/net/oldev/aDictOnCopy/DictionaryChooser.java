package net.oldev.aDictOnCopy;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DictionaryChooser {

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

}