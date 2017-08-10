package net.oldev.aDictOnCopy;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * An abstract foreground service that listens to clipboard changes
 */
public abstract class ClipChangedListenerForegroundService extends ClipChangedListenerService {
    public static final String ACTION_START_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.STOP_FOREGROUND";

    abstract protected CharSequence getServiceDisplayName();

    /**
     *
     * @return the id to be used for the ongoing notification for foreground service. It should be a constant.
     * @see android.app.Service#startForeground(int, Notification)
     */
    abstract protected int getOngoingNotificationId();

    /**
     *
     * @return the resource id for the icon used in the ongoing notification.
     */
    abstract protected int getNotificationSmallIconId();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        PLog.d(LIFECYCLE_LOG_FORMAT, "onStartCommand(): action=<%s>", action);
        switch(action) {
            case ACTION_START_FOREGROUND:
                doStartForeground();
                break;
            case ACTION_STOP_FOREGROUND:
                doStopForeground();
                break;
            default:
                PLog.w("  .onStartCommand(): Unknown intent action <" + action + ">");
        }

        return START_STICKY;
    }

    private void doStartForeground() {
        PLog.d(LIFECYCLE_LOG_FORMAT, "doStartForeground()");
        toastMsg("Starting " + getServiceDisplayName() + "...");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(getNotificationSmallIconId())
                .setContentTitle(getServiceDisplayName())
                .setContentText("Touch to stop.");

        // Set a PendingIntent to stop the copy service
        Intent stopIntent = new Intent(getApplicationContext(), DictionaryOnCopyService.class);
        stopIntent.setAction(ACTION_STOP_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        startForeground(getOngoingNotificationId(), notification);
    }

    private void doStopForeground() {
        PLog.d(LIFECYCLE_LOG_FORMAT, "doStopForeground()");
        toastMsg("Stopping " + getServiceDisplayName() + "...");

        stopForeground(true);
        stopSelf();
    }

    private void toastMsg(String msg) {
        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
    }
}
