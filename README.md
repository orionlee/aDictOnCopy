# Dictionary On Copy 
- Launch a Dictionary App when a word is copied to clipboard.

Launcher Icons source:
- Christopher Schreiner https://materialdesignicons.com/

## TODOs
- [x] A button to start the service / app itself as a shortcut to stop
- [x] Log service cycle in details
- [x] Review log statements and level
- [x] Add pause/resume on notification bar
- [x] Allow users to specify dictionary to use.
- [x] Bug fixes
  - [x] Livio's dictionaries do not support colordict action well.
  - [x] Clipboard Data null handling
- [x] Misc. UI cleanup
  - [x] UI layout / style in todos
  - [x] Notification title (using app name is redundant)
  - [x] Prompt users to start service after selecting a dictionary
- [x] i18N
- [ ] More info on supported dictionaries, or broaden the list of supported dictionaries (with other more generic intent action)
- [ ] Testing 
  - [x] complete various scenarios in androidTest
  - [x] refactor so that StubPackageManger can be injected before Activity is created (hence used in onCreate())
  - [x] create a PackageManagerLite interface so that I can create a stub without using mock (mock is slow on devices)
  - [x] setup code coverage, for both test and androidTest
  - [x] service androidTest mainly on life cycle
  - [x] service androidTest on clip handling logic
  - [x] (Abandoned, too much android dependency requiring functioning stub of Intent, ClipData, its dependency, etc.)service unitTest on the details of the logic.
  - [ ] `MAYBE` create a minimal androidTest that only tests bindings (require inject a mock UI Model)
- [x] Consider to adopt data binding (which could help to move activity logic outside to a standalone UI Model for testing)  
  - [x] initial pilot
  - [x] Make UI Model unit-testable
  - [x] binding listeners too, reducing boilerplate codes
  - [x] incorporate to master
- [ ] tweak styles
  - [ ] static members
  - [ ] test methods name using _ to differeniate cases
  - [ ] arrange members to consistent layout (keep public interface?!)
  - [ ] fix android studio config for the above: import  android platform style xml?! 
- [ ] Refactor
  - [x] use Dagger2 : make the Intent / PackageManager injection (for tests)  cleaner
  