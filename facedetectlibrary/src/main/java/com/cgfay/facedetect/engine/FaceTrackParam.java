package com.cgfay.facedetect.engine;

import com.cgfay.facedetect.listener.FaceTrackerCallback;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
//import com.megvii.facepp.sdk.Facepp;

/**
 * 人脸检测参数
 */
public final class FaceTrackParam {


    // Rotation angle
    public int rotateAngle;
    // Whether camera preview detection, true is preview detection, false is still image detection
//    public boolean previewTrack;
    // Whether to allow 3D pose angle
    public boolean enable3DPose;
    // Whether to allow area detection
    public boolean enableROIDetect;
    // Detection area scaling
    public float roiRatio;

    // Whether the back camera
    public boolean isBackCamera;
    // Whether to allow face age detection
    public boolean enableFaceProperty;
    // Whether to allow multiple face detection
    public boolean enableMultiFace;
    // Minimum face size
    public int minFaceSize;
    // Detection interval
    public int detectInterval;
    // Detection mode
    public int trackMode;
    // detection callback
    public FaceTrackerCallback trackerCallback;

    private static class FaceParamHolder {
        public static FaceTrackParam instance = new FaceTrackParam();
    }

    private FaceTrackParam() {
        reset();
    }

    public static FaceTrackParam getInstance() {
        return FaceParamHolder.instance;
    }

    /**
     * reset to initial state
     */
    public void reset() {
//        previewTrack = true;
        enable3DPose = false;
        enableROIDetect = false;
        roiRatio = 0.8f;
//        enable106Points = true;
        isBackCamera = false;
        enableFaceProperty = false;
        enableMultiFace = true;
        minFaceSize = 200;
        detectInterval = 25;
        trackMode = FaceMeshDetectorOptions.FACE_MESH;
                //Facepp.FaceppConfig.DETECTION_MODE_TRACKING;
        trackerCallback = null;
    }

}
