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
- [x] APK size reduction (by removing the use of AppCompat support lib)
- [x] `NO-ACTION` reduce runtime memory usage (start service without fully initializing MainActivity did not cut down memory usage)
- [x] Add Open Dictionary action to Notification
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
- [ ] tweak code styles
  - [x] static members
  - [x] test methods name using _ to differentiate cases
  - [ ] arrange members to consistent layout (keep public interface?!)
  - [ ] fix android studio config for the above: import  android platform style xml?! 
- [ ] Refactor
  - [x] use Dagger2 : make the Intent / PackageManager injection (for tests)  cleaner
  - [x] (NO-OP. An overkill) consider use dagger-android
  
- [x] Optimize build speed
  - [x] (NO-OP) Use the latest tools
  - [x] (DEFERRED, too small) Create a build variant for development + Avoid compiling unnecessary resources
  - [x] (N/A) Disable Crashlytics for your debug builds
  - [x] (DONE, little improve) Use static build config values with your debug build :
  - [x] (NO-OP) Use static dependency versions
  - [x] (N/A) Enable offline mode
  - [x] (NO-OP) Enable configuration on demand
  - [x] (NO-OP) Create library modules
  - [x] (NO-OP) Create tasks for custom build logic
  - [x] (DONE, no improvement)Configure dexOptions
  - [x] (NO-OP) Increase Gradle's heap size
  - [x] (NO-OP)Convert images to WebP
  - [x] (DEFERRED) Disable PNG crunching
  - [x] (NO-OP) Enable Instant Run
  - [x] (NO-OP) Enable Build Cache
  - [x] (N/A) Disable annotation processors (dagger2 requires annotation processors)
  - [x] (DONE, no change needed) Profile the build
  

## Build Speed Optimization Notes
  
### Static Build Config Values  
| Changes       |  no change |   source   |  unitTest  | noChAnTst  | androidTst |
| ------------- | ---------- | ---------- | ---------- | ---------- | ---------- |  
| Baseline      |  03/44/21  |  09/44/25  |  05/44/25  |  03/65/25  |  05/65/30  |
| StaticBConf   |  03/44/21  |  09/44/25  |  05/44/25  |  03/65/25  |  05/65/30  |

Legends: 
- {elapsed time in seconds} / {# gradle tasks run} / {#tasks not UP-TO-DATE}
- androidTest command line : use task `assembleDebugAndroidTest` 

StaticBConf (Use static build config values):
- Summary: No noticeable difference, but it does clean up build.gradle logic
- No noticeable difference on command line: neither time, nor #tasks not UP-TO-DATE
- No noticeable difference on Android Studio either. Both Baseline and StaticBConf:  
  - can use Instant Run upon changing Activity
  - calls `:app:assembleDebug` to generate full apk anyway (It doesn't install it though)
  - `:app:assembleDebug`  is no op (aka UP-TO-DATE) when there is indeed no code change in main/ source for both cases 
  - Instant Run doesn't apply to running android test either (make it even less useful)  
  
### Dex Options
- setting `preDexLibraries true` does not yield noticeable gain in incremental builds
  isolated tests (adding a private method to a service). Leave it on for now