package net.cactii.flash;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class WidgetOptionsActivity extends PreferenceActivity {
  
  public int mAppWidgetId;
  public Context mContext;

  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      this.mContext = this;
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      if (extras != null) {
          mAppWidgetId = extras.getInt(
                  AppWidgetManager.EXTRA_APPWIDGET_ID, 
                  AppWidgetManager.INVALID_APPWIDGET_ID);
          Log.d("TorchOptions", "Widget id: " + mAppWidgetId);
      }

      Intent resultValue = new Intent();
      resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(RESULT_OK, resultValue);
      finish();
  }
}
