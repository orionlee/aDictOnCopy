package net.oldev.aDictOnCopy.di;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import net.oldev.aDictOnCopy.DictionaryOnCopyService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SystemModule {

    private static class IntentLauncherImpl implements DictionaryOnCopyService.IntentLauncher {
        @Override
        public void start(@NonNull Context context, @NonNull Intent intent) {
            context.startActivity(intent);
        }
    }

    /**
     * The default intent launcher (simply using context.startActivity)
     */
    public static final DictionaryOnCopyService.IntentLauncher INTENT_LAUNCHER = new IntentLauncherImpl();

    @Provides
    @Singleton
    PackageManager providePackageManager(Context context) {
        return context.getPackageManager();
    }

    @Provides
    @Singleton
    DictionaryOnCopyService.IntentLauncher provideIntentLauncher() {
        return INTENT_LAUNCHER;
    }

}
