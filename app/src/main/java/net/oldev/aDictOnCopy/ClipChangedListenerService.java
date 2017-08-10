package net.oldev.aDictOnCopy;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;

/**
 * An abstract service that listens to clipboard changes.
 *
 * Based on the Clipboard watcher service at
 * https://stackoverflow.com/a/22287217
 */
public abstract class ClipChangedListenerService extends Service {

    private final ClipboardManager.OnPrimaryClipChangedListener listener;

    public ClipChangedListenerService() {
        listener = createListener();
    }

    /**
     * The implementation will be invoked in constructor.
     * It should not assume the instance is fully initialized yet.
     *
     */
    abstract protected ClipboardManager.OnPrimaryClipChangedListener createListener();

    @Override
    public void onCreate() {
        super.onCreate();
        PLog.d(this.getClass().getSimpleName() + ".onCreate()");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PLog.d(this.getClass().getSimpleName() + ".onDestroy()");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(listener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
