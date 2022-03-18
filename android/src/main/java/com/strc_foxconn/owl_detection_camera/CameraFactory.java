package com.strc_foxconn.owl_detection_camera;

import android.content.Context;

import java.util.Map;

import io.flutter.plugin.common.MessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class CameraFactory extends PlatformViewFactory
{
    /**
     * @param createArgsCodec the codec used to decode the args parameter of {@link #create}.
     */
    public CameraFactory(MessageCodec<Object> createArgsCodec)
    {
        super(createArgsCodec);
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args)
    {
        final Map<String, Object> creationParams = (Map<String, Object>) args;

        return new CameraView(context, viewId, creationParams);
    }
}
