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

// Based on the Clipboard wacther service at
// https://stackoverflow.com/a/22287217
public class DictionaryOnCopyService extends Service {

    public static final String ACTION_START_FOREGROUND = "net.oldev.aDictOnCopy.DictionaryOnCopyService.START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "net.oldev.aDictOnCopy.DictionaryOnCopyService.STOP_FOREGROUND";

    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };

    @Override 
    public void onCreate() {
        super.onCreate();
        PLog.d("DictionaryOnCopyService.onCreate()");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PLog.d("DictionaryOnCopyService.onDestroy");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        PLog.d("DictionaryOnCopyService.onStartCommand(): action=<%s>", action);
        switch(action) {
            case ACTION_START_FOREGROUND:
                doStartForeground();
                break;
            case ACTION_STOP_FOREGROUND:
                doStopForeground();
                break;
            default:
                PLog.w("DictionaryOnCopyService.onStartCommand(): Unknown intent action <" + action + ">");
        }

        return START_STICKY;
    }

    private static final int ONGOING_NOTIFICATION_ID = 99;

    private void doStartForeground() {
        String msg = "Starting Dictionary On Copy...";
        PLog.d(msg);
        dbgMsg(msg);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.dictionary)
                .setContentTitle("Dictionary On Copy")
                .setContentText("Touch to stop.");

        // Set a PendingIntent to stop the copy service
        Intent stopIntent = new Intent(getApplicationContext(), DictionaryOnCopyService.class);
        stopIntent.setAction(ACTION_STOP_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void doStopForeground() {
        String msg = "Stopping Dictionary On Copy...";
        PLog.d(msg);
        dbgMsg(msg);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void dbgMsg(String msg) {
        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
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
