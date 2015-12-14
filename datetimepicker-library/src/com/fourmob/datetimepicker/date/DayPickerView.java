package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.fourmob.datetimepicker.R;
import com.fourmob.datetimepicker.Utils;

public class DayPickerView extends FrameLayout implements SimpleMonthPagerAdapter.MonthPagerListener {

    private ViewPager mViewPager;
    private SimpleMonthPagerAdapter mPagerAdapter;

    private ImageView mBackButton;
    private ImageView mForwardButton;

    private DayPickerListener mListener;

    public DayPickerView(Context context, DayPickerParams params) {
        super(context);
        init(context, params);
    }

    private void init(Context context, DayPickerParams params) {
        LayoutInflater.from(context).inflate(R.layout.day_picker, this, true);

        mBackButton = (ImageView) findViewById(R.id.day_picker_back);
        mForwardButton = (ImageView) findViewById(R.id.day_picker_forward);

        setNavigationSelector(mBackButton);
        setNavigationSelector(mForwardButton);

        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = mViewPager.getCurrentItem();
                if (currentItem > 0) {
                    mViewPager.setCurrentItem(currentItem - 1, true);
                }
            }
        });

        mForwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = mViewPager.getCurrentItem();
                if (currentItem < mPagerAdapter.getCount()) {
                    mViewPager.setCurrentItem(currentItem + 1, true);
                }
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.day_picker_pager);
        mViewPager.setPageMargin(context.getResources().getDimensionPixelSize(R.dimen.day_picker_pager_margin));

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateNavigationButtons();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mPagerAdapter = new SimpleMonthPagerAdapter(params);
        mPagerAdapter.setListener(this);

        mViewPager.setAdapter(mPagerAdapter);
        updatePosition(params);
    }

    /**
     * Configures a backwards compatible state list to avoid adding further drawables.
     *
     * @param navigationButton the button to assign the selector to.
     */
    private void setNavigationSelector(ImageView navigationButton) {
        ColorStateList colours = getResources().getColorStateList(R.color.chevron_selector);
        Drawable d = DrawableCompat.wrap(navigationButton.getDrawable());
        DrawableCompat.setTintList(d, colours);
        navigationButton.setImageDrawable(d);
    }

    /**
     * Updates the enabled state of the navigation buttons.
     */
    private void updateNavigationButtons() {
        int currentItem = mViewPager.getCurrentItem();
        int count = mPagerAdapter.getCount();

        mBackButton.setEnabled(count > 1 && currentItem != 0);
        mForwardButton.setEnabled(count > 1 && currentItem != count - 1);
    }

    public void updateParams(DayPickerParams params) {
        mPagerAdapter.updateParams(params, true);
        updatePosition(params);
    }

    private void updatePosition(DayPickerParams params) {
        int position = Utils.getMonthsBetweenDates(params.minDate.month, params.minDate.year, params.selectedDate.month, params.selectedDate.year) - 1;
        mViewPager.setCurrentItem(position);
        updateNavigationButtons();
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

    public int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }

    public void setCurrentItem(int currentItem) {
        mViewPager.setCurrentItem(currentItem);
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