package net.oldev.aDictOnCopy.di;

import net.oldev.aDictOnCopy.DictionaryOnCopyService;
import net.oldev.aDictOnCopy.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, SystemModule.class} )
public interface AppComponent {

    void inject(DictionaryOnCopyService dictionaryOnCopyService);
    void inject(MainActivity mainActivity);

}
