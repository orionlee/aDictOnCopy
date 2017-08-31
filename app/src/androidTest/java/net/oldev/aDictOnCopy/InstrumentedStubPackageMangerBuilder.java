package net.oldev.aDictOnCopy;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extends base StubPackageBuilder to take advantage of androidTest environment
 */
class InstrumentedStubPackageMangerBuilder extends StubPackageMangerBuilder {

    public InstrumentedStubPackageMangerBuilder(int numDictAvailable) {
        super(numDictAvailable);
    }

    @Override
    List<ResolveInfo> buildRiListAll() {
        List<ResolveInfo> riListAll = new ArrayList<ResolveInfo>();

        riListAll.add(mockResolveInfo("livio.pack.lang.en_US.mock",
                                      "English (Mock)",
                                      net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_livio));

        riListAll.add(mockResolveInfo("com.socialnmobile.colordict.mock",
                                      "ColorDict (Mock)",
                                      net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_colordict));

        return Collections.unmodifiableList(riListAll);
    }

    private static class InstrumentedStubResolveInfo extends StubResolveInfo {

        public InstrumentedStubResolveInfo(@NonNull String packageName, @NonNull String label, int iconIdIfAvailable) {
            super(packageName, label, iconIdIfAvailable);
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            if (mIconIdIfAvailable > 0) {
                return InstrumentationRegistry.getContext().getResources().getDrawable(mIconIdIfAvailable, null);
            } else {
                return null;
            }
        }

    }

    @Override
    ResolveInfo mockResolveInfo(String packageName, String label, int iconIdIfAvailable) {
        return new InstrumentedStubResolveInfo(packageName, label, iconIdIfAvailable);
    }

    public static void stubDictionariesAvailableInDictionaryManager(int numDictAvailable) {
        final PackageManager stubPkgMgr = new InstrumentedStubPackageMangerBuilder(numDictAvailable).build();

        DictionaryManager.msPackageManagerHolderForTest = new DictionaryManager.PackageManagerHolder() {
            @NonNull
            @Override
            public PackageManager getManager() {
                return stubPkgMgr;
            }
        };
    }

}
