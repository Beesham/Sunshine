package com.beesham.sunshine;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;

import java.util.ArrayList;
import com.beesham.sunshine.data.WeatherContract;
import com.beesham.sunshine.sync.SunshineSyncAdapter;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ForecastFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ForecastFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = ForecastFragment.this.getClass().getSimpleName();

    private boolean mUseTodayLayout;

    private static final int FORECAST_LOADER = 0;

    private ListView forcastLV;
    private int mPosition = ListView.INVALID_POSITION;

    private ArrayList<String> forcastAL;
    private ForecastAdapter mForecastAdapter;

    private static  final String SELECTED_KEY = "selected_position";

    ForecastData forecastData;
    private String forecastDataStr;

    private String mParam1;
    private String mParam2;

    public interface Callback{
        public void onItemSelected(Uri dateUri);
    }

    private static final String[] FORECAST_COLUMNS ={
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    static  final int COL_WEATHER_ID = 0;
    static  final int COL_WEATHER_DATE = 1;
    static  final int COL_WEATHER_DESC = 2;
    static  final int COL_WEATHER_MAX_TEMP = 3;
    static  final int COL_WEATHER_MIN_TEMP = 4;
    static  final int COL_LOCATION_SETTINGS = 5;
    static  final int COL_WEATHER_CONDITION_ID = 6;
    static  final int COL_COORD_LAT = 7;
    static  final int COL_COORD_LONG = 8;



    private OnFragmentInteractionListener mListener;

    public ForecastFragment() {
        setHasOptionsMenu(true);
    }

    public static ForecastFragment newInstance(String param1, String param2) {
        ForecastFragment fragment = new ForecastFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }

        forecastData = new ForecastData();
        forcastAL = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

        final Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri, null, null, null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_forcast, container, false);

       // mForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forcast, R.id.list_item_forcast_textview, forcastAL);

        forcastLV = (ListView) rootView.findViewById(R.id.listView_forcast);
        forcastLV.setAdapter(mForecastAdapter);

        forcastLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v(LOG_TAG,forcastLV.getAdapter().getItem(position).toString());
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if(cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)));
                }
            mPosition = position;
            }
        });


        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)){
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != ListView.INVALID_POSITION){
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        Log.d(LOG_TAG,"Menu id: "+Integer.toString(id));
        switch(id){
//            case(R.id.action_refresh):
//                updateWeather();
//                Log.d(LOG_TAG,"Executing Refresh");
//                return true;

            case(R.id.action_settings):
                Log.d(LOG_TAG,"Executing Settings");
                startActivity( new Intent(getActivity(), SettingsActivity.class));
                return true;

            case(R.id.action_viewLocation):
                Log.d(LOG_TAG,"Executing Viewing location");
                viewPreferredLocation();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void onLocationChange(){
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    public void updateWeather(){
//       Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
//        alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,
//                Utility.getPreferredLocation(getActivity()));
//
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
//
//        AlarmManager am = ((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE));
//        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent);

        SunshineSyncAdapter.syncImmediately(getActivity());

    }

    public void viewPreferredLocation(){

        String postalCode = Utility.getPreferredLocation(getActivity());

        if(null != mForecastAdapter){
            Cursor cursor = mForecastAdapter.getCursor();

            if(null != cursor){
                cursor.moveToPosition(0);
                String postLat = cursor.getString(COL_COORD_LAT);
                String posLong = cursor.getString(COL_COORD_LONG);

                Uri geoLocation = Uri.parse("geo:" + postLat + "," + posLong);

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(geoLocation);
                if(i.resolveActivity(getActivity().getPackageManager()) != null){
                    startActivity(i);
                }else{
                    Log.d(LOG_TAG, "Could't call" + geoLocation.toString() + ", no receiveing apps installed!");
                }
            }
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
      /*  if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
        if(mPosition != ListView.INVALID_POSITION){
            forcastLV.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }




}
