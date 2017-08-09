package net.oldev.aDictOnCopy;

import android.util.Log;

/**
 * Log wrapper to standardize on the log messages emitted.
 */
class PLog {
    private PLog() {} // only has static methods
    private static final String TAG = "DictionaryOnCopy";
    
    static void w(String msg, Throwable t) {
        Log.w(TAG, msg, t);
    }

    static void w(String msg) {
        Log.w(TAG, msg);
    }
    

    static void d(String msg) {
        Log.d(TAG, msg);
    }

    static void d(String msgFormat, Object... args) {
        final String msg= String.format(msgFormat, args);
        d(msg);
    }

    // Intended to be used sparingly
    static void v(String msg) {
        if (BuildConfig.DEBUG) { // only used for development
            Log.v(TAG, msg);
        }
    }

    static void v(String msgFormat, Object... args) {
        final String msg= String.format(msgFormat, args);
        v(msg);
    }

}