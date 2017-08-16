package net.oldev.aDictOnCopy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * An abstract foreground service that listens to clipboard changes
 */
public abstract class ClipChangedListenerForegroundService extends ClipChangedListenerService {
    public static final String ACTION_START_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.STOP_FOREGROUND";
    public static final String ACTION_PAUSE = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.PAUSE";
    public static final String ACTION_RESUME = "net.oldev.aDictOnCopy.ClipChangedListenerForegroundService.RESUME";

    abstract protected CharSequence getServiceDisplayName();

    /**
     *
     * @return the id to be used for the ongoing notification for foreground service. It should be a constant.
     * @see android.app.Service#startForeground(int, Notification)
     */
    abstract protected int getOngoingNotificationId();

    abstract protected CharSequence getNotificationTitle();

    public static interface NotificationResources {
        /**
         *
         * @return the resource id for the icon used in the ongoing notification.
         */
        int getNotificationSmallIconId();

        /**
         *
         * @return the resource id for the icon used if the service is paused.
         * 0 if the service does not allow pause.
         */
        int getPauseNotificationSmallIconId();

        int getPauseActionIconId();

        int getResumeActionIconId();
    }

    abstract protected NotificationResources getNotificationResources();

    protected boolean allowPause() {
        return getNotificationResources().getPauseNotificationSmallIconId() > 0;
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
        toastMsg(getString(R.string.info_msgf_starting_service, getServiceDisplayName()));

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
        builder.setSmallIcon(getNotificationResources().getPauseNotificationSmallIconId());

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
        toastMsg(getString(R.string.info_msgf_stopping_service,getServiceDisplayName()));

        stopForeground(true);
        stopSelf();
    }

    private NotificationCompat.Builder createBasicBuilder() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(getNotificationResources().getNotificationSmallIconId())
                .setContentTitle(getNotificationTitle())
                .setContentText(getString(R.string.noti_msg_touch_to_stop))
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
        builder.addAction(getNotificationResources().getPauseActionIconId(), getString(R.string.noti_btn_pause), pausePendingIntent);
        return builder;
    }

    private NotificationCompat.Builder addActionResume(NotificationCompat.Builder builder) {
        Intent pauseIntent = new Intent(getApplicationContext(), this.getClass());
        pauseIntent.setAction(ACTION_RESUME);
        PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(getNotificationResources().getResumeActionIconId(), getString(R.string.noti_btn_resume), pausePendingIntent);
        return builder;
    }

    private void toastMsg(String msg) {
        android.widget.Toast.makeText(getApplicationContext(), msg,
                android.widget.Toast.LENGTH_LONG).show();
    }

}
