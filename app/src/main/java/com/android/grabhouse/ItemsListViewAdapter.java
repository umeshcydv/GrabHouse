package com.android.grabhouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by umeshchandrayadav on 21/03/15.
 */
public class ItemsListViewAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<ItemDetail> itemDetails;
    private Context context;

    public ItemsListViewAdapter(List<ItemDetail> itemDetails, LayoutInflater inflater, Context context) {
        this.itemDetails = itemDetails;
        this.inflater = inflater;
        this.context = context;
    }

    @Override
    public int getCount() {
        return itemDetails.size();
    }

    @Override
    public Object getItem(int position) {
        return itemDetails.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.cell_image, parent, false);
            convertView.setTag(new ViewModal(convertView));
        }
        ViewModal viewModal = (ViewModal) convertView.getTag();
        viewModal.bindData(position, (ItemDetail) getItem(position));
        return convertView;
    }

    public class ViewModal {

        @InjectView(R.id.iv_pic) ImageView mItemImage;
        @InjectView(R.id.tv_location_info) TextView mItemLoactionDetail;
        @InjectView(R.id.tv_address) TextView mItemAddress;
        @InjectView(R.id.tv_timestamp) TextView mItemUploadTimestamp;
        @InjectView(R.id.pb_loading) ProgressBar mProgressBar;

        public ViewModal(View view) {
            ButterKnife.inject(this, view);
        }

        public void bindData(int position, ItemDetail itemDetail) {
            if (itemDetail.getUrl() != null) {
                ImageLoader.getInstance().displayImage(itemDetail.getUrl(), mItemImage, MainActivity.options, new SimpleImageLoadingListener() {

                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
                        mProgressBar.setProgress(0);
                        mProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        mProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                }, new ImageLoadingProgressListener() {
                    @Override
                    public void onProgressUpdate(String imageUri, View view, int current, int total) {
                        mProgressBar.setProgress(Math.round(100.0f * current / total));
                    }
                });
            } else if (itemDetail.getBitmap() != null) {
                mItemImage.setImageBitmap(itemDetail.getBitmap());
            }
            if (itemDetail.getLatitude() != null && itemDetail.getLongitude() != null) {
                mItemLoactionDetail.setText(String.format("Latitude: %s, Longitude: %s", itemDetail.getLatitude(), itemDetail.getLatitude()));
            }
            if (itemDetail.getAddress() != null) {
                mItemAddress.setText(String.format("Address: %s", itemDetail.getAddress()));
            }
            if (itemDetail.getTimestamp() != null) {
                mItemUploadTimestamp.setText(String.format("Updated: %s", itemDetail.getTimestamp()));
            }
        }

    }


}
