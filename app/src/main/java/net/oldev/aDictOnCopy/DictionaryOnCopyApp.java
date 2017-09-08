package net.oldev.aDictOnCopy;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import net.oldev.aDictOnCopy.di.AppComponent;
import net.oldev.aDictOnCopy.di.AppModule;
import net.oldev.aDictOnCopy.di.DaggerAppComponent;
import net.oldev.aDictOnCopy.di.SystemModule;

public class DictionaryOnCopyApp extends Application {

    public static DictionaryOnCopyApp from(@NonNull Context context) {
        return (DictionaryOnCopyApp) context.getApplicationContext();
    }

    private AppComponent mAppComponent;

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

    @VisibleForTesting
    void setAppComponent(AppComponent appComponent) {
        mAppComponent = appComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAppComponent = initDagger();
    }

    protected AppComponent initDagger() {
        return DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .systemModule(new SystemModule())
                .build();
    }

}
