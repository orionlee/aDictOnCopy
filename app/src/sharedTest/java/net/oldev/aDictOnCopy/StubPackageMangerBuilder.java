package net.oldev.aDictOnCopy;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.test.mock.MockPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


class StubPackageMangerBuilder {

    static List<ResolveInfo> RI_LIST_ALL; // to be initialized once.

    /**
     * Constants to refer to the dictionary packages defined in
     * #RI_LIST_ALL
     *
     * livio package has special handling so it is helpful to state clearly
     * whether one uses livio or not.
     */
    static int IDX_LIVIO; // to be initialized once.
    static int IDX_SOME_DICT; // to be initialized once.

    private final int mNumDictAvailable;

    public StubPackageMangerBuilder(int numDictAvailable) {
        if (RI_LIST_ALL == null) {
            RI_LIST_ALL = buildRiListAll();
        }

        if (numDictAvailable > RI_LIST_ALL.size()) {
            throw new IllegalArgumentException(String.format("numDictAvailable <%s> is larger than max <%s>",
                                                             numDictAvailable, RI_LIST_ALL.size()));
        }
        mNumDictAvailable = numDictAvailable;
    }

    @SuppressWarnings("deprecation")
    private static class StubPackageManager extends MockPackageManager {
        private final List<ResolveInfo> mRiList;

        public StubPackageManager(List<ResolveInfo> riListAll, int numDictAvailable) {
            super();
            List<ResolveInfo> riList = new ArrayList<>();
            for(int i = 0; i < numDictAvailable; i++) {
                final ResolveInfo ri = riListAll.get(i);
                riList.add(ri);
            }
            mRiList = Collections.unmodifiableList(riList);
        }

        @Override
        public ResolveInfo resolveActivity(Intent intent, int flags) {
            if (isDictionaryAction(intent) && flags == PackageManager.MATCH_DEFAULT_ONLY) {
                for (ResolveInfo ri : mRiList) {
                    if (ri.activityInfo.packageName.equals(intent.getPackage())) {
                        return ri;
                    }
                    // else continue to check the next candidate.
                }
                return null; // none found
            } else {
                return null;
            }
        }

        @Override
        public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
            if (isDictionaryAction(intent) && flags == PackageManager.MATCH_DEFAULT_ONLY) {
                return mRiList;
            } else {
                return new ArrayList<>();
            }
        }


        private static boolean isDictionaryAction(Intent intent) {
            if (intent == null) {
                return false;
            }

            switch(intent.getAction()) {
                case "colordict.intent.action.SEARCH":
                case Intent.ACTION_SEARCH:
                    return true;
                default:
                    return false;
            }
        }

    }

    public PackageManager build() {
        return new StubPackageManager(RI_LIST_ALL, mNumDictAvailable);
    }


    /*
     * This is a non-static method so that it can use subclass-specific #mockResolveInfo
     */
    private List<ResolveInfo> buildRiListAll() {
        List<ResolveInfo> riListAll = new ArrayList<>();

        for (Object[] riData : riListAllData()) {
            riListAll.add(mockResolveInfo((String)riData[0], // packageName
                                          (String)riData[1], // label
                                          (Integer)riData[2] // icon resource ID
            ));
        }

        // Define the constants for test's readability
        // MUST be consistent with what #riListAllData() return
        IDX_LIVIO = 0;
        IDX_SOME_DICT = 1;

        return Collections.unmodifiableList(riListAll);
    }

    /**
     * The data to be used to define the default #RI_LIST_ALL
     * Note: if the data is changed, please check the related class / methods
     *
     * @see #buildRiListAll
     * @see androidTest's subclass  StubPackageManagerBuilder#riListAllData
     */
    List<Object[]> riListAllData() {
        // Icon resource IDs not applicable in unit test environment
        // hence -1
        // Subclass for androidTest would override the method and supply
        // proper icon resource IDs
        return Arrays.asList(new Object[][] {
                {"livio.pack.lang.en_US.mock", "English (Mock)", -1},
                {"com.socialnmobile.colordict.mock", "ColorDict (Mock)", -1}
                             });
    }



    static class StubResolveInfo extends ResolveInfo {
        private final @NonNull String mLabel;
        final int mIconIdIfAvailable;

        public StubResolveInfo(@NonNull String packageName, @NonNull String label, int iconIdIfAvailable) {
            super();
            ActivityInfo ai = new ActivityInfo();
            ai.packageName = packageName;
            this.activityInfo = ai;

            mLabel = label;
            mIconIdIfAvailable = iconIdIfAvailable;
        }

        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return mLabel;
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return null;
        }
    }

    ResolveInfo mockResolveInfo(String packageName, String label, int iconIdIfAvailable) {
        return new StubResolveInfo(packageName, label, iconIdIfAvailable);
    }

}
