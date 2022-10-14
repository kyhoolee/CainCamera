package com.cgfay.landmark;

import android.util.SparseArray;

/**
 * Face Keypoint Engine
 */
public final class LandmarkEngine {

    private static class EngineHolder {
        public static LandmarkEngine instance = new LandmarkEngine();
    }

    private LandmarkEngine() {
        mFaceArrays = new SparseArray<OneFace>();
    }

    public static LandmarkEngine getInstance() {
        return EngineHolder.instance;
    }

    private final Object mSyncFence = new Object();

    // list of face objects
    // Due to the limited number of face data,
    // the number of faces in the image is less than a thousand,
    // and the face index is continuous, using SparseArray is better than Hashmap
    private final SparseArray<OneFace> mFaceArrays;

    // The current direction of the phone,
    // 0 means the front screen,
    // 3 means the reverse,
    // 1 means the left screen,
    // 2 means the right screen
    private float mOrientation;
    private boolean mNeedFlip;

    /**
     * Set the rotation angle
     * @param orientation
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * Whether the setting needs to be flipped
     * @param flip
     */
    public void setNeedFlip(boolean flip) {
        mNeedFlip = flip;
    }

    /**
     * Set the number of faces
     * @param size
     */
    public void setFaceSize(int size) {
        synchronized (mSyncFence) {
            // 剔除脏数据，有可能在前一次检测的人脸多余当前人脸
            if (mFaceArrays.size() > size) {
                mFaceArrays.removeAtRange(size, mFaceArrays.size() - size);
            }
        }
    }

    /**
     * Is there a face
     * @return
     */
    public boolean hasFace() {
        boolean result;
        synchronized (mSyncFence) {
            result = mFaceArrays.size() > 0;
        }
        return result;
    }

    /**
     * Get a face keypoint data object
     * @return
     */
    public OneFace getOneFace(int index) {
        OneFace oneFace = null;
        synchronized (mSyncFence) {
            oneFace = mFaceArrays.get(index);
            if (oneFace == null) {
                oneFace = new OneFace();
            }
        }
        return oneFace;
    }

    /**
     * Insert a face keypoint data object
     * @param index
     */
    public void putOneFace(int index, OneFace oneFace) {
        synchronized (mSyncFence) {
            mFaceArrays.put(index, oneFace);
        }
    }

    /**
     * Get the number of faces
     * @return
     */
    public int getFaceSize() {
        return mFaceArrays.size();
    }

    /**
     * Get a list of faces
     * @return
     */
    public SparseArray<OneFace> getFaceArrays() {
        return mFaceArrays;
    }

    /**
     * Clear all face objects
     */
    public void clearAll() {
        synchronized (mSyncFence) {
            mFaceArrays.clear();
        }
    }

    /**
     * Calculate extra face vertices, add 8 extra vertex coordinates
     * @param vertexPoints
     * @param index
     */
    public void calculateExtraFacePoints(float[] vertexPoints, int index) {
        if (vertexPoints == null || index >= mFaceArrays.size() || mFaceArrays.get(index) == null
                || mFaceArrays.get(index).vertexPoints.length + 8 * 2 > vertexPoints.length) {
            return;
        }
        OneFace oneFace = mFaceArrays.get(index);
        // Copy key data
        System.arraycopy(oneFace.vertexPoints, 0, vertexPoints, 0, oneFace.vertexPoints.length);
        // Added face key points
        float[] point = new float[2];
        // center of lips
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.mouthUpperLipBottom * 2],
                vertexPoints[FaceLandmark.mouthUpperLipBottom * 2 + 1],
                vertexPoints[FaceLandmark.mouthLowerLipTop * 2],
                vertexPoints[FaceLandmark.mouthLowerLipTop * 2 + 1]
        );
        vertexPoints[FaceLandmark.mouthCenter * 2] = point[0];
        vertexPoints[FaceLandmark.mouthCenter * 2 + 1] = point[1];

        // left eyebrow
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftEyebrowUpperMiddle * 2],
                vertexPoints[FaceLandmark.leftEyebrowUpperMiddle * 2 + 1],
                vertexPoints[FaceLandmark.leftEyebrowLowerMiddle * 2],
                vertexPoints[FaceLandmark.leftEyebrowLowerMiddle * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftEyebrowCenter * 2] = point[0];
        vertexPoints[FaceLandmark.leftEyebrowCenter * 2 + 1] = point[1];

        // Right eyebrow
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightEyebrowUpperMiddle * 2],
                vertexPoints[FaceLandmark.rightEyebrowUpperMiddle * 2 + 1],
                vertexPoints[FaceLandmark.rightEyebrowLowerMiddle * 2],
                vertexPoints[FaceLandmark.rightEyebrowLowerMiddle * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightEyebrowCenter * 2] = point[0];
        vertexPoints[FaceLandmark.rightEyebrowCenter * 2 + 1] = point[1];

        // center of forehead
        vertexPoints[FaceLandmark.headCenter * 2] = vertexPoints[FaceLandmark.eyeCenter * 2] * 2.0f - vertexPoints[FaceLandmark.noseLowerMiddle * 2];
        vertexPoints[FaceLandmark.headCenter * 2 + 1] = vertexPoints[FaceLandmark.eyeCenter * 2 + 1] * 2.0f - vertexPoints[FaceLandmark.noseLowerMiddle * 2 + 1];

        // The left side of the forehead,
        // note: this point is not very accurate, follow-up optimization
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftEyebrowLeftTopCorner * 2],
                vertexPoints[FaceLandmark.leftEyebrowLeftTopCorner * 2 + 1],
                vertexPoints[FaceLandmark.headCenter * 2],
                vertexPoints[FaceLandmark.headCenter * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftHead * 2] = point[0];
        vertexPoints[FaceLandmark.leftHead * 2 + 1] = point[1];

        // On the right side of the forehead,
        // note: this point is not very accurate, follow-up optimization
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightEyebrowRightTopCorner * 2],
                vertexPoints[FaceLandmark.rightEyebrowRightTopCorner * 2 + 1],
                vertexPoints[FaceLandmark.headCenter * 2],
                vertexPoints[FaceLandmark.headCenter * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightHead * 2] = point[0];
        vertexPoints[FaceLandmark.rightHead * 2 + 1] = point[1];

        // left cheek center
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.leftCheekEdgeCenter * 2],
                vertexPoints[FaceLandmark.leftCheekEdgeCenter * 2 + 1],
                vertexPoints[FaceLandmark.noseLeft * 2],
                vertexPoints[FaceLandmark.noseLeft * 2 + 1]
        );
        vertexPoints[FaceLandmark.leftCheekCenter * 2] = point[0];
        vertexPoints[FaceLandmark.leftCheekCenter * 2 + 1] = point[1];

        // Right cheek center
        FacePointsUtils.getCenter(point,
                vertexPoints[FaceLandmark.rightCheekEdgeCenter * 2],
                vertexPoints[FaceLandmark.rightCheekEdgeCenter * 2 + 1],
                vertexPoints[FaceLandmark.noseRight * 2],
                vertexPoints[FaceLandmark.noseRight * 2 + 1]
        );
        vertexPoints[FaceLandmark.rightCheekCenter * 2] = point[0];
        vertexPoints[FaceLandmark.rightCheekCenter * 2 + 1] = point[1];
    }

    /**
     * calculate image edge points
     * @param vertexPoints
     */
    private void calculateImageEdgePoints(float[] vertexPoints) {
        if (vertexPoints == null || vertexPoints.length < 122 * 2) {
            return;
        }

        if (mOrientation == 0) {
            vertexPoints[114 * 2] = 0;
            vertexPoints[114 * 2 + 1] = 1;
            vertexPoints[115 * 2] = 1;
            vertexPoints[115 * 2 + 1] = 1;
            vertexPoints[116 * 2] = 1;
            vertexPoints[116 * 2 + 1] = 0;
            vertexPoints[117 * 2] = 1;
            vertexPoints[117 * 2 + 1] = -1;
        } else if (mOrientation == 1) {
            vertexPoints[114 * 2] = 1;
            vertexPoints[114 * 2 + 1] = 0;
            vertexPoints[115 * 2] = 1;
            vertexPoints[115 * 2 + 1] = -1;
            vertexPoints[116 * 2] = 0;
            vertexPoints[116 * 2 + 1] = -1;
            vertexPoints[117 * 2] = -1;
            vertexPoints[117 * 2 + 1] = -1;
        } else if (mOrientation == 2) {
            vertexPoints[114 * 2] = -1;
            vertexPoints[114 * 2 + 1] = 0;
            vertexPoints[115 * 2] = -1;
            vertexPoints[115 * 2 + 1] = 1;
            vertexPoints[116 * 2] = 0;
            vertexPoints[116 * 2 + 1] = 1;
            vertexPoints[117 * 2] = 1;
            vertexPoints[117 * 2 + 1] = 1;
        } else if (mOrientation == 3) {
            vertexPoints[114 * 2] = 0;
            vertexPoints[114 * 2 + 1] = -1;
            vertexPoints[115 * 2] = -1;
            vertexPoints[115 * 2 + 1] = -1;
            vertexPoints[116 * 2] = -1;
            vertexPoints[116 * 2 + 1] = 0;
            vertexPoints[117 * 2] = -1;
            vertexPoints[117 * 2 + 1] = 1;
        }
        // The vertex coordinates of 118 ~ 121 and 114 ~ 117 are just reversed
        vertexPoints[118 * 2] = -vertexPoints[114 * 2];
        vertexPoints[118 * 2 + 1] = -vertexPoints[114 * 2 + 1];
        vertexPoints[119 * 2] = -vertexPoints[115 * 2];
        vertexPoints[119 * 2 + 1] = -vertexPoints[115 * 2 + 1];
        vertexPoints[120 * 2] = -vertexPoints[116 * 2];
        vertexPoints[120 * 2 + 1] = -vertexPoints[116 * 2 + 1];
        vertexPoints[121 * 2] = -vertexPoints[117 * 2];
        vertexPoints[121 * 2 + 1] = -vertexPoints[117 * 2 + 1];

        // Whether it needs to be flipped or not?
        // When previewing the front camera, the key points are flipped,
        // so the key points at the edge of the image should also be flipped.
        if (mNeedFlip) {
            for (int i = 0; i < 8; i++) {
                vertexPoints[(114 + i) * 2] = -vertexPoints[(114 + i) * 2];
                vertexPoints[(114 + i) * 2 + 1] = -vertexPoints[(114 + i) * 2 + 1];
            }
        }

    }

    /**
     * Get the coordinates for beauty processing
     * @param vertexPoints vertex coordinates, a total of 122 vertices
     * @param texturePoints texture coordinates, a total of 122 vertices
     * @param faceIndex face index
     */
    public void updateFaceAdjustPoints(float[] vertexPoints, float[] texturePoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length != 122 * 2
                || texturePoints == null || texturePoints.length != 122 * 2) {
            return;
        }
        // Calculate additional face vertex coordinates
        calculateExtraFacePoints(vertexPoints, faceIndex);
        // Calculate image edge vertex coordinates
        calculateImageEdgePoints(vertexPoints);
        // Calculate texture coordinates
        for (int i = 0; i < vertexPoints.length; i++) {
            texturePoints[i] = vertexPoints[i] * 0.5f + 0.5f;
        }
    }

    /**
     * Shadow (retouching) vertex coordinates,
     * the vertex coordinates of the entire face are used for grooming
     * @param vetexPoints
     * @param faceIndex
     */
    public void getShadowVertices(float[] vetexPoints, int faceIndex) {

    }

    /**
     * Get cheek (blush) vertex coordinates
     * @param vertexPoints
     * @param faceIndex
     */
    public void getBlushVertices(float[] vertexPoints, int faceIndex) {

    }

    /**
     * Get eyebrow vertex coordinates
     * @param vertexPoints
     * @param faceIndex
     */
    public void getEyeBrowVertices(float[] vertexPoints, int faceIndex) {

    }

    /**
     * To get the vertex coordinates of the eyes (eye shadow, eyeliner, etc.),
     * please refer to the eye mask label in the assets directory.jpg
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getEyeVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 80
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }

        // Keypoint 0 ~ 3, index = 0 ~ 3 4 points
        for (int i = 0; i < 4; i++) {
            vertexPoints[i * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[i * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 + 1];
        }

        // Key points 29 ~ 33, index = 4 ~ 8 5 points
        for (int i = 29; i < 34; i++) {
            vertexPoints[(i - 29 + 4) * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[(i - 29 + 4) * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 + 1];
        }

        // Key points 42 ~ 44, index = 9 ~ 11 3 points
        for (int i = 42; i < 45; i++) {
            vertexPoints[(i - 42 + 9) * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[(i - 42 + 9) * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 +  1];
        }

        // Key points 52 ~ 73, index = 12 ~ 33 22 points
        for (int i = 52; i < 74; i++) {
            vertexPoints[(i - 52 + 12) * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[(i - 52 + 12) * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 + 1];
        }

        // upper center of right eye
        vertexPoints[34 * 2] = mFaceArrays.get(faceIndex).vertexPoints[75 * 2];
        vertexPoints[34 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[75 * 2 + 1];

        // Right eye lower center
        vertexPoints[35 * 2] = mFaceArrays.get(faceIndex).vertexPoints[76 * 2];
        vertexPoints[35 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[76 * 2 + 1];

        // key point 78
        vertexPoints[36 * 2] = mFaceArrays.get(faceIndex).vertexPoints[78 * 2];
        vertexPoints[36 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[78 * 2 + 1];

        // key point 79
        vertexPoints[37 * 2] = mFaceArrays.get(faceIndex).vertexPoints[79 * 2];
        vertexPoints[37 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[79 * 2 + 1];

        // Center point below left eyebrow
        vertexPoints[38 * 2] = (mFaceArrays.get(faceIndex).vertexPoints[3 * 2] + mFaceArrays.get(faceIndex).vertexPoints[44 * 2]) * 0.5f;
        vertexPoints[38 * 2 + 1] = (mFaceArrays.get(faceIndex).vertexPoints[3 * 2 + 1] + mFaceArrays.get(faceIndex).vertexPoints[44 * 2 + 1]) * 0.5f;

        // Center point below right eyebrow
        vertexPoints[39 * 2] = (mFaceArrays.get(faceIndex).vertexPoints[29 * 2] + mFaceArrays.get(faceIndex).vertexPoints[44 * 2]) * 0.5f;
        vertexPoints[39 * 2 + 1] = (mFaceArrays.get(faceIndex).vertexPoints[29 * 2 + 1] + mFaceArrays.get(faceIndex).vertexPoints[44 * 2 + 1]) * 0.5f;
    }

    /**
     * Get the vertex coordinates of lips (lip gloss)
     * @param vertexPoints stores the lip vertex coordinates
     * @param faceIndex face index
     */
    public synchronized void getLipsVertices(float[] vertexPoints, int faceIndex) {
        // The lips have a total of 20 vertices, the size must be 40
        if (vertexPoints == null || vertexPoints.length < 40
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        // Copy 84 ~ 103 a total of 20 vertex coordinates
        for (int i = 0; i < 20; i++) {
            // vertex coordinates
            vertexPoints[i * 2] = mFaceArrays.get(faceIndex).vertexPoints[(84 + i) * 2];
            vertexPoints[i * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[(84 + i) * 2 + 1];
        }
    }

    /**
     * Get the vertex coordinates needed for bright eyes
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getBrightEyeVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 32
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        // edge of eye index = 0 ~ 11
        for (int i = 52; i < 64; i++) {
            vertexPoints[(i - 52) * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[(i - 52) * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 + 1];
        }

        vertexPoints[12 * 2] = mFaceArrays.get(faceIndex).vertexPoints[72 * 2];
        vertexPoints[12 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[72 * 2 + 1];

        vertexPoints[13 * 2] = mFaceArrays.get(faceIndex).vertexPoints[73 * 2];
        vertexPoints[13 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[73 * 2 + 1];

        vertexPoints[14 * 2] = mFaceArrays.get(faceIndex).vertexPoints[75 * 2];
        vertexPoints[14 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[75 * 2 + 1];

        vertexPoints[15 * 2] = mFaceArrays.get(faceIndex).vertexPoints[76 * 2];
        vertexPoints[15 * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[76 * 2 + 1];

    }

    /**
     * Get the vertex coordinates needed for beauty teeth, 12 vertices around the mouth
     * @param vertexPoints
     * @param faceIndex
     */
    public synchronized void getBeautyTeethVertices(float[] vertexPoints, int faceIndex) {
        if (vertexPoints == null || vertexPoints.length < 24
                || faceIndex >= mFaceArrays.size() || mFaceArrays.get(faceIndex) == null) {
            return;
        }
        for (int i = 84; i < 96; i++) {
            vertexPoints[(i - 84) * 2] = mFaceArrays.get(faceIndex).vertexPoints[i * 2];
            vertexPoints[(i - 84) * 2 + 1] = mFaceArrays.get(faceIndex).vertexPoints[i * 2 + 1];
        }
    }
}
