package com.fourmob.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.fourmob.datetimepicker.R;

import java.util.ArrayList;
import java.util.List;

public class YearPickerView extends LinearLayout implements AdapterView.OnItemClickListener, DatePickerDialog.OnDateChangedListener, AbsListView.OnScrollListener {

	private ListView mListView;
	private YearAdapter mAdapter;

	private View mMoreContentIndicator;
	private boolean mUseContentIndicator;

	private final DatePickerController mController;
	private int mViewSize;

	public YearPickerView(Context context, DatePickerController datePickerController) {
		super(context);
		mController = datePickerController;
		mController.registerOnDateChangedListener(this);

		setOrientation(VERTICAL);

		Resources resources = context.getResources();
		mViewSize = resources.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height);

		mListView = new ListView(context);

		// Ensures that the selector on the text view is used.
		mListView.setSelector(android.R.color.transparent);
		mListView.setCacheColorHint(Color.TRANSPARENT);

        LayoutParams listLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        listLayoutParams.weight = 1;
		addView(mListView, listLayoutParams);

		mUseContentIndicator = true;
		mMoreContentIndicator = LayoutInflater.from(context).inflate(R.layout.year_picker_footer, this, false);
		addView(mMoreContentIndicator);

		init();
		mListView.setOnScrollListener(this);
		mListView.setOnItemClickListener(this);
		mListView.setDividerHeight(0);
		onDateChanged();
	}

    private static int getYearFromTextView(TextView view) {
        return Integer.valueOf(view.getText().toString());
    }

	private void init() {
		ArrayList<Integer> years = new ArrayList<Integer>();
		for (int year = mController.getMinYear(); year <= mController.getMaxYear(); year++) {
			years.add(year);
		}
		mAdapter = new YearAdapter(years);
		mListView.setAdapter(mAdapter);

		// Check whether the 'more content indicator should be visible at all'
		if (isAtBottom()) {
			mUseContentIndicator = false;
			mMoreContentIndicator.setVisibility(GONE);
		}
	}

	private boolean isAtBottom() {
		return mListView.getLastVisiblePosition() == mListView.getAdapter().getCount() -1 &&
				mListView.getChildAt(mListView.getChildCount() - 1).getBottom() <= mListView.getHeight();
	}

	public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
	}

	public void onDateChanged() {
		mAdapter.notifyDataSetChanged();
		postSetSelectionCentered(mController.getSelectedDay().year - mController.getMinYear());
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mController.tryVibrate();
        TextView clickedView = (TextView) view;
        if (clickedView != null) {
            mController.onYearSelected(getYearFromTextView(clickedView));
            mAdapter.notifyDataSetChanged();
        }
	}

	public void postSetSelectionCentered(int position) {
		postSetSelectionFromTop(position, mViewSize / 3);
	}

	public void postSetSelectionFromTop(final int position, final int y) {
		post(new Runnable() {
			public void run() {
				mListView.setSelectionFromTop(position, y);
				requestLayout();
			}
		});
	}

    public int getFirstVisiblePosition() {
        if (mListView == null) {
            return 0;
        }
        return mListView.getFirstVisiblePosition();
    }

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (!mUseContentIndicator) {
			// Don't do anything since the indicator is never visible.
			return;
		}

		final int lastItem = firstVisibleItem + visibleItemCount;
		if (lastItem == totalItemCount && isAtBottom()) {
			mMoreContentIndicator.setVisibility(GONE);
		} else {
			mMoreContentIndicator.setVisibility(VISIBLE);
		}
	}

	private class YearAdapter extends BaseAdapter {
		private static final int DEFAULT_ITEM_TYPE = 0;
		private static final int SELECTED_ITEM_TYPE = 1;
		private List<Integer> years;

		public YearAdapter(List<Integer> years) {
			this.years = years;
		}

		@Override
		public int getCount() {
			return years.size();
		}

		@Override
		public Integer getItem(int position) {
			return years.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			if (getItem(position) == mController.getSelectedDay().year) {
				return SELECTED_ITEM_TYPE;
			}
			return DEFAULT_ITEM_TYPE;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				int layoutId;
				if (getItemViewType(position) == SELECTED_ITEM_TYPE) {
					layoutId = R.layout.year_label_text_view_selected;
				} else {
					layoutId = R.layout.year_label_text_view;
				}
				convertView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
			}
			Integer year = getItem(position);

			TextView yearLabel = (TextView) convertView;
			yearLabel.setText(String.format("%d", year));

            return convertView;
        }
	}
}