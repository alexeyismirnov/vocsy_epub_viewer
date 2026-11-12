package com.vocsy.epub_viewer;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipFile;

/**
 * FolioReader / r2-streamer never closes the EPUB {@link ZipFile}. CloseGuard then
 * reports "A resource failed to call ZipFile.close" and file descriptors leak after
 * opening several saint lives.
 */
final class FolioZipCloser implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "FolioZipCloser";
    private static final String FOLIO_ACTIVITY = "com.folioreader.ui.activity.FolioActivity";
    private static boolean registered = false;

    static void register(Application application) {
        if (application == null || registered) {
            return;
        }
        registered = true;
        application.registerActivityLifecycleCallbacks(new FolioZipCloser());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (!FOLIO_ACTIVITY.equals(activity.getClass().getName())) {
            return;
        }
        // FolioActivity calls super.onDestroy() before Server.stop(), so wait until
        // onDestroy has finished before closing the zip.
        new Handler(Looper.getMainLooper()).post(() -> closeZip(activity));
    }

    private static void closeZip(Activity activity) {
        try {
            Field pubBoxField = activity.getClass().getDeclaredField("pubBox");
            pubBoxField.setAccessible(true);
            Object pubBox = pubBoxField.get(activity);
            if (pubBox == null) {
                return;
            }
            Method getContainer = pubBox.getClass().getMethod("getContainer");
            Object container = getContainer.invoke(pubBox);
            if (container == null) {
                return;
            }
            Method getZipFile = container.getClass().getMethod("getZipFile");
            Object zip = getZipFile.invoke(container);
            if (zip instanceof ZipFile) {
                ((ZipFile) zip).close();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Unable to close EPUB ZipFile", t);
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
}
