package net.oldev.aDictOnCopy;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import java.util.List;

import javax.inject.Inject;


public class DictionaryOnCopyService extends ClipChangedListenerForegroundService {

    //
    // For query service running state (locally within the app)
    //
    private static boolean msRunning = false;
    public static boolean isRunning() {
        return msRunning;
    }

    /**
     * A low-level interface that allows tests to stub Context#startActivity
     * to isolate external dictionary package dependency.
     * The interface exposes Intent so that tests can verify the intent used to launch
     */
    public static interface IntentLauncher {
        void start(Context ctx, @NonNull Intent intent);
    }

    private static class IntentLauncherImpl implements IntentLauncher {
        @Override
        public void start(@NonNull Context context, @NonNull Intent intent) {
            context.startActivity(intent);
        }
    }

    private static IntentLauncher sIntentLauncherDefault = new IntentLauncherImpl();

    @VisibleForTesting
    static IntentLauncher sIntentLauncherForTest = null;


    @Inject
    PackageManager mPackageManager;

    @Override
    public void onCreate() {
        super.onCreate();
        DictionaryOnCopyApp.from(this).getAppComponent().inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int res = super.onStartCommand(intent, flags, startId);
        msRunning = true;
        return res;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        msRunning = false;
    }

    //
    // Implement hooks for foreground service
    //

    private static final int ONGOING_NOTIFICATION_ID = 99;

    @Override
    protected int getOngoingNotificationId() {
        return ONGOING_NOTIFICATION_ID;
    }

    private static final NotificationResources NOTIFICATION_RESOURCES = new NotificationResources() {

        @Override
        public @StringRes int getContentTitle() { return R.string.noti_title; }

        @Override
        public @StringRes int getContentText() { return R.string.noti_msg_touch_to_stop; }

        @Override
        public @DrawableRes int getNotificationSmallIcon() { return R.drawable.dictionary; }

        @Override
        public int getPauseNotificationSmallIcon() { return R.drawable.dictionary_pause; }

        @Override
        public int getPauseActionIcon() { return R.drawable.btn_pause; }

        @Override
        public int getPauseActionText() { return R.string.noti_btn_pause; }

        @Override
        public int getResumeActionIcon() { return R.drawable.btn_resume; }

        @Override
        public int getResumeActionText() { return R.string.noti_btn_resume; }
    };

    @Override
    protected NotificationResources getNotificationResources() {
        return NOTIFICATION_RESOURCES;
    }

    private static final ServiceResources SERVICE_RESOURCES = new ServiceResources() {
        @Override
        public int getDisplayName() { return R.string.app_name; }

        @Override
        public int getStartingServiceTextf() { return R.string.info_msgf_starting_service; }

        @Override
        public int getStoppingServiceTextf() { return R.string.info_msgf_stopping_service; }
    };

    @Override
    protected ServiceResources getServiceResources() {
        return SERVICE_RESOURCES;
    }

    //
    // Implement actual clipboard logic, i.e., ClipChangedListenerService
    //

    @Override
    protected OnPrimaryClipChangedListener createListener() {
        return new OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                performClipboardCheck();
            }
        };
    }

    private void performClipboardCheck() {
        try {
            ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cb.hasPrimaryClip()) {
                ClipData cd = cb.getPrimaryClip();
                ClipDescription desc = cd.getDescription();
                String descText = desc.toString();
                if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                    // Extra verbose debug messages to diagnose why the service (aka listener) is fired twice when
                    // a copy is performed.
                    //
                    // Conclusions:
                    // - it seems to be some android weiredness that when html text is copied
                    //   (tried on browsers and other apps), system clipboard manager fires
                    //    the listener twice (or even 3 times) in a row.
                    // - It can be shown from the debug statement: the current time logged differs by a few ms.
                    //   Furthermore, the oid of the listener instance is the same, suggesting it is not the case
                    //   2 instances of the listener has been registered.
                    // - It does not happen when the text to be copied is plain text
                    // - Tested on 2 devices, ibe with Andriid 7.1 and one with 4.4
                    // - There does not seem to be any workaround, barring some custom logic that tracks the last time
                    //   the listener was fired and prevent it from firing again (with the same text) within a short timespan.
                    //   Given there is no user-visible difference (of firing the dictionary multiple times in a few ms),
                    //   I opt for leaving it alone.
                    //
                    ///descText += String.format(" { oid:[%s] , ts:[%s] , label:[%s] , mimeTypeCount:[%s] , intent:[%s] } ",
                    ///        listener.hashCode(), System.currentTimeMillis(), desc.getLabel(), desc.getMimeTypeCount(), cd.getItemAt(0).getIntent());

                    // For plain text, sometimes using cd.getItemAt(0).getText()).toString() will throw null pointers
                    // simplify the flow using coerceToText
                    CharSequence clipText = cd.getItemAt(0).coerceToText(getApplicationContext());
                    String msg = "<" + descText + "> " + clipText;
                    PLog.v(msg);
                    boolean launched = launchDictionaryIfAWord(clipText);
                    if (!launched) dbgMsg("[Not a word]" + msg);
                } else {
                    String msg = "!<" + descText + "> " + (cd.getItemAt(0).toString());
                    PLog.v(msg);
                    dbgMsg("[Unsupported type]" + msg);
                }
            }
        } catch (Throwable t) {
            // Catch any exception here as a safety net, to prevent the app crashes.
            PLog.e("Unhandled errors in DictionaryOnCopyService.performClipboardCheck().", t);
            toastMsg(getString(R.string.err_msg_service_unexpected_err));
        }
    }

    //
    // Actual logic in launching dictionary
    //

    private static final int MAX_NUM_WORDS_IN_TEXT = 5;
    private static final int MAX_NUM_CHARS_IN_TEXT = 100;
    @VisibleForTesting
    static boolean isAWord(CharSequence text) {
        String textStr = text.toString().trim();
        if (textStr.isEmpty()) { // NO-op for empty string
            return false;
        }

        if (textStr.length() > MAX_NUM_CHARS_IN_TEXT) {
            return false;
        }

        // ignore URIs
        if (textStr.matches("^(https?|file|mailto|tel):.*")) {
            return false;
        }

        // ignore numbers
        if (textStr.matches("^[$0-9.,\\s%/]+$")) {
            return false;
        }

        // ignore text with too many words (e.g., a phrase or a sentence)
        final String[] splitted = textStr.split("\\s+", MAX_NUM_WORDS_IN_TEXT + 1);
        if (splitted.length > MAX_NUM_WORDS_IN_TEXT) {
            return false;
        }

        // Pass all tests. text considered to be a word
        return true;
    }

    private boolean launchDictionaryIfAWord(CharSequence text) {
        if (isAWord(text)) {
            launchDictionary(text);
            return true;
        } else {
            return false;
        }
    }

    private void launchDictionary(CharSequence word) {
        final SettingsModel settings = new SettingsModel(this);
        String dictPkg = settings.getPackageName();
        String action = settings.getAction();
        if (dictPkg.startsWith("livio.pack.lang.") && action != Intent.ACTION_SEARCH) {
            PLog.v("DictionaryOnCopyService.launchDictionary(): Livio-specific workaround for action. package=%s", dictPkg);
            // Livio dictionaries support colordict's action, but somehow it only brings up the app without word lookup.
            // So here I use Livio's default action string
            action = Intent.ACTION_SEARCH;
        }
        Intent intent = new Intent(action);
        intent.setPackage(dictPkg);
        intent.putExtra(SearchManager.QUERY, word);
        // FLAG_ACTIVITY_NO_USER_ACTION is needed to make system back button work, i.e.,
        // after dictionary is launched, pressing back button will go back to the previous app.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        PLog.v("DictionaryOnCopyService.launchDictionary(): word=<%s>, intent=<%s>", word, intent);
        if (isIntentAvailable(this, intent)) { // check if intent is available ?
            IntentLauncher launcher = (sIntentLauncherForTest == null ?
                    sIntentLauncherDefault : sIntentLauncherForTest);
            launcher.start(this, intent);
        } else {
            toastMsg(getString(R.string.err_msgf_service_dict_not_found_at_intent_launch, dictPkg));
        }
    }

    private boolean isIntentAvailable(Context context, Intent intent) {
        List<ResolveInfo> lri = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (lri != null) && (lri.size() > 0);
    }

    private void toastMsg(String msg) {

        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
    }

    private void dbgMsg(String msg) {
        if (BuildConfig.DEBUG) {
            android.widget.Toast.makeText(getApplicationContext(), msg,
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    public static class SettingsModel {

        private static final String PREFERENCES_KEY = "net.oldev.aDictOnCopy";
        private static final String PREFS_PACKAGE_NAME = "dict.packageName";

        private final Context mCtx;

        public SettingsModel(@NonNull Context ctx) {
            mCtx = ctx;
        }


        /**
         *
         * @return the action to be used for the dictionary intent. Constant for now.
         */
        public @NonNull String getAction() {
            // Intent.ACTION_SEARCH would work, but it may be too generic.
            // use color dict intent limits the packages to a manageable level (at UI).
            return "colordict.intent.action.SEARCH";
        }

        public @Nullable String getPackageName() {
            return getPrefs().getString(PREFS_PACKAGE_NAME, null);
        }

        /**
         *
         * @param packageName the package name of the dictionary app to be launched
         */
        public void setPackageName(String packageName) {
            SharedPreferences.Editor editor = getPrefs().edit();
            editor.putString(PREFS_PACKAGE_NAME, packageName);
            // After repeated testing, it is confirmed asynchronous apply() is sufficient.
            // getting packageName in fireChangeEvent() would reliably pick up the new value
            editor.apply();
        }

        private SharedPreferences getPrefs() {
            SharedPreferences prefs =
                    mCtx.getSharedPreferences(PREFERENCES_KEY,
                            Context.MODE_PRIVATE);
            return prefs;
        }
    }


    /**
     * Convenience helper to start this background service,
     * if it has not been started.
     */
    public static ComponentName startForeground(Context ctx) {
        Intent intent = new Intent(ctx.getApplicationContext(), DictionaryOnCopyService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        ComponentName res = ctx.startService(intent);
        PLog.v("DictionaryOnCopyService.startForeground(ctx): %s", res);
        return res;
    }

    public static void stopForeground(Context ctx) {
        Intent intent = new Intent(ctx.getApplicationContext(), DictionaryOnCopyService.class);
        intent.setAction(ACTION_STOP_FOREGROUND);
        ComponentName res = ctx.startService(intent);
        PLog.v("DictionaryOnCopyService.stopForeground(ctx): %s", res);
    }
}
