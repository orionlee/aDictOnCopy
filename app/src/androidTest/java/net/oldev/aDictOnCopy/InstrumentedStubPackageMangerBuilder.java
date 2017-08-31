package net.oldev.aDictOnCopy;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

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

}
