package net.cactii.flash;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {

    public static MainActivity ma;
    public Camera mCamera;
    private TorchWidgetProvider mWidgetProvider;
    // Thread to handle strobing
    private boolean mTorchOn;
    // Strobe frequency slider.
    private SeekBar slider;
    // Period of strobe, in milliseconds
    private int strobeperiod;
    // Label showing strobe frequency
    private TextView strobeLabel;
    // On button
    private Button buttonOn;
    // Strobe toggle
    private CheckBox buttonStrobe;
    // Strobe has timed out
    private Context context;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        ma = this;
        context = this.getApplicationContext();
        buttonOn = (Button) findViewById(R.id.buttonOn);
        buttonStrobe = (CheckBox) findViewById(R.id.strobe);
        strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        slider = (SeekBar) findViewById(R.id.slider);

        strobeperiod = 100;
        mTorchOn = false;

        mWidgetProvider = TorchWidgetProvider.getInstance();

        // Preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        strobeLabel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                buttonStrobe.setChecked(!buttonStrobe.isChecked());
            }

        });

        buttonOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TorchService.class);

                intent.putExtra("strobe", buttonStrobe.isChecked());
                intent.putExtra("period", strobeperiod);

                if (!mTorchOn) {
                    startService(intent);
                    mTorchOn = true;
                    buttonOn.setText("Off");
                    buttonStrobe.setEnabled(false);
                    if (!buttonStrobe.isChecked())
                        slider.setEnabled(false);
                } else {
                    stopService(intent);
                    mTorchOn = false;
                    buttonOn.setText("On");
                    buttonStrobe.setEnabled(true);
                    slider.setEnabled(true);
                }
            }

        });

        // Strobe frequency slider bar handling
        setProgressBarVisibility(true);
        slider.setHorizontalScrollBarEnabled(true);
        slider.setProgress(200 - preferences.getInt("strobeperiod", 100));
        strobeperiod = preferences.getInt("strobeperiod", 100);
        strobeLabel.setText("Strobe frequency: " + 500 / strobeperiod + "Hz");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                strobeperiod = 201 - progress;
                if (strobeperiod < 20)
                    strobeperiod = 20;
                strobeLabel.setText("Strobe frequency: " + 500 / strobeperiod + "Hz");

                Intent intent = new Intent("net.cactii.flash.SET_STROBE");
                intent.putExtra("period", strobeperiod);
                sendBroadcast(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });


        // Show the about dialog, the first time the user runs the app.
        if (!preferences.getBoolean("aboutSeen", false)) {
            this.openAboutDialog();

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean("aboutSeen", true)
                    .apply();
        }
    }

    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt("strobeperiod", this.strobeperiod)
                .apply();
        this.updateWidget();
        super.onPause();
    }

    public void onDestroy() {
        this.updateWidget();
        super.onDestroy();
    }

    public void onResume() {
        if (this.TorchServiceRunning(context)) {
            buttonOn.setText("Off");
            buttonStrobe.setEnabled(false);
            if (!buttonStrobe.isChecked())
                slider.setEnabled(false);
            this.mTorchOn = true;
        }
        this.updateWidget();
        super.onResume();
    }

    private boolean TorchServiceRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

        if (!(svcList.size() > 0))
            return false;
        for (int i = 0; i < svcList.size(); i++) {
            RunningServiceInfo serviceInfo = svcList.get(i);
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().endsWith(".TorchService")
                    || serviceName.getClassName().endsWith(".RootTorchService"))
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        menu.addSubMenu(0, 0, 0, "About Torch");
        return supRetVal;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);
        this.openAboutDialog();
        return supRetVal;
    }


    private void openAboutDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("About")
                .setView(view)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Log.d(MSG_TAG, "Close pressed");
                    }
                })
                .show();
    }

    void updateWidget() {
        this.mWidgetProvider.updateAllStates(context);
    }

}
