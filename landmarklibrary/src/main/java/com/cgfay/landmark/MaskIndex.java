package com.cgfay.landmark;

/**
 * mask index
 */
public final class MaskIndex {

    private MaskIndex() {

    }

    /**
     * 眼睛遮罩索引，参考眼睛遮罩标注
     */
    public static final short[] eyeMaskIndices = new short[] {
            // 左脸颊边沿 11
            0, 1, 12,
            1, 2, 12,
            12, 2, 17,
            17, 2, 3,
            17, 3, 38,
            17, 38, 33,
            33, 38, 16,
            16, 38, 11,
            16, 11, 15,
            15, 11, 36,
            36, 11, 10,

            // 右脸颊边沿 11
            10, 11, 37,
            37, 11, 18,
            18, 11, 23,
            23, 11, 39,
            23, 39, 35,
            35, 39, 22,
            22, 39, 4,
            22, 4, 5,
            22, 5, 21,
            21, 5, 6,
            21, 6, 7,

            // 右眉毛与眼睛夹缝 11
            21, 7, 9,
            21, 9, 31,
            21, 31, 20,
            20, 31, 30,
            20, 30, 34,
            34, 30, 29,
            34, 29, 19,
            19, 29, 28,
            19, 28, 18,
            18, 28, 37,
            37, 28, 10,

            // 两眉毛中间 1
            10, 28, 27,

            // 左眉毛与眼睛夹缝 11
            10, 27, 36,
            36, 27, 15,
            15, 27, 14,
            14, 27, 26,
            14, 26, 32,
            32, 26, 25,
            32, 25, 13,
            13, 25, 24,
            13, 24, 12,
            12, 24, 8,
            12, 8, 0,

            // 左眼睛 6
            12, 17, 13,
            13, 17, 32,
            32, 17, 33,
            33, 32, 16,
            16, 32, 14,
            14, 16, 15,

            // 右眼睛 6
            18, 23, 19,
            19, 23, 34,
            34, 23, 35,
            34, 35, 22,
            34, 22, 20,
            20, 22, 21,
    };

    /**
     * 唇彩索引, 0 ~ 19 表示关键点 84 ~ 103
     */
    public static final short[] LipsMaskIndices = new short[] {
            // 上嘴唇部分
            0, 1, 12,
            12, 1, 13,
            13, 1, 2,
            2, 13, 14,
            2, 14, 3,
            3, 14, 4,
            4, 14, 15,
            4, 15, 5,
            5, 15, 16,
            5, 16, 6,
            // 下嘴唇部分
            6, 16, 7,
            16, 7, 17,
            17, 7, 8,
            17, 8, 18,
            18, 8, 9,
            18, 9, 10,
            18, 10, 19,
            19, 10, 11,
            19, 11, 12,
            12, 11, 0,
    };

    /**
     * 美牙遮罩索引，10个三角形，可参考 美牙标注.jpg
     */
    public static final short[] teethMaskIndices = new short[] {
            0, 11, 1,
            1, 11, 10,
            1, 10, 2,
            2, 10, 3,
            3, 10, 9,
            3, 9, 8,
            3, 8, 4,
            4, 8, 5,
            5, 8, 7,
            5, 7, 6,
    };

    /**
     * 眼睛索引，参照 亮眼标注.jpg
     */
    private final short[] brightEyeIndices = new short[] {
            0, 5, 1,
            1, 5, 12,
            12, 5, 13,
            12, 13, 4,
            12, 4, 2,
            2, 4, 3,

            6, 7, 11,
            7, 11, 14,
            14, 11, 15,
            14, 15, 10,
            14, 10, 8,
            8, 10, 9
    };
}
