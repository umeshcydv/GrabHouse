package com.android.grabhouse;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by umeshchandrayadav on 22/03/15.
 */
public class ImageUploadActivity extends ActionBarActivity {

    @InjectView(R.id.iv_pic) ImageView mUploadPic;
    @InjectView(R.id.tv_location_info) TextView mLocationInfo;
    @InjectView(R.id.tv_address) TextView mAddress;
    @InjectView(R.id.btn_update) Button mUpdate;
    @InjectView(R.id.btn_cancel) Button mCancelUpdate;
    @InjectView(R.id.pb_loading) ProgressBar mProgressBar;

    private static final String SERVICE_ENDPOINT = "https://grabhouse.com/";
    public final String ACCEPT = "application/json; charset=utf-8";
    public final String CONTENT_TYPE = "application/json; charset=utf-8";
    public final String ACCEPT_LANGUAGE = "en-US,en;q=0.5";

    private String latitude = "";
    private String longitude = "";
    private String address = "";

    private GPSTracker gpsTracker;
    private boolean serviceConnected;
    private File file;
    private DecimalFormat decimalFormat = new DecimalFormat("#.0000");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);
        ButterKnife.inject(this);
        initializeView();
    }

    @Override
    protected void onStart() {
        Intent intent = new Intent(this, GPSTracker.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceConnected) {
            unbindService(connection);
            serviceConnected = false;
        }
    }

    private void initializeView() {
        Bitmap photo = (Bitmap) getIntent().getExtras().getParcelable("data");
        String path = Environment.getExternalStorageDirectory().getPath() + "/temp/image.png";
        file = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            photo.compress(Bitmap.CompressFormat.PNG, 85, fos);
            fos.close();
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }

        mUploadPic.setImageBitmap(photo);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            GPSTracker.LocationServiceBinder binder = (GPSTracker.LocationServiceBinder) iBinder;
            gpsTracker = binder.getService();
            serviceConnected = true;
            getLocationAndAddress();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            gpsTracker = null;
            serviceConnected = false;
        }
    };

    private void getLocationAndAddress() {
        if (gpsTracker.canGetLocation()) {
            latitude = String.valueOf(decimalFormat.format(gpsTracker.getLatitude()));
            longitude = String.valueOf(decimalFormat.format(gpsTracker.getLongitude()));
            address = gpsTracker.getAddressLine(getApplicationContext()) + ", " + gpsTracker.getLocality(getApplicationContext());
            mLocationInfo.setText(String.format("Latitude: %s, Longitude: %s", latitude, longitude));
            mAddress.setText("Address: " + address);

        } else {
            showSettingsAlert();
        }
    }

    private void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Enable GPS");
        alertDialog.setMessage("Please Enable Location Service.");
        alertDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                ImageUploadActivity.this.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    @OnClick(R.id.btn_update)
    public void updatePhoto() {
        showProgressBar();
        TypedFile uploadFile = new TypedFile("image/*", file);
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(SERVICE_ENDPOINT)
                .setRequestInterceptor(requestInterceptor)
                .build();
        GrabHouseAPI grabHouseAPI = adapter.create(GrabHouseAPI.class);
        grabHouseAPI.updateImage(uploadFile, latitude, longitude, address, new Callback<JsonObject>() {
            @Override
            public void success(JsonObject jsonObject, Response response) {
                if (jsonObject != null) {
                    hideProgressBar();
                    Log.d("response", jsonObject.toString());
                    ItemDetail itemDetail = new ItemDetail();
                    itemDetail.setUrl(jsonObject.getAsJsonPrimitive("url").toString());
                    itemDetail.setLatitude(latitude);
                    itemDetail.setLongitude(longitude);
                    itemDetail.setAddress(address);
                    itemDetail.setTimestamp(dateFormat.format(new Date()));
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("data", itemDetail);
                    Intent returnIntent = new Intent();
                    returnIntent.putExtras(bundle);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("responseError", error.toString());
                hideProgressBar();
                mUpdate.setText("Retry");
                Toast.makeText(ImageUploadActivity.this, "Update Failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mUploadPic.setAlpha(.5f);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
        mUploadPic.setAlpha(.9f);
    }

    @OnClick(R.id.btn_cancel)
    public void onClickCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }


    RequestInterceptor requestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestFacade request) {
            request.addHeader("Accept", ACCEPT);
            request.addHeader("Content-Type", CONTENT_TYPE);
            request.addHeader("Accept-Language", ACCEPT_LANGUAGE);
        }
    };


    public interface GrabHouseAPI {
        @Multipart
        @POST("/test")
        void updateImage(@Part("image") TypedFile image, @Query("lat") String latitude, @Query("long") String longitude, @Query("address") String address, Callback<JsonObject> callback);
    }
}
