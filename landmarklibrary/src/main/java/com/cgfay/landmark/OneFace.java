package com.cgfay.landmark;

import android.util.SparseArray;

/**
 * A person's keypoint data object
 */
public class OneFace {

//    public static final int GENDER_MAN = 0;
//    public static final int GENDER_WOMAN = 1;
//    public float confidence;

    // Pitch angle (rotate around the x-axis)
    public float pitch;
    // Yaw angle (rotation around the y-axis)
    public float yaw;
    // roll angle (rotation around the z-axis)
    public float roll;

//    public float age;
//    public int gender;

    // vertex coordinates
    public float[] vertexPoints;

    @Override
    protected OneFace clone() {
        OneFace copy = new OneFace();
//        copy.confidence = this.confidence;
        copy.pitch = this.pitch;
        copy.yaw = this.yaw;
        copy.roll = this.roll;
//        copy.age = this.age;
//        copy.gender = this.gender;
        copy.vertexPoints = this.vertexPoints.clone();
        return copy;
    }

    /**
     * copy data
     * @param origin
     * @return
     */
    public static OneFace[] arrayCopy(OneFace[] origin) {
        if (origin == null) {
            return null;
        }
        OneFace[] copy = new OneFace[origin.length];
        for (int i = 0; i < origin.length; i++) {
            copy[i] = origin[i].clone();
        }
        return copy;
    }

    /**
     * copy data
     * @param origin
     * @return
     */
    public static OneFace[] arrayCopy(SparseArray<OneFace> origin) {
        if (origin == null) {
            return null;
        }
        OneFace[] copy = new OneFace[origin.size()];
        for (int i = 0; i < origin.size(); i++) {
            copy[i] = origin.get(i).clone();
        }
        return copy;
    }
}
