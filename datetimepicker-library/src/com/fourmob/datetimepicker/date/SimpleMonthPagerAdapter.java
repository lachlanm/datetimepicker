package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.fourmob.datetimepicker.Utils;

import java.util.HashMap;
import java.util.Stack;

public class SimpleMonthPagerAdapter extends PagerAdapter implements SimpleMonthView.OnDayClickListener {
    private static final String TAG = "SimpleMonthAdapter";

    private CalendarDay mSelectedDay;
    private int mTotalMonths;

    private HashMap<String, SimpleMonthView> mCurrentViews;
    private Stack<SimpleMonthView> mRecycledViewsList;

    private DayPickerView.DayPickerParams mParams;
    private MonthPagerListener mListener;

    public SimpleMonthPagerAdapter(DayPickerView.DayPickerParams params) {
        updateParams(params, false);

        mCurrentViews = new HashMap<>();
        mRecycledViewsList = new Stack<SimpleMonthView>();
    }

    private boolean isDayInMonth(CalendarDay date, int year, int month) {
        return (date.year == year) && (date.month == month);
    }

    public int getCount() {
        return mTotalMonths;
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

    public void updateParams(DayPickerView.DayPickerParams params, boolean invalidate) {
        mParams = params;
        mSelectedDay = mParams.selectedDate;

        mTotalMonths = Utils.getMonthsBetweenDates(mParams.minDate.month, mParams.minDate.year, mParams.maxDate.month, mParams.maxDate.year);

        if (invalidate) {
            notifyDataSetChanged();
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        SimpleMonthView monthView = createOrRecycleMonthView(container.getContext());

        int firstMonth = mParams.minDate.month;
        final int month = (position + firstMonth) % Utils.MONTHS_IN_YEAR;
        final int year = (position + firstMonth) / Utils.MONTHS_IN_YEAR + mParams.minDate.year;

        int selectedDay = -1;
        if (isDayInMonth(mSelectedDay, year, month)) {
            selectedDay = mSelectedDay.day;
        }

        monthView.reuse();

        updateMonthViewParams(monthView, selectedDay, month, year);

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

    private void updateMonthViewParams(SimpleMonthView monthView, int selectedDay, int month, int year) {
        HashMap<String, Integer> drawingParams = new HashMap<String, Integer>();
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_SELECTED_DAY, selectedDay);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_YEAR, year);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MONTH, month);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_WEEK_START, mParams.firstDayOfWeek);

        CalendarDay minDate = mParams.minDate;
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_DAY, minDate.day);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_MONTH, minDate.month);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MIN_DATE_YEAR, minDate.year);

        CalendarDay maxDate = mParams.maxDate;
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_DAY, maxDate.day);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_MONTH, maxDate.month);
        drawingParams.put(SimpleMonthView.VIEW_PARAMS_MAX_DATE_YEAR, maxDate.year);

        monthView.setMonthParams(drawingParams);
        monthView.invalidate();
    }

    private String createViewKey(int month, int year) {
        return month + "." + year;
    }

    @Override
    public int getItemPosition(Object object) {
        // Enables notifyDataSetChanged to function correctly.
        return POSITION_NONE;
    }

    @Override
    public void onDayClick(SimpleMonthView simpleMonthView, CalendarDay calendarDay) {
        if (calendarDay != null) {
            CalendarDay minDate = mParams.minDate;
            CalendarDay maxDate = mParams.maxDate;

            if ((calendarDay.isAfter(minDate) || calendarDay.equals(minDate)) &&
                    (calendarDay.isBefore(maxDate) || calendarDay.equals(maxDate))) {
                onDayTapped(calendarDay);
            } else {
                Log.i(TAG, "ignoring push since day is after minDate or before maxDate");
            }
        }
    }

    protected void onDayTapped(CalendarDay calendarDay) {
        setSelectedDayInternal(calendarDay);
        if (mListener != null) {
            mListener.onDateSelected(calendarDay.year, calendarDay.month, calendarDay.day);
        }
    }

    private void setSelectedDayInternal(CalendarDay calendarDay) {
        mSelectedDay = calendarDay;

        // Invalidate all the other month views.
        for (String key : mCurrentViews.keySet()) {
            if (!key.equals(createViewKey(calendarDay.month, calendarDay.year))) {
                mCurrentViews.get(key).clearSelection();
            }
        }
    }

    public void setListener(MonthPagerListener listener) {
        mListener = listener;
    }

    public interface MonthPagerListener {
        void onDateSelected(int year, int month, int day);
    }

}