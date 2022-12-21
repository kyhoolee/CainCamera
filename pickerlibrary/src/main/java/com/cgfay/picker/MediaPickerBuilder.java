package com.cgfay.picker;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.cgfay.picker.fragment.MediaPickerFragment;
import com.cgfay.picker.selector.OnMediaSelector;
import com.cgfay.picker.loader.MediaLoader;

public final class MediaPickerBuilder {

    private OnMediaSelector mMediaSelector;
    private MediaPicker mMediaPicker;
    private MediaPickerParam mPickerParam;

    public MediaPickerBuilder(MediaPicker engine) {
        mMediaPicker = engine;
        mPickerParam = new MediaPickerParam();
        MediaPickerManager.getInstance().reset();
    }

    /**
     * Whether to display the photo item list
     * @param show
     * @return
     */
    public MediaPickerBuilder showCapture(boolean show) {
        mPickerParam.setShowCapture(show);
        return this;
    }

    /**
     * whether to show video
     * @param show
     * @return
     */
    public MediaPickerBuilder showVideo(boolean show) {
        mPickerParam.setShowVideo(show);
        return this;
    }

    /**
     * whether to show pictures
     * @param show
     * @return
     */
    public MediaPickerBuilder showImage(boolean show) {
        mPickerParam.setShowImage(show);
        return this;
    }

    /**
     * number of items in a row
     * @param spanCount
     * @return
     */
    public MediaPickerBuilder spanCount(int spanCount) {
        if (spanCount < 0) {
            throw new IllegalArgumentException("spanCount cannot be less than zero");
        }
        mPickerParam.setSpanCount(spanCount);
        return this;
    }

    /**
     * dividing line size
     * @param spaceSize
     * @return
     */
    public MediaPickerBuilder spaceSize(int spaceSize) {
        if (spaceSize < 0) {
            throw new IllegalArgumentException("spaceSize cannot be less than zero");
        }
        mPickerParam.setSpaceSize(spaceSize);
        return this;
    }

    /**
     * Set whether there is an edge dividing line
     * @param hasEdge
     * @return
     */
    public MediaPickerBuilder setItemHasEdge(boolean hasEdge) {
        mPickerParam.setItemHasEdge(hasEdge);
        return this;
    }

    public MediaPickerBuilder setAutoDismiss(boolean autoDismiss) {
        mPickerParam.setAutoDismiss(autoDismiss);
        return this;
    }

    /**
     * image loader
     * @param loader
     * @return
     */
    public MediaPickerBuilder ImageLoader(MediaLoader loader) {
        MediaPickerManager.getInstance().setMediaLoader(loader);
        return this;
    }

    /**
     * set media selection listener
     * @param selector
     */
    public MediaPickerBuilder setMediaSelector(OnMediaSelector selector) {
        mMediaSelector = selector;
        return this;
    }

    /**
     * Show Fragment
     */
    public void show() {
        FragmentActivity activity = mMediaPicker.getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = null;
        if (mMediaPicker.getFragment() != null) {
            fragmentManager = mMediaPicker.getFragment().getChildFragmentManager();
        } else {
            fragmentManager = activity.getSupportFragmentManager();
        }
        Fragment oldFragment = fragmentManager.findFragmentByTag(MediaPickerFragment.TAG);
        if (oldFragment != null) {
            fragmentManager.beginTransaction()
                    .remove(oldFragment)
                    .commitAllowingStateLoss();
        }
        MediaPickerFragment fragment = new MediaPickerFragment();
        fragment.setOnMediaSelector(mMediaSelector);
        Bundle bundle = new Bundle();
        bundle.putSerializable(MediaPicker.PICKER_PARAMS, mPickerParam);
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction()
                .add(fragment, MediaPickerFragment.TAG)
                .commitAllowingStateLoss();
    }
}
