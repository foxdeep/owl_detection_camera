package com.strc_foxconn.owl_detection_camera.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView
{
    private final String TAG = "Owl_AutoFitTextureView";

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(mRatioHeight, mRatioWidth);
        }
    }
}

