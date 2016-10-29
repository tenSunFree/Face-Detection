package net.macdidi.face_detection;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_CODE = 0x110;
    private ImageView mPhoto;
    private Button mGetImage;
    private Button mDetect;
    private View mWaitting;
    private TextView mTip;
    private String mCurrentPhotoStr;
    private Bitmap mPhotoImg;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        initEvent();

        mPaint = new Paint();
    }


    private void initViews() {
        mPhoto = (ImageView) findViewById(R.id.photo);
        mGetImage = (Button) findViewById(R.id.getImage);
        mDetect = (Button) findViewById(R.id.detect);
        mWaitting = findViewById(R.id.waitting);
        mTip = (TextView) findViewById(R.id.tip);
    }


    private void initEvent() {
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == PICK_CODE) {
            if(intent != null) {
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cursor.getString(idx);
                cursor.close();

                resizePhoto();

                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("開始分析吧 →");
            }
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }


    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoStr, options);

        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr, options);
    }


    private static final int MSG_SUCESS = 0x111;
    private static final int MSG_ERROR = 0x112;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {            
                case MSG_SUCESS :
                    mWaitting.setVisibility(View.GONE);

                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);

                    mPhoto.setImageBitmap(mPhotoImg);

                    break;

                case MSG_ERROR :
                    mWaitting.setVisibility(View.GONE);

                    String errorMsg = (String) msg.obj;

                    if(TextUtils.isEmpty(errorMsg)) {
                        mTip.setText("請連接網路");
                    }
                    else {
                        mTip.setText(errorMsg);
                    }

                    break;
            }

            super.handleMessage(msg);
        }
    };


    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg, 0, 0, null);

        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("已完成 " + faceCount + " 人分析");

            for(int i = 0; i < faceCount; i++) {
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");
                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();
                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();

                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);

                canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);
                canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);
                canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2, mPaint);

                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if(bitmap.getWidth() < mPhoto.getWidth() && bitmap.getHeight() < mPhoto.getHeight()) {
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / mPhoto.getWidth(), bitmap.getHeight() * 1.0f / mPhoto.getHeight());
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
                }

                canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h / 2 - ageBitmap.getHeight(), null);

                mPhotoImg = bitmap;
            }
        } 
        catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView textView = (TextView) mWaitting.findViewById(R.id.age_gender);
        textView.setText(age + "");

        if(isMale) {
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.f_01), null, null, null);
        }
        else {
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.f_02), null, null, null);
        }

        textView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(textView.getDrawingCache());
        textView.destroyDrawingCache();

        return bitmap;
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.getImage :
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                
                break;

            case R.id.detect :
                mWaitting.setVisibility(View.VISIBLE);

                if(mCurrentPhotoStr != null && !mCurrentPhotoStr.trim().equals("")) {
                    resizePhoto();
                }
                else {
                    mPhotoImg = BitmapFactory.decodeResource(getResources(), R.drawable.i_03);
                }

                FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message message = Message.obtain();
                        message.what = MSG_SUCESS;
                        message.obj = result;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message message = Message.obtain();
                        message.what = MSG_ERROR;
                        message.obj = exception.getErrorMessage();
                        handler.sendMessage(message);
                    }
                });

                break;
        }
    }
}
