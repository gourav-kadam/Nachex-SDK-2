package com.nachex.ui.camera;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraThreadPool {

    static Timer timerFocus = null;

    /*
     * Focus frequency
     */
    static final long cameraScanInterval = 2000;

    /*
     * thread pool size
     */
    private static int poolCount = Runtime.getRuntime().availableProcessors();

    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(poolCount);

    /**
     * Add tasks to the thread pool
     * @param runnable task
     */
    public static void execute(Runnable runnable) {
        fixedThreadPool.execute(runnable);
    }

    /**
     * Create a timer task with timed focus
     * @param runnable focus code
     * @return Timer Timer object, used to terminate autofocus
     */
    public static Timer createAutoFocusTimerTask(final Runnable runnable) {
        if (timerFocus != null) {
            return timerFocus;
        }
        timerFocus = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        timerFocus.scheduleAtFixedRate(task, 0, cameraScanInterval);
        return timerFocus;
    }

    /**
     * Terminate the autofocus task, actually call the cancel method and clear the object
     * However, the task in execution cannot be terminated, and additional processing is required
     *
     */
    public static void cancelAutoFocusTimer() {
        if (timerFocus != null) {
            timerFocus.cancel();
            timerFocus = null;
        }
    }
}
