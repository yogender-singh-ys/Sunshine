package com.example.yogender.wear;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
Listener service for the sunshine watch face based on the sample implementation
in the developer guide, see
{@link https://developer.android.com/training/wearables/data-layer/events.html#Listen}
 */
public class SunshineWearableListenerService extends WearableListenerService {
    private static final String LOG_TAG = SunshineWearableListenerService.class.getSimpleName();
    public static final String WEATHER_RECEIVED_PATH = "/weather";
    static final String DESC = "com.example.android.sunshine.app.desc";
    static final String HIGH = "com.example.android.sunshine.app.high";
    static final String LOW = "com.example.android.sunshine.app.low";
    static final String ICON = "com.example.android.sunshine.app.icon";
    static final String UPDATE_ACTION = "com.example.android.sunshine.app.update_watchface";
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(LOG_TAG, "onDataChanged:  " + dataEvents);
        final List events = FreezableUtils.freezeIterable(dataEvents);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess()) {
            Log.e(LOG_TAG, "Failed to connect to GoogleApiClient");
        }

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }
            if (dataEvent.getDataItem().getUri().getPath().equals("/weather")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                DataMap dataMap = dataMapItem.getDataMap();
                Log.d(LOG_TAG, "Got data item:  " + dataMap);
                Intent updateWeatherIntent = new Intent();
                updateWeatherIntent.setAction(UPDATE_ACTION);
                if (dataMap.containsKey(ICON)) {
                    Asset iconAsset = dataMap.getAsset(ICON);
                    Bitmap bm = LoadBitmapFromAsset(iconAsset);
                    updateWeatherIntent.putExtra(ICON, bm);
                }
                if (dataMap.containsKey(HIGH)) {
                    String highTemperature = dataMapItem.getDataMap().getString(HIGH);
                    updateWeatherIntent.putExtra(HIGH, highTemperature);
                }
                if (dataMap.containsKey(LOW)) {
                    String lowTemperature = dataMapItem.getDataMap().getString(LOW);
                    updateWeatherIntent.putExtra(LOW, lowTemperature);
                }
                if (dataMap.containsKey(DESC)) {
                    String weatherString = dataMapItem.getDataMap().getString(DESC);
                    updateWeatherIntent.putExtra(DESC, weatherString);
                }
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(updateWeatherIntent);
                Log.d(LOG_TAG, "Sent broadcast event");
            }
        }
    }

    private Bitmap LoadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            Log.e(LOG_TAG, "Error:  asset is null");
            return  null;
        }
        ConnectionResult result = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (!result.isSuccess()) {
            Log.w(LOG_TAG, "Connection result failed");
            return null;
        }
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(LOG_TAG, "Requested an unknown asset");
            return null;
        }

        return BitmapFactory.decodeStream(assetInputStream);
    }
}