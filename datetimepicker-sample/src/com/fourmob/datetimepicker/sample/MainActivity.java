package com.fourmob.datetimepicker.sample;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.CalendarDay;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog.OnDateSetListener;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    public static final String DATEPICKER_TAG = "datepicker";
    public static final String TIMEPICKER_TAG = "timepicker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Calendar calendar = Calendar.getInstance();

        findViewById(R.id.dateButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(MainActivity.this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), isVibrate());
                datePickerDialog.setVibrate(isVibrate());

                if (getSetting(R.id.checkBoxUseConstraints)) {
                    Calendar minDate = Calendar.getInstance();
                    minDate.setTime(new Date());
                    minDate.add(Calendar.DAY_OF_YEAR, -20);

                    Calendar maxDate = Calendar.getInstance();
                    maxDate.setTime(new Date());
                    maxDate.add(Calendar.DAY_OF_YEAR, 20);

                    datePickerDialog.setDateConstraints(new CalendarDay(minDate), new CalendarDay(maxDate));
                }

                datePickerDialog.setCloseOnSingleTapDay(getSetting(R.id.checkBoxCloseOnSingleTapDay));
                datePickerDialog.show(getSupportFragmentManager(), DATEPICKER_TAG);
            }
        });

        findViewById(R.id.timeButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(MainActivity.this, calendar.get(Calendar.HOUR_OF_DAY) ,calendar.get(Calendar.MINUTE), getSetting(R.id.checkBoxIs24HourMode), false);
                timePickerDialog.setVibrate(isVibrate());
                timePickerDialog.setCloseOnSingleTapMinute(getSetting(R.id.checkBoxCloseOnSingleTapMinute));
                timePickerDialog.show(getSupportFragmentManager(), TIMEPICKER_TAG);
            }
        });

        if (savedInstanceState != null) {
            DatePickerDialog dpd = (DatePickerDialog) getSupportFragmentManager().findFragmentByTag(DATEPICKER_TAG);
            if (dpd != null) {
                dpd.setOnDateSetListener(this);
            }

            TimePickerDialog tpd = (TimePickerDialog) getSupportFragmentManager().findFragmentByTag(TIMEPICKER_TAG);
            if (tpd != null) {
                tpd.setOnTimeSetListener(this);
            }
        }
    }

    private boolean getSetting(@IdRes int checkboxRes) {
        return ((CheckBox) findViewById(checkboxRes)).isChecked();
    }

    private boolean isVibrate() {
        return getSetting(R.id.checkBoxVibrate);
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
        Toast.makeText(MainActivity.this, "new date:" + year + "-" + month + "-" + day, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        Toast.makeText(MainActivity.this, "new time:" + hourOfDay + "-" + minute, Toast.LENGTH_LONG).show();
    }
}
