package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Stack;

public class SimpleMonthPagerAdapter extends PagerAdapter implements SimpleMonthView.OnDayClickListener {

    private static final String TAG = "SimpleMonthAdapter";
    protected static final int MONTHS_IN_YEAR = 12;

    private final DatePickerController mController;

    private CalendarDay mSelectedDay;
    private CalendarDay mMinDate;
    private CalendarDay mMaxDate = null;

    private HashMap<String, SimpleMonthView> mCurrentViews;
    private Stack<SimpleMonthView> mRecycledViewsList;

    public SimpleMonthPagerAdapter(DatePickerController datePickerController) {
        mCurrentViews = new HashMap<>();
        mRecycledViewsList = new Stack<SimpleMonthView>();

        mController = datePickerController;
        init();
        setSelectedDay(mController.getSelectedDay());
    }

    private boolean isSelectedDayInMonth(int year, int month) {
        return (mSelectedDay.year == year) && (mSelectedDay.month == month);
    }

    public int getCount() {
        return mController.getMonthCount();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private SimpleMonthView createOrRecycleMonthView(Context context) {
        SimpleMonthView monthView;
        if (mRecycledViewsList.isEmpty()) {
            monthView = new SimpleMonthView(context);
            monthView.setOnDayClickListener(this);
        } else {
            monthView = mRecycledViewsList.pop();
        }

        return monthView;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        SimpleMonthView monthView = createOrRecycleMonthView(container.getContext());

        final int month = (position + mController.getFirstMonth()) % MONTHS_IN_YEAR;
        final int year = (position + mController.getFirstMonth()) / MONTHS_IN_YEAR + mController.getMinYear();

        int selectedDay = -1;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = mSelectedDay.day;
        }

        monthView.reuse();

        HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_YEAR, year);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MONTH, month);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_WEEK_START, mController.getFirstDayOfWeek());
        if (mMinDate != null) {
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_DAY, mMinDate.day);
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_MONTH, mMinDate.month);
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_YEAR, mMinDate.year);
        }
        if (mMaxDate != null) {
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_DAY, mMaxDate.day);
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_MONTH, mMaxDate.month);
            drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_YEAR, mMaxDate.year);
        }
        monthView.setMonthParams(drawingParams);
        monthView.invalidate();

        container.addView(monthView);

        mCurrentViews.put(createViewKey(month, year), monthView);

        return monthView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        SimpleMonthView recycledView = (SimpleMonthView) object;
        container.removeView(recycledView);

        mCurrentViews.remove(createViewKey(recycledView.getMonth(), recycledView.getYear()));
        mRecycledViewsList.push(recycledView);
    }

    private String createViewKey(int month, int year) {
        return month + "." + year;
    }

    @Override
    public int getItemPosition(Object object) {
        // Enables notifyDataSetChanged to function correctly.
        return POSITION_NONE;
    }

    protected void init() {
        mSelectedDay = new CalendarDay(System.currentTimeMillis());
    }

    public void onDayClick(SimpleMonthView simpleMonthView, CalendarDay calendarDay) {
        if (calendarDay != null) {
            if ((this.mMinDate == null || calendarDay.isAfter(this.mMinDate) || calendarDay.equals(this.mMinDate)) &&
                    (this.mMaxDate == null || calendarDay.isBefore(this.mMaxDate) || calendarDay.equals(this.mMaxDate))) {
                onDayTapped(calendarDay);
            } else {
                Log.i(TAG, "ignoring push since day is after minDate or before maxDate");
            }
        }
    }

    protected void onDayTapped(CalendarDay calendarDay) {
        mController.tryVibrate();
        mController.onDayOfMonthSelected(calendarDay.year, calendarDay.month, calendarDay.day);
        setSelectedDay(calendarDay);
    }

    public void setSelectedDay(CalendarDay calendarDay) {
        mSelectedDay = calendarDay;

        // Invalidate all the other month views.
        for (String key : mCurrentViews.keySet()) {
            if (!key.equals(createViewKey(calendarDay.month, calendarDay.year))) {
                mCurrentViews.get(key).clearSelection();
            }
        }
    }

    /**
     * sets the min date. All previous days are disabled.
     *
     * @param mMinDate
     */
    public void setMinDate(CalendarDay mMinDate, boolean invalidate) {
        boolean changeOccurred = (mMinDate != this.mMinDate);
        this.mMinDate = mMinDate;

        // This is a fundamental change, the entire view pager may need to change.
        if (changeOccurred && invalidate) {
            notifyDataSetChanged();
        }
    }

    public void setMaxDate(CalendarDay mMaxDate, boolean invalidate) {
        boolean changeOccurred = (mMaxDate != this.mMaxDate);
        this.mMaxDate = mMaxDate;

        // This is a fundamental change, the entire view pager may need to change.
        if (changeOccurred && invalidate) {
            notifyDataSetChanged();
        }
    }

}