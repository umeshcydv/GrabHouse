package com.android.grabhouse;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by umeshchandrayadav on 10/01/15.
 */
public class FetchAndSaveDataService extends Service {

    public SharedPreferences preferences;
    private Gson gson = new Gson();
    private IBinder mBinder = new ServiceBinder();
    private List<ItemDetail> details;


    public List<ItemDetail> getItems() {
        Type type = new TypeToken<List<ItemDetail>>() {
        }.getType();
        details = gson.fromJson(preferences.getString("item_details", ""), type);
        return details;
    }

    public void saveItemDetail(ItemDetail itemDetail) {
        SharedPreferences.Editor editor = preferences.edit();
        List<ItemDetail> items = getItems();
        items.add(0, itemDetail);
        editor.putString("item_details", gson.toJson(items));
        editor.commit();
    }

    public void saveAllItems(List<ItemDetail> items) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("item_details", gson.toJson(items));
        editor.commit();
    }

    public class ServiceBinder extends Binder {

        public FetchAndSaveDataService getService() {
            return FetchAndSaveDataService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        preferences = getSharedPreferences("com.android.grabhouse", MODE_PRIVATE);
        return mBinder;
    }
}
