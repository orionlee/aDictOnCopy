package net.oldev.aDictOnCopy;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import java.util.List;

/**
 * Extends base StubPackageBuilder to take advantage of androidTest environment
 */
class InstrumentedStubPackageMangerBuilder extends StubPackageMangerBuilder {

    public InstrumentedStubPackageMangerBuilder(int numDictAvailable) {
        super(numDictAvailable);
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

    /**
     * Supplies #RI_LIST_ALL with functioning stub icons
     */
    @Override
    List<Object[]> riListAllData() {
        List<Object[]> data = super.riListAllData();

        if (data.size() != 2) {
            throw new AssertionError("The methods assumes superclass's implementation has 2 elements. Actual:" +
                                             data.size());
        }

        data.get(0)[2] = net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_livio;
        data.get(1)[2] = net.oldev.aDictOnCopy.debug.test.R.mipmap.ic_mock_colordict;

        return data;
    }


}
