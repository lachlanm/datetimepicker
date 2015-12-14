package com.fourmob.datetimepicker.date;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
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
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class DatePickerDialog extends DialogFragment implements View.OnClickListener, DatePickerController {

    private static final String KEY_SELECTED_YEAR = "year";
    private static final String KEY_SELECTED_MONTH = "month";
    private static final String KEY_SELECTED_DAY = "day";
    private static final String KEY_VIBRATE = "vibrate";

    // https://code.google.com/p/android/issues/detail?id=13050
    private static final int MAX_YEAR = 2037;
    private static final int MIN_YEAR = 1902;

    private static final int MONTHS_IN_YEAR = 12;

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
    private HashSet<OnDateChangedListener> mListeners = new HashSet<OnDateChangedListener>();
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
    private CalendarDay minDate;
    private boolean viewInitialized = false;
    private CalendarDay maxDate;

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

    /**
     * Set the minimum allowed date
     * Note : the month index starts from 0, rest all from 1
     */
    public void setMinDate(CalendarDay minDate) {
        if (mDayPickerView != null) {
            mDayPickerView.setMinDate(minDate);
        }
        this.minDate = minDate;
    }

    /**
     * Set the maximum allowed date
     * Note : the month index starts from 0, rest all from 1
     */
    public void setMaxDate(CalendarDay maxDate) {
        if (mDayPickerView != null) {
            mDayPickerView.setMaxDate(maxDate);
        }
        this.maxDate = maxDate;
    }

    public void setVibrate(boolean vibrate) {
        mVibrate = vibrate;
    }

    public void setPulseAnimationsEnabled(boolean usePulseAnimations) {
        mUsePulseAnimations = usePulseAnimations;
    }

    private void setCurrentView(int currentView) {
        setCurrentView(currentView, false);
    }

    private void setCurrentView(int currentView, boolean forceRefresh) {
        long timeInMillis = mCalendar.getTimeInMillis();

        TextView selectedLabel = null;
        boolean monthAndDayViewSelected = false;
        boolean yearViewSelected = false;
        String contentDescription = null;
        String announcement = null;

        switch (currentView) {
            case MONTH_AND_DAY_VIEW:
                selectedLabel = mMonthAndDayView;

                mDayPickerView.onDateChanged();
                monthAndDayViewSelected = true;

                String monthDayDesc = DateUtils.formatDateTime(getActivity(), timeInMillis, DateUtils.FORMAT_SHOW_DATE);
                contentDescription = mDayPickerDescription + ": " + monthDayDesc;
                announcement = mSelectDay;
                break;

            case YEAR_VIEW:
                selectedLabel = mYearView;

                mYearPickerView.onDateChanged();
                yearViewSelected = true;

                contentDescription = mYearPickerDescription + ": " + YEAR_FORMAT.format(timeInMillis);
                announcement = mSelectYear;
                break;
        }

        if (selectedLabel == null) {
            return;
        }

        // Update the view switcher and set the appropriate selection state.
        if (mCurrentView != currentView || forceRefresh) {
            mMonthAndDayView.setSelected(monthAndDayViewSelected);
            mYearView.setSelected(yearViewSelected);
            mAnimator.setDisplayedChild(currentView);
            mCurrentView = currentView;
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

    private void updateDisplay(boolean announce) {
        /*if (mDayOfWeekView != null) {
            mDayOfWeekView.setText(mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                    Locale.getDefault()).toUpperCase(Locale.getDefault()));
        }

        mSelectedMonthTextView.setText(mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                Locale.getDefault()).toUpperCase(Locale.getDefault()));*/

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

    private void updatePickers() {
        Iterator<OnDateChangedListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDateChanged();
        }
    }

    public int getFirstDayOfWeek() {
        return mWeekStart;
    }

    public int getMaxYear() {
        if (maxDate == null) {
            return MAX_YEAR;
        }
        return maxDate.year;
    }

    public int getMinYear() {
        if (minDate == null) {
            return MIN_YEAR;
        }
        return minDate.year;
    }

    @Override
    public int getFirstMonth() {
        if (minDate == null) {
            return Calendar.JANUARY;
        }
        return minDate.month;
    }

    @Override
    public int getLastMonth() {
        if (maxDate == null) {
            return Calendar.DECEMBER;
        }
        return maxDate.month;
    }

    public int getMonthCount() {
        return (getMaxYear() - getMinYear()) * MONTHS_IN_YEAR + getLastMonth() - getFirstMonth() + 1;
    }

    public CalendarDay getSelectedDay() {
        return new CalendarDay(mCalendar);
    }

    public void initialize(OnDateSetListener onDateSetListener, int year, int month, int day, boolean vibrate) {
        if (year > MAX_YEAR)
            throw new IllegalArgumentException("year end must < " + MAX_YEAR);
        if (year < MIN_YEAR)
            throw new IllegalArgumentException("year end must > " + MIN_YEAR);
        mCallBack = onDateSetListener;
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        mVibrate = vibrate;
    }

    public void onClick(View view) {
        tryVibrate();
        if (view.getId() == R.id.date_picker_year)
            setCurrentView(YEAR_VIEW);
        else if (view.getId() == R.id.date_picker_month_and_day)
            setCurrentView(MONTH_AND_DAY_VIEW);
    }

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Activity activity = getActivity();
		activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		mVibrator = ((Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE));
		if (bundle != null) {
			mCalendar.set(Calendar.YEAR, bundle.getInt(KEY_SELECTED_YEAR));
			mCalendar.set(Calendar.MONTH, bundle.getInt(KEY_SELECTED_MONTH));
			mCalendar.set(Calendar.DAY_OF_MONTH, bundle.getInt(KEY_SELECTED_DAY));

			minDate = bundle.getParcelable(KEY_MIN_DATE);
			maxDate = bundle.getParcelable(KEY_MAX_DATE);

			mVibrate = bundle.getBoolean(KEY_VIBRATE);
		}
	}

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle bundle) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.viewInitialized = true;
        View view = layoutInflater.inflate(R.layout.date_picker_dialog, null);

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
        mDayPickerView = new DayPickerView(activity, this);
        if (this.minDate != null) mDayPickerView.setMinDate(this.minDate);
        if (this.maxDate != null) mDayPickerView.setMaxDate(this.maxDate);
        mYearPickerView = new YearPickerView(activity, this);

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

        updateDisplay(false);
        setCurrentView(currentView, true);

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

    private void onDoneButtonClick() {
        tryVibrate();
        if (mCallBack != null) {
            mCallBack.onDateSet(this, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        }
        dismiss();
    }

    public void onDayOfMonthSelected(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        if(viewInitialized){
            updatePickers();
            updateDisplay(true);

            if (mCloseOnSingleTapDay) {
                onDoneButtonClick();
            }
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(KEY_SELECTED_YEAR, mCalendar.get(Calendar.YEAR));
        bundle.putInt(KEY_SELECTED_MONTH, mCalendar.get(Calendar.MONTH));
        bundle.putInt(KEY_SELECTED_DAY, mCalendar.get(Calendar.DAY_OF_MONTH));
        bundle.putInt(KEY_WEEK_START, mWeekStart);
        bundle.putParcelable(KEY_MIN_DATE, minDate);
        bundle.putParcelable(KEY_MAX_DATE, maxDate);
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
    }

    public void onYearSelected(int year) {
        // Ensure the day doesn't exceed the month.
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);
        int month = mCalendar.get(Calendar.MONTH);

        int daysInMonth = Utils.getDaysInMonth(month, year);
        if (day > daysInMonth) {
            day = daysInMonth;
        }

        // Check min and max ranges.
        if (minDate != null) {
            if (year == minDate.year) {
                if (month < minDate.month) {
                    month = minDate.month;
                }
                if (month == minDate.month && day < minDate.day) {
                    day = minDate.day;
                }
            }
        }
        if (maxDate != null) {
            if (year == maxDate.year) {
                if (month > maxDate.month) {
                    month = maxDate.month;
                }
                if (month == maxDate.month && day > maxDate.day) {
                    day = maxDate.day;
                }
            }
        }

        // We assume the year is correct since it comes from the year picker.
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.YEAR, year);

        //updatePickers();
        setCurrentView(MONTH_AND_DAY_VIEW);
        updateDisplay(true);
    }

    public void registerOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        mListeners.add(onDateChangedListener);
    }

    public void setFirstDayOfWeek(int startOfWeek) {
        if (startOfWeek < Calendar.SUNDAY || startOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and " +
                    "Calendar.SATURDAY");
        }
        mWeekStart = startOfWeek;
        if (mDayPickerView != null) {
            mDayPickerView.onChange();
        }
    }

    public void setOnDateSetListener(OnDateSetListener onDateSetListener) {
        mCallBack = onDateSetListener;
    }

    /**
     * @deprecated Use setMinDate and setMaxDate instead.
     */
    public void setYearRange(int minYear, int maxYear) {
        if (maxYear < minYear)
            throw new IllegalArgumentException("Year end must be larger than year start");
        if (maxYear > MAX_YEAR)
            throw new IllegalArgumentException("max year end must < " + MAX_YEAR);
        if (minYear < MIN_YEAR)
            throw new IllegalArgumentException("min year end must > " + MIN_YEAR);

        if (minDate == null) {
            // Set the max date to the start of January.
            minDate = new CalendarDay(minYear, 1, 1);
        } else {
            minDate.year = minYear;
        }

        if (maxDate == null) {
            // Set the max date to the end of december.
            maxDate = new CalendarDay(minYear, 12, 31);
        } else {
            maxDate.year = maxYear;
        }

        if (mDayPickerView != null)
            mDayPickerView.onChange();
    }

    public void tryVibrate() {
        if (mVibrator != null && mVibrate) {
            long timeInMillis = SystemClock.uptimeMillis();
            if (timeInMillis - mLastVibrate >= 125L) {
                mVibrator.vibrate(5L);
                mLastVibrate = timeInMillis;
            }
        }
    }

    public void setCloseOnSingleTapDay(boolean closeOnSingleTapDay) {
        mCloseOnSingleTapDay = closeOnSingleTapDay;
    }

    public void setSelectedDay(GregorianCalendar date) {
        //mCalendar.set(date.get(GregorianCalendar.YEAR), date.get(GregorianCalendar.MONTH), date.get(GregorianCalendar.DAY_OF_MONTH));
        onDayOfMonthSelected(date.get(GregorianCalendar.YEAR), date.get(GregorianCalendar.MONTH), date.get(GregorianCalendar.DAY_OF_MONTH));
    }


    static abstract interface OnDateChangedListener {
        public abstract void onDateChanged();
    }

    public static abstract interface OnDateSetListener {
        public abstract void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day);
    }
}