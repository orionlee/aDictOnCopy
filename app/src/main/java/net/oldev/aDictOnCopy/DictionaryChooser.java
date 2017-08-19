package net.oldev.aDictOnCopy;


import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Construct UI to choose a dictionary app
 */
public class DictionaryChooser {

    public static interface OnSelectedListener {
        void onSelected(DictionaryManager.DictChoiceItem item);
    }

    private final Activity mCtx;
    @VisibleForTesting final DictionaryManager mDictMgr;

    public DictionaryChooser(@NonNull Activity ctx, @NonNull String action) {
        mCtx = ctx;
        mDictMgr = new DictionaryManager(ctx.getPackageManager(), action);
    }

    public DictionaryManager getManager() {
        return mDictMgr;
    }

    public void prompt(final OnSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
        builder.setTitle(R.string.prompt_select_dict_title);

        final List<DictionaryManager.DictChoiceItem> choices = mDictMgr.getAvailableDictionaries();
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

    private class DictChoicesAdapter extends BaseAdapter {

        private final List<DictionaryManager.DictChoiceItem> mChoices;

        public DictChoicesAdapter(List<DictionaryManager.DictChoiceItem> choices) {
            mChoices = choices;
        }

        @Override
        public int getCount() {
            return mChoices.size();
        }

        @Override
        public DictionaryManager.DictChoiceItem getItem(int position) {
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

