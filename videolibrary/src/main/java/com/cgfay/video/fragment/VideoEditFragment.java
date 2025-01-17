package com.cgfay.video.fragment;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cgfay.filter.glfilter.resource.FilterHelper;
import com.cgfay.filter.glfilter.resource.bean.ResourceData;
import com.cgfay.media.CainMediaEditor;
import com.cgfay.media.CainMediaPlayer;
import com.cgfay.media.IMediaPlayer;
import com.cgfay.uitls.fragment.BackPressedDialogFragment;
import com.cgfay.uitls.utils.DensityUtils;
import com.cgfay.uitls.utils.DisplayUtils;
import com.cgfay.uitls.utils.FileUtils;
import com.cgfay.uitls.utils.StringUtils;
import com.cgfay.uitls.widget.RoundOutlineProvider;
import com.cgfay.video.R;
import com.cgfay.video.adapter.VideoEffectAdapter;
import com.cgfay.video.adapter.VideoEffectCategoryAdapter;
import com.cgfay.video.adapter.VideoFilterAdapter;
import com.cgfay.video.bean.EffectMimeType;
import com.cgfay.video.bean.EffectType;
import com.cgfay.video.widget.EffectSelectedSeekBar;
import com.cgfay.video.widget.VideoTextureView;
import com.cgfay.video.widget.WaveCutView;

import java.io.IOException;

/**
 * Effects editing page
 */
public class VideoEditFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "VideoEditFragment";

    private Activity mActivity;

    private String mVideoPath;                      // video stream path
    private String mMusicPath;                      // background music path
    private float mSourceVolumePercent = 0.5f;      // Source volume percentage
    private float mBackgroundVolumePercent = 0.5f;  // Background music volume percentage
    private long mBackgroundDuration;               // background music duration，ms

    private View mContentView;
    // Playback controls
    private RelativeLayout mLayoutPlayer;
    private VideoTextureView mVideoPlayerView;
    private ImageView mIvVideoPlay;

    // Effect selection bar
    private LinearLayout mLayoutEffect;             // Effects layout
    private TextView mTvVideoCurrent;               // current position
    private EffectSelectedSeekBar mSbEffectSelected;// Progress bar with special effects selected
    private TextView mTvVideoDuration;              // video duration
    private TextView mTvEffectTips;                 // Special effect tips
    private TextView mTvEffectCancel;               // undo button
    private RecyclerView mListEffectView;           // Effect list
    private RecyclerView mListEffectCategoryView;   // Effects directory list
    private boolean mEffectShowing;                 // Effect page display status
    private VideoEffectAdapter mEffectAdapter;      // Effects List Adapter
    private VideoEffectCategoryAdapter mEffectCategoryAdapter; // Effects Directory Listing Adapter

    // top control bar
    private View mLayoutTop;
    // top sub control bar
    private RelativeLayout mLayoutSubTop;
    // Bottom control bar
    private View mLayoutBottom;
    // Bottom sub control bar
    private FrameLayout mLayoutSubBottom;

    // Volume adjustment page
    private View mLayoutVolumeChange;
    private SeekBar mSbBackgroundVolume;

    // music clipping page
    private View mLayoutCutMusic;
    private WaveCutView mWaveCutView;
    private TextView mTvMusicCurrent;
    private TextView mTvMusicDuration;

    // Filter list
    private RecyclerView mListFilterView;
    private VideoFilterAdapter mFilterAdapter;


    // player
    private MediaPlayer mAudioPlayer;
    private CainMediaPlayer mCainMediaPlayer;
    private AudioManager mAudioManager;
    private CainMediaEditor mMediaEditor;
    private int mPlayViewWidth;
    private int mPlayViewHeight;
    private int mVideoWidth;
    private int mVideoHeight;

    public static VideoEditFragment newInstance() {
        return new VideoEditFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContentView =  inflater.inflate(R.layout.fragment_video_edit, container, false);
        return mContentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        // Player display controls
        mLayoutPlayer = mContentView.findViewById(R.id.layout_player);
        mVideoPlayerView = mContentView.findViewById(R.id.video_player_view);
        mVideoPlayerView.setSurfaceTextureListener(mSurfaceTextureListener);
        mVideoPlayerView.setOnClickListener(this);
        mIvVideoPlay = mContentView.findViewById(R.id.iv_video_play);
        mIvVideoPlay.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= 21) {
            mVideoPlayerView.setOutlineProvider(new RoundOutlineProvider(DensityUtils.dp2px(mActivity, 7.5f)));
            mVideoPlayerView.setClipToOutline(true);
        }
        RelativeLayout.LayoutParams playerParams = (RelativeLayout.LayoutParams) mVideoPlayerView.getLayoutParams();
        if (DisplayUtils.isFullScreenDevice(mActivity)) {
            playerParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.layout_edit_bottom_height);
        } else {
            playerParams.bottomMargin = 0;
        }
        mVideoPlayerView.setLayoutParams(playerParams);

        /////////////////////////////////////////////////////////
        // 1. Effect features
        // Effects control bar
        mLayoutEffect = mContentView.findViewById(R.id.layout_effect);
        mTvVideoCurrent = mContentView.findViewById(R.id.tv_video_current);
        mSbEffectSelected = mContentView.findViewById(R.id.sb_select_effect);
        mSbEffectSelected.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mTvVideoDuration = mContentView.findViewById(R.id.tv_video_duration);
        mTvEffectTips = mContentView.findViewById(R.id.tv_video_edit_effect_tips);
        mTvEffectCancel = mContentView.findViewById(R.id.tv_video_edit_effect_cancel);
        mTvEffectCancel.setOnClickListener(this);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mActivity);
        ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.HORIZONTAL);


        // Effect list
        mListEffectView = mContentView.findViewById(R.id.list_video_edit_effect);
        mListEffectView.setLayoutManager(layoutManager);
        mEffectAdapter = new VideoEffectAdapter(
                mActivity,
                EffectFilterHelper.getInstance().getEffectFilterData());
        mEffectAdapter.setOnEffectChangeListener(mEffectChangeListener);
        mListEffectView.setAdapter(mEffectAdapter);

        // Effects directory list
        mListEffectCategoryView = mContentView.findViewById(R.id.list_video_edit_effect_category);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(mActivity);
        ((LinearLayoutManager) manager).setOrientation(LinearLayoutManager.HORIZONTAL);
        mListEffectCategoryView.setLayoutManager(manager);
        mEffectCategoryAdapter = new VideoEffectCategoryAdapter(mActivity);
        mEffectCategoryAdapter.setOnEffectCategoryChangeListener(mEffectCategoryChangeListener);
        mListEffectCategoryView.setAdapter(mEffectCategoryAdapter);

        // Effects directory list
        mLayoutTop = mContentView.findViewById(R.id.layout_top);
        mContentView.findViewById(R.id.btn_edit_back).setOnClickListener(this);
        mContentView.findViewById(R.id.btn_select_music).setOnClickListener(this);

        /////////////////////////////////////////////////////////
        // 2. Control bar
        // Top sub control bar
        mLayoutSubTop = mContentView.findViewById(R.id.layout_sub_top);
        mContentView.findViewById(R.id.btn_sub_cancel).setOnClickListener(this);
        mContentView.findViewById(R.id.btn_sub_save).setOnClickListener(this);

        // Bottom control bar
        mLayoutBottom = mContentView.findViewById(R.id.layout_bottom);
        mContentView.findViewById(R.id.btn_edit_effect).setOnClickListener(this);
        mContentView.findViewById(R.id.btn_edit_filter).setOnClickListener(this);
        mContentView.findViewById(R.id.btn_edit_stickers).setOnClickListener(this);
        mContentView.findViewById(R.id.btn_edit_next).setOnClickListener(this);

        // Bottom sub control bar
        mLayoutSubBottom = mContentView.findViewById(R.id.layout_sub_bottom);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCainMediaPlayer == null) {
            mCainMediaPlayer = new CainMediaPlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.resume();
            mIvVideoPlay.setVisibility(View.GONE);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.pause();
            mIvVideoPlay.setVisibility(View.VISIBLE);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
    }

    public void onBackPressed() {
        DialogFragment fragment = new BackPressedDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BackPressedDialogFragment.MESSAGE, R.string.video_edit_back_press);
        fragment.setArguments(bundle);
        fragment.show(getChildFragmentManager(), "");
    }

    @Override
    public void onDestroyView() {
        mContentView = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager = null;
        }
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.reset();
            mCainMediaPlayer = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer = null;
        }
        FileUtils.deleteFile(mVideoPath);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.video_player_view) {
            if (mCainMediaPlayer != null) {
                if (mCainMediaPlayer.isPlaying()) {
                    pausePlayer();
                } else {
                    resumePlayer();
                }
            }
        } else if (id == R.id.iv_video_play) {
            resumePlayer();
        } else if (id == R.id.btn_edit_back) {
            onBackPressed();
        } else if (id == R.id.btn_select_music) {
            selectMusic();
        } else if (id == R.id.btn_sub_cancel) {
            subChangeCancel();
        } else if (id == R.id.btn_sub_save) {
            subChangeSave();
        } else if (id == R.id.btn_edit_effect) {
            showChangeEffectLayout(true);
        } else if (id == R.id.btn_edit_filter) {
            showSelectFilterLayout(true);
        } else if (id == R.id.btn_edit_stickers) {
            showSelectStickersLayout(true);
        } else if (id == R.id.btn_edit_next) {
            saveAllChange();
        } else if (id == R.id.iv_volume_change_save) {
            showVolumeChangeLayout(false);
        } else if (id == R.id.iv_cut_music_save) {
            showCutMusicLayout(false);
        } else if (id == R.id.tv_video_edit_effect_cancel) {   // 撤销特效
            // TODO Undo the last effect
        }
    }

    private void resetBottomView() {
        mLayoutSubBottom.removeAllViews();
        mLayoutSubBottom.setVisibility(View.GONE);
        mLayoutBottom.setVisibility(View.VISIBLE);
        mLayoutTop.setVisibility(View.VISIBLE);
        mLayoutSubTop.setVisibility(View.GONE);
    }

    private void selectMusic() {
        resetBottomView();
        if (mOnSelectMusicListener != null) {
            mOnSelectMusicListener.onOpenMusicSelectPage();
        }
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.pause();
            mIvVideoPlay.setVisibility(View.VISIBLE);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
    }

    /**
     * Whether to show the volume adjustment layout
     * @param showSubView
     */
    private void showVolumeChangeLayout(boolean showSubView) {
        if (showSubView) {
            mLayoutBottom.setVisibility(View.GONE);
            if (mLayoutVolumeChange == null) {
                mLayoutVolumeChange = LayoutInflater.from(mActivity).inflate(R.layout.view_volume_change, null);
                mLayoutVolumeChange.findViewById(R.id.iv_volume_change_save).setOnClickListener(this);
                ((SeekBar)mLayoutVolumeChange.findViewById(R.id.sb_volume_source))
                        .setOnSeekBarChangeListener(mVolumeChangeListener);
                mSbBackgroundVolume = mLayoutVolumeChange.findViewById(R.id.sb_volume_background);
                mSbBackgroundVolume.setOnSeekBarChangeListener(mVolumeChangeListener);
                if (mMusicPath != null) {
                    mSbBackgroundVolume.setMax(100);
                    mSbBackgroundVolume.setProgress((int)(mBackgroundVolumePercent * 100));
                } else {
                    mSbBackgroundVolume.setMax(0);
                    mSbBackgroundVolume.setProgress(0);

                }
            }
            mLayoutSubBottom.removeAllViews();
            mLayoutSubBottom.addView(mLayoutVolumeChange);
            mLayoutSubBottom.setVisibility(View.VISIBLE);
            mLayoutTop.setVisibility(View.GONE);
        } else {
            resetBottomView();
        }
    }

    /**
     * Whether to show clip music layout
     * @param showSubView
     */
    private void showCutMusicLayout(boolean showSubView) {
        if (showSubView && mMusicPath != null) {
            mLayoutBottom.setVisibility(View.GONE);
            if (mLayoutCutMusic == null) {
                mLayoutCutMusic = LayoutInflater.from(mActivity).inflate(R.layout.view_music_cut, null);
                mLayoutCutMusic.findViewById(R.id.iv_cut_music_save).setOnClickListener(this);
                mTvMusicCurrent = mLayoutCutMusic.findViewById(R.id.tv_audio_current);
                mTvMusicDuration = mLayoutCutMusic.findViewById(R.id.tv_audio_duration);
                mWaveCutView = mLayoutCutMusic.findViewById(R.id.wave_cut_view);
                mWaveCutView.setOnDragListener(mCutMusicListener);
                if (mMusicPath != null) {
                    mWaveCutView.setMax((int) mBackgroundDuration);
                    mWaveCutView.setProgress(0);
                    mTvMusicDuration.setText(StringUtils.generateStandardTime((int) mBackgroundDuration));
                } else {
                    mWaveCutView.setMax(50);
                    mWaveCutView.setProgress(0);
                }
            }
            mLayoutSubBottom.removeAllViews();
            mLayoutSubBottom.addView(mLayoutCutMusic);
            mLayoutSubBottom.setVisibility(View.VISIBLE);
            mLayoutTop.setVisibility(View.GONE);
        } else {
            resetBottomView();
        }
    }

    private void subChangeCancel() {
        resetBottomView();
        if (mEffectShowing) {
            showChangeEffectLayout(false);
            if (mCainMediaPlayer != null) {
                mCainMediaPlayer.changeEffect(-1);
            }
        }
    }

    private void subChangeSave() {
        resetBottomView();
        if (mEffectShowing) {
            showChangeEffectLayout(false);
        }
    }

    /**
     * Show effects page
     * @param showSubView
     */
    private void showChangeEffectLayout(boolean showSubView) {
        mEffectShowing = showSubView;


        if (showSubView) {
            // 1. Animating show effect list - in mLayoutEffect

            AnimatorSet animatorSet = new AnimatorSet();
            // Effects page shows animation
            ValueAnimator effectShowAnimator = ValueAnimator.ofFloat(1f, 0f);
            effectShowAnimator.setDuration(400);
            final LinearLayout.LayoutParams effectParams =
                    (LinearLayout.LayoutParams) mLayoutEffect.getLayoutParams();
            final LinearLayout.LayoutParams playerParams =
                    (LinearLayout.LayoutParams) mLayoutPlayer.getLayoutParams();
            mPlayViewWidth = mLayoutPlayer.getWidth();
            mPlayViewHeight = mLayoutPlayer.getHeight();
            final int minPlayViewHeight = mPlayViewHeight - DensityUtils.dp2px(mActivity, 200);
            final float playerViewScale = mPlayViewWidth/(float)mPlayViewHeight;
            effectShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    effectParams.bottomMargin = (int) (-DensityUtils.dp2px(mActivity, 200)
                            * (float)animation.getAnimatedValue());
                    mLayoutEffect.setLayoutParams(effectParams);
                    playerParams.width = (int) (
                            (
                                    minPlayViewHeight
                                    + (
                                            (mPlayViewHeight - minPlayViewHeight)
                                                    * (float)animation.getAnimatedValue()
                                    )
                            )
                                    * playerViewScale
                    );
                    playerParams.bottomMargin = (int) (
                            DensityUtils.dp2px(mActivity, 18)
                                    * (1f - (float)animation.getAnimatedValue()
                            )
                    );
                    mLayoutPlayer.setLayoutParams(playerParams);
                }
            });
            animatorSet.playSequentially(effectShowAnimator);
            animatorSet.start();

            mLayoutBottom.setVisibility(View.GONE);
            mLayoutTop.setVisibility(View.GONE);
            mLayoutSubTop.setVisibility(View.VISIBLE);

            pausePlayer();
        } else {
            // 2. Animating hide effect list - in mLayoutEffect

            AnimatorSet animatorSet = new AnimatorSet();
            // Effects page exit animation
            ValueAnimator effectExitAnimator = ValueAnimator.ofFloat(0f, 1f);
            effectExitAnimator.setDuration(400);
            final LinearLayout.LayoutParams effectParams = (LinearLayout.LayoutParams) mLayoutEffect.getLayoutParams();
            final LinearLayout.LayoutParams playerParams = (LinearLayout.LayoutParams) mLayoutPlayer.getLayoutParams();
            final int minPlayViewHeight = mPlayViewHeight - DensityUtils.dp2px(mActivity, 200);
            final float playerViewScale = mPlayViewWidth/(float)mPlayViewHeight;
            effectExitAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    effectParams.bottomMargin = (int) (-DensityUtils.dp2px(mActivity, 200)
                            * (float)animation.getAnimatedValue()
                    );
                    mLayoutEffect.setLayoutParams(effectParams);
                    playerParams.width = (int) (
                            (minPlayViewHeight
                                    + ((mPlayViewHeight - minPlayViewHeight)
                                    * (float)animation.getAnimatedValue())
                            )
                                    * playerViewScale
                    );
                    playerParams.bottomMargin = (int) (DensityUtils.dp2px(mActivity, 18)
                            * (1f - (float)animation.getAnimatedValue())
                    );
                    mLayoutPlayer.setLayoutParams(playerParams);
                }
            });
            animatorSet.playSequentially(effectExitAnimator);
            animatorSet.start();

            resumePlayer();
        }
    }

    /**
     * Switch effect directory
     * @param type
     */
    private void changeEffectCategoryView(EffectMimeType type) {
        if (type == EffectMimeType.FILTER) {

        } else if (type == EffectMimeType.TRANSITION) {

        } else if (type == EffectMimeType.MULTIFRAME) {

        } else if (type == EffectMimeType.TIME) {

        }
    }

    /**
     * Display the Select Filter page
     * @param showSubView
     */
    private void showSelectFilterLayout(boolean showSubView) {
        if (showSubView) {
            if (mListFilterView == null) {
                mListFilterView = new RecyclerView(mActivity);
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mActivity);
                ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.HORIZONTAL);
                mListFilterView.setLayoutManager(layoutManager);
            }
            if (mFilterAdapter == null) {
                mFilterAdapter = new VideoFilterAdapter(mActivity, FilterHelper.getFilterList());
            }
            mFilterAdapter.setOnFilterChangeListener(mFilterChangeListener);
            mListFilterView.setAdapter(mFilterAdapter);
            mLayoutSubBottom.removeAllViews();
            mLayoutSubBottom.addView(mListFilterView);
            mLayoutSubBottom.setVisibility(View.VISIBLE);

            mLayoutBottom.setVisibility(View.GONE);
            mLayoutTop.setVisibility(View.GONE);
            mLayoutSubTop.setVisibility(View.VISIBLE);
        } else {
            resetBottomView();
        }
    }

    /**
     * Select sticker page
     * @param showSubView
     */
    private void showSelectStickersLayout(boolean showSubView) {
        if (showSubView) {

        } else {
            resetBottomView();
        }
    }

    /**
     * Save all changes, composite video
     */
    private void saveAllChange() {

    }

    private void resumePlayer() {
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.resume();
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
        }
        mIvVideoPlay.setVisibility(View.GONE);
    }

    private void seekTo(long timeMs) {
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.resume();
            mCainMediaPlayer.seekTo(timeMs);
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
            mAudioPlayer.seekTo((int)timeMs);
        }
        mIvVideoPlay.setVisibility(View.GONE);
    }

    private void pausePlayer() {
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.pause();
        }
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
        mIvVideoPlay.setVisibility(View.VISIBLE);
    }

    /**
     * Set the video stream path
     * @param videoPath
     */
    public void setVideoPath(String videoPath) {
        mVideoPath = videoPath;
    }

    /**
     * Set the background music path
     * @param musicPath
     * @param duration
     */
    public void setSelectedMusic(String musicPath, long duration) {
        mMusicPath = musicPath;
        mBackgroundDuration = duration;
        if (mCainMediaPlayer != null) {
            mCainMediaPlayer.resume();
            mIvVideoPlay.setVisibility(View.GONE);
        }
        if (mAudioPlayer == null) {
            mAudioPlayer = new MediaPlayer();
        } else {
            mAudioPlayer.reset();
        }
        try {
            mAudioPlayer.setDataSource(mMusicPath);
            mAudioPlayer.setLooping(true);
            mAudioPlayer.prepare();
            mAudioPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioPlayer.setVolume(mBackgroundVolumePercent, mBackgroundVolumePercent);
        if (mSbBackgroundVolume != null) {
            mSbBackgroundVolume.setMax(100);
            mSbBackgroundVolume.setProgress((int)(mBackgroundVolumePercent * 100));
        }
        if (mWaveCutView != null) {
            mWaveCutView.setMax((int) mBackgroundDuration);
            mWaveCutView.setProgress(0);
            mTvMusicDuration.setText(StringUtils.generateStandardTime((int) mBackgroundDuration));
        }
    }

    // video display monitor
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (mSurfaceTexture == null) {
                mSurfaceTexture = surface;
                openMediaPlayer();
            } else {
                mVideoPlayerView.setSurfaceTexture(mSurfaceTexture);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return mSurfaceTexture == null;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /**
     * Open the video player
     */
    private void openMediaPlayer() {
        mContentView.setKeepScreenOn(true);
        mCainMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final IMediaPlayer mp) {
                mp.start();
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSbEffectSelected != null) {
                            mSbEffectSelected.setMax(mp.getDuration());
                            mSbEffectSelected.setProgress(mp.getCurrentPosition());
                        }
                        if (mTvVideoCurrent != null) {
                            mTvVideoCurrent.setText(StringUtils.generateStandardTime((int)mp.getCurrentPosition()));
                        }
                        if (mTvVideoDuration != null) {
                            mTvVideoDuration.setText(StringUtils.generateStandardTime((int)mp.getDuration()));
                        }
                    }
                });
            }
        });
//        mCainMediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
//            @Override
//            public void onVideoSizeChanged(IMediaPlayer mediaPlayer, int width, int height) {
//                if (mediaPlayer.getRotate() % 180 != 0) {
//                    mVideoPlayerView.setVideoSize(height, width);
//                } else {
//                    mVideoPlayerView.setVideoSize(width, height);
//                }
//            }
//        });
        mCainMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {

            }
        });

        mCainMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onError: what = " + what + ", extra = " + extra);
                return false;
            }
        });

        mCainMediaPlayer.setOnCurrentPositionListener(new CainMediaPlayer.OnCurrentPositionListener() {
            @Override
            public void onCurrentPosition(IMediaPlayer mp, long current, long duration) {
                if (mTvVideoCurrent != null) {
                    mTvVideoCurrent.setText(StringUtils.generateStandardTime((int)current));
                }
                if (mSbEffectSelected != null) {
                    mSbEffectSelected.setProgress((float)current);
                }
            }
        });
        try {
            mCainMediaPlayer.setDataSource(mVideoPath);
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mCainMediaPlayer.setSurface(mSurface);
            mCainMediaPlayer.setVolume(mSourceVolumePercent, mSourceVolumePercent);
            mCainMediaPlayer.setOption(CainMediaPlayer.OPT_CATEGORY_PLAYER, "vcodec", "h264_mediacodec");
            mCainMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sliding monitor with special effects selection
     */
    private EffectSelectedSeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new EffectSelectedSeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgress(int progress, boolean fromUser) {
            if (fromUser) {
                Log.d(TAG, "onProgress: progress = " + progress);
            }
        }

        @Override
        public void onStopTrackingTouch(int progress) {
            seekTo(progress);
        }

        @Override
        public void onStartTrackingTouch() {
            pausePlayer();
        }
    };


    /**
     * volume adjustment monitor
     */
    private SeekBar.OnSeekBarChangeListener mVolumeChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar.getId() == R.id.sb_volume_source && fromUser) {
                mSourceVolumePercent = (float) progress  / (float) seekBar.getMax();
            } else if (seekBar.getId() == R.id.sb_volume_background && fromUser) {
                if (seekBar.getMax() > 0) {
                    mBackgroundVolumePercent = (float) progress / (float) seekBar.getMax();
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sb_volume_background) {
                if (mAudioPlayer != null) {
                    mAudioPlayer.setVolume(mBackgroundVolumePercent, mBackgroundVolumePercent);
                }
            } else if (seekBar.getId() == R.id.sb_volume_source) {
                if (mCainMediaPlayer != null) {
                    Log.d(TAG, "onStopTrackingTouch: volume = " + mSourceVolumePercent);
                    mCainMediaPlayer.setVolume(mSourceVolumePercent, mSourceVolumePercent);
                }
            }
        }
    };

    /**
     * cropped music monitor
     */
    private WaveCutView.OnDragListener mCutMusicListener = new WaveCutView.OnDragListener() {
        @Override
        public void onDragging(int position) {
            mTvMusicCurrent.setText(StringUtils.generateStandardTime(position));
        }

        @Override
        public void onDragFinish(float position) {
            if (mAudioPlayer != null) {
                mAudioPlayer.seekTo((int)position);
            }
        }
    };

    /**
     * Filter list change callback
     */
    private VideoFilterAdapter.OnFilterChangeListener mFilterChangeListener = new VideoFilterAdapter.OnFilterChangeListener() {
        @Override
        public void onFilterChanged(ResourceData resourceData) {

        }
    };

    /**
     * Effect list toggle
     */
    private VideoEffectAdapter.OnEffectChangeListener mEffectChangeListener = new VideoEffectAdapter.OnEffectChangeListener() {
        @Override
        public void onEffectChanged(EffectType effectType) {
            if (mCainMediaPlayer != null) {
                mCainMediaPlayer.changeEffect(effectType.getName());
            }
        }
    };

    /**
     * special effects directory switch
     */
    private VideoEffectCategoryAdapter.OnEffectCategoryChangeListener mEffectCategoryChangeListener = new VideoEffectCategoryAdapter.OnEffectCategoryChangeListener() {
        @Override
        public void onCategoryChange(EffectMimeType mimeType) {
            if (mimeType == EffectMimeType.FILTER) {
                mEffectAdapter.changeEffectData(EffectFilterHelper.getInstance().getEffectFilterData());
            } else if (mimeType == EffectMimeType.MULTIFRAME) {
                mEffectAdapter.changeEffectData(EffectFilterHelper.getInstance().getEffectMultiData());
            } else if (mimeType == EffectMimeType.TRANSITION) {
                mEffectAdapter.changeEffectData(EffectFilterHelper.getInstance().getEffectTransitionData());
            } else {
                mEffectAdapter.changeEffectData(null);
            }
        }
    };

    /**
     * page action listener
     */
    public interface OnSelectMusicListener {

        void onOpenMusicSelectPage();
    }

    /**
     * Add page action listener
     * @param listener
     */
    public void setOnSelectMusicListener(OnSelectMusicListener listener) {
        mOnSelectMusicListener = listener;
    }

    private OnSelectMusicListener mOnSelectMusicListener;
}
