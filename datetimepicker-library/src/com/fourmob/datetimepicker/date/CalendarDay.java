package com.fourmob.datetimepicker.date;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CalendarDay implements Parcelable {
    private Calendar calendar;

    public final int day;
    public final int month;
    public final int year;

    public CalendarDay() {
        this(System.currentTimeMillis());
    }

    public CalendarDay(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public CalendarDay(long timeInMillis) {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        calendar.setTimeInMillis(timeInMillis);
        month = this.calendar.get(Calendar.MONTH);
        year = this.calendar.get(Calendar.YEAR);
        day = this.calendar.get(Calendar.DAY_OF_MONTH);
    }

    public CalendarDay(Calendar calendar) {
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    private CalendarDay(Parcel in) {
        year = in.readInt();
        month = in.readInt();
        day = in.readInt();
    }

    public static final Creator CREATOR = new Creator() {
        public CalendarDay createFromParcel(Parcel in) {
            return new CalendarDay(in);
        }

        public CalendarDay[] newArray(int size) {
            return new CalendarDay[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(year);
        dest.writeInt(month);
        dest.writeInt(day);
    }

    public boolean equals(CalendarDay o) {
        if (o.year == year && o.day == day && o.month == month) {
            return true;
        } else return false;
    }

    /**
     * returns true if day is after day
     *
     * @param day
     * @return
     */
    public boolean isAfter(CalendarDay day) {
        return convertToDate(this).after(convertToDate(day));
    }

    public boolean isBefore(CalendarDay day) {
        return convertToDate(this).before(convertToDate(day));
    }

    public Date convertToDate(CalendarDay day) {
        Date utilDate = null;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            utilDate = formatter.parse(day.year + "/" + day.month + "/" + day.day);
        } catch (ParseException e) {
            Log.e("SimpleMonthAdapter", "error while converting", e);
        }
        return utilDate;
    }
}
