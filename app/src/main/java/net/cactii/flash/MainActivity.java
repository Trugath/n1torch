package net.cactii.flash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainnew);
        final Button buttonOn = (Button) findViewById(R.id.buttonOn);
        final CheckBox buttonStrobe = (CheckBox) findViewById(R.id.strobe);
        final TextView strobeLabel = (TextView) findViewById(R.id.strobeTimeLabel);
        final SeekBar slider = (SeekBar) findViewById(R.id.slider);

        // Preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int strobeperiod = preferences.getInt("strobeperiod", 100);

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

                if (!TorchService.isRunning(getApplicationContext())) {
                    startService(intent);
                    buttonOn.setText("Off");
                    buttonStrobe.setEnabled(false);
                    if (!buttonStrobe.isChecked())
                        slider.setEnabled(false);
                } else {
                    stopService(intent);
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
        strobeLabel.setText("Strobe frequency: " + 500 / strobeperiod + "Hz");
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                int strobeperiod = 201 - progress;
                if (strobeperiod < 20)
                    strobeperiod = 20;
                strobeLabel.setText("Strobe frequency: " + 500 / strobeperiod + "Hz");

                preferences.edit()
                        .putInt("strobeperiod", strobeperiod)
                        .apply();

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
        this.updateWidget();
        super.onPause();
    }

    public void onDestroy() {
        this.updateWidget();
        super.onDestroy();
    }

    public void onResume() {
        if (TorchService.isRunning(this)) {
            ((Button) findViewById(R.id.buttonOn)).setText("Off");

            final CheckBox buttonStrobe = (CheckBox) findViewById(R.id.strobe);
            buttonStrobe.setEnabled(false);
            if (!buttonStrobe.isChecked())
                findViewById(R.id.slider).setEnabled(false);
        }
        this.updateWidget();
        super.onResume();
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
        TorchWidgetProvider.getInstance().updateAllStates(this);
    }
}
