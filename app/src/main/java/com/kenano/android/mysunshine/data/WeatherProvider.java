package com.kenano.android.mysunshine.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by KenanO on 9/5/16.
 */
public class WeatherProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    static UriMatcher buildUriMatcher() {

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        return matcher;
    }

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{

        //create sql query.
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        //build a inner join query
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }


    //for selection components of query use question-mark placement syntax.
    //the "?" symbols will be replaced with values placed into selectionArgs in the query method
    //location.location_setting = ?
    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    //location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";

    private WeatherDbHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());

        return true;
    }

    /**
     * Queries the content provider for data
     *
     * @param uri uri containing the query details
     * @param projection columns of data requested
     * @param selection used for filtering results
     * @param selectionArgs args for result filtering
     * @param sortOrder order of the results.
     * @return a cursor containing results
     */
    @Nullable
    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {


        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and builds query to the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                //build the query and execute it.
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                //build the query and execute it.
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                //if the uri didnt match any of the cases something is wrong.
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        //registers the cursor to watch for changes in the uri. not exactly sure what this means.
        // ill get back to this later.
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /**
     *Since there can be different types of request on the content provider this method matches
     * the uri to determine the request type.
     *
     * @param uri - to be matched to determine the type of request.
     * @return A string which is defined in the contract that ids the type of request
     */
    @Nullable
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * inserts data into content provider.
     *
     * @param uri used to specify request
     * @param values the values to be inserted into content provider.
     * @return a uri representing the data entered.
     */
    @Nullable
    @Override
    public Uri insert(Uri uri,
                      ContentValues values) {

        //get an instance of the db being used.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        //determine type of request using uri matcher.
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        //select which table go enter data based on uri.
        switch (match) {
            case WEATHER: {

                //changes date to what db uses.
                normalizeDate(values);

                //insert weather entry, build a uri that represents entry. return it or throw
                //exception
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {

                //insert location entry, build a uri that represents entry. return it or throw
                //exception
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        //send notification th contnent provider has changed.
        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    /**
     * deletes data from content provider
     *
     * @param uri specifies request type
     * @param selection used to filter request
     * @param selectionArgs args used for filtering
     * @return number of rows deleted
     */
    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs) {

        //get and instance of the db.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        //determine type of request using uri matcher.
        final int match = sUriMatcher.match(uri);

        //number of rows deleted
        int rowsDeleted;

        // this makes delete all rows return the number of rows deleted
        if ( null == selection )
            selection = "1";

        switch (match) {
            case WEATHER:

                //remove data or throw exception.
                rowsDeleted = db.delete(
                        WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:

                //remove data or throw exception.
                rowsDeleted = db.delete(
                        WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    /**
     * Update data in content provider
     *
     * @param uri specifies request type
     * @param values the values to be inserted into content provider.
     * @param selection  used to filter request
     * @param selectionArgs  args used for filtering
     * @return
     */
    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {

        //get and instance of the db.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        //determine type of request using uri matcher.
        final int match = sUriMatcher.match(uri);

        //number of rows removed
        int rowsUpdated;

        switch (match) {
            case WEATHER:

                //changes date to what db uses.
                normalizeDate(values);

                //update or throw exception
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case LOCATION:

                //update or throw exception
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    /**
     * Executes a query for requesting weather for a specified location.
     * Extracts query parameters from the uri.
     *
     * @param uri the location and date are passed through this uri.
     * @param projection the columns of data requested
     * @param sortOrder order of the results
     * @return a cursor containing the results.
     */
    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /**
     * Executes a query for requesting weather for a specified location and date.
     * Extracts query parameters from the uri.
     *
     * @param uri the location and date are passed through this uri.
     * @param projection the columns of data requested
     * @param sortOrder order of the results
     * @return a cursor containing the results.
     */
    private Cursor getWeatherByLocationSettingAndDate(
            Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }

    /**
     * Extracts date from contentvalues obj and normalizes date to db date class used
     * ( Julian day at UTC.)
     * @param values obj which the date is inside.
     */
    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }
}
