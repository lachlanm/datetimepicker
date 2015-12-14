package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.widget.AbsListView;

import com.fourmob.datetimepicker.R;
import com.fourmob.datetimepicker.Utils;

public class DayPickerView extends ViewPager implements SimpleMonthPagerAdapter.MonthPagerListener {

    private SimpleMonthPagerAdapter mPagerAdapter;
    private DayPickerListener mListener;

    public DayPickerView(Context context, DayPickerParams params) {
        super(context);
        setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        init(params);

        setPageMargin(context.getResources().getDimensionPixelSize(R.dimen.day_picker_pager_margin));
    }

    private void init(DayPickerParams params) {
        mPagerAdapter = new SimpleMonthPagerAdapter(params);
        mPagerAdapter.setListener(this);

        setAdapter(mPagerAdapter);
        updatePosition(params);
    }

    public void updateParams(DayPickerParams params) {
        mPagerAdapter.updateParams(params, true);
        updatePosition(params);
    }

    private void updatePosition(DayPickerParams params) {
        int position = Utils.getMonthsBetweenDates(params.minDate.month, params.minDate.year, params.selectedDate.month, params.selectedDate.year) - 1;
        setCurrentItem(position);
    }

    @Override
    public void onDateSelected(int year, int month, int day) {
        if (mListener != null) {
            mListener.onDateSelected(year, month, day);
        }
    }

    public void setListener(DayPickerListener listener) {
        mListener = listener;
    }

    public interface DayPickerListener {
        void onDateSelected(int year, int month, int day);
    }

    public static class DayPickerParams {
        public final int firstDayOfWeek;

        public final CalendarDay selectedDate;
        public final CalendarDay minDate;
        public final CalendarDay maxDate;

        public DayPickerParams(CalendarDay minDate, CalendarDay maxDate, CalendarDay selectedDate, int firstDayOfWeek) {
            this.selectedDate = selectedDate;
            this.firstDayOfWeek = firstDayOfWeek;
            this.minDate = minDate;
            this.maxDate = maxDate;
        }
    }
}