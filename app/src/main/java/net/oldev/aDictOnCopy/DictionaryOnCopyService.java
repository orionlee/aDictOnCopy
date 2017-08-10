package net.oldev.aDictOnCopy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.List;

public class DictionaryOnCopyService extends ClipChangedListenerForegroundService {

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

    @Override
    protected int getNotificationSmallIconId() {
        return R.drawable.dictionary;
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
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb.hasPrimaryClip()) {
            ClipData cd = cb.getPrimaryClip();
            ClipDescription desc = cd.getDescription();
            String descText = desc.toString();
            if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                CharSequence clipText = (cd.getItemAt(0).getText()).toString();
                String msg = "<" + descText + "> " + clipText;
                PLog.d(msg);
                boolean launched = launchDictionaryIfAWord(clipText);
                if (!launched) dbgMsg("[Not word]" + msg);
            } else if (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                CharSequence clipText = cd.getItemAt(0).coerceToText(getApplicationContext());
                String msg = "<" + descText + "> " + clipText;
                PLog.d(msg);
                boolean launched = launchDictionaryIfAWord(clipText);
                if (!launched) dbgMsg("[Not word]" + msg);
            } else {
                String msg = "!<" + descText + "> " + (cd.getItemAt(0).toString());
                PLog.d(msg);
                dbgMsg(msg);
            }
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
        String dictPkg = "livio.pack.lang.en_US";
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.setPackage(dictPkg); //you can use also livio.pack.lang.it_IT, livio.pack.lang.es_ES, livio.pack.lang.de_DE, livio.pack.lang.pt_BR or livio.pack.lang.fr_FR
        intent.putExtra(SearchManager.QUERY, word);
        // FLAG_ACTIVITY_NO_USER_ACTION is needed to make system back button work, i.e.,
        // after dictionary is launched, pressing back button will go back to the previous app.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
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

    private void dbgMsg(String msg) {
        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
    }


    /**
     * Convenience helper to start this background service, 
     * if it has not been started.
     */
    public static ComponentName startForeground(Context ctx) {
        Intent intent = new Intent(ctx.getApplicationContext(), DictionaryOnCopyService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        ComponentName res = ctx.startService(intent);
        PLog.v("DictionaryOnCopyService.startForeground(): %s", res);
        return res;
    }

}
