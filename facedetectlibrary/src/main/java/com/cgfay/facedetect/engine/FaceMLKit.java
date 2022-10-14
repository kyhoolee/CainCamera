package com.cgfay.facedetect.engine;



import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;

import com.cgfay.facedetect.utils.FaceppConstraints;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import java.util.List;

public class FaceMLKit {

//    private FaceMeshDetector detector;
    private FaceMLKitConfig config;
    public FaceMLKit() {
    }


//    public String init(Context context) {
//        FaceMeshDetectorOptions.Builder optionsBuilder = new FaceMeshDetectorOptions.Builder();
//        optionsBuilder.setUseCase(FaceMeshDetectorOptions.FACE_MESH);
//        detector = FaceMeshDetection.getClient(optionsBuilder.build());
//
//        config = new FaceMLKitConfig();
//
//        return this.getErrorType(0);
//    }



    public FaceMLKitConfig getFaceMLKitConfig() {
        if(config == null) {
            config = new FaceMLKitConfig();
        }
        return config;
    }

    public void setFaceMLKitConfig(FaceMLKitConfig config) {
        this.config = config;
    }

//    public Task<List<FaceMesh>> detect(byte[] imageData, int width, int height) {
//
//        InputImage image = InputImage.fromByteArray(
//                imageData,
//                width,
//                height,
//                config.rotation,
//                InputImage.IMAGE_FORMAT_NV21);
//        Task<List<FaceMesh>> result = detector.process(image);
//
//
//        return result;
//    }


    public Face getLandmark(FaceMesh face) {
        Face f = new Face();

        f.rect = face.getBoundingBox();

        f.points = new PointF[106];

        int[] mapIdx = FaceppConstraints.MLKIT_TO_FACEPP;

        for(int i = 0 ; i < 106 ; i ++) {
            if (mapIdx[i] > 0) {
                f.points[i] = getPointF(face.getAllPoints().get(mapIdx[i]));
            } else {
                f.points[i] = new PointF();
            }
        }

        for(int i = 0  ; i < 106 ; i ++) {
            if(mapIdx[i] < 0){

                if( i < 72) {

                    f.points[i].x = f.points[i-1].x/2 + f.points[i+1].x/2;
                    f.points[i].y = f.points[i-1].y/2 + f.points[i+1].y/2;
                } else if (i > 72 && i < 104) {
                    f.points[i].x = f.points[i-1].x/2 + f.points[i-2].x/2;
                    f.points[i].y = f.points[i-1].y/2 + f.points[i-2].y/2;
                } else {
                    if (i == 104) {
                        f.points[i].x = f.points[74].x;
                        f.points[i].y = f.points[74].y;
                    } else if (i == 105) {
                        f.points[i].x = f.points[77].x;
                        f.points[i].y = f.points[77].y;
                    }
                }
            }
        }

        return f;
    }

    private PointF getPointF(FaceMeshPoint p) {
        PointF result = new PointF(p.getPosition().getX(), p.getPosition().getY());
        return result;
    }



//    public void release() {
//        if(this.detector != null)
//            this.detector.close();
//    }


    private String getErrorType(int retCode) {
        switch(retCode) {
            case -1:
                return "MG_RETCODE_FAILED";
            case 0:
                return "MG_RETCODE_OK";
            case 1:
                return "MG_RETCODE_INVALID_ARGUMENT";
            case 2:
                return "MG_RETCODE_INVALID_HANDLE";
            case 3:
                return "MG_RETCODE_INDEX_OUT_OF_RANGE";
            case 101:
                return "MG_RETCODE_EXPIRE";
            case 102:
                return "MG_RETCODE_INVALID_BUNDLEID";
            case 103:
                return "MG_RETCODE_INVALID_LICENSE";
            case 104:
                return "MG_RETCODE_INVALID_MODEL";
            case 1801:
                return "MG_RETCODE_LOAD_ERROR";
            default:
                return null;
        }
    }

    public static class FaceMLKitConfig {
        public static final int DETECTION_MODE_NORMAL = 0;
        public static final int DETECTION_MODE_TRACKING = 1;
        public static final int DETECTION_MODE_TRACKING_FAST = 3;
        public static final int DETECTION_MODE_TRACKING_ROBUST = 4;
        public static final int DETECTION_MODE_TRACKING_RECT = 5;
        public static final int MG_FPP_DETECTIONMODE_TRACK_RECT = 6;
        public int minFaceSize;
        public int rotation;
        public int interval;
        public int detectionMode;
        public int roi_left;
        public int roi_top;
        public int roi_right;
        public int roi_bottom;
        public float face_confidence_filter;
        public int one_face_tracking;

        public FaceMLKitConfig() {
        }
    }

    public static class Face {
        public int trackID;
        public int index;
        public float pitch;
        public float yaw;
        public float roll;
        public Rect rect;
        public PointF[] points;

        public Face() {
        }
    }


}

