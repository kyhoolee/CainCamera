package com.cgfay.facedetect.engine;

import android.content.Context;
import com.cgfay.facedetect.listener.FaceTrackerCallback;

//import com.megvii.facepp.sdk.Facepp;
//import com.megvii.licensemanager.sdk.LicenseManager;

/**
 * Face detector
 */
public final class FaceTracker {

    private static final String TAG = "FaceTracker";
    private static final boolean VERBOSE = false;

    private final Object mSyncFence = new Object();

    // Face detection parameters
    private FaceTrackParam mFaceTrackParam;

    // Detection thread
    private TrackerThread mTrackerThread;

    private static class FaceTrackerHolder {
        private static FaceTracker instance = new FaceTracker();
    }

    private FaceTracker() {
        mFaceTrackParam = FaceTrackParam.getInstance();
    }

    public static FaceTracker getInstance() {
        return FaceTrackerHolder.instance;
    }

    /**
     * Detection callback
     * @param callback
     * @return
     */
    public FaceTrackerBuilder setFaceCallback(FaceTrackerCallback callback) {
        return new FaceTrackerBuilder(this, callback);
    }

    /**
     * Prepare detector
     */
    void initTracker() {
        synchronized (mSyncFence) {
            mTrackerThread = new TrackerThread("FaceTrackerThread");
            mTrackerThread.start();
            mTrackerThread.waitUntilReady();
        }
    }

    /**
     * Initialize face detection
     * @param context context
     * @param orientation image angle
     * @param width image width
     * @param height image height
     */
    public void prepareFaceTracker(Context context, int orientation, int width, int height) {
        synchronized (mSyncFence) {
            if (mTrackerThread != null) {
                mTrackerThread.prepareFaceTracker(context, orientation, width, height);
            }
        }
    }

    /**
     * Detect faces
     * @param data
     * @param width
     * @param height
     */
    public void trackFace(byte[] data, int width, int height) {
        synchronized (mSyncFence) {
            if (mTrackerThread != null) {
                mTrackerThread.trackFace(data, width, height);
            }
        }
    }

    /**
     * Destroy detector
     */
    public void destroyTracker() {
        synchronized (mSyncFence) {
            mTrackerThread.quitSafely();
        }
    }

    /**
     * Set is-back-camera
     * @param backCamera
     * @return
     */
    public FaceTracker setBackCamera(boolean backCamera) {
        mFaceTrackParam.isBackCamera = backCamera;
        return this;
    }

    /**
     * enable 3D pose
     * @param enable
     * @return
     */
    public FaceTracker enable3DPose(boolean enable) {
        mFaceTrackParam.enable3DPose = enable;
        return this;
    }

    /**
     * Enable detect region of interest
     * @param enable
     * @return
     */
    public FaceTracker enableROIDetect(boolean enable) {
        mFaceTrackParam.enableROIDetect = enable;
        return this;
    }



    /**
     * If detect multiple face
     * @param enable
     * @return
     */
    public FaceTracker enableMultiFace(boolean enable) {
        mFaceTrackParam.enableMultiFace = enable;
        return this;
    }

    /**
     * If detect face property
     * @param enable
     * @return
     */
    public FaceTracker enableFaceProperty(boolean enable) {
        mFaceTrackParam.enableFaceProperty = enable;
        return this;
    }

    /**
     * Minimum detected face size
     * @param size
     * @return
     */
    public FaceTracker minFaceSize(int size) {
        mFaceTrackParam.minFaceSize = size;
        return this;
    }

    /**
     * Detection time interval
     * @param interval
     * @return
     */
    public FaceTracker detectInterval(int interval) {
        mFaceTrackParam.detectInterval = interval;
        return this;
    }

    /**
     * Set track mode
     * @param mode
     * @return
     */
    public FaceTracker trackMode(int mode) {
        mFaceTrackParam.trackMode = mode;
        return this;
    }

    /**
     * Face++SDK verification -->> Convert to MLKit facemesh
     */
    public static void requestFaceNetwork(Context context) {
//        if (Facepp.getSDKAuthType(ConUtil.getFileContent(context, R.raw
//                .megviifacepp_0_5_2_model)) == 2) {// 非联网授权
//            FaceTrackParam.getInstance().canFaceTrack = true;
//            return;
//        }
//        final LicenseManager licenseManager = new LicenseManager(context);
//        licenseManager.setExpirationMillis(Facepp.getApiExpirationMillis(context,
//                ConUtil.getFileContent(context, R.raw.megviifacepp_0_5_2_model)));
//        String uuid = ConUtil.getUUIDString(context);
//        long apiName = Facepp.getApiName();
//        String url = FaceppConstraints.US_LICENSE_URL;
//        licenseManager.setAuthTimeBufferMillis(0);
//        licenseManager.takeLicenseFromNetwork(url, uuid, FaceppConstraints.API_KEY, FaceppConstraints.API_SECRET, apiName,
//                "1",
//                new LicenseManager.TakeLicenseCallback() {
//                    @Override
//                    public void onSuccess() {
//                        if (VERBOSE) {
//                            Log.d(TAG, "success to register license!");
//                        }
//                        FaceTrackParam.getInstance().canFaceTrack = true;
//                    }
//
//                    @Override
//                    public void onFailed(int i, byte[] bytes) {
//                        if (VERBOSE) {
//                            Log.d(TAG, "Failed to register license!");
//                        }
//                        FaceTrackParam.getInstance().canFaceTrack = false;
//                    }
//                });
    }



}
