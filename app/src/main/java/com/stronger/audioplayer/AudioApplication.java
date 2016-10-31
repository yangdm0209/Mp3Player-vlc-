package com.stronger.audioplayer;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import org.videolan.vlc.util.Strings;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by stronger on 15-12-19.
 */
public class AudioApplication extends Application {
    private static AudioApplication instance;
    public final static String SLEEP_INTENT = Strings.buildPkgString("SleepIntent");
    private ThreadPoolExecutor mThreadPool = new ThreadPoolExecutor(0, 2, 2, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    /**
     * @return the main context of the Application
     */
    public static Context getAppContext()
    {
        return instance;
    }

    /**
     * @return the main resources from the Application
     */
    public static Resources getAppResources()
    {
        return instance.getResources();
    }

    public static void runBackground(Runnable runnable) {
        instance.mThreadPool.execute(runnable);
    }
}
