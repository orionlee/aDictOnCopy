package net.oldev.aDictOnCopy.di;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, StubSystemModule.class})
public interface TestAppComponent extends AppComponent {
}
