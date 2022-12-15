package com.strc_foxconn.owl_detection_camera;

import static android.content.Context.CAMERA_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.strc_foxconn.owl_detection_camera.listener.CameraCaptures;
import com.strc_foxconn.owl_detection_camera.listener.FaceDetectListener;
import com.strc_foxconn.owl_detection_camera.listener.OnMethodCallback;
import com.strc_foxconn.owl_detection_camera.utils.ToastUtils;
import com.strc_foxconn.owl_detection_camera.views.AutoFitTextureView;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraHelper
{
    private static final String TAG = "Owl_CameraHelper";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static int mRotateDegree;

    public static float PREVIEW_WIDTH = 900;                                             //預覽的寬度
    public static float PREVIEW_HEIGHT = 1600;

    public static float PREVIEW_RATE= PREVIEW_HEIGHT/PREVIEW_WIDTH;

    public static String sResolution = "";

    private final int SHOW_HIT_SECONDS = 120; //depends on fps.
    private final int BITMAP_SCALE_SIZE = 4;

    private int SAVE_WIDTH = 900;                                                 //保存圖片的寬度
    private int SAVE_HEIGHT = 1600;                                               //保存圖片的高度

    private int mCameraSensorOrientation = 0;                                       //攝影機方向

    private int mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;

    private int mDisplayRotation;                                                   //手機方向
    private int mImgRotateForMLKit = -1;

    private int mIsCapturing = Defines.CAMERA_ACTION.NONE;
    private int mBufferCount = 0; //以防相機例外為拍照。
    private int mCountWrongPost = 0;//用來提示使用者臉該往哪擺
    private int mDetectMode = OnMethodCallback.BLEND_MODE;

    public int mRotate = 0;

    private float mScaleX = 1,mScaleY = 1;

    private boolean mIsDetectFaceFromMLKit = false;

    private String mCameraId = "1";

    private RectF mFaceFrameRect;
    private RectF mCaptureFaceRect;

    private List<RectF> mFacesRect = new ArrayList<>();                            //保存人臉座標訊息

    private Handler mCameraHandler;
    private Handler mHandler;

    private HandlerThread handlerThread =null;

    private Size mPreviewSize = new Size((int)PREVIEW_WIDTH, (int)PREVIEW_HEIGHT);            //預覽大小
    private Size mSavePicSize = new Size(SAVE_WIDTH, SAVE_HEIGHT);                  //保存圖片大小

    private Activity mActivity;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraManager mCameraManager;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;

    private CameraCharacteristics mCameraCharacteristics;

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Surface mSurface;

    public Matrix mTextureViewMatrix;

    private CameraCaptures mCameraCaptures;

    private AutoFitTextureView mTextureView;

    private FaceDetectListener mFaceDetectListener;                                  //人臉偵測回調

    private FaceDetectorOptions mRealTimeOpts = null;
    private FaceDetector mDetector;

    private final MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    public void setFaceDetectListener(FaceDetectListener listener)
    {
        this.mFaceDetectListener = listener;
    }

    public CameraHelper(Activity activity, AutoFitTextureView autoFitTextureView)
    {
        mActivity = activity;
        mTextureView = autoFitTextureView;
    }

    public void setHandler(Handler aHandler)
    {
        mHandler = aHandler;
    }

    public void init()
    {
        mPreviewSize = new Size((int)PREVIEW_WIDTH, (int)PREVIEW_HEIGHT);
        mSavePicSize = new Size(SAVE_WIDTH, SAVE_HEIGHT);
        // Real-time contour detection
        if (mRealTimeOpts == null){
            FaceDetectorOptions.Builder temp = new FaceDetectorOptions.Builder();
            temp.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST);
            temp.setMinFaceSize(1);
            temp.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL);
            mRealTimeOpts = temp.build();
            mDetector = FaceDetection.getClient(mRealTimeOpts);
        }

        mRotate =0;
        mScaleX = 1;
        mScaleY = 1;
        mImgRotateForMLKit=-1;

        if(mTextureViewMatrix!=null)
        {
            mTextureViewMatrix.reset();
            mTextureViewMatrix = null;
        }

        if(Defines.sVersion == Defines.VERSION.JWS_DEVICE)
            mCameraFacing = 1; //JWS特殊硬體相機，1為RGB相機

        mDisplayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        if(handlerThread==null)
        {
            handlerThread = new HandlerThread("CameraThread");
            handlerThread.start();
        }

        mCameraHandler = new Handler(handlerThread.getLooper());

        mMultiFormatReader.setHints(null);

        if(mTextureView.isAvailable())
        {
            initCameraInfo();
            setTextureViewSurfaceTextureListeners();
        }
        else{
            setTextureViewSurfaceTextureListeners();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Defines.FILE_PATH = mActivity.getExternalFilesDir(null).getPath()+ "/OwlFaceIdCamera/";
        }
        else {
            Defines.FILE_PATH = mActivity.getFilesDir().getPath()+ "/OwlFaceIdCamera/";
        }
    }

    public void setTextureViewSurfaceTextureListeners()
    {
        if(mTextureView.getSurfaceTextureListener()==null)
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public void stop()
    {
        closeCamera();
    }

    public RectF getCaptureFaceRect()
    {
        return mCaptureFaceRect;
    }

    private void closeCamera()
    {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
        } catch (InterruptedException ie){
            ie.printStackTrace();
        }

        mCameraOpenCloseLock.release();
        mPreviewSize = new Size((int)PREVIEW_WIDTH,(int) PREVIEW_HEIGHT);
        mSavePicSize = new Size(SAVE_WIDTH, SAVE_HEIGHT);

        mCaptureRequestBuilder.removeTarget(mSurface);

        if (mCameraDevice!=null)
        {
//            mTextureView.setSurfaceTextureListener(null);
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void initCameraInfo()
    {
        mCameraManager = (CameraManager) mActivity.getSystemService(CAMERA_SERVICE);

        if (mCameraManager == null)
            return;

        String[] cameraIdList = new String[0];
        try
        {
            cameraIdList = mCameraManager.getCameraIdList();
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
            return;
        }

        if (cameraIdList.length == 0)
        {
            ToastUtils.showToast(mActivity, mActivity.getResources().getString(R.string.Com_no_available_camera));
            return;
        }

        for (String cameraId : cameraIdList)
        {
            CameraCharacteristics cameraCharacteristics = null;

            try {
                cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            if (cameraCharacteristics != null)
            {
                int facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                if(Defines.sVersion == Defines.VERSION.NORMAL)
                {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK)
                        continue;
                }

                if (facing == mCameraFacing) {
                    mCameraId = cameraId;
                    mCameraCharacteristics = cameraCharacteristics;
                }
            }
        }

        if (mCameraCharacteristics == null)
            return;

        //獲取攝影機方向
        mCameraSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        //獲取StreamConfigurationMap，他是管理攝影鏡頭支持的所有輸出格式和尺寸
        StreamConfigurationMap configurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] savePicSize = configurationMap.getOutputSizes(ImageFormat.JPEG);     //保存照片尺寸
        Size[] previewSize = configurationMap.getOutputSizes(SurfaceTexture.class); //預覽尺寸

        boolean exchange = exchangeWidthAndHeight(mDisplayRotation, mCameraSensorOrientation);

        mSavePicSize = getBestSize(
                exchange ? mSavePicSize.getHeight() : mSavePicSize.getWidth(),
                exchange ? mSavePicSize.getWidth() : mSavePicSize.getHeight(),
                exchange ? mSavePicSize.getHeight() : mSavePicSize.getWidth(),
                exchange ? mSavePicSize.getWidth() : mSavePicSize.getHeight(),
                Arrays.asList(savePicSize));
        sResolution = sResolution + "Save size: "+mSavePicSize+"\n";

        mPreviewSize = getBestSize(
                exchange ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                exchange ? mPreviewSize.getWidth() : mPreviewSize.getHeight(),
                exchange ? mTextureView.getHeight() : mTextureView.getWidth(),
                exchange ? mTextureView.getWidth() : mTextureView.getHeight(),
                Arrays.asList(previewSize));
        sResolution = sResolution + "Preview size: "+mSavePicSize+"\n";

        Log.d(TAG,"initCameraInfo() mPreviewSize Width: "+mPreviewSize.getWidth()+""+" Height: " +mPreviewSize.getHeight());
        Log.d(TAG,"initCameraInfo() mSavePicSize Width: "+mSavePicSize.getWidth()+""+" Height: " +mSavePicSize.getHeight());

        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

        //根據預覽的尺寸大小調整TextureView的大小，保證畫面不被拉伸
        int orientation = mActivity.getResources().getConfiguration().orientation;

        mTextureView.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            configureTextureViewTransform();
        } else {
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            if(Surface.ROTATION_180 == rotation)
                configureTextureViewTransform();
        }

        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG,7);
        mImageReader.setOnImageAvailableListener(onImageAvailableListener, mCameraHandler);

        if(Defines.sVersion == Defines.VERSION.JWS_DEVICE)
            configureTextureViewTransform();

        openCamera();
    }

    private void configureTextureViewTransform()
    {
        if (null == mTextureView)
            return;

        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, mPreviewSize.getWidth(),  mPreviewSize.getHeight());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
        {
            float scaleY = (float) mPreviewSize.getWidth() / mPreviewSize.getHeight();
            float scaleX = (float)  mPreviewSize.getHeight() / mPreviewSize.getWidth();
            Log.d(TAG,"configureTextureViewTransform s scaleX: "+scaleX+" scaleY: "+scaleY);
            mScaleX = scaleX;
            mScaleY = scaleY;
            mRotate = 90 * (rotation - 2);
            matrix.preScale(scaleX, scaleY, centerX, centerY);
            matrix.postRotate(mRotate, centerX, centerY);

            Log.d(TAG,"configureTextureViewTransform 90");
        }else if (Surface.ROTATION_180 == rotation) {
            Log.d(TAG,"configureTextureViewTransform 180");
            viewRect = new RectF(0, 0, mPreviewSize.getHeight(),  mPreviewSize.getWidth());
            centerX = viewRect.centerX();
            centerY = viewRect.centerY();
            mRotate = 180;
            matrix.postRotate(mRotate, centerX, centerY);
        }
        mTextureViewMatrix = matrix;
        mTextureView.setTransform(mTextureViewMatrix);
    }

    private CameraCaptureSession.StateCallback onStateCallback = new CameraCaptureSession.StateCallback()
    {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
        {
            mCameraCaptureSession = cameraCaptureSession;
            try {
                mPreviewRequest = mCaptureRequestBuilder.build();
                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallBack, mCameraHandler);
            } catch (Exception e) {
                Log.d(TAG, "createCaptureSession() onConfigured() error:" + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession)
        {
//            ToastUtils.showToast(mActivity, mActivity.getResources().getString(R.string.Com_open_camera_session_fail));
//                initCameraInfo();
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session)
        {
            super.onClosed(session);
            Log.d(TAG,"createCaptureSession() onClosed()");
        }
    };

    //保存圖片
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener()
    {
        @Override
        public void onImageAvailable(ImageReader imageReader)
        {
            //當確定有人臉Score超過50時，mIsCapturing設定為true.
            if(mIsCapturing == Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE)
            {
                Image tempImage = imageReader.acquireLatestImage();
                if(tempImage!=null)
                {
                    if(tempImage.getPlanes()!=null)
                    {
//                        Image image =  imageReader.acquireLatestImage();
                        ByteBuffer buf = tempImage.getPlanes()[0].getBuffer();
                        byte[] imageBytes= new byte[buf.remaining()];
                        buf.get(imageBytes);
                        final Bitmap bmp= BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
                        Log.d(TAG,"onImageAvailable() get picture bf callback face");
//                        Global.sCapturePictureTimingSeconds = Global.getCurrentSeconds();
                        if(mCaptureFaceRect == null)
                        {
                            closeFaceDetect(false);
                        }
                        else{
                            closeFaceDetect(true);
                            mCameraCaptures.onCaptureCallback(bmp,Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE,null,0);
                        }
                        Log.d(TAG,"onImageAvailable() get picture af callback face");
                        tempImage.close();

//                        unlockFocus();
                    }
                }
            }
            else if(mIsCapturing == Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE)
            {
                try
                {
                    Image tempImage = imageReader.acquireLatestImage();
                    if(tempImage!=null)
                    {
                        if(tempImage.getPlanes()!=null)
                        {
                            //資料有效寬度，一般的，圖片width <= rowStride，這也是導致byte[].length <= capacity的原因
                            // 所以只取width部分
                            int width = tempImage.getWidth();
                            int height = tempImage.getHeight();

                            ByteBuffer buf = tempImage.getPlanes()[0].getBuffer();
                            byte[] imageBytes= new byte[buf.remaining()];
                            buf.get(imageBytes);
                            final Bitmap bmp= BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
                            decode(bmp);
                            tempImage.close();
                        }
                    }
                }
                catch(Exception e)
                {
                    Log.d(TAG,"Exception: "+e.toString());
                }
            }
        }
    };

    //如果設備是支援google人臉偵測，這個值就算是true，也不會使用到MLKit人臉偵測
    public void startFaceDetectWithMLKit()
    {
        mIsDetectFaceFromMLKit = true;
    }

    public void setCameraCaptureListener(CameraCaptures aCameraCaptures)
    {
        mCameraCaptures = aCameraCaptures;
    }

    @SuppressLint("MissingPermission")
    private void openCamera()
    {
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback()
            {
                @Override
                public void onOpened(@NotNull CameraDevice aCameraDevice)
                {
                    mCameraOpenCloseLock.release();
                    mCameraDevice = aCameraDevice;
                    try {
                        createCaptureSession();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NotNull CameraDevice cameraDevice)
                {
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NotNull CameraDevice cameraDevice, int i) {
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                    ToastUtils.showToast(mActivity, mActivity.getResources().getString(R.string.Com_open_camera_fail));
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            ToastUtils.showToast(mActivity, mActivity.getResources().getString(R.string.Com_open_camera_fail) + e.getMessage());
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void closeFaceDetect(boolean aIsClose)
    {
        try
        {
            if(aIsClose)
            {
                mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE;
                mIsDetectFaceFromMLKit = false;
            }
            else
            {
                mIsCapturing = Defines.CAMERA_ACTION.NONE;
                mIsDetectFaceFromMLKit = true;
            }
        }
        catch(IllegalStateException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 創建預覽Session
     */
    private void createCaptureSession() throws CameraAccessException
    {
        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        mSurface = new Surface(mTextureView.getSurfaceTexture());
        mCaptureRequestBuilder.addTarget(mSurface);  //target為surface，就是將CaptureRequest的建構器與Surface對象綁定再一起
//        mCaptureRequestBuilder.addTarget(mImageReader.getSurface());  //target為surface，就是將CaptureRequest的建構器與Surface對象綁定再一起
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);     // 閃光燈
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);// 自動對焦

        mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()),onStateCallback, mCameraHandler);
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallBack = new CameraCaptureSession.CaptureCallback()
    {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
        {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure)
        {
            super.onCaptureFailed(session, request, failure);
            ToastUtils.showToast(mActivity, mActivity.getResources().getString(R.string.Com_open_camera_session_fail));
        }
    };

    private void captureStillPicture(CameraCaptureSession aCameraCaptureSession)
    {
        try {
            final Activity activity = mActivity;

            if (null == activity || null == mCameraDevice)
                return;

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            int newRotation = getJpegOrientation(mCameraCharacteristics,rotation);

            mRotateDegree = newRotation;
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, newRotation);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback()
            {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
                {
                }
            };

            //mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation)
    {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;

        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing) throws CameraAccessException
    {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (isFrontFacing)
        {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else
        { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;
    }

    //只有使用MLKit的時候才會呼叫這邊
    public void handleFaceFromMLKit()
    {
        if(mIsCapturing==Defines.CAMERA_ACTION.NONE)
        {
            //原本透過getScore來判定是否打卡或簽到，因為getScore在某些版本有問題，目前改成在這邊加入判斷人臉位置
            mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE;
            captureStillPicture(null);
        }
    }

    public void scanQRCode(Bitmap bmp)
    {
        mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE;
        decode(bmp);
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param aBitmap   The YUV preview frame.
     * using at QRCode flow.
     */
    private void decode(Bitmap aBitmap)
    {
        Result rawResult = null;

        int[] pixels = new int[aBitmap.getWidth() * aBitmap.getHeight()];
        aBitmap.getPixels(pixels, 0, aBitmap.getWidth(), 0, 0, aBitmap.getWidth(), aBitmap.getHeight());
        RGBLuminanceSource sourceRGB = new RGBLuminanceSource(aBitmap.getWidth(), aBitmap.getHeight(), pixels);

        HybridBinarizer temp = new HybridBinarizer(sourceRGB);
        BinaryBitmap bitmap = new BinaryBitmap(temp);

        try
        {
            rawResult = mMultiFormatReader.decodeWithState(bitmap);
        }
        catch (ReaderException re)
        {
            // continue
        } finally
        {
            mMultiFormatReader.reset();
        }

        if (rawResult != null)
        {
            mCameraCaptures.onCaptureCallback(null,Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE,rawResult,(int)0);
        }
        else
        {
            mIsCapturing = Defines.CAMERA_ACTION.NONE;
        }
    }

    public void setFaceFrameRect(RectF faceFrameRect)
    {
        mFaceFrameRect = faceFrameRect;
    }

    public void setDetectModel(int aDetectMode)
    {
        mDetectMode = aDetectMode;
    }

    /**
     * 根據提供的參數值返回與指定寬高相等或是最接近的尺寸
     *
     * @param targetWidth  目標寬度
     * @param targetHeight 目標高度
     * @param maxWidth     最大寬度(即TextureView寬度)
     * @param maxHeight    最大高度(即TextureView的高度)
     * @param sizeList     支持的Size列表
     * @return 返回與指定寬高相等或最接近的尺寸
     */
    private Size getBestSize(int targetWidth, int targetHeight, int maxWidth, int maxHeight, List<Size> sizeList)
    {
//        float rate = 0.5f; //按比例找最接近的
        float rate = 500.0f;//長寬差值
        Size pickerSize = null;
        ArrayList<Size> pickList = new ArrayList<>();

//        Log.d(TAG,"getBestSize() CameraHelper.PREVIEW_RATE: "+CameraHelper.PREVIEW_RATE);
//        float screenRate = (float)MainActivity.sRealWindowHeight/(float)MainActivity.sRealWindowWidth;
//        Log.d(TAG,"getBestSize() CameraHelper screenRate: "+screenRate);

        int screenSmallBorder = Math.min(CameraView.sRealScreenWidth, CameraView.sRealScreenHeight);
        int screenBigBorder = Math.max(CameraView.sRealScreenWidth, CameraView.sRealScreenHeight)+CameraView.sRealStatusBarHeight;

        for (Size size : sizeList)
        {
            sResolution = sResolution + size.getWidth()+"x"+size.getHeight()+"\n";
            float sizeBigBorder = Math.max(size.getWidth(), size.getHeight());
            float sizeSmallBorder = Math.min(size.getWidth(), size.getHeight());
//            float sizeRate = sizeBigBorder/sizeSmallBorder;//按比例找最接近的
            Log.d(TAG,"getBestSize() CameraView.sRealWindowWidth: "+CameraView.sRealScreenWidth +" MainActivity.sRealWindowHeight: "+CameraView.sRealScreenHeight);
//            Log.d(TAG,"getBestSize() sizeSmallBorder: "+sizeSmallBorder+" sizeBigBorder: "+sizeBigBorder);
            float newWidth = Math.abs(screenSmallBorder - sizeSmallBorder);
            float newHeight = Math.abs(screenBigBorder - sizeBigBorder);
            float diff = newWidth+newHeight;
//            Log.d(TAG,"getBestSize() diff: "+diff);
            if(diff < rate && (sizeSmallBorder >= screenSmallBorder) && sizeBigBorder < 2500 )
            {
                rate = diff;
                pickerSize = size;
            }

//            if(Math.abs(sizeRate-screenRate) <= rate)
//            {
//                if(sizeBigBorder < 2000 && sizeBigBorder>1000)
//                {
//                pickerSize = size;
////                pickerSize = new Size((int)sizeSmallBorder,(int)sizeBigBorder);
//                rate = Math.abs(sizeRate-screenRate);
//                pickList.add(pickerSize);
////                    break;
//                }
//            }
        }

        if(pickerSize == null)
        {
            //透過寬高找最適大小
            List<Size> bigEnough = new ArrayList<>();   //比指定寬高大的Size列表
            List<Size> notBigEnough = new ArrayList<>(); //比指定寬高小的Size列表

            for (Size size : sizeList)
            {
                float sizeSmallBorder = Math.min(size.getWidth(), size.getHeight());
                float sizeBigBorder = Math.max(size.getWidth(), size.getHeight());

                //寬<=最大寬度  &&  高<=最大高度  &&  寬高比 == 目标值寬高比
                if (sizeSmallBorder >= screenSmallBorder && sizeBigBorder >= screenBigBorder && sizeSmallBorder == sizeBigBorder * 900 / 1600)
                {
                    if (sizeSmallBorder >= 900 && sizeBigBorder >= 1600)
                    {
                        bigEnough.add(size);
                    } else {
                        notBigEnough.add(size);
                    }
                }
            }

            //選擇bigEnough中最小的值  或 notBigEnough中最大的值
            if (bigEnough.size() > 0)
            {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else if (notBigEnough.size() > 0)
            {
                return Collections.max(notBigEnough, new CompareSizesByArea());
            } else
            {
                return sizeList.get(0);
            }
            //透過寬高最適大小
        }
        else{
            return pickerSize;
        }

    }

    private class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size size1, Size size2)
        {
            return Long.signum(size1.getWidth() * size1.getHeight() - size2.getWidth() * size2.getHeight());
        }
    }

    //根據提供的屏幕方向[displayRotation]和相機方向[sensorOrientation]返回是否需要交換寬高
    private boolean exchangeWidthAndHeight(int displayRotation, int sensorOrientation)
    {
        boolean exchange = false;
        if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180)
        {
            if (sensorOrientation == 90 || sensorOrientation == 270)
                exchange = true;
        }

        if (displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270)
        {
            if (sensorOrientation == 0 || sensorOrientation == 180)
                exchange = true;
        }
        return exchange;
    }

    private void releaseCamera()
    {
        if (mCameraCaptureSession != null)
            mCameraCaptureSession.close();

        mCameraCaptureSession = null;

        if (mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mImageReader != null)
        {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
        {
            initCameraInfo();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
        {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
        {
            releaseCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface)
        {
            if(mIsDetectFaceFromMLKit && mIsCapturing == Defines.CAMERA_ACTION.NONE)
            {
                mIsDetectFaceFromMLKit = false;

                if(mDetectMode != OnMethodCallback.QRCODE_MODE)
                {
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Bitmap frame = mTextureView.getBitmap();
                            Bitmap originalBitmap = Bitmap.createBitmap(frame,0,0,frame.getWidth(),frame.getHeight(),mTextureViewMatrix,false);
                            Bitmap textureScaleBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) originalBitmap.getWidth() / BITMAP_SCALE_SIZE, (int) originalBitmap.getHeight() / BITMAP_SCALE_SIZE, false);

                            if(mImgRotateForMLKit==-1)
                                mImgRotateForMLKit = 0;

                            //混合模式必須以偵測人臉為主來切會QRCode/FaceDetection mode.
                            InputImage inputImage = InputImage.fromBitmap(textureScaleBitmap, 0);
                            mDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<com.google.mlkit.vision.face.Face>>()
                            {
                                @Override
                                public void onSuccess(List<com.google.mlkit.vision.face.Face> faces)
                                {
                                    if (faces != null && !faces.isEmpty())
                                    {
                                        RectF bounds = new RectF(faces.get(0).getBoundingBox().left * BITMAP_SCALE_SIZE, faces.get(0).getBoundingBox().top * BITMAP_SCALE_SIZE, faces.get(0).getBoundingBox().right * BITMAP_SCALE_SIZE, faces.get(0).getBoundingBox().bottom * BITMAP_SCALE_SIZE);

                                        mFacesRect.add(bounds);
                                        mActivity.runOnUiThread(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                mFaceDetectListener.onFaceDetect(null, mFacesRect.remove(0));
                                            }
                                        });

                                        if(mCountWrongPost==0)
                                        {
                                            Message msg = new Message();
                                            msg.what = R.id.show_face_hint;
                                            msg.arg1 = Defines.DETECTION_HINT_FIT_CENTER;
                                            msg.obj = bounds;
                                            mHandler.sendMessage(msg);
                                            mCountWrongPost = SHOW_HIT_SECONDS;
                                        }

                                        //原本透過getScore來判定是否打卡或簽到，因為getScore在某些版本有問題，目前改成在這邊加入判斷人臉位置
                                        if (mFaceFrameRect.contains(bounds))
                                        {
                                            //抓到小於300的臉忽略。
                                            if(Math.min(bounds.width(),bounds.height())<300)
                                            {
                                                if(mCountWrongPost<10)
                                                {
                                                    Message msg = new Message();
                                                    msg.what = R.id.show_face_hint;
                                                    msg.arg1 = Defines.DETECTION_HINT_FORWARD;
                                                    msg.obj = bounds;
                                                    mHandler.sendMessage(msg);
                                                    mCountWrongPost = SHOW_HIT_SECONDS;// depends on fps
                                                }
                                                else
                                                {
                                                    mCountWrongPost--;
                                                }
                                                mIsDetectFaceFromMLKit = true;
                                            }
                                            else
                                            {
                                                Log.d(TAG,"handleFaces() get face mFaceFrameRect.contains(rawFaceRect)");
                                                mCaptureFaceRect = bounds;
                                                closeFaceDetect(true);
                                                RectF newT = new RectF(bounds.left,  bounds.top,  bounds.right, bounds.bottom);
                                                //1.5倍做法
                                                float gapW = (float)((newT.width()*1.5-newT.width())/2);
                                                newT.left = newT.left-gapW;
                                                newT.right = newT.right+gapW;
                                                float gapH = (float)((newT.height()*1.5-newT.height())/2);
                                                newT.top = newT.top-gapH;
                                                newT.bottom = newT.bottom+gapH;
                                                Bitmap temp = Bitmap.createBitmap(originalBitmap, (int) newT.left, (int) newT.top, (int) newT.width(), (int) newT.height());
                                                mCameraCaptures.onCaptureCallback(temp, Defines.CAMERA_ACTION.IS_CAPTURING_FROM_FACE, null, 0);
                                                mCountWrongPost = 0;
                                            }
                                        }
                                        else
                                        {
                                            double distance = Math.sqrt(Math.pow(mFaceFrameRect.centerX()-bounds.centerX(),2)+Math.pow(mFaceFrameRect.centerY()-bounds.centerY(),2));
                                            Log.d(TAG,"distance: "+distance+" mCountWrongPost: "+mCountWrongPost );
                                            if(mCountWrongPost<10)
                                            {
                                                if(distance>45 && (mFaceFrameRect.width() < bounds.width() || mFaceFrameRect.height() < bounds.height()))
                                                {
                                                    Message msg = new Message();
                                                    msg.what = R.id.show_face_hint;
                                                    msg.arg1 = Defines.DETECTION_HINT_BACKWARD;
                                                    msg.obj = bounds;
                                                    mHandler.sendMessage(msg);
                                                    mCountWrongPost = SHOW_HIT_SECONDS;
                                                }
                                            }

                                            if(mCountWrongPost>=10)
                                                mCountWrongPost--;

                                            mIsDetectFaceFromMLKit = true;
                                        }
                                    }else
                                    {
                                        if(mDetectMode == OnMethodCallback.BLEND_MODE)
                                        {
                                            mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE;
                                            decode(textureScaleBitmap);
                                        }

                                        mIsDetectFaceFromMLKit = true;
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener()
                            {
                                    @Override
                                    public void onFailure(@NonNull Exception e)
                                    {
                                        if(mDetectMode == OnMethodCallback.BLEND_MODE){
                                            mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE;
                                            decode(textureScaleBitmap);
                                        }
                                        mIsDetectFaceFromMLKit = true;
                                    }
                            });
                        }
                    }).start();
                }
                else
                {
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Bitmap frame = mTextureView.getBitmap();
                            Bitmap originalBitmap = Bitmap.createBitmap(frame,0,0,frame.getWidth(),frame.getHeight(),mTextureViewMatrix,false);
                            Bitmap textureScaleBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) originalBitmap.getWidth() / BITMAP_SCALE_SIZE, (int) originalBitmap.getHeight() / BITMAP_SCALE_SIZE, false);
                            mIsCapturing = Defines.CAMERA_ACTION.IS_CAPTURING_FROM_QRCODE;
                            decode(textureScaleBitmap);
                            mIsDetectFaceFromMLKit = true;
                        }
                    }).start();
                }
            }
        }
    };
}
