package com.cgfay.facedetect.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cgfay.facedetect.utils.ConUtil;
import com.cgfay.facedetect.utils.SensorEventUtil;
import com.cgfay.facedetectlibrary.R;
import com.cgfay.landmark.LandmarkEngine;
import com.cgfay.landmark.OneFace;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * detection thread
 */
public class TrackerThread extends Thread {
    private static final boolean VERBOSE = false;

    private final Object mStartLock = new Object();
    private boolean mReady = false;

    // face detection entity
    private FaceMLKit facepp;
    // sensor listener
    private SensorEventUtil mSensorUtil;

    private Looper mLooper;
    private @Nullable
    Handler mHandler;

    private FaceMeshDetector detector;
    private Executor executor;

    public TrackerThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
            mHandler = new Handler(mLooper);
        }
        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();
        }
        Looper.loop();
        synchronized (this) {
            release();
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        synchronized (mStartLock) {
            mReady = false;
        }
    }

    /**
     * Wait for the thread to be ready to complete
     */
    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * Quit safely
     * @return
     */
    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    /**
     * 获取Looper
     * @return
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }

    /**
     * Initialize face detection
     * @param context context
     * @param orientation image angle
     * @param width image width
     * @param height image height
     */
    public void prepareFaceTracker(final Context context, final int orientation,
                                   final int width, final int height) {
        waitUntilReady();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                internalPrepareFaceTracker(context, orientation, width, height);
            }
        });
    }

    /**
     * Detect faces
     * @param data image data, NV21 or RGBA format
     * @param width image width
     * @param height image height
     * @return whether the detection is successful
     */
    public void trackFace(final byte[] data, final int width, final int height) {
        waitUntilReady();
        internalTrackFace(data, width, height);
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
    }


    /**
     * Release resource
     */
    private void release() {
        ConUtil.releaseWakeLock();
        if (facepp != null) {
            //facepp.release();
            detector.close();

            facepp = null;
        }
    }

    /**
     * Initialize face detection
     * @param context context
     * @param orientation image angle, set the camera's angle when previewing, if it's a static image, it's 0
     * @param width image width
     * @param height image height
     */
    private synchronized void internalPrepareFaceTracker(Context context, int orientation, int width, int height) {
        FaceTrackParam faceTrackParam = FaceTrackParam.getInstance();
        release();

        facepp = new FaceMLKit();
        if (mSensorUtil == null) {
            mSensorUtil = new SensorEventUtil(context);
        }
        ConUtil.acquireWakeLock(context);
//        if (!true) { // faceTrackParam.previewTrack
//            faceTrackParam.rotateAngle = orientation;
//        } else {
//            faceTrackParam.rotateAngle = faceTrackParam.isBackCamera ? orientation : 360 - orientation;
//        }
        faceTrackParam.rotateAngle = faceTrackParam.isBackCamera ? orientation : 360 - orientation;

        int left = 0;
        int top = 0;
        int right = width;
        int bottom = height;

        // Limited detection area
        if (faceTrackParam.enableROIDetect) {
            float line = height * faceTrackParam.roiRatio;
            left = (int) ((width - line) / 2.0f);
            top = (int) ((height - line) / 2.0f);
            right = width - left;
            bottom = height - top;
        }

//        facepp.init(context);
        FaceMeshDetectorOptions.Builder optionsBuilder = new FaceMeshDetectorOptions.Builder();
        optionsBuilder.setUseCase(FaceMeshDetectorOptions.FACE_MESH);
        detector = FaceMeshDetection.getClient(optionsBuilder.build());

        executor = Executors.newSingleThreadExecutor();


        FaceMLKit.FaceMLKitConfig faceppConfig = facepp.getFaceMLKitConfig();
        faceppConfig.interval = faceTrackParam.detectInterval;
        faceppConfig.minFaceSize = faceTrackParam.minFaceSize;
        faceppConfig.roi_left = left;
        faceppConfig.roi_top = top;
        faceppConfig.roi_right = right;
        faceppConfig.roi_bottom = bottom;
        faceppConfig.one_face_tracking = faceTrackParam.enableMultiFace ? 0 : 1;
        faceppConfig.detectionMode = faceTrackParam.trackMode;
        facepp.setFaceMLKitConfig(faceppConfig);
    }

    /**
     * Detect faces
     * @param data image data, NV21 for preview and RGBA for still images
     * @param width image width
     * @param height image height
     * @return whether the detection is successful
     */
    private synchronized void internalTrackFace(byte[] data, int width, int height) {
        FaceTrackParam faceTrackParam = FaceTrackParam.getInstance();
        if (facepp == null) {
            LandmarkEngine.getInstance().setFaceSize(0);
            if (faceTrackParam.trackerCallback != null) {
                faceTrackParam.trackerCallback.onTrackingFinish();
            }
            return;
        }

        // Adjust inspection and supervision
        long faceDetectTime_action = System.currentTimeMillis();
        // Get device rotation
        int orientation = mSensorUtil.orientation; // faceTrackParam.previewTrack true ?  : 0;
        int rotation = 0;
        if (orientation == 0) {         // 0
            rotation = faceTrackParam.rotateAngle;
        } else if (orientation == 1) {  // 90
            rotation = 0;
        } else if (orientation == 2) {  // 270
            rotation = 180;
        } else if (orientation == 3) {  // 180
            rotation = 360 - faceTrackParam.rotateAngle;
        }
        // Set the rotation angle
        FaceMLKit.FaceMLKitConfig faceppConfig = facepp.getFaceMLKitConfig();
        if (faceppConfig.rotation != rotation) {
            faceppConfig.rotation = rotation;
            facepp.setFaceMLKitConfig(faceppConfig);
        }

        // Face Detection
        InputImage image = InputImage.fromByteArray(
                data,
                width,
                height,
                faceppConfig.rotation,
                InputImage.IMAGE_FORMAT_NV21);
        Task<List<FaceMesh>> faceTask = detector.process(image);


//        return result;
//        Task<List<FaceMesh>> faceTask = facepp.detect(
//                data, width, height);

        faceTask.addOnSuccessListener(
                executor,
                new OnSuccessListener<List<FaceMesh>>() {
                    @Override
                    public void onSuccess(List<FaceMesh> faces) {
                        // Task completed successfully
                        postHandleFace(
                                faces, width, height,
                                faceDetectTime_action, orientation, faceTrackParam
                                );
//                        image.close();
                    }
                })
                .addOnFailureListener(
                        executor,
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });


    }

    private void postHandleFace(
            List<FaceMesh> faces, int width, int height,
            long faceDetectTime_action,
            int orientation,
            FaceTrackParam faceTrackParam
        ) {
        // Calculate detection time
        if (VERBOSE) {
            final long algorithmTime = System.currentTimeMillis() - faceDetectTime_action;
            Log.d("onFaceTracking", "track time = " + algorithmTime);
        }

        // Set the rotation direction
        LandmarkEngine.getInstance().setOrientation(orientation);
        // Whether the setting needs to be flipped
        boolean needFlip = true && !faceTrackParam.isBackCamera; //faceTrackParam.previewTrack
        LandmarkEngine.getInstance().setNeedFlip(needFlip);

        // Calculate face key points
        if (faces != null && faces.size() > 0) {
            for (int index = 0; index < faces.size(); index++) {
                // number of key points
                FaceMLKit.Face face = facepp.getLandmark(faces.get(index));



                OneFace oneFace = LandmarkEngine.getInstance().getOneFace(index);


                // Attitude Angle and Confidence
                oneFace.pitch = face.pitch;
                if (faceTrackParam.isBackCamera) {
                    oneFace.yaw = -face.yaw;
                } else {
                    oneFace.yaw = face.yaw;
                }
                oneFace.roll = face.roll;

//                // Pitch angle (rotate around the x-axis)
//                // Yaw angle (rotation around the y-axis)
//                // roll angle (rotation around the z-axis)


                if (true) { // faceTrackParam.previewTrack

                    if (faceTrackParam.isBackCamera) {
                        oneFace.roll = (float) (Math.PI / 2.0f + oneFace.roll);
                    } else {
                        oneFace.roll = (float) (Math.PI / 2.0f - face.roll);
                    }
                }

                // In the preview state, the width and height are exchanged
                if (true) { //faceTrackParam.previewTrack
                    if (orientation == 1 || orientation == 2) {
                        int temp = width;
                        width = height;
                        height = temp;
                    }
                }

                // Get a person's keypoint coordinates
                if (oneFace.vertexPoints == null || oneFace.vertexPoints.length != face.points.length * 2) {
                    oneFace.vertexPoints = new float[face.points.length * 2];
                }
                for (int i = 0; i < face.points.length; i++) {
                    // orientation = 0, 3 for portrait, 1, 2 for landscape
                    float x = (face.points[i].x / height) * 2 - 1;
                    float y = (face.points[i].y / width) * 2 - 1;
                    float[] point = new float[] {x, -y};
                    if (orientation == 1) {
                        if (true && faceTrackParam.isBackCamera) { //faceTrackParam.previewTrack
                            point[0] = -y;
                            point[1] = -x;
                        } else {
                            point[0] = y;
                            point[1] = x;
                        }
                    } else if (orientation == 2) {
                        if (true && faceTrackParam.isBackCamera) { //faceTrackParam.previewTrack
                            point[0] = y;
                            point[1] = x;
                        } else {
                            point[0] = -y;
                            point[1] = -x;
                        }
                    } else if (orientation == 3) {
                        point[0] = -x;
                        point[1] = y;
                    }
                    // vertex coordinates
                    if (true) { //faceTrackParam.previewTrack
                        if (faceTrackParam.isBackCamera) {
                            oneFace.vertexPoints[2 * i] = point[0];
                        } else {
                            oneFace.vertexPoints[2 * i] = -point[0];
                        }
                    } else {
                        // In the non-preview state, the left and right do not need to be flipped
                        oneFace.vertexPoints[2 * i] = point[0];
                    }
                    oneFace.vertexPoints[2 * i + 1] = point[1];
                }
                // Insert face object
                LandmarkEngine.getInstance().putOneFace(index, oneFace);
            }
        }
        // Set the number of faces
        LandmarkEngine.getInstance().setFaceSize(faces!= null ? faces.size() : 0);
        // Detection completion callback
        if (faceTrackParam.trackerCallback != null) {
            faceTrackParam.trackerCallback.onTrackingFinish();
        }
    }
}

