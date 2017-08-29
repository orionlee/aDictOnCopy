package net.oldev.aDictOnCopy;

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

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


    private final String mAction; // action string to be used to launch a dictionary service


    @VisibleForTesting final PackageManager mPkgMgr;

    @VisibleForTesting
    static interface PackageManagerHolder {
        @NonNull PackageManager getManager();
    }

    /**
     * Usage: set a custom holder in test statically,
     * so that it can be used before Activity instances are created.
     */
    @VisibleForTesting
    static PackageManagerHolder msPackageManagerHolderForTest  = null;

    public DictionaryManager(@NonNull PackageManager pm, @NonNull String action) {
        mAction = action;
        mPkgMgr = msPackageManagerHolderForTest == null ? pm : msPackageManagerHolderForTest.getManager();
    }

    public @Nullable DictChoiceItem getInfoOfPackage(String packageName) {
        Intent intent = createIntentWithAction(mAction);
        intent.setPackage(packageName);
        intent.putExtra(SearchManager.QUERY, "test");

        ResolveInfo ri = mPkgMgr.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return ( (ri != null) ? toDictChoiceItem(ri) : null);

    }

    public @NonNull List<DictChoiceItem> getAvailableDictionaries() {
        Intent intent = createIntentWithAction(mAction);
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

    private static @NonNull Intent createIntentWithAction(@NonNull String action) {
        return ( msIntentFactoryForTest == null ?
                new Intent(action) :
                msIntentFactoryForTest.withAction(action));
    }

    static interface IntentFactory {
        @NonNull Intent withAction(String action);
    }

    @VisibleForTesting
    static IntentFactory msIntentFactoryForTest = null;

}
