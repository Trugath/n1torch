package net.cactii.flash;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;

public class TorchWidgetProvider extends AppWidgetProvider {

    private static final ComponentName THIS_APPWIDGET = new ComponentName("net.cactii.flash", "net.cactii.flash.TorchWidgetProvider");

    private static WeakReference<TorchWidgetProvider> sInstance = null;

    static synchronized TorchWidgetProvider getInstance() {
        TorchWidgetProvider result;

        // get stored result if it exists
        if (sInstance != null && (result = sInstance.get()) != null) {
            return result;
        }

        // new instance
        sInstance = new WeakReference<TorchWidgetProvider>(result = new TorchWidgetProvider());
        return result;
    }

    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId, int buttonId) {
        Intent launchIntent = new Intent();
        Log.d("TorchWidget", "Widget id: " + appWidgetId);
        launchIntent.setClass(context, TorchWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + appWidgetId + "/" + buttonId));
        return PendingIntent
                .getBroadcast(context, 0 /* no requestCode */, launchIntent, 0 /*
                                                                        * no
                                                                        * flags
                                                                        */);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            views.setOnClickPendingIntent(R.id.btn, getLaunchPendingIntent(context, appWidgetId, 0));
            this.updateState(context, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId;
            int widgetId;
            widgetId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[0]);
            buttonId = Integer.parseInt(data.getSchemeSpecificPart().split("/")[1]);

            Log.d("TorchWidget", "Button Id is: " + widgetId);
            if (buttonId == 0) {
                Intent pendingIntent;

                if (TorchService.isRunning(context)) {
                    context.stopService(new Intent(context, TorchService.class));
                    this.updateAllStates(context);
                    return;
                }

                pendingIntent = new Intent(context, TorchService.class);

                if (TorchService.isRunning(context)) {
                    context.stopService(pendingIntent);
                } else {
                    context.startService(pendingIntent);
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.updateAllStates(context);
        }
    }

    public void updateAllStates(Context context) {
        final AppWidgetManager am = AppWidgetManager.getInstance(context);
        for (int id : am.getAppWidgetIds(THIS_APPWIDGET))
            this.updateState(context, id);
    }

    void updateState(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        if (TorchService.isRunning(context)) {
            views.setImageViewResource(R.id.img_torch, R.drawable.icon);
        } else {
            views.setImageViewResource(R.id.img_torch, R.drawable.widget_off);
        }

        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(appWidgetId, views);
    }
}
