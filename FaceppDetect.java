package net.macdidi.face_detection;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


public class FaceppDetect {
    public interface CallBack {
        void success(JSONObject result);

        void error(FaceppParseException exception);
    }


    public static void detect(final Bitmap bitmap, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    Bitmap bmSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    byte[] arrays = stream.toByteArray();
                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject jsonObject = requests.detectionDetect(parameters);

                    if(callBack != null) {
                        callBack.success(jsonObject);
                    }
                }
                catch (FaceppParseException e) {
                    e.printStackTrace();

                    if(callBack != null) {
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }
}
