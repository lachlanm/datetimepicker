package com.fourmob.datetimepicker.date;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;

import com.fourmob.datetimepicker.R;
import com.fourmob.datetimepicker.Utils;
import com.nineoldandroids.animation.ObjectAnimator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerDialog extends DialogFragment implements View.OnClickListener, YearPickerView.YearPickerListener, DayPickerView.DayPickerListener {

    private static final String KEY_SELECTED_YEAR = "year";
    private static final String KEY_SELECTED_MONTH = "month";
    private static final String KEY_SELECTED_DAY = "day";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_PULSE_ANIMATE = "pulse_animate";

    // https://code.google.com/p/android/issues/detail?id=13050
    private static final CalendarDay MINIMUM_POSSIBLE_DATE = new CalendarDay(1902, 1, 1);
    private static final CalendarDay MAXIMUM_POSSIBLE_DATE = new CalendarDay(2037, 12, 31);

    private static final int UNINITIALIZED = -1;
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final int YEAR_VIEW = 1;

    public static final int ANIMATION_DELAY = 500;
    public static final String KEY_WEEK_START = "week_start";
    public static final String KEY_MIN_DATE = "min_date";
    public static final String KEY_MAX_DATE = "max_date";
    public static final String KEY_CURRENT_VIEW = "current_view";
    public static final String KEY_LIST_POSITION = "list_position";
    public static final String KEY_LIST_POSITION_OFFSET = "list_position_offset";

    private static SimpleDateFormat DAY_MONTH_FORMAT = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
    private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());

    private final Calendar mCalendar = Calendar.getInstance();
    private OnDateSetListener mCallBack;

    private AccessibleDateAnimator mAnimator;
    private boolean mDelayAnimation = true;
    private long mLastVibrate;
    private int mCurrentView = UNINITIALIZED;

    private int mWeekStart = mCalendar.getFirstDayOfWeek();

    private String mDayPickerDescription;
    private String mYearPickerDescription;
    private String mSelectDay;
    private String mSelectYear;

    private DayPickerView mDayPickerView;
    private TextView mMonthAndDayView;
    private Vibrator mVibrator;
    private YearPickerView mYearPickerView;
    private TextView mYearView;

    private boolean mVibrate = true;
    private boolean mUsePulseAnimations = true;
    private boolean mCloseOnSingleTapDay;

    @NonNull
    private CalendarDay mMinDate = MINIMUM_POSSIBLE_DATE;
    @NonNull
    private CalendarDay mMaxDate = MAXIMUM_POSSIBLE_DATE;

    private boolean mIsViewInitialized = false;

    public DatePickerDialog() {
        // Empty constructor required for dialog fragment. DO NOT REMOVE
    }

    public static DatePickerDialog newInstance(OnDateSetListener onDateSetListener, int year, int month, int day) {
        return newInstance(onDateSetListener, year, month, day, true);
    }

    public static DatePickerDialog newInstance(OnDateSetListener onDateSetListener, int year, int month, int day, boolean vibrate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog();
        datePickerDialog.initialize(onDateSetListener, year, month, day, vibrate);
        return datePickerDialog;
    }

    private void initialize(OnDateSetListener onDateSetListener, int year, int month, int day, boolean vibrate) {
        if (year > MAXIMUM_POSSIBLE_DATE.year)
            throw new IllegalArgumentException("year end must < " + MAXIMUM_POSSIBLE_DATE.year);

        if (year < MINIMUM_POSSIBLE_DATE.year)
            throw new IllegalArgumentException("year end must > " + MINIMUM_POSSIBLE_DATE.year);

        mCallBack = onDateSetListener;
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        mVibrate = vibrate;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mVibrator = ((Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE));
        if (bundle != null) {
            mCalendar.set(Calendar.YEAR, bundle.getInt(KEY_SELECTED_YEAR));
            mCalendar.set(Calendar.MONTH, bundle.getInt(KEY_SELECTED_MONTH));
            mCalendar.set(Calendar.DAY_OF_MONTH, bundle.getInt(KEY_SELECTED_DAY));

            CalendarDay savedMinDate = bundle.getParcelable(KEY_MIN_DATE);
            if (savedMinDate != null) {
                mMinDate = savedMinDate;
            } else {
                mMinDate = MINIMUM_POSSIBLE_DATE;
            }

            CalendarDay savedMaxDate = bundle.getParcelable(KEY_MAX_DATE);
            if (savedMaxDate != null) {
                mMaxDate = savedMaxDate;
            } else {
                mMaxDate = MAXIMUM_POSSIBLE_DATE;
            }

            mVibrate = bundle.getBoolean(KEY_VIBRATE);
            mUsePulseAnimations = bundle.getBoolean(KEY_PULSE_ANIMATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle bundle) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mIsViewInitialized = true;

        View view = layoutInflater.inflate(R.layout.date_picker_dialog, parent, false);

        mMonthAndDayView = (TextView) view.findViewById(R.id.date_picker_month_and_day);
        mMonthAndDayView.setOnClickListener(this);

        mYearView = ((TextView) view.findViewById(R.id.date_picker_year));
        mYearView.setOnClickListener(this);

        int listPosition = -1;
        int currentView = MONTH_AND_DAY_VIEW;
        int listPositionOffset = 0;
        if (bundle != null) {
            mWeekStart = bundle.getInt(KEY_WEEK_START);
            currentView = bundle.getInt(KEY_CURRENT_VIEW);
            listPosition = bundle.getInt(KEY_LIST_POSITION);
            listPositionOffset = bundle.getInt(KEY_LIST_POSITION_OFFSET);
        }

        Activity activity = getActivity();
        mDayPickerView = new DayPickerView(activity, createDayPickerParams());
        mDayPickerView.setListener(this);

        mYearPickerView = new YearPickerView(activity);
        mYearPickerView.setListener(this);

        Resources resources = getResources();
        mDayPickerDescription = resources.getString(R.string.day_picker_description);
        mSelectDay = resources.getString(R.string.select_day);
        mYearPickerDescription = resources.getString(R.string.year_picker_description);
        mSelectYear = resources.getString(R.string.select_year);

        mAnimator = ((AccessibleDateAnimator) view.findViewById(R.id.animator));
        mAnimator.addView(mDayPickerView);
        mAnimator.addView(mYearPickerView);
        mAnimator.setDateMillis(mCalendar.getTimeInMillis());

        AlphaAnimation inAlphaAnimation = new AlphaAnimation(0.0F, 1.0F);
        inAlphaAnimation.setDuration(300L);
        mAnimator.setInAnimation(inAlphaAnimation);

		AlphaAnimation outAlphaAnimation = new AlphaAnimation(1.0F, 0.0F);
		outAlphaAnimation.setDuration(300L);
		mAnimator.setOutAnimation(outAlphaAnimation);

		Button doneButton = ((Button) view.findViewById(R.id.done_button));
        Button cancelButton = ((Button) view.findViewById(R.id.cancel_button));
		doneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onDoneButtonClick();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // Assign the programmatic state list drawable to allow attributes.
        ColorStateList selector = Utils.createThemedTextColorStateList(activity);
        doneButton.setTextColor(selector);
        cancelButton.setTextColor(selector);

        updateTitleContent(false);
        setCurrentPicker(currentView, true);

        if (listPosition != -1) {
            if (currentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView.setCurrentItem(listPosition);
            }
            if (currentView == YEAR_VIEW) {
                mYearPickerView.postSetSelectionFromTop(listPosition, listPositionOffset);
            }
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(KEY_SELECTED_YEAR, mCalendar.get(Calendar.YEAR));
        bundle.putInt(KEY_SELECTED_MONTH, mCalendar.get(Calendar.MONTH));
        bundle.putInt(KEY_SELECTED_DAY, mCalendar.get(Calendar.DAY_OF_MONTH));
        bundle.putInt(KEY_WEEK_START, mWeekStart);
        bundle.putParcelable(KEY_MIN_DATE, mMinDate);
        bundle.putParcelable(KEY_MAX_DATE, mMaxDate);
        bundle.putInt(KEY_CURRENT_VIEW, mCurrentView);

        int listPosition = -1;
        if (mCurrentView == 0) {
            listPosition = mDayPickerView.getCurrentItem();
        }
        if (mCurrentView == 1) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            bundle.putInt(KEY_LIST_POSITION_OFFSET, mYearPickerView.getFirstPositionOffset());
        }
        bundle.putInt(KEY_LIST_POSITION, listPosition);
        bundle.putBoolean(KEY_VIBRATE, mVibrate);
        bundle.putBoolean(KEY_PULSE_ANIMATE, mUsePulseAnimations);
    }

    @Override
    public void onClick(View view) {
        tryVibrate();
        if (view.getId() == R.id.date_picker_year)
            setCurrentPicker(YEAR_VIEW);
        else if (view.getId() == R.id.date_picker_month_and_day)
            setCurrentPicker(MONTH_AND_DAY_VIEW);
    }

    @Override
    public void onYearSelected(int year) {
        tryVibrate();

        // Ensure the day doesn't exceed the month.
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);
        int month = mCalendar.get(Calendar.MONTH);

        int daysInMonth = Utils.getDaysInMonth(month, year);
        if (day > daysInMonth) {
            day = daysInMonth;
        }

        // Check min and max ranges.
        if (year == mMinDate.year) {
            if (month < mMinDate.month) {
                month = mMinDate.month;
            }
            if (month == mMinDate.month && day < mMinDate.day) {
                day = mMinDate.day;
            }
        }
        if (year == mMaxDate.year) {
            if (month > mMaxDate.month) {
                month = mMaxDate.month;
            }
            if (month == mMaxDate.month && day > mMaxDate.day) {
                day = mMaxDate.day;
            }
        }

        // We assume the year is correct since it comes from the year picker.
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.YEAR, year);

        //updatePickers();
        setCurrentPicker(MONTH_AND_DAY_VIEW);
        updateTitleContent(true);
    }

    @Override
    public void onDateSelected(int year, int month, int day) {
        setSelectedDate(year, month, day);
    }

    public void setFirstDayOfWeek(int startOfWeek) {
        if (startOfWeek < Calendar.SUNDAY || startOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and " +
                    "Calendar.SATURDAY");
        }
        mWeekStart = startOfWeek;
        updateDayPickerParams();
    }

    public void setOnDateSetListener(OnDateSetListener onDateSetListener) {
        mCallBack = onDateSetListener;
    }

    public void setSelectedDate(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);

        if (mIsViewInitialized) {
            updateDayPickerParams();
            updateTitleContent(true);

            if (mCloseOnSingleTapDay) {
                onDoneButtonClick();
            }
        }
    }

    /**
     * @deprecated Use {@link #setDateConstaints} instead.
     * <p>
     * Set the minimum allowed date
     * Note : the month index starts from 0, rest all from 1
     * </p>
     */
    public void setMinDate(CalendarDay minDate) {
        setDateConstaints(minDate, this.mMaxDate);
    }

    /**
     * @deprecated Use {@link #setDateConstaints} instead.
     * <p>
     * Set the maximum allowed date
     * Note : the month index starts from 0, rest all from 1
     * </p>
     */
    public void setMaxDate(CalendarDay maxDate) {
        setDateConstaints(this.mMinDate, maxDate);
    }

    /**
     * @deprecated Use setMinDate and setMaxDate instead.
     */
    public void setYearRange(int minYear, int maxYear) {
        setDateConstaints(new CalendarDay(minYear, mMinDate.month, mMinDate.day), new CalendarDay(maxYear, mMaxDate.month, mMaxDate.day));
    }

    public void setDateConstaints(CalendarDay minDate, CalendarDay maxDate) {
        if (minDate.isAfter(maxDate))
            throw new IllegalArgumentException("Max date must be larger than min date");

        if (minDate.year < MINIMUM_POSSIBLE_DATE.year)
            throw new IllegalArgumentException("Min date year end must > " + MINIMUM_POSSIBLE_DATE.year);

        if (maxDate.year > MAXIMUM_POSSIBLE_DATE.year)
            throw new IllegalArgumentException("Max date year end must < " + MAXIMUM_POSSIBLE_DATE.year);

        this.mMinDate = minDate;
        this.mMaxDate = maxDate;

        updateDayPickerParams();
    }

    public void setCloseOnSingleTapDay(boolean closeOnSingleTapDay) {
        mCloseOnSingleTapDay = closeOnSingleTapDay;
    }

    public void setVibrate(boolean vibrate) {
        mVibrate = vibrate;
    }

    public void setPulseAnimationsEnabled(boolean usePulseAnimations) {
        mUsePulseAnimations = usePulseAnimations;
    }

    private void setCurrentPicker(int pickerId) {
        setCurrentPicker(pickerId, false);
    }

    private void setCurrentPicker(int pickerId, boolean forceRefresh) {
        long timeInMillis = mCalendar.getTimeInMillis();

        TextView selectedLabel = null;
        boolean monthAndDayViewSelected = false;
        boolean yearViewSelected = false;
        String contentDescription = null;
        String announcement = null;

        switch (pickerId) {
            case MONTH_AND_DAY_VIEW:
                selectedLabel = mMonthAndDayView;

                updateDayPickerParams();
                monthAndDayViewSelected = true;

                String monthDayDesc = DateUtils.formatDateTime(getActivity(), timeInMillis, DateUtils.FORMAT_SHOW_DATE);
                contentDescription = mDayPickerDescription + ": " + monthDayDesc;
                announcement = mSelectDay;
                break;

            case YEAR_VIEW:
                selectedLabel = mYearView;

                mYearPickerView.updateContent(getSelectedDay().year, mMinDate.year, mMaxDate.year);
                yearViewSelected = true;

                contentDescription = mYearPickerDescription + ": " + YEAR_FORMAT.format(timeInMillis);
                announcement = mSelectYear;
                break;
        }

        if (selectedLabel == null) {
            return;
        }

        // Update the view switcher and set the appropriate selection state.
        if (mCurrentView != pickerId || forceRefresh) {
            mMonthAndDayView.setSelected(monthAndDayViewSelected);
            mYearView.setSelected(yearViewSelected);
            mAnimator.setDisplayedChild(pickerId);
            mCurrentView = pickerId;
        }

        // Shows a pulse animation when the view is clicked, or the dialog opens.
        if (mUsePulseAnimations) {
            ObjectAnimator pulseAnimator = Utils.getPulseAnimator(selectedLabel, 0.9F, 1.05F);
            if (mDelayAnimation) {
                pulseAnimator.setStartDelay(ANIMATION_DELAY);
                mDelayAnimation = false;
            }
            pulseAnimator.start();
        }

        mAnimator.setContentDescription(contentDescription);
        Utils.tryAccessibilityAnnounce(mAnimator, announcement);
    }

    private void updateTitleContent(boolean announce) {
        this.mCalendar.setFirstDayOfWeek(mWeekStart);

        mMonthAndDayView.setText(DAY_MONTH_FORMAT.format(mCalendar.getTime()));
        mYearView.setText(YEAR_FORMAT.format(mCalendar.getTime()));

        // Accessibility.
        long millis = mCalendar.getTimeInMillis();
        mAnimator.setDateMillis(millis);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        String monthAndDayText = DateUtils.formatDateTime(getActivity(), millis, flags);
        mMonthAndDayView.setContentDescription(monthAndDayText);

        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            String fullDateText = DateUtils.formatDateTime(getActivity(), millis, flags);
            Utils.tryAccessibilityAnnounce(mAnimator, fullDateText);
        }
    }

    private CalendarDay getSelectedDay() {
        return new CalendarDay(mCalendar);
    }

    private DayPickerView.DayPickerParams createDayPickerParams() {
        return new DayPickerView.DayPickerParams(
                mMinDate, mMaxDate, new CalendarDay(mCalendar), mWeekStart
        );
    }

    private void updateDayPickerParams() {
        if (mDayPickerView == null) {
            return;
        }

        mDayPickerView.updateParams(createDayPickerParams());
    }

    private void onDoneButtonClick() {
        tryVibrate();
        if (mCallBack != null) {
            mCallBack.onDateSet(this, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        }
        dismiss();
    }

    private void tryVibrate() {
        if (mVibrator != null && mVibrate) {
            long timeInMillis = SystemClock.uptimeMillis();
            if (timeInMillis - mLastVibrate >= 125L) {
                mVibrator.vibrate(5L);
                mLastVibrate = timeInMillis;
            }
        }
    }

    public interface OnDateSetListener {
        void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day);
    }
}