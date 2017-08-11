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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;


public class DictionaryOnCopyService extends ClipChangedListenerForegroundService {

    //
    // For query service running state (locally within the app)
    //
    private static boolean msRunning = false;
    public static boolean isRunning() {
        return msRunning;
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
    protected CharSequence getServiceDisplayName() {
        return getString(R.string.app_name);
    }

    @Override
    protected int getOngoingNotificationId() {
        return ONGOING_NOTIFICATION_ID;
    }

    private static final NotificationResources NOTIFICATION_RESOURCES = new NotificationResources() {
        @Override
        public int getNotificationSmallIconId() { return R.drawable.dictionary; }

        @Override
        public int getPauseNotificationSmallIconId() { return R.drawable.dictionary_pause; }

        @Override
        public int getPauseActionIconId() { return R.drawable.btn_pause; }

        @Override
        public int getResumeActionIconId() { return R.drawable.btn_resume; }
    };

    @Override
    protected NotificationResources getNotificationResources() {
        return NOTIFICATION_RESOURCES;
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
                    // For plain text, sometimes using cd.getItemAt(0).getText()).toString() will throw null pointers
                    // simplify the flow using coerceToText
                    CharSequence clipText = cd.getItemAt(0).coerceToText(getApplicationContext());
                    String msg = "<" + descText + "> " + clipText;
                    PLog.v(msg);
                    boolean launched = launchDictionaryIfAWord(clipText);
                    if (!launched) dbgMsg("[Not word]" + msg);
                } else {
                    String msg = "!<" + descText + "> " + (cd.getItemAt(0).toString());
                    PLog.v(msg);
                    dbgMsg("[Unsupported type]" + msg);
                }
            }
        } catch (Throwable t) {
            // Catch any exception here as a safety net, to prevent the app crashes.
            PLog.e("Unhandled errors in DictionaryOnCopyService.performClipboardCheck().", t);
            toastMsg("Unexpected errors in Dictionary On Copy. Please try again.");
        }
    }

    //
    // Actual logic in launching dictionary
    //

    private static final int MAX_NUM_WORDS_IN_TEXT = 3;
    private static boolean isAWord(CharSequence text) {
        String textStr = text.toString().trim();
        if (textStr.isEmpty()) { // NO-op for empty string
            return false;
        }
        final String[] splitted = textStr.split("\\s+", MAX_NUM_WORDS_IN_TEXT + 1);
        return splitted.length <= MAX_NUM_WORDS_IN_TEXT;
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
        String dictPkg = SettingsModel.getPackageName(this);
        String action = SettingsModel.getAction(this);
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

        PLog.v("DictionaryOnCopyService.launchDictionary(): intent=%s", intent);
        if (isIntentAvailable(this, intent)) // check if intent is available ?
            startActivity(intent);
        else {
            dbgMsg("Dictionary <" + dictPkg  + "> not found.");
        }
    }

    private boolean isIntentAvailable(Context context, Intent intent) {
        List<ResolveInfo> lri = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
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

        public static @NonNull String getAction(Context ctx) {
            // Intent.ACTION_SEARCH would work, but it may be too generic.
            // use color dict intent limits the packages to a manageable level.
            return "colordict.intent.action.SEARCH";
        }

        public static @Nullable String getPackageName(Context ctx) {
            return getPrefs(ctx).getString(PREFS_PACKAGE_NAME, null);
        }

        /**
         *
          * @param packageName the package name of the dictionary app to be launched
         */
        public static void setPackageName(String packageName, Context ctx) {
            SharedPreferences.Editor editor = getPrefs(ctx).edit();
            editor.putString(PREFS_PACKAGE_NAME, packageName);
            editor.commit();
        }

        private static SharedPreferences getPrefs(Context ctx) {
            SharedPreferences prefs =
                    ctx.getSharedPreferences(PREFERENCES_KEY,
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
