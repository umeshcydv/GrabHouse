package com.android.grabhouse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {

    @InjectView(R.id.lv_items)
    ListView mListView;

    public List<ItemDetail> itemDetails = new ArrayList<>();
    private static final int CAMERA_REQUEST = 1888;
    private static final int UPDATE_REQUEST = 1889;
    private ItemsListViewAdapter adapter;
    public static DisplayImageOptions options;
    private boolean isServiceConnected;
    private FetchAndSaveDataService fetchAndSaveDataService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        initializeImageOptions();
        initialize();
    }

    private void initializeImageOptions() {
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.default_bg)
                .showImageForEmptyUri(R.drawable.default_bg)
                .showImageOnFail(R.drawable.default_bg)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, FetchAndSaveDataService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FetchAndSaveDataService.ServiceBinder binder = (FetchAndSaveDataService.ServiceBinder) iBinder;
            fetchAndSaveDataService = binder.getService();
            isServiceConnected = true;
            if(itemDetails == null) {
                itemDetails = fetchAndSaveDataService.getItems();
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            fetchAndSaveDataService = null;
            isServiceConnected = false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (isServiceConnected) {
            unbindService(connection);
            isServiceConnected = false;
        }
    }

    private void initialize() {
        adapter = new ItemsListViewAdapter(itemDetails, getLayoutInflater(), getApplicationContext());
        mListView.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            //Bitmap photo = (Bitmap) data.getExtras().get("data");
            data.setClass(this, ImageUploadActivity.class);
            startActivityForResult(data, UPDATE_REQUEST);

        }
        if (requestCode == UPDATE_REQUEST) {
            if (resultCode == RESULT_OK) {
                ItemDetail itemDetail = (ItemDetail) data.getExtras().get("data");
                itemDetails.add(0, itemDetail);
                adapter.notifyDataSetChanged();
                if (fetchAndSaveDataService != null) {
                    fetchAndSaveDataService.saveAllItems(itemDetails);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_upload) {
            onUploadClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void onUploadClick(View v) {
        Intent intent = new Intent(MainActivity.this, FetchAndSaveDataService.class);
        startService(intent);
    }

    public void onUploadClick() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }
}
