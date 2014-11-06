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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TorchService extends Service {
    private static final String MSG_TAG = "TorchNotRoot";
    private TimerTask mStrobeTask;
    private Timer mStrobeTimer;
    private int mStrobePeriod;
    private Handler mHandler;
    private Camera mCamera;
    private IntentReceiver mReceiver;

    private Runnable mStrobeRunnable;

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

    public void onCreate() {
        this.mStrobeRunnable = new Runnable() {
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
        this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);

        this.mStrobeTimer = new Timer();

        this.mHandler = new Handler() {

        };
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
            this.mStrobePeriod = intent.getIntExtra("period", 200) / 4;
            this.mStrobeTimer.schedule(this.mStrobeTask, 0,
                    this.mStrobePeriod);
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
        this.mStrobeTimer.cancel();
        this.mCamera.stopPreview();
        this.mCamera.release();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        this.unregisterReceiver(this.mReceiver);
        stopForeground(true);
    }

    void Reshedule(int period) {
        this.mStrobeTask.cancel();
        this.mStrobeTask = new WrapperTask(this.mStrobeRunnable);

        this.mStrobePeriod = period / 4;
        this.mStrobeTimer.schedule(this.mStrobeTask, 0, this.mStrobePeriod);
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
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Reshedule(intent.getIntExtra("period", 200));
                }

            });
        }
    }
}
