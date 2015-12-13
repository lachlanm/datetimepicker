package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;

import com.fourmob.datetimepicker.R;
import com.fourmob.datetimepicker.Utils;

import java.security.InvalidParameterException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SimpleMonthView extends View {

    private static final String VIEW_PARAMS_HEIGHT = "height";
    public static final String VIEW_PARAMS_MONTH = "month";
    public static final String VIEW_PARAMS_YEAR = "year";
    public static final String VIEW_PARAMS_SELECTED_DAY = "selected_day";
    public static final String VIEW_PARAMS_WEEK_START = "week_start";
    public static final String VIEW_PARAMS_MIN_DATE_DAY = "minDateDay";
    public static final String VIEW_PARAMS_MAX_DATE_DAY = "maxDateDay";

    public static final String VIEW_PARAMS_MIN_DATE_MONTH = "minDateMonth";
    public static final String VIEW_PARAMS_MIN_DATE_YEAR ="minDateYear";
    public static final String VIEW_PARAMS_MAX_DATE_MONTH = "maxDateMonth";
    public static final String VIEW_PARAMS_MAX_DATE_YEAR ="maxDateYear";

    protected static final int DEFAULT_HEIGHT = 32;
    protected static final int DEFAULT_NUM_ROWS = 6;
    protected final static int DAY_SEPARATOR_WIDTH = 1;
    protected final static int MIN_HEIGHT = 10;

	private int daySelectedCircleSize;
	private int miniDayNumberTextSize;
	private int monthDayLabelTextSize;
	private int monthHeaderSize;
	private int monthLabelTextSize;

    private final int mDayDisabledTextColor;
    private int mPadding = 0;

    private String mDayOfWeekTypeface;
    private String mMonthTitleTypeface;

    private Paint mMonthDayLabelPaint;
    private Paint mMonthNumPaint;
    private Paint mMonthTitlePaint;
    private Paint mSelectedCirclePaint;
    private Paint mHoveredCirclePaint;

    private int mDayTextColor;
    private int mSelectedTextColor;

    private int mTodayNumberColor;

    private boolean mHasToday = false;
    private int mSelectedDay = -1;
    private int mHoveredDay = -1;
    private int mToday = -1;
    private int mWeekStart = 1;
    private int mNumDays = 7;
    private int mNumCells = mNumDays;
    private int mDayOfWeekStart = 0;
    private int mMonth;
    private int mRowHeight = DEFAULT_HEIGHT;
    private int mWidth;
    private int mYear;

	private final Calendar mCalendar;
	private final Calendar mDayLabelCalendar;

    private int mNumRows = DEFAULT_NUM_ROWS;

	private DateFormatSymbols mDateFormatSymbols = new DateFormatSymbols();

    private OnDayClickListener mOnDayClickListener;
    private CalendarDay mMinDate;
    private CalendarDay mMaxDate;

    public SimpleMonthView(Context context) {
		super(context);
		Resources resources = context.getResources();
		mDayLabelCalendar = Calendar.getInstance();
		mCalendar = Calendar.getInstance();

		mDayOfWeekTypeface = resources.getString(R.string.day_of_week_label_typeface);
		mMonthTitleTypeface = resources.getString(R.string.sans_serif);

		mDayTextColor = resources.getColor(R.color.date_picker_text_normal);
        mSelectedTextColor = resources.getColor(R.color.date_picker_text_selected);
        mDayDisabledTextColor = resources.getColor(R.color.done_text_color_disabled);
		mTodayNumberColor = Utils.getPrimaryColor(context);

		miniDayNumberTextSize = resources.getDimensionPixelSize(R.dimen.day_number_size);
		monthLabelTextSize = resources.getDimensionPixelSize(R.dimen.month_label_size);
		monthDayLabelTextSize = resources.getDimensionPixelSize(R.dimen.month_day_label_text_size);
		monthHeaderSize = resources.getDimensionPixelOffset(R.dimen.month_list_item_header_height);
		daySelectedCircleSize = resources.getDimensionPixelSize(R.dimen.day_number_select_circle_radius);

		mRowHeight = ((resources.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height) - monthHeaderSize) / DEFAULT_NUM_ROWS);

        initView();
	}

	private int calculateNumRows() {
        int offset = findDayOffset();
        int dividend = (offset + mNumCells) / mNumDays;
        int remainder = (offset + mNumCells) % mNumDays;
        return (dividend + (remainder > 0 ? 1 : 0));
	}

	private void drawMonthDayLabels(Canvas canvas) {
        int y = monthHeaderSize - (monthDayLabelTextSize / 2);
        int dayWidthHalf = (mWidth - mPadding * 2) / (mNumDays * 2);

        for (int i = 0; i < mNumDays; i++) {
            int calendarDay = (i + mWeekStart) % mNumDays;
            int x = (2 * i + 1) * dayWidthHalf + mPadding;
            mDayLabelCalendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            canvas.drawText(mDateFormatSymbols.getShortWeekdays()[mDayLabelCalendar
                    .get(Calendar.DAY_OF_WEEK)].toUpperCase(Locale.getDefault()).substring(0, 1), x, y, mMonthDayLabelPaint);
        }
	}

	private void drawMonthTitle(Canvas canvas) {
        int x = (mWidth + 2 * mPadding) / 2;
        int y = (monthHeaderSize - monthDayLabelTextSize) / 2 + (monthLabelTextSize / 3);
        canvas.drawText(getMonthAndYearString(), x, y, mMonthTitlePaint);
	}

	private int findDayOffset() {
        return (mDayOfWeekStart < mWeekStart ? (mDayOfWeekStart + mNumDays) : mDayOfWeekStart)
                - mWeekStart;
	}

	private String getMonthAndYearString() {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        long millis = mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), millis, millis, flags);
    }

	private void onDayClick(CalendarDay calendarDay) {
		if (mOnDayClickListener != null) {
			mOnDayClickListener.onDayClick(this, calendarDay);
        }
	}

	private boolean sameDay(int monthDay, Time time) {
		return (mYear == time.year) && (mMonth == time.month) && (monthDay == time.monthDay);
	}

	protected void drawMonthNums(Canvas canvas) {
		int y = (mRowHeight + miniDayNumberTextSize) / 2 - DAY_SEPARATOR_WIDTH + monthHeaderSize;
		int paddingDay = (mWidth - 2 * mPadding) / (2 * mNumDays);
		int dayOffset = findDayOffset();
		int day = 1;
        boolean hasDisabledDays = false;
        if (mMinDate != null && mMonth <= mMinDate.month && mYear <= mMinDate.year) {
            hasDisabledDays = true;
        }
        if (mMaxDate != null && mMonth >= mMaxDate.month && mYear >= mMaxDate.year) {
            hasDisabledDays = true;
        }

        while (day <= mNumCells) {
            boolean isDisabledDay = false;
			int x = paddingDay * (1 + dayOffset * 2) + mPadding;

            if (hasDisabledDays) {
                isDisabledDay = isDisabledDay(day);
            }

            int textColor = mDayTextColor;
            if (!isDisabledDay) {
                if (mHasToday && (mToday == day)) {
                    textColor = mTodayNumberColor;
                }

                // Only show circles if this is not a disabled day.
                if (mSelectedDay == day) {
                    textColor = mSelectedTextColor;
                    canvas.drawCircle(x, y - miniDayNumberTextSize / 3, daySelectedCircleSize, mSelectedCirclePaint);

                } else if (mHoveredDay == day) {
                    canvas.drawCircle(x, y - miniDayNumberTextSize / 3, daySelectedCircleSize, mHoveredCirclePaint);
                }
            } else {
                textColor = mDayDisabledTextColor;
            }

            mMonthNumPaint.setColor(textColor);
			canvas.drawText(String.format("%d", day), x, y, mMonthNumPaint);

			dayOffset++;
			if (dayOffset == mNumDays) {
				dayOffset = 0;
				y += mRowHeight;
			}
			day++;
		}
	}

	public @Nullable CalendarDay getDayFromLocation(float x, float y) {
		int padding = mPadding;
		if ((x < padding) || (x > mWidth - mPadding)) {
			return null;
		}

		int yDay = (int) (y - monthHeaderSize) / mRowHeight;
		int day = 1 + ((int) ((x - padding) * mNumDays / (mWidth - padding - mPadding)) - findDayOffset()) + yDay * mNumDays;

        if (day <= 0 || day > mNumCells) {
            // Since this isn't part of the month being displayed, return an invalid result.
            return null;
        }

		return new CalendarDay(mYear, mMonth, day);
	}

	protected void initView() {
        mMonthTitlePaint = new Paint();
        mMonthTitlePaint.setFakeBoldText(true);
        mMonthTitlePaint.setAntiAlias(true);
        mMonthTitlePaint.setTextSize(monthLabelTextSize);
        mMonthTitlePaint.setTypeface(Typeface.create(mMonthTitleTypeface, Typeface.BOLD));
        mMonthTitlePaint.setColor(mDayTextColor);
        mMonthTitlePaint.setTextAlign(Align.CENTER);
        mMonthTitlePaint.setStyle(Style.FILL);

        mSelectedCirclePaint = new Paint();
        mSelectedCirclePaint.setFakeBoldText(true);
        mSelectedCirclePaint.setAntiAlias(true);
        mSelectedCirclePaint.setColor(mTodayNumberColor);
        mSelectedCirclePaint.setTextAlign(Align.CENTER);
        mSelectedCirclePaint.setStyle(Style.FILL);

        mHoveredCirclePaint = new Paint();
        mHoveredCirclePaint.setFakeBoldText(true);
        mHoveredCirclePaint.setAntiAlias(true);
        mHoveredCirclePaint.setColor(mDayDisabledTextColor);
        mHoveredCirclePaint.setTextAlign(Align.CENTER);
        mHoveredCirclePaint.setStyle(Style.FILL);

        mMonthDayLabelPaint = new Paint();
        mMonthDayLabelPaint.setAntiAlias(true);
        mMonthDayLabelPaint.setTextSize(monthDayLabelTextSize);
        mMonthDayLabelPaint.setColor(mDayTextColor);
        mMonthDayLabelPaint.setTypeface(Typeface.create(mDayOfWeekTypeface, Typeface.NORMAL));
        mMonthDayLabelPaint.setStyle(Style.FILL);
        mMonthDayLabelPaint.setTextAlign(Align.CENTER);

        mMonthNumPaint = new Paint();
        mMonthNumPaint.setAntiAlias(true);
        mMonthNumPaint.setTextSize(miniDayNumberTextSize);
        mMonthNumPaint.setStyle(Style.FILL);
        mMonthNumPaint.setTextAlign(Align.CENTER);
	}

	protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
		drawMonthDayLabels(canvas);
		drawMonthNums(canvas);
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows + monthHeaderSize + getResources().getDimensionPixelOffset(R.dimen.month_view_bottom_pad));
    }

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
	}

    private boolean isValidCalendarDay(CalendarDay calendarDay) {
        return calendarDay != null && !isDisabledDay(calendarDay.day);
    }

    private boolean isDisabledDay(int day) {
        return (mMinDate != null && mMinDate.isAfter(new CalendarDay(mYear, mMonth, day))) ||
                (mMaxDate != null && mMaxDate.isBefore(new CalendarDay(mYear, mMonth, day)));
    }

	public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                CalendarDay selectedCalendarDay = getDayFromLocation(event.getX(), event.getY());
                if (isValidCalendarDay(selectedCalendarDay)) {

                    // Only invalidate if the value has changed.
                    if (selectedCalendarDay.day != mSelectedDay) {
                        onDayClick(selectedCalendarDay);

                        mSelectedDay = selectedCalendarDay.day;
                        invalidate();
                    }

                    return true;
                }
                break;

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                CalendarDay hoveredCalendarDay = getDayFromLocation(event.getX(), event.getY());
                if (isValidCalendarDay(hoveredCalendarDay)) {

                    // Only invalidate if the value has changed.
                    if (hoveredCalendarDay.day != mHoveredDay) {
                        mHoveredDay = hoveredCalendarDay.day;
                        invalidate();
                    }

                    return true;
                }
                break;

            default:
                break;
        }

        // We need to remove the hover effect.
        if (mHoveredDay != -1) {
            mHoveredDay = -1;
            invalidate();
        }

        return true;
    }

	public void reuse() {
        mNumRows = DEFAULT_NUM_ROWS;
		requestLayout();
	}

	public void setMonthParams(HashMap<String, Integer> params) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw new InvalidParameterException("You must specify month and year for this view");
        }
		setTag(params);

        if (params.containsKey(VIEW_PARAMS_MIN_DATE_DAY) && params.containsKey(VIEW_PARAMS_MIN_DATE_MONTH) && params.containsKey(VIEW_PARAMS_MIN_DATE_YEAR)) {
            this.mMinDate = new CalendarDay(params.get(VIEW_PARAMS_MIN_DATE_YEAR), params.get(VIEW_PARAMS_MIN_DATE_MONTH), params.get(VIEW_PARAMS_MIN_DATE_DAY));
        }

        if (params.containsKey(VIEW_PARAMS_MAX_DATE_DAY) && params.containsKey(VIEW_PARAMS_MAX_DATE_MONTH) && params.containsKey(VIEW_PARAMS_MAX_DATE_YEAR)) {
            this.mMaxDate = new CalendarDay(params.get(VIEW_PARAMS_MAX_DATE_YEAR), params.get(VIEW_PARAMS_MAX_DATE_MONTH), params.get(VIEW_PARAMS_MAX_DATE_DAY));
        }

        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mRowHeight = params.get(VIEW_PARAMS_HEIGHT);
            if (mRowHeight < MIN_HEIGHT) {
                mRowHeight = MIN_HEIGHT;
            }
        }

        mSelectedDay = -1;
        if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
            mSelectedDay = params.get(VIEW_PARAMS_SELECTED_DAY);
        }

        mMonth = params.get(VIEW_PARAMS_MONTH);
        mYear = params.get(VIEW_PARAMS_YEAR);

        final Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        mHasToday = false;
        mToday = -1;

		mCalendar.set(Calendar.MONTH, mMonth);
		mCalendar.set(Calendar.YEAR, mYear);
		mCalendar.set(Calendar.DAY_OF_MONTH, 1);
		mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = params.get(VIEW_PARAMS_WEEK_START);
        } else {
            mWeekStart = mCalendar.getFirstDayOfWeek();
        }

        mNumCells = Utils.getDaysInMonth(mMonth, mYear);
        for (int i = 0; i < mNumCells; i++) {
            final int day = i + 1;
            if (sameDay(day, today)) {
                mHasToday = true;
                mToday = day;
            }
        }

        mNumRows = calculateNumRows();
	}

	public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
		mOnDayClickListener = onDayClickListener;
	}

    public int getMonth() {
        return mMonth;
    }

    public int getYear() {
        return mYear;
    }

    public void clearSelection() {
        mSelectedDay = -1;
        invalidate();
    }

    public static abstract interface OnDayClickListener {
		public abstract void onDayClick(SimpleMonthView simpleMonthView, CalendarDay calendarDay);
	}
}