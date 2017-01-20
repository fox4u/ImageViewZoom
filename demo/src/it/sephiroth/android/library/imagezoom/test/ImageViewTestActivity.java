package it.sephiroth.android.library.imagezoom.test;

import android.app.Notification;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchDoubleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouch.OnImageViewTouchSingleTapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;
import it.sephiroth.android.library.imagezoom.test.utils.DecodeUtils;

public class ImageViewTestActivity extends AppCompatActivity
{

    private static final String LOG_TAG = "image-test";

    ImageViewTouch mImage;
    ImageButton mButton1;
    ImageButton mButton2;
    ImageButton mButtonCam;

    ImageViewTouchBase.Layer mPosIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Toast.makeText(this, "ImageViewTouch.VERSION: " + ImageViewTouch.VERSION, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mImage = (ImageViewTouch) findViewById(R.id.image);

        // set the default image display type
        mImage.setDisplayType(DisplayType.NONE);

        mImage.setSingleTapListener(
            new OnImageViewTouchSingleTapListener() {

                @Override
                public void onSingleTapConfirmed() {
                    Log.d(LOG_TAG, "onSingleTapConfirmed");
                }
            }
        );

        mImage.setDoubleTapListener(
            new OnImageViewTouchDoubleTapListener() {

                @Override
                public void onDoubleTap() {
                    Log.d(LOG_TAG, "onDoubleTap");
                }
            }
        );

        mImage.setOnDrawableChangedListener(
            new OnDrawableChangeListener() {

                @Override
                public void onDrawableChanged(Drawable drawable) {
                    Log.i(LOG_TAG, "onBitmapChanged: " + drawable);
                }
            }
        );

        mButton1 = (ImageButton) findViewById(R.id.button_plus);
        mButton2 = (ImageButton) findViewById(R.id.button_minus);
        mButtonCam = (ImageButton) findViewById(R.id.button_camera);

        mButton1.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mImage.zoomIn();
                    }
                }
        );

        mButton2.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mImage.zoomOut();
                    }
                }
        );

        mButtonCam.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PointF p = mPosIcon.getOffset();
                        Matrix m = mImage.getImageViewMatrix();
                        float pts[] = {p.x, p.y};
                        m.mapPoints(pts);
                        mImage.zoomToAndCenter(2.5f, pts[0], pts[1], 300);
                        //mPosIcon.setVisible(!mPosIcon.isVisible());
                    }
                }
        );
        selectImageByFile(Environment.getExternalStorageDirectory() + "/Download/lab_map.jpg");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    Matrix imageMatrix;

    protected void selectImageByUri(boolean small, Uri imageUri){

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        int size = (int) (Math.min(metrics.widthPixels, metrics.heightPixels) / 0.55);

        if (small) {
            size /= 3;
        }

        Bitmap bitmap = DecodeUtils.decode(this, imageUri, size, size);
        Bitmap overlay = BitmapFactory.decodeResource(getResources(), R.drawable.navi_map_gps_locked);

        if (null != bitmap) {
            Log.d(LOG_TAG, "screen size: " + metrics.widthPixels + "x" + metrics.heightPixels);
            Log.d(LOG_TAG, "bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            mImage.setOnDrawableChangedListener(
                    new OnDrawableChangeListener() {
                        @Override
                        public void onDrawableChanged(final Drawable drawable) {
                            Log.v(LOG_TAG, "image scale: " + mImage.getScale() + "/" + mImage.getMinScale());
                            Log.v(LOG_TAG, "scale type: " + mImage.getDisplayType() + "/" + mImage.getScaleType());

                        }
                    }
            );
            //mImage.setImageBitmap(bitmap, null, -1, -1);

            mImage.setImageDrawable(new FastBitmapDrawable(bitmap));
            PointF p = new PointF(670, 460);
            mPosIcon = mImage.addLayer(new FastBitmapDrawable(overlay));
            mPosIcon.setOffset(p);
            mPosIcon.setNoScale(true);
//            mPosIcon.setVisible(false);

        } else {
            Toast.makeText(this, "Failed to load the image", Toast.LENGTH_LONG).show();
        }

    }

    public void selectRandomImage(boolean small) {
        Cursor c = getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (c != null) {
            int count = c.getCount();
            int position = (int) (Math.random() * count);
            //int position = 0;
            if (c.moveToPosition(position)) {
                long id = c.getLong(c.getColumnIndex(Images.Media._ID));
                Uri imageUri = Uri.parse(Images.Media.EXTERNAL_CONTENT_URI + "/" + id);

                Log.d("image", imageUri.toString());
                selectImageByUri(small, imageUri);
            }
            c.close();
            return;
        }
    }

    public void selectImageByFile(String filePath){
        if(!filePath.contains("file://")) {
            filePath = "file://" + filePath;
        }
        Uri imageUri = Uri.parse(filePath);
        selectImageByUri(false, imageUri);
    }

    private Bitmap getOverlayBitmap(String name) {
        String file = null;

        if (TextUtils.isEmpty(name)) {
            try {
                String[] files = getAssets().list("images");

                if (null != files && files.length > 0) {
                    int position = (int) (Math.random() * files.length);
                    file = files[position];
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            file = name;
        }

        try {
            InputStream stream = getAssets().open("images/" + file);
            try {
                return BitmapFactory.decodeStream(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
