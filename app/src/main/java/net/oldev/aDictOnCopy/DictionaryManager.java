package net.oldev.aDictOnCopy;

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A class provides metadata for available dictionaries
 */
public class DictionaryManager {

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

    /**
     * Interfaces for creating new Intent instances
     * It is needed in cases such as unit tests, where
     * simple <code>new Intent()</code> would not work.
     */
    public static interface IntentFactory {
        @NonNull Intent withAction(String action);
    }

    private static class IntentFactoryDefaultImpl implements DictionaryManager.IntentFactory {
        @NonNull
        @Override
        public Intent withAction(String action) {
            return new Intent(action);
        }
    }
    public static IntentFactory INTENT_FACTORY_DEFAULT = new IntentFactoryDefaultImpl();

    private final String mAction; // action string to be used to launch a dictionary service

    private final PackageManager mPkgMgr;
    private final IntentFactory mIntentFactory;

    public DictionaryManager(@NonNull PackageManager pm, @NonNull IntentFactory intentFactory, @NonNull String action) {
        mPkgMgr = pm;
        mIntentFactory = intentFactory;
        mAction = action;
    }

    public @Nullable DictChoiceItem getInfoOfPackage(String packageName) {
        Intent intent = mIntentFactory.withAction(mAction);
        intent.setPackage(packageName);
        intent.putExtra(SearchManager.QUERY, "test");

        ResolveInfo ri = mPkgMgr.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return ( (ri != null) ? toDictChoiceItem(ri) : null);

    }

    public @NonNull List<DictChoiceItem> getAvailableDictionaries() {
        Intent intent = mIntentFactory.withAction(mAction);
        intent.putExtra(SearchManager.QUERY, "test");
        List<ResolveInfo> lri = mPkgMgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        ArrayList<DictChoiceItem> items = new ArrayList<DictChoiceItem>(lri.size());
        for (int i = 0; i < lri.size(); i++) {
            items.add(toDictChoiceItem(lri.get(i)));
        }
        return items;
    }

    private @NonNull DictChoiceItem toDictChoiceItem(@NonNull ResolveInfo ri) {
        return new DictChoiceItem(ri.activityInfo.packageName,
                ri.loadLabel(mPkgMgr),
                ri.loadIcon(mPkgMgr));
    }

}
