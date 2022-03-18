package com.strc_foxconn.owl_detection_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.params.Face;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.zxing.Result;
import com.strc_foxconn.owl_detection_camera.listener.CameraCaptures;
import com.strc_foxconn.owl_detection_camera.listener.FaceDetectListener;
import com.strc_foxconn.owl_detection_camera.listener.OnMethodCallback;
import com.strc_foxconn.owl_detection_camera.utils.ToastUtils;
import com.strc_foxconn.owl_detection_camera.utils.Utility;
import com.strc_foxconn.owl_detection_camera.views.AutoFitTextureView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.plugin.platform.PlatformView;

public class CameraView implements PlatformView,Handler.Callback, FaceDetectListener, CameraCaptures
{
    private final String TAG = "owlCPCameraView";

    public static int sRealWindowWidth;
    public static int sRealWindowHeight;

    public static String sResolution = "";

    private int mStartX, mStartY;

    private boolean mHasInit = false;

    private String mHintFitCenter = "請將臉部，對應人像框";
    private String mHintForward = "請向前一點";
    private String mHintBackward = "有點太近了";

    private RectF mFaceFrameRect;

    private Handler mHandler = new Handler(this);

    private LayoutInflater mInflater;
    private View mConvertView;

    private CameraHelper mCameraHelper;
    private AutoFitTextureView mTextureView;

    private Context mContext;

    CameraView(Context context, int viewId, Map<String, Object> creationParams)
    {
        mContext = context;
        mInflater = LayoutInflater.from(context);  //動態布置
        mConvertView = mInflater.inflate(R.layout.camera_main_layout, null);

        init();
//      Log.d(TAG,"Rendered on a native Android view (id: " + viewId + ")");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void init()
    {
        mTextureView = mConvertView.findViewById(R.id.texture);

        Display display = OwlDetectionCameraPlugin.mActivity.getWindowManager().getDefaultDisplay();
        sRealWindowWidth = display.getWidth();
        sRealWindowHeight = display.getHeight();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        sResolution = sResolution + "sRealWindowWidth: "+sRealWindowWidth+" sRealWindowHeight: "+sRealWindowHeight+"\n";

        Defines.sFACE_SCALE = (float)Defines.METRICS_XDPI_SRANDARD /metrics.xdpi;
        int densityDpi = (int)(metrics.density * 160f);
        float changeStandard = Defines.METRICS_XDPI_SRANDARD;
        if(metrics.density!=3)
        {
            float gap = 76*((metrics.density/3));
            changeStandard = densityDpi-gap;
        }

        Log.d(TAG,"init() changeStandard: "+changeStandard);

        Defines.sFACE_SCALE  = metrics.xdpi/changeStandard;
        Defines.sFACE_SCALE_Y  = metrics.ydpi/changeStandard;

        mCameraHelper = new CameraHelper(OwlDetectionCameraPlugin.mActivity, mTextureView);
        mCameraHelper.setCameraCaptureListener(this);
        mCameraHelper.setFaceDetectListener(this);
        mCameraHelper.setHandler(mHandler);

        mStartX = (sRealWindowWidth / 2) - (OwlDetectionCameraPlugin.mFaceFrameSize.get(0) / 2);
        mStartY = (sRealWindowHeight / 2) - (OwlDetectionCameraPlugin.mFaceFrameSize.get(1) / 2);

        if(!mHasInit)
        {
            mCameraHelper.init();
            mHasInit = true;
        }

        float faceGap = mContext.getResources().getDimension(R.dimen.detect_face_gap);

        mFaceFrameRect = new RectF(mStartX+faceGap, mStartY+faceGap, sRealWindowWidth - mStartX-faceGap , sRealWindowHeight - mStartY - faceGap);

        mCameraHelper.setFaceFrameRect(mFaceFrameRect);

        //from native
        OwlDetectionCameraPlugin.setOnMethodCallBack(new OnMethodCallback()
        {
            @Override
            public void onStartDetectFace()
            {
                Log.d("20220317Josh","closeFaceDetect() 2");

                if(mCameraHelper!=null)
                    mCameraHelper.closeFaceDetect(false);
            }

            @Override
            public void onStopDetectFace()
            {
                if(mCameraHelper!=null)
                    mCameraHelper.closeFaceDetect(true);
            }

            @Override
            public void onResumeEvent()
            {
                intoOnResumeEvent();
            }

            @Override
            public void onPauseEvent()
            {
                initOnPausePause();
            }

            @Override
            public void onScreenBright(int aValue)
            {
                switch(aValue)
                {
                    case OnMethodCallback.SCREEN_BRIGHT:
                    {
                        setScreenBright(100);
                    }
                    break;
                    case OnMethodCallback.SCREEN_DARK:
                    {
                        setScreenBright(10);
                    }
                    break;
                }
            }

            @Override
            public void onSetFaceDetectionHintText(String aFitCenter, String aHitForward, String aHintBackward)
            {
                mHintFitCenter = aFitCenter;
                mHintForward = aHitForward;
                mHintBackward = aHintBackward;
            }
        });
    }

    private void setScreenBright(int aBrightness)
    {
        WindowManager.LayoutParams layoutParams = OwlDetectionCameraPlugin.mActivity.getWindow().getAttributes();
        layoutParams.screenBrightness = aBrightness / (float)255;
        OwlDetectionCameraPlugin.mActivity.getWindow().setAttributes(layoutParams);
    }

    public void intoOnResumeEvent()
    {
        if(!mHasInit)
        {
            mCameraHelper.init();
            mHasInit = true;
        }

        if(mCameraHelper !=null)
        {
            Log.d("20220317Josh","closeFaceDetect() 3");

            mCameraHelper.closeFaceDetect(false);
            mCameraHelper.startFaceDetectWithMLKit();
        }

        Utility.deleteFolder();
    }

    public void initOnPausePause()
    {
        mHasInit = false;
        if(mCameraHelper!=null)
            mCameraHelper.stop();
    }

    //透過這邊回傳所要的原生畫面
    @Override
    public View getView()
    {
        return mConvertView;
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public boolean handleMessage(@NonNull Message msg)
    {
        if (msg.what == R.id.get_face_rect)
        {

        }
        else if(msg.what == R.id.start_capture)
        {
            Log.d("20220317Josh","closeFaceDetect() 4");

            mCameraHelper.closeFaceDetect(false);
            mCameraHelper.startFaceDetectWithMLKit();
        }
        else if(msg.what == R.id.start_detect_face) //只有使用MLKit的時候才會呼叫這邊
        {
            if(msg.arg1!=0)
            {
                mCameraHelper.handleFaceFromMLKit();
            }
            else{
                Bitmap bitmap = (Bitmap)msg.obj;
                mCameraHelper.scanQRCode(bitmap,bitmap.getWidth(),bitmap.getHeight());
                mCameraHelper.startFaceDetectWithMLKit();
            }
        }
        else if(msg.what == R.id.show_face_hint)
        {
            int action = msg.arg1;
            switch(action)
            {
                case Defines.DETECTION_HINT_FIT_CENTER:
                {
                    ToastUtils.showToastForFaceHint(mContext,mHintFitCenter);
                }
                break;
                case Defines.DETECTION_HINT_FORWARD:
                {
                    ToastUtils.showToastForFaceHint(mContext,mHintForward);
                }
                break;
                case Defines.DETECTION_HINT_BACKWARD:
                {
                    ToastUtils.showToastForFaceHint(mContext,mHintBackward);
                }
                break;
            }
        }
        return false;
    }

    @Override
    public void onFaceDetect(List<Face> aFaces, RectF aFacesRect)
    {
        double[] faceRectangle = new double[]{aFacesRect.left,aFacesRect.top,aFacesRect.right,aFacesRect.bottom};
        HashMap<String,double[]> arguments = new HashMap<>();
        arguments.put("face_rectangle",faceRectangle);
        OwlDetectionCameraPlugin.mMethodChannel.invokeMethod(OwlDetectionCameraPlugin.mMethodChannels.INVOKE_ID_FACE_RECTANGLE,arguments);
    }

    @Override
    public void onCaptureCallback(Bitmap aBitmap, int aAction, Result aQRCodeResult, int aScaleFactor)
    {
        (OwlDetectionCameraPlugin.mActivity).runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                 switch(aAction)
                 {
                     case Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE:
                     {
                         Bitmap recognizeBitmap = resizeBitmap(aBitmap);
                         String aTempUUID = UUID.randomUUID().toString();
                         new saveToFileTask(recognizeBitmap,aTempUUID+".jpg").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                     }
                     break;
                     case Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE:
                     {
                         String text = aQRCodeResult.getText();
                         Log.d(TAG,"onCaptureCallback() IS_CAPTURING_FROM_QRCODE text: "+text);
                         HashMap<String,String> arguments = new HashMap<>();
                         arguments.put("qrcode_result",text);
                         OwlDetectionCameraPlugin.mMethodChannel.invokeMethod(OwlDetectionCameraPlugin.mMethodChannels.INVOKE_ID_QRCODE_TEXT,arguments);
                         mHandler.sendEmptyMessage(R.id.start_capture);
                     }
                     break;
                 }
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap aBitmap)
    {
        Log.d(TAG, "resizeBitmap() getWidth: " + aBitmap.getWidth() + " getHeight: " + aBitmap.getHeight());
        RectF faceRect = mCameraHelper.getCaptureFaceRect();
        Bitmap temp = aBitmap;
        int midWidth = temp.getWidth()/2;
        boolean usingFaceRect = false;
        if(midWidth > faceRect.width())
        {
            usingFaceRect = true;
            //1.5 times
            float gapW = (float)((faceRect.width()*1.5-faceRect.width())/2);
            faceRect.left = faceRect.left-gapW;
            faceRect.right = faceRect.right+gapW;

            float gap = (float)((faceRect.height()*1.5-faceRect.height())/2);
            faceRect.top = faceRect.top-gap;
            faceRect.bottom = faceRect.bottom+gap;
        }

        //become to 500
//        if(faceRect.width()<500)
//        {
//            float gap = (500-faceRect.width())/2;
//            faceRect.left = faceRect.left-gap;
//            faceRect.right = faceRect.right+gap;
//        }
//
//        if(faceRect.height()<500)
//        {
//            float gap = (500-faceRect.height())/2;
//            faceRect.top = faceRect.top-gap;
//            faceRect.bottom = faceRect.bottom+gap;
//        }

        if(faceRect.left<0)
            faceRect.left = 0;
        if(faceRect.right>sRealWindowWidth)
            faceRect.right=sRealWindowWidth;

        if(faceRect.top<0)
            faceRect.top = 0;
        if(faceRect.bottom>sRealWindowHeight)
            faceRect.bottom=sRealWindowHeight;

        Matrix matrix = new Matrix();
        if (aBitmap.getWidth() > aBitmap.getHeight())
        {
            matrix.postRotate(CameraHelper.mRotateDegree);
            matrix.preScale(-1f,1f);
            temp = Bitmap.createBitmap(aBitmap, 0, 0, aBitmap.getWidth(), aBitmap.getHeight(), matrix, true);
            if(usingFaceRect)
                temp = Bitmap.createBitmap(temp, (int)faceRect.left, (int)faceRect.top,(int)faceRect.width(), (int)faceRect.height());
            temp = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }
        else
        {
            matrix.preScale(-1f,1f);
            temp = Bitmap.createBitmap(aBitmap, 0, 0, aBitmap.getWidth(), aBitmap.getHeight(), matrix, true);
            if(usingFaceRect)
                temp = Bitmap.createBitmap(temp, (int)faceRect.left, (int)faceRect.top,(int)faceRect.width(), (int)faceRect.height());
            temp = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
        }

        Bitmap resizedBitmap = null;
        if (Math.max(temp.getWidth(), temp.getHeight()) > 1500)
        {
            int scaleSize = getScale(temp.getWidth(), temp.getHeight(), 1000, 1000);
            int newWidth = temp.getWidth() / scaleSize;
            int newHeight = temp.getHeight() / scaleSize;
            Log.d(TAG, "resizeBitmap() newWidth: " + newWidth + " newHeight: " + newHeight);
            resizedBitmap = Bitmap.createScaledBitmap(temp, newWidth, newHeight, false);
        }
        else
        {
            resizedBitmap = temp;
        }

        return resizedBitmap;
    }

    private int getScale(int originalWidth, int originalHeight, int aScreenWidth, int aScreenHeight)
    {
        float ratioWidth = (float) Math.max(originalWidth, originalHeight) / (float) Math.max(aScreenWidth, aScreenHeight);
        float ratioHeight = (float) Math.min(originalWidth, originalHeight) / (float) Math.min(aScreenWidth, aScreenHeight);
        float getScale = Math.max(ratioWidth, ratioHeight);
        return (int) Math.ceil(getScale);
    }

    class saveToFileTask extends AsyncTask<String, Void, File>
    {
        Bitmap mBitmap = null;
        String mFileName = "";

        public saveToFileTask(Bitmap aBitmap, String aFileName)
        {
            mBitmap = aBitmap;
            mFileName = aFileName;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected File doInBackground(String... photoName)
        {
            return Defines.savePicture(mBitmap, Defines.FILE_PATH, mFileName, 100);
        }

        @Override
        protected void onPostExecute(File file)
        {
            super.onPostExecute(file);

            HashMap<String,String> arguments = new HashMap<>();
            arguments.put("imageName",mFileName);
            arguments.put("imagePath",file.getAbsolutePath());
            OwlDetectionCameraPlugin.mMethodChannel.invokeMethod(OwlDetectionCameraPlugin.mMethodChannels.INVOKE_ID_IMAGE_PATH_NAME,arguments);
        }
    }
}

