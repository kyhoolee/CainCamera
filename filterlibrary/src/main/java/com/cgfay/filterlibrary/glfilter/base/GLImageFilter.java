package com.cgfay.filterlibrary.glfilter.base;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.text.TextUtils;

import com.cgfay.filterlibrary.glfilter.utils.OpenGLUtils;
import com.cgfay.filterlibrary.glfilter.utils.TextureRotationUtils;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * 基类滤镜
 * Created by cain on 2017/7/9.
 */

public class GLImageFilter {

    protected static final String VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;                                   \n" +
            "attribute vec4 aPosition;                                  \n" +
            "attribute vec4 aTextureCoord;                              \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "void main() {                                              \n" +
            "    gl_Position = uMVPMatrix * aPosition;                  \n" +
            "    textureCoordinate = aTextureCoord.xy;                  \n" +
            "}                                                          \n";

    protected static final String FRAGMENT_SHADER_2D = "" +
            "precision mediump float;                                   \n" +
            "varying vec2 textureCoordinate;                            \n" +
            "uniform sampler2D inputTexture;                                \n" +
            "void main() {                                              \n" +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate); \n" +
            "}                                                          \n";


    protected Context mContext;
    // 当前时钟
    protected float mCurrentTimer;

    private final LinkedList<Runnable> mRunOnDraw;

    // 纹理字符串
    protected String mVertexShader;
    protected String mFragmentShader;

    // 是否初始化成功
    protected boolean mIsInitialized;
    // 滤镜是否可用，默认可用
    protected boolean mFilterEnable = true;

    // 顶点坐标缓冲
    protected FloatBuffer mVertexBuffer;
    // 纹理坐标缓冲
    protected FloatBuffer mTextureBuffer;
    // 每个顶点坐标有几个参数
    protected int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    // 顶点坐标数量
    protected int mVertexCount = TextureRotationUtils.CubeVertices.length / mCoordsPerVertex;

    // 句柄
    protected int mProgramHandle;
    protected int mMVPMatrixHandle;
    protected int mPositionHandle;
    protected int mTextureCoordinateHandle;
    protected int mInputTextureHandle;

    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;

    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    // FBO的宽高，可能跟输入的纹理大小不一致
    protected int mFrameWidth = -1;
    protected int mFrameHeight = -1;

    // FBO
    protected int[] mFramebuffers;
    protected int[] mFramebufferTextures;

    // 变换矩阵
    protected float[] mMVPMatrix = new float[16];
    // 缩放矩阵
    protected float[] mTexMatrix = new float[16];

    public GLImageFilter(Context context) {
        this(context, VERTEX_SHADER, FRAGMENT_SHADER_2D);
    }

    public GLImageFilter(Context context, String vertexShader, String fragmentShader) {
        mContext = context;
        mRunOnDraw = new LinkedList<>();

        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);

        initIdentityMatrix();
        // 初始化程序句柄
        initProgramHandle();
    }

    /**
     * 初始化程序句柄
     */
    public void initProgramHandle() {
        // 只有在shader都不为空的情况下才初始化程序句柄
        if (!TextUtils.isEmpty(mVertexShader) && !TextUtils.isEmpty(mFragmentShader)) {
            mProgramHandle = OpenGLUtils.createProgram(mVertexShader, mFragmentShader);
            mPositionHandle = GLES30.glGetAttribLocation(mProgramHandle, "aPosition");
            mTextureCoordinateHandle = GLES30.glGetAttribLocation(mProgramHandle, "aTextureCoord");
            mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            mInputTextureHandle = GLES30.glGetUniformLocation(mProgramHandle, "inputTexture");
            mIsInitialized = true;
        } else {
            mPositionHandle = OpenGLUtils.GL_NOT_INIT;
            mTextureCoordinateHandle = OpenGLUtils.GL_NOT_INIT;
            mMVPMatrixHandle = OpenGLUtils.GL_NOT_INIT;
            mInputTextureHandle = OpenGLUtils.GL_NOT_TEXTURE;
            mIsInitialized = false;
        }
    }

    /**
     * Surface发生变化时调用
     * @param width
     * @param height
     */
    public void onInputSizeChanged(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     *  显示视图发生变化时调用
     * @param width
     * @param height
     */
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }

    /**
     * 绘制Frame
     * @param textureId
     */
    public boolean drawFrame(int textureId) {
        return drawFrame(textureId, mVertexBuffer, mTextureBuffer);
    }

    /**
     * 绘制Frame
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer,
                          FloatBuffer textureBuffer) {
        // 没有初始化、输入纹理不合法、滤镜不可用时直接返回
        if (!mIsInitialized || textureId == OpenGLUtils.GL_NOT_INIT || !mFilterEnable) {
            return false;
        }

        // 设置视口大小
        GLES30.glViewport(0, 0, mDisplayWidth, mDisplayHeight);

        // 使用当前的program
        GLES30.glUseProgram(mProgramHandle);
        // 运行延时任务
        runPendingOnDrawTasks();

        // 绑定顶点坐标缓冲
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, mCoordsPerVertex,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // 绑定纹理坐标缓冲
        textureBuffer.position(0);
        GLES30.glVertexAttribPointer(mTextureCoordinateHandle, 2,
                GLES30.GL_FLOAT, false, 0, textureBuffer);
        GLES30.glEnableVertexAttribArray(mTextureCoordinateHandle);
        // 绑定总变换矩阵
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // 绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(getTextureType(), textureId);
        GLES30.glUniform1i(mInputTextureHandle, 0);
        onDrawArraysBegin();
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mVertexCount);
        onDrawArraysAfter();
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mTextureCoordinateHandle);
        GLES30.glBindTexture(getTextureType(), 0);

        return true;
    }

    /**
     * 绘制到FBO
     * @param textureId
     * @return FBO绑定的Texture
     */
    public int drawFrameBuffer(int textureId) {
        return drawFrameBuffer(textureId, mVertexBuffer, mTextureBuffer);
    }

    /**
     * 绘制到FBO
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     * @return FBO绑定的Texture
     */
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 没有FBO、没初始化、输入纹理不合法、滤镜不可用时，直接返回
        if (textureId == OpenGLUtils.GL_NOT_TEXTURE || mFramebuffers == null
                || !mIsInitialized || !mFilterEnable) {
            return textureId;
        }

        // 绑定FBO
        GLES30.glViewport(0, 0, mFrameWidth, mFrameHeight);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFramebuffers[0]);
        // 使用当前的program
        GLES30.glUseProgram(mProgramHandle);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();

        // 绑定顶点坐标缓冲
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, mCoordsPerVertex,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // 绑定纹理坐标缓冲
        textureBuffer.position(0);
        GLES30.glVertexAttribPointer(mTextureCoordinateHandle, 2,
                GLES30.GL_FLOAT, false, 0, textureBuffer);
        GLES30.glEnableVertexAttribArray(mTextureCoordinateHandle);
        // 绑定总变换矩阵
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // 绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(getTextureType(), textureId);
        GLES30.glUniform1i(mInputTextureHandle, 0);
        onDrawArraysBegin();
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mVertexCount);
        onDrawArraysAfter();
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mTextureCoordinateHandle);
        GLES30.glBindTexture(getTextureType(), 0);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return mFramebufferTextures[0];
    }

    /**
     * 获取Texture类型
     * GLES30.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    /**
     * 调用drawArrays之前，方便添加其他属性
     */
    public void onDrawArraysBegin() {

    }

    /**
     * drawArrays调用之后，方便销毁其他属性
     */
    public void onDrawArraysAfter() {

    }

    /**
     * 释放资源
     */
    public void release() {
        if (mIsInitialized) {
            GLES30.glDeleteProgram(mProgramHandle);
            mProgramHandle = OpenGLUtils.GL_NOT_INIT;
        }
        destroyFrameBuffer();
    }

    /**
     * 创建FBO
     * @param width
     * @param height
     */
    public void initFrameBuffer(int width, int height) {
        if (!isInitialized()) {
            return;
        }
        if (mFramebuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
            destroyFrameBuffer();
        }
        if (mFramebuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFramebuffers = new int[1];
            mFramebufferTextures = new int[1];
            OpenGLUtils.createFrameBuffer(mFramebuffers, mFramebufferTextures, width, height);
        }
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (!mIsInitialized) {
            return;
        }
        if (mFramebufferTextures != null) {
            GLES30.glDeleteTextures(1, mFramebufferTextures, 0);
            mFramebufferTextures = null;
        }

        if (mFramebuffers != null) {
            GLES30.glDeleteFramebuffers(1, mFramebuffers, 0);
            mFramebuffers = null;
        }
        mFrameWidth = -1;
        mFrameWidth = -1;
    }

    /**
     * 初始化单位矩阵
     */
    public void initIdentityMatrix() {
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(mTexMatrix, 0);
    }

    /**
     * 设置变换矩阵
     * @param matrix
     */
    public void setMVPMatrix(float[] matrix) {
        if (!Arrays.equals(mMVPMatrix, matrix)) {
            mMVPMatrix = matrix;
        }
    }

    /**
     * 判断是否初始化
     * @return
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * 设置滤镜是否可用
     * @param enable
     */
    public void setFilterEnable(boolean enable) {
        mFilterEnable = enable;
    }

    /**
     * 设置时钟
     * @param currentTimer
     */
    public void setTimerValue(float currentTimer) {
        mCurrentTimer = currentTimer;
    }

    /**
     * 获取输出宽度
     * @return
     */
    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    /**
     * 获取输出高度
     * @return
     */
    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES30.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 添加延时任务
     * @param runnable
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    /**
     * 运行延时任务
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
}