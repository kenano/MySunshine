package com.kenano.android.mysunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Created by KenanO on 10/2/16.
 */
public class ForecastAdapter extends CursorAdapter {

    /**
     * {@link ForecastAdapter} exposes a list of weather forecasts
     * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
     */
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);

        return view;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.
        TextView tv = (TextView)view;
        tv.setText(convertCursorRowToUXFormat(cursor));
    }

    /**
     * This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
     * string.
     *
     * @param cursor
     * @return
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {

        //get the max/min column data from cursor.
        //Cursor generated by loader in ForecastFragment using projection.
        //COL_WEATHER_MAX_TEMP/COL_WEATHER_MIN_TEMP indexes are also specified and matches to
        //the number of columns in the projection.
        String highAndLow = formatHighLows(
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(high, isMetric) + "/" +
                Utility.formatTemperature(low, isMetric);

        return highLowStr;
    }
}
