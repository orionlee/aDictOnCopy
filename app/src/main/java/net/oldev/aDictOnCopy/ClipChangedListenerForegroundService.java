package net.oldev.aDictOnCopy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

/**
 * An abstract foreground service that listens to clipboard changes
 */
public abstract class ClipChangedListenerForegroundService extends ClipChangedListenerService {
    public static final String ACTION_START_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.STOP_FOREGROUND";
    public static final String ACTION_PAUSE = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.PAUSE";
    public static final String ACTION_RESUME = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.RESUME";

    /**
     *
     * @return the id to be used for the ongoing notification for foreground service. It should be a constant.
     * @see android.app.Service#startForeground(int, Notification)
     */
    abstract protected int getOngoingNotificationId();

    public static interface NotificationResources {

        @StringRes int getContentTitle();

        @StringRes int getContentText();

        /**
         *
         * @return the resource id for the icon used in the ongoing notification.
         */
        @DrawableRes int getNotificationSmallIcon();

        // Notes: the following are all @StringRes or @DrawableRes
        // But since they are optional (can be zero), they are not annotated as such/

        /**
         *
         * @return the resource id for the icon used if the service is paused.
         * 0 if the service does not allow pause.
         */
        int getPauseNotificationSmallIcon();

        int getPauseActionIcon();
        int getPauseActionText();

        int getResumeActionIcon();
        int getResumeActionText();
    }

    abstract protected NotificationResources getNotificationResources();

    public abstract interface ServiceResources {
        @StringRes int getDisplayName();
        @StringRes int getStartingServiceTextf();
        @StringRes int getStoppingServiceTextf();
    }

    abstract protected ServiceResources getServiceResources();

    private CharSequence getServiceDisplayName() {
        return getString(getServiceResources().getDisplayName());
    }

    protected boolean allowPause() {
        return getNotificationResources().getPauseNotificationSmallIcon() > 0;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = ( intent != null ? intent.getAction() : "[null-intent]"); // In some edge cases, intent is null
        PLog.d(LIFECYCLE_LOG_FORMAT, "onStartCommand(): action=<" + action + ">");
        switch(action) {
            case ACTION_START_FOREGROUND:
                doStartForeground();
                break;
            case ACTION_STOP_FOREGROUND:
                doStopForeground();
                break;
            case ACTION_PAUSE:
                doPause();
                break;
            case ACTION_RESUME:
                doResume();
                break;
            default:
                PLog.w("  .onStartCommand(): Unknown intent action <" + action + ">");
        }

        return START_STICKY;
    }

    private void doStartForeground() {
        PLog.d(LIFECYCLE_LOG_FORMAT, "doStartForeground()");
        toastMsg(getString(getServiceResources().getStartingServiceTextf(), getServiceDisplayName()));

        NotificationCompat.Builder builder = createBasicBuilder();

        if (allowPause()) {
            addActionPause(builder);
        }

        Notification notification = builder.build();
        startForeground(getOngoingNotificationId(), notification);
    }

    private void doPause() {
        // The actual work
        pause();

        // Update UI
        NotificationCompat.Builder builder = createBasicBuilder();
        builder.setSmallIcon(getNotificationResources().getPauseNotificationSmallIcon());

        addActionResume(builder);

        NotificationManager notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMgr.notify(getOngoingNotificationId(), builder.build());
    }

    private void doResume() {
        // The actual work
        resume();

        // Update UI
        NotificationCompat.Builder builder = createBasicBuilder();
        addActionPause(builder);

        NotificationManager notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMgr.notify(getOngoingNotificationId(), builder.build());
    }

    private void doStopForeground() {
        PLog.d(LIFECYCLE_LOG_FORMAT, "doStopForeground()");
        toastMsg(getString(getServiceResources().getStoppingServiceTextf(),getServiceDisplayName()));

        stopForeground(true);
        stopSelf();
    }

    private NotificationCompat.Builder createBasicBuilder() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(getNotificationResources().getNotificationSmallIcon())
                .setContentTitle(getString(getNotificationResources().getContentTitle()))
                .setContentText(getString(getNotificationResources().getContentText()))
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        // Set a PendingIntent to stop the copy service
        Intent stopIntent = new Intent(getApplicationContext(), this.getClass());
        stopIntent.setAction(ACTION_STOP_FOREGROUND);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        return builder;
    }

    private NotificationCompat.Builder addActionPause(NotificationCompat.Builder builder) {
        Intent pauseIntent = new Intent(getApplicationContext(), this.getClass());
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(getNotificationResources().getPauseActionIcon(), getString(getNotificationResources().getPauseActionText()), pausePendingIntent);
        return builder;
    }

    private NotificationCompat.Builder addActionResume(NotificationCompat.Builder builder) {
        Intent pauseIntent = new Intent(getApplicationContext(), this.getClass());
        pauseIntent.setAction(ACTION_RESUME);
        PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(getNotificationResources().getResumeActionIcon(), getString(getNotificationResources().getResumeActionText()), pausePendingIntent);
        return builder;
    }

    private void toastMsg(String msg) {
        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
    }

}
