package com.strc_foxconn.owl_detection_camera.views;

import static com.strc_foxconn.owl_detection_camera.CameraView.sRealScreenHeight;
import static com.strc_foxconn.owl_detection_camera.CameraView.sRealScreenWidth;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView
{
    private final String TAG = "Owl_AutoFitTextureView";

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private double mToShortBoardRate = 1.0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height)
    {
        if (width < 0 || height < 0)
        {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        Log.d(TAG,"setAspectRatio() width: "+width+" height: "+height);
        requestLayout();
    }

    public int[] getRatioWH()
    {
        return new int[]{mRatioWidth,mRatioHeight};
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(sRealScreenWidth, sRealScreenHeight);

//        if (0 == mRatioWidth || 0 == mRatioHeight) {
//            setMeasuredDimension(width, height);
//        } else {
//            if(width > mRatioHeight && height > mRatioWidth)
//            {
//                mToShortBoardRate = (float)width/(float)mRatioHeight;
//                int newHeight =(int)(mRatioHeight*mToShortBoardRate);
//                int newWidth =(int)(mRatioWidth*mToShortBoardRate);
//                Log.d(TAG,"onMeasure() newWidth: "+newWidth+" newHeight: "+newHeight);
//                setMeasuredDimension(newHeight,newWidth);
//            }
//            else{
//                //為了不讓相機影像變行成螢幕寬高比例。
//                setMeasuredDimension(mRatioWidth, mRatioHeight);
//            }
//        }
    }
}

