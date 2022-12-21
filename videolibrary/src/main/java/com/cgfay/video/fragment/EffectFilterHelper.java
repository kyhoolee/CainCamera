package com.cgfay.video.fragment;

import com.cgfay.video.bean.EffectMimeType;
import com.cgfay.video.bean.EffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 特效助手
 * 备注：这里的特效滤镜都放到了Native层，名称和id要跟Native层GLFilter中的滤镜对应。
 */
public class EffectFilterHelper {

    // List of filter effects
    private final List<EffectType> mEffectFilterList = new ArrayList<>();
    // Transition Effects List
    private final List<EffectType> mEffectTransitionList = new ArrayList<>();
    // Split screen effect list
    private final List<EffectType> mEffectMultiList = new ArrayList<>();

    private static EffectFilterHelper instance;

    public static EffectFilterHelper getInstance() {
        if (instance == null) {
            instance = new EffectFilterHelper();
        }
        return instance;
    }

    private EffectFilterHelper() {
        initAssertEffect();
    }

    private void initAssertEffect() {
        mEffectFilterList.clear();
        mEffectTransitionList.clear();
        mEffectMultiList.clear();
        // Filter effect
        mEffectFilterList.add(new EffectType(EffectMimeType.FILTER, "body shadow", 0x000, "assets://thumbs/effect/icon_effect_soul_stuff.png"));
        mEffectFilterList.add(new EffectType(EffectMimeType.FILTER, "jitter", 0x001, "assets://thumbs/effect/icon_effect_shake.png"));
        mEffectFilterList.add(new EffectType(EffectMimeType.FILTER, "illusion", 0x002, "assets://thumbs/effect/icon_effect_illusion.png"));
        mEffectFilterList.add(new EffectType(EffectMimeType.FILTER, "zoom", 0x003, "assets://thumbs/effect/icon_effect_scale.png"));
        mEffectFilterList.add(new EffectType(EffectMimeType.FILTER, "flash white", 0x004, "assets://thumbs/effect/icon_effect_glitter_white.png"));

        // Split screen effects
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "blurry split", 0x200, "assets://thumbs/effect/icon_frame_blur.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "black-white", 0x201, "assets://thumbs/effect/icon_frame_bw_three.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "2-screen", 0x202, "assets://thumbs/effect/icon_frame_two.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "3-screen", 0x203, "assets://thumbs/effect/icon_frame_three.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "4-screen", 0x204, "assets://thumbs/effect/icon_frame_four.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "6-screen", 0x205, "assets://thumbs/effect/icon_frame_six.png"));
        mEffectMultiList.add(new EffectType(EffectMimeType.MULTIFRAME, "9-screen", 0x206, "assets://thumbs/effect/icon_frame_nine.png"));
    }

    /**
     * Get filter effect data
     * @return
     */
    public List<EffectType> getEffectFilterData() {
        return mEffectFilterList;
    }

    /**
     * Get transition effect data
     * @return
     */
    public List<EffectType> getEffectTransitionData() {
        return mEffectTransitionList;
    }

    /**
     * Get split screen effect data
     * @return
     */
    public List<EffectType> getEffectMultiData() {
        return mEffectMultiList;
    }

}
