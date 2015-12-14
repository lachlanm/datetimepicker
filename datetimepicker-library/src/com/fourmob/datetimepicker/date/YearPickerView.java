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

public class YearPickerView extends LinearLayout implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

	private ListView mListView;
	private YearAdapter mAdapter;

	ArrayList<Integer> yearRange = new ArrayList<Integer>();
	private int mSelectedYear = -1;
	private YearPickerListener mListener;

	private View mMoreContentIndicator;
	private boolean mUseContentIndicator;

	private int mViewSize;

	public YearPickerView(Context context) {
		super(context);

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

		mAdapter = new YearAdapter(yearRange);
		mListView.setAdapter(mAdapter);

		mListView.setOnScrollListener(this);
		mListView.setOnItemClickListener(this);
		mListView.setDividerHeight(0);
	}

    private static int getYearFromTextView(TextView view) {
        return Integer.valueOf(view.getText().toString());
    }

	private boolean isAtBottom() {
		int count = mListView.getAdapter().getCount();
		if (count == 0) {
			return true;
		}

		if (mListView.getLastVisiblePosition() == count - 1) {
			return mListView.getChildAt(count - 1).getBottom() <= mListView.getHeight();
		}
		return false;
	}

	public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
	}

	public void updateContent(int selectedYear, int minYear, int maxYear) {
		mSelectedYear = selectedYear;

		// Update the year range if needed.
		if (yearRange.size() > 0 && (yearRange.get(0) != minYear || yearRange.get(yearRange.size() - 1) != maxYear)) {
			yearRange.clear();
		}
		if (yearRange.size() == 0) {
			for (int year = minYear; year <= maxYear; year++) {
				yearRange.add(year);
			}
		}

		mAdapter.notifyDataSetChanged();

		// Check whether the 'more content indicator should be visible at all'
		if (isAtBottom()) {
			mUseContentIndicator = false;
			mMoreContentIndicator.setVisibility(GONE);
		}

		postSetSelectionFromTop(selectedYear - minYear, mViewSize / 3);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView clickedView = (TextView) view;
        if (clickedView != null) {
			mAdapter.notifyDataSetChanged();

			if (mListener != null) {
				mListener.onYearSelected(getYearFromTextView(clickedView));
			}
        }
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

	public void setListener(YearPickerListener listener) {
		mListener = listener;
	}

	public interface YearPickerListener {
		void onYearSelected(int year);
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
			if (getItem(position) == mSelectedYear) {
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