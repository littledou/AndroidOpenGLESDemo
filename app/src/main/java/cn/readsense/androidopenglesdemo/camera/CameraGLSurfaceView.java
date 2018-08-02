/*
 *
 * CameraView.java
 *
 * Created by Wuwang on 2016/11/14
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package cn.readsense.androidopenglesdemo.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description:
 */
public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "CameraGLSurfaceView";

    private CameraManager cameraManager;
    private CameraDrawer mCameraDrawer;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private Runnable mRunnable;

    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        cameraManager = new CameraManager();
        mCameraDrawer=new CameraDrawer(getResources());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraDrawer.onSurfaceCreated(gl,config);
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
        }
        cameraManager.open(cameraId);
        mCameraDrawer.setCameraId(cameraId);
        Point point=cameraManager.getPreviewSize();
        mCameraDrawer.setDataSize(point.x,point.y);

        cameraManager.setPreviewTexture(mCameraDrawer.getSurfaceTexture());

        mCameraDrawer.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });

        cameraManager.preview();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraDrawer.setViewSize(width,height);
        GLES20.glViewport(0,0,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraDrawer.onDrawFrame(gl);
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraManager.close();
    }
}
