package net.oldev.aDictOnCopy.di;

import android.content.Context;

import net.oldev.aDictOnCopy.DictionaryOnCopyApp;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private final DictionaryOnCopyApp mApplication;
    public AppModule(DictionaryOnCopyApp application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return mApplication;
    }

}
