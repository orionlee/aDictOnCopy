package net.oldev.aDictOnCopy.di;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class StubSystemModule {

    private final @NonNull PackageManager mPackageManager;

    public StubSystemModule(@NonNull PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @Provides
    @Singleton
    PackageManager providePackageManager(Context context) {
        return mPackageManager;
    }

}
