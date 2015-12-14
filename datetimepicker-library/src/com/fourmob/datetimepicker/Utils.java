package com.fourmob.datetimepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;

import java.util.Calendar;


public class Utils {

    public static final int PULSE_ANIMATOR_DURATION = 544;
    public static final int MONTHS_IN_YEAR = 12;

	public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case Calendar.JANUARY:
            case Calendar.MARCH:
            case Calendar.MAY:
            case Calendar.JULY:
            case Calendar.AUGUST:
            case Calendar.OCTOBER:
            case Calendar.DECEMBER:
                return 31;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            case Calendar.FEBRUARY:
                return (year % 4 == 0) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
	}

    public static int getMonthsBetweenDates(int startMonth, int startYear, int endMonth, int endYear) {
        return (endYear - startYear) * MONTHS_IN_YEAR + endMonth - startMonth + 1;
    }

	public static ObjectAnimator getPulseAnimator(View labelToAnimate, float decreaseRatio, float increaseRatio) {
        Keyframe k0 = Keyframe.ofFloat(0f, 1f);
        Keyframe k1 = Keyframe.ofFloat(0.275f, decreaseRatio);
        Keyframe k2 = Keyframe.ofFloat(0.69f, increaseRatio);
        Keyframe k3 = Keyframe.ofFloat(1f, 1f);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe("scaleX", k0, k1, k2, k3);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe("scaleY", k0, k1, k2, k3);
        ObjectAnimator pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(labelToAnimate, scaleX, scaleY);
        pulseAnimator.setDuration(PULSE_ANIMATOR_DURATION);

        return pulseAnimator;
    }

	public static boolean isJellybeanOrLater() {
		return Build.VERSION.SDK_INT >= 16;
	}

    /**
     * Try to speak the specified text, for accessibility. Only available on JB or later.
     * @param text Text to announce.
     */
    @SuppressLint("NewApi")
    public static void tryAccessibilityAnnounce(View view, CharSequence text) {
        if (isJellybeanOrLater() && view != null && text != null) {
            view.announceForAccessibility(text);
        }
    }

    public static boolean isTouchExplorationEnabled(AccessibilityManager accessibilityManager) {
        if (Build.VERSION.SDK_INT >= 14) {
            return accessibilityManager.isTouchExplorationEnabled();
        } else {
            return false;
        }
    }

    public static int getColorAttribute(Context context, int attribute) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(typedValue.data, new int[]{attribute});
            return a.getColor(0, Color.BLACK);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public static int getPrimaryColor(Context context) {
        return getColorAttribute(context, R.attr.colorPrimary);
    }

    public static int getPrimaryDarkColor(Context context) {
        return getColorAttribute(context, R.attr.colorPrimaryDark);
    }

    public static ColorStateList createThemedTextColorStateList(Context context) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
        };

        int[] colors = new int[]{
                Utils.getPrimaryColor(context),
                context.getResources().getColor(R.color.done_text_color_disabled)
        };

        return new ColorStateList(states, colors);
    }
}
