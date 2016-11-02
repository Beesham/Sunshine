package com.beesham.sunshine;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.widget.TextView;

import java.util.ArrayList;
import com.beesham.sunshine.data.WeatherContract;
import com.beesham.sunshine.sync.SunshineSyncAdapter;

/**
 *
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOG_TAG = ForecastFragment.this.getClass().getSimpleName();

    private boolean mUseTodayLayout, mAutoSelectView;

    private static final int FORECAST_LOADER = 0;

    private RecyclerView mForcastRecyclerView;
    private TextView mEmptyView;
    private int mPosition = RecyclerView.NO_POSITION;

    private ArrayList<String> forcastAL;
    private ForecastAdapter mForecastAdapter;
    private boolean mHoldForTransition;

    private static  final String SELECTED_KEY = "selected_position";

    ForecastData forecastData;
    private int mChoiceMode;

    public interface Callback{
        void onItemSelected(Uri dateUri, ForecastAdapter.ForecastAdapterViewHolder viewHolder);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }

        forecastData = new ForecastData();
        forcastAL = new ArrayList<>();

    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
        mHoldForTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransitions, false);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //String locationSetting = Utility.getPreferredLocation(getActivity());

      //  String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
       // Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

//        final Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri, null, null, null, sortOrder);
        final View rootView = inflater.inflate(R.layout.fragment_forcast, container, false);
        mEmptyView = (TextView) rootView.findViewById(R.id.emptyView);

        mForecastAdapter = new ForecastAdapter(getActivity(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder forecastAdapterViewHolder) {
                String locationSetting = Utility.getPreferredLocation(getActivity());
                ((Callback) getActivity())
                        .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                locationSetting, date), forecastAdapterViewHolder);

                mPosition = forecastAdapterViewHolder.getAdapterPosition();
            }
        }, mEmptyView, mChoiceMode);



        mForcastRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView_forcast);
        mForcastRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mForcastRecyclerView.setHasFixedSize(true);
        mForcastRecyclerView.setAdapter(mForecastAdapter);

        final View parallaxView = rootView.findViewById(R.id.parallax_bar);
        if(parallaxView != null) {
         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
             mForcastRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                 @Override
                 public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                     super.onScrolled(recyclerView, dx, dy);
                     int max = parallaxView.getHeight();
                     if(dy > 0){
                         parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy /2));
                     }else{
                         parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy /2));
                     }
                 }
             });
         }
        }

        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)){
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(mHoldForTransition){
            getActivity().supportPostponeEnterTransition();
        }
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
        if(mPosition != RecyclerView.NO_POSITION){
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        Log.d(LOG_TAG,"Menu id: "+Integer.toString(id));
        switch(id){
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
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void viewPreferredLocation(){

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

    @Override
    public void onResume() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_location_status_key))){
            updateEmptyView();
        }
    }

    private void updateEmptyView(){
        if(mEmptyView != null){
            int message = R.string.empty_list;
            @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getContext());
            switch (location){
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                    message = R.string.empty_forecast_list_server_down;
                    break;

                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                    message = R.string.empty_forecast_list_server_error;
                    break;

                case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                    message = R.string.empty_forecast_list_invalid_location;
                    break;

                default:
                    if(!Utility.isOnline(getActivity())){
                        message = R.string.no_connectivity;
                    }
            }
            mEmptyView.setText(message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mForcastRecyclerView != null) mForcastRecyclerView.clearOnScrollListeners();
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
        if(mForecastAdapter.getItemCount() == 0){
            if(!Utility.isOnline(getActivity())){
                mEmptyView.setText(getString(R.string.empty_list) + "\n" + getString(R.string.no_connectivity));
            }
        }
        if(mPosition != ListView.INVALID_POSITION){
            mForcastRecyclerView.smoothScrollToPosition(mPosition);
        }
        if(cursor.getCount() == 0) {
            getActivity().supportStartPostponedEnterTransition();
        }else{
            mForcastRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mForcastRecyclerView.getChildCount() > 0) {
                        mForcastRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = mForecastAdapter.getSelectedItemPosition();
                        if ( RecyclerView.NO_POSITION == itemPosition ) itemPosition = 0;
                        RecyclerView.ViewHolder vh = mForcastRecyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if ( null != vh && mAutoSelectView ) {
                            mForecastAdapter.selectView( vh );
                        }
                        if(mHoldForTransition){
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });

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
        void onFragmentInteraction(Uri uri);
    }




}
