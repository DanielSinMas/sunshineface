package com.example.daniel.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private int max = 0;
    private int min = 0;
    private String weather_id = "";
    private GoogleApiClient mGoogleApiClient;
    private String nodeId;
    private byte[] payload;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for(DataEvent dataevent : dataEvents){
            if(dataevent.getType() == DataEvent.TYPE_CHANGED){
                DataMap dataMap = DataMapItem.fromDataItem(dataevent.getDataItem()).getDataMap();
                String path = dataevent.getDataItem().getUri().getPath();
                Log.e("Cambios", "Cambios");
                if(path.equals("/sunshine")){
                    max = dataMap.getInt("max");
                    min = dataMap.getInt("min");
                    weather_id = dataMap.getString("weather_id");
                    Log.e("Data item", weather_id + " - min: " + min + " - max:" + max);
                }

                Uri uri = dataevent.getDataItem().getUri();
                nodeId = uri.getHost();
                payload = uri.toString().getBytes();
            }
        }

        SharedPreferences shared = getApplicationContext().getSharedPreferences("com.daniel.sunshineface", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt("max", max);
        editor.putInt("min", min);
        editor.putString("weather_id", weather_id);
        editor.apply();
        editor.commit();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        if (!mGoogleApiClient.isConnected()) {
            return;
        }

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/sunshine");
        putDataMapRequest.getDataMap().putInt("max", max);
        putDataMapRequest.getDataMap().putInt("min", min);
        putDataMapRequest.getDataMap().putString("weather_id", weather_id);
        putDataMapRequest.setUrgent();
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e("Sending", "ERROR: failed to putDataItem, status code: "
                                    + dataItemResult.getStatus().getStatusCode());
                        }
                    }
                });

        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId,
                "/data-item-received", payload);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
