package cn.readsense.androidopenglesdemo.image;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.readsense.androidopenglesdemo.util.ShaderUtils;

public class ImageRenderer implements GLSurfaceView.Renderer {
    //顶点着色器
    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vCoordinate;" +
                    "uniform mat4 vMatrix;" +
                    "varying vec2 aCoordinate;" +

                    "void main() {" +
                    "   gl_Position = vMatrix*vPosition;" +
                    "   aCoordinate = vCoordinate;" +
                    "}";

    //片元着色器


    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D vTexture;" +
                    "uniform int vChangeType;" +
                    "uniform vec3 vChangeColor;" +

                    "varying vec2 aCoordinate;" +

                    "void modifyColor(vec4 color){" +
                    "   color.r=max(min(color.r,1.0),0.0);" +
                    "   color.g=max(min(color.g,1.0),0.0);" +
                    "   color.b=max(min(color.b,1.0),0.0);" +
                    "   color.a=max(min(color.a,1.0),0.0);" +
                    "}" +
                    "void main(){" +
                    "vec4 nColor=texture2D(vTexture,aCoordinate);" +
                    "if(vChangeType==1){" +
                    "   float c=nColor.r*vChangeColor.r+nColor.g*vChangeColor.g+nColor.b*vChangeColor.b;" +
                    "   gl_FragColor=vec4(c,c,c,nColor.a);" +
                    "}else if(vChangeType==2){" +
                    "   vec4 deltaColor=nColor+vec4(vChangeColor,0.0);" +
                    "   modifyColor(deltaColor);" +
                    "   gl_FragColor=deltaColor;" +
                    "}else{" +
                    "   gl_FragColor=nColor;" +
                    "}" +
                    "}";
    //顶点坐标
    private final float[] sPos = {
            -1.0f, 1.0f,    //左上角
            -1.0f, -1.0f,   //左下角
            1.0f, 1.0f,     //右上角
            1.0f, -1.0f     //右下角
    };

    //纹理坐标
    private final float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };


    private int mProgram;
    private int glHPosition;
    private int glHTexture;
    private int glHCoordinate;
    private int glHMatrix;
    private int hIsHalf;
    private int glHUxy;
    private int vChangeType;
    private int vChangeColor;
    private Bitmap mBitmap;

    private FloatBuffer bPos;
    private FloatBuffer bCoord;

    private int textureId;
    private boolean isHalf;

    private float uXY;

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    //滤镜参数
    float filter_color[] = new float[]{0.299f, 0.587f, 0.114f};


    public ImageRenderer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bPos = bb.asFloatBuffer();
        bPos.put(sPos);
        bPos.position(0);
        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        bCoord = cc.asFloatBuffer();
        bCoord.put(sCoord);
        bCoord.position(0);


    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        int vertexShader = ShaderUtils.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = ShaderUtils.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        //创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram();
        //将顶点着色器加入到程序
        GLES20.glAttachShader(mProgram, vertexShader);
        //将片元着色器加入到程序中
        GLES20.glAttachShader(mProgram, fragmentShader);
        //连接到着色器程序
        GLES20.glLinkProgram(mProgram);

        glHPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glHCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glHTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        glHMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        hIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");
        glHUxy = GLES20.glGetUniformLocation(mProgram, "uXY");
        vChangeType = GLES20.glGetUniformLocation(mProgram, "vChangeType");
        vChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //计算变换矩阵
        GLES20.glViewport(0, 0, width, height);

        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float sWH = w / (float) h;
        float sWidthHeight = width / (float) height;
        if (width > height) {
            if (sWH > sWidthHeight) {
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight * sWH, sWidthHeight * sWH, -1, 1, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight / sWH, sWidthHeight / sWH, -1, 1, 3, 7);
            }
        } else {
            if (sWH > sWidthHeight) {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / sWidthHeight * sWH, 1 / sWidthHeight * sWH, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH / sWidthHeight, sWH / sWidthHeight, 3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        //添加滤镜？？？
        /**
         *  NONE(0,new float[]{0.0f,0.0f,0.0f}),
         GRAY(1,new float[]{0.299f,0.587f,0.114f}),
         COOL(2,new float[]{0.0f,0.0f,0.1f}),
         WARM(2,new float[]{0.1f,0.1f,0.0f}),
         BLUR(3,new float[]{0.006f,0.004f,0.002f}),
         MAGN(4,new float[]{0.0f,0.0f,0.4f});
         */
//        GLES20.glUniform1i(vChangeType, 1);
//        GLES20.glUniform3fv(vChangeColor, 1, filter_color, 0);

        GLES20.glUniform1i(hIsHalf, isHalf ? 1 : 0);
        GLES20.glUniform1f(glHUxy, uXY);
        GLES20.glUniformMatrix4fv(glHMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glEnableVertexAttribArray(glHPosition);
        GLES20.glEnableVertexAttribArray(glHCoordinate);
        GLES20.glUniform1i(glHTexture, 0);
        textureId = createTexture();
        GLES20.glVertexAttribPointer(glHPosition, 2, GLES20.GL_FLOAT, false, 0, bPos);
        GLES20.glVertexAttribPointer(glHCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    private int createTexture() {
        int[] texture = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

            return texture[0];
        }
        return 0;
    }

}
