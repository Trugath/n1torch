package net.cactii.flash;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TorchService extends Service {
    static final String SET_STROBE = "net.cactii.flash.SET_STROBE";

    private static final String MSG_TAG = "TorchService";
    private TimerTask mStrobeTask;
    private Timer mStrobeTimer;
    private Camera mCamera;
    private IntentReceiver mReceiver;

    static boolean isRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo serviceInfo : svcList) {
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().endsWith(".TorchService"))
                return true;
        }
        return false;
    }

    private Runnable getStrobeRunnable() {
        return new Runnable() {
            public int mCounter = 4;
            public boolean mOn;

            public void run() {
                if (!this.mOn) {
                    if (this.mCounter-- < 1) {
                        Camera.Parameters params = mCamera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(params);
                        this.mOn = true;
                    }
                } else {
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(params);
                    this.mCounter = 4;
                    this.mOn = false;
                }
            }
        };
    }

    public void onCreate() {
        this.mStrobeTask = new WrapperTask(getStrobeRunnable());
        this.mStrobeTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MSG_TAG, "Starting torch");
        try {
            this.mCamera = Camera.open();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        if (intent != null && intent.getBooleanExtra("strobe", false)) {
            this.mCamera.startPreview();
            this.mStrobeTimer.schedule(this.mStrobeTask, 0, intent.getIntExtra("period", 200) / 4);
        } else {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            this.mCamera.setParameters(parameters);
        }

        this.mReceiver = new IntentReceiver();
        registerReceiver(this.mReceiver, new IntentFilter("net.cactii.flash.SET_STROBE"));

        Notification notification = new Notification(R.drawable.notification_icon, "Torch on", System.currentTimeMillis());
        notification.setLatestEventInfo(this, "Torch on", "Torch currently on", PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
        startForeground(0, notification);
        return START_STICKY;
    }


    public void onDestroy() {
        this.mStrobeTask.cancel();
        this.mStrobeTimer.cancel();
        this.mCamera.stopPreview();
        this.mCamera.release();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        this.unregisterReceiver(this.mReceiver);
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public class WrapperTask extends TimerTask {
        private final Runnable target;

        public WrapperTask(Runnable target) {
            this.target = target;
        }

        public void run() {
            target.run();
        }
    }

    public class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction().equals(TorchService.SET_STROBE)) {
                mStrobeTask.cancel();
                mStrobeTask = new WrapperTask(getStrobeRunnable());
                mStrobeTimer.schedule(mStrobeTask, 0, intent.getIntExtra("period", 200) / 4);
            }
        }
    }
}
