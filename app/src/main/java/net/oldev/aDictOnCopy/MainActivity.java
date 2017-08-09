package net.oldev.aDictOnCopy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Intent extra data key for disabling the service.
    public static final String STOP = "stop";
    private final int mNotificationId = 101;
    private NotificationManager mNotifyMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Now setup the UI
        setContentView(R.layout.activity_main);

        // Gets an instance of the NotificationManager service
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start or stop the service.
        // It needs to be in onResume() rather than onCreate(), to support the workflow of
        //   launch > press notification to stop > do something else > launch again,
        // the subsequent launch will invoke onResume(), but may not invoke onCreate(), if the previous instance still exists.
        //
        // Complication: for the case of the previous instance still exists,
        // the intent will still be the one with STOP. I need to be able to clear it.
        Intent intent = getIntent();
        PLog.v("intent: disable=<%s> , intent=<%s>", intent.getBooleanExtra(STOP, false), intent.toString());
        if (intent.getBooleanExtra(STOP, false)) {
            PLog.d("Stopping the service upon receiving an intent");
            DictionaryOnCopyService.stop(getApplicationContext());
            mNotifyMgr.cancel(mNotificationId);
            Toast.makeText(getApplicationContext(), "Dictionary On Copy stopped.", Toast.LENGTH_LONG).show();
            // once it is stopped. clear the stop extra flag so that when the screen is shown again
            // the next time, it will not be treated as stop.
            intent.removeExtra(STOP);
        } else {
            // Normal startup code path
            DictionaryOnCopyService.start(getApplicationContext());
            setNotification();
            Toast.makeText(getApplicationContext(), "Dictionary On Copy started.", Toast.LENGTH_LONG).show();
        }
    }


    private void setNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.dictionary)
                .setContentTitle("Dictionary On Copy")
                .setContentText("Touch to stop.");

        builder.setOngoing(true);

        // Set a PendingIntent to disable the copy service
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(STOP, Boolean.TRUE);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // the service remains even when the activity is destroyed.
    }
}

