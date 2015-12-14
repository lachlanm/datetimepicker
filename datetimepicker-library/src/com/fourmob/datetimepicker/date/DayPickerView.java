package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.widget.AbsListView;

import com.fourmob.datetimepicker.R;

public class DayPickerView extends ViewPager implements DatePickerDialog.OnDateChangedListener {

    private SimpleMonthPagerAdapter mPagerAdapter;
    private final DatePickerController mController;

    protected CalendarDay mSelectedDay = new CalendarDay();

    public DayPickerView(Context context, DatePickerController datePickerController) {
        super(context);
        mController = datePickerController;
        mController.registerOnDateChangedListener(this);
        setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        init();
        onDateChanged();

        setPageMargin(context.getResources().getDimensionPixelSize(R.dimen.day_picker_pager_margin));
    }

    public void init() {
        setupAdapter(null);
        setAdapter(mPagerAdapter);
    }

    public void onChange() {
        setupAdapter(null);
        setAdapter(mPagerAdapter);
    }

    public void onDateChanged() {
        if (!mController.getSelectedDay().equals(mSelectedDay)) {
            mSelectedDay.set(mController.getSelectedDay());
            mPagerAdapter.setSelectedDay(mSelectedDay);
        }

        int position = (mSelectedDay.year - mController.getMinYear()) * 12 + mSelectedDay.month - mController.getFirstMonth();
        setCurrentItem(position);
    }

    public void setMinDate(CalendarDay day){
        mPagerAdapter.setMinDate(day, true);
    }

    public void setMaxDate(CalendarDay day) {
        mPagerAdapter.setMaxDate(day, true);
    }

    protected void setupAdapter(CalendarDay mMaxDate) {
        if (mPagerAdapter == null) {
            mPagerAdapter = new SimpleMonthPagerAdapter(mController);
        }
        mPagerAdapter.setSelectedDay(this.mSelectedDay);
        mPagerAdapter.setMinDate(mMaxDate, false);
        mPagerAdapter.notifyDataSetChanged();
    }

}