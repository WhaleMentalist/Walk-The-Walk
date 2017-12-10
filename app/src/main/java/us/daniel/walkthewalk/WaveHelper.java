package us.daniel.walkthewalk;

import android.animation.AnimatorSet;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.gelitenight.waveview.library.WaveView;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class WaveHelper {

    /** Instance of wave view */
    private WaveView mWaveView;

    /** The animations that will be played */
    private AnimatorSet mAnimatorSet;

    /** The water level that animation will start at */
    private float startLevel;

    /** The water level that animation will end at*/
    private float endLevel;

    public WaveHelper(WaveView waveView, float start, float end) {
        mWaveView = waveView;
        startLevel = start;
        endLevel = end;
        initAnimation();
    }

    /**
     *
     */
    public void start() {
        mWaveView.setShowWave(true);
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    /**
     *
     */
    public void cancel() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet.end();
        }
    }

    /**
     *
     */
    private void initAnimation() {
        List<Animator> animators = new ArrayList<>();

        // horizontal animation.
        // wave waves infinitely.
        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
                mWaveView, "waveShiftRatio", 0f, 1f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(1000);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        animators.add(waveShiftAnim);

        // vertical animation.
        // water level increases from 0 to center of WaveView
        ObjectAnimator mWaterLevelAnim = ObjectAnimator.ofFloat(
                mWaveView, "waterLevelRatio", startLevel, endLevel);
        mWaterLevelAnim.setDuration(10000);
        mWaterLevelAnim.setInterpolator(new DecelerateInterpolator());
        animators.add(mWaterLevelAnim);

        // amplitude animation.
        // wave grows big then grows small, repeatedly
        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(
                mWaveView, "amplitudeRatio", 0.0001f, 0.05f);
        amplitudeAnim.setRepeatCount(ValueAnimator.INFINITE);
        amplitudeAnim.setRepeatMode(ValueAnimator.REVERSE);
        amplitudeAnim.setDuration(5000);
        amplitudeAnim.setInterpolator(new LinearInterpolator());
        animators.add(amplitudeAnim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
    }
}
