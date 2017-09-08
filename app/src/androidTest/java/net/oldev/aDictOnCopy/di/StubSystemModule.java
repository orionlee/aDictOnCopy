package net.oldev.aDictOnCopy.di;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import net.oldev.aDictOnCopy.DictionaryOnCopyService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class StubSystemModule {

    private final @NonNull PackageManager mPackageManager;
    private final @NonNull DictionaryOnCopyService.IntentLauncher mIntentLauncher;

    public StubSystemModule(@NonNull PackageManager packageManager,
                            @NonNull DictionaryOnCopyService.IntentLauncher intentLauncher) {
        mPackageManager = packageManager;
        mIntentLauncher = intentLauncher;
    }

    @Provides
    @Singleton
    PackageManager providePackageManager(@SuppressWarnings("UnusedParameters") Context context) {
        return mPackageManager;
    }

    @Provides
    @Singleton
    DictionaryOnCopyService.IntentLauncher provideIntentLauncher() {
        return mIntentLauncher;
    }


}
