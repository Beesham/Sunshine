package com.beesham.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.beesham.sunshine.data.WeatherContract;
import com.bumptech.glide.Glide;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder>  {

    private final String LOG_TAG = ForecastAdapter.this.getClass().getSimpleName();

    private final View mEmptyView;
    private final ForecastAdapterOnClickHandler mOnClickHandler;
    private final ItemChoiceManager mICM;

    private final Context mContext;
    private Cursor mCursor;

    private boolean mUseTodayLayout = true;

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE = 1;

    public static interface ForecastAdapterOnClickHandler{
        void onClick(Long date, ForecastAdapterViewHolder forecastAdapterViewHolder);
    }

    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ForecastAdapterViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(R.id.list_item_icon);
            dateView = (TextView) itemView.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) itemView.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) itemView.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) itemView.findViewById(R.id.list_item_low_textview);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mOnClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mICM.onClick(this);
        }
    }

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler handler, View emptyView, int choiceMode) {
        mContext = context;
        mOnClickHandler = handler;
        mEmptyView = emptyView;

        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    public void setUseTodayLayout(boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if(viewGroup instanceof RecyclerView){
            int layoutId = -1;
            switch(viewType){
                case VIEW_TYPE_TODAY:
                    layoutId = R.layout.list_item_forecast_today;
                    break;

                case VIEW_TYPE_FUTURE:
                    layoutId = R.layout.list_item_forcast;
                    break;
            }
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(layoutId, viewGroup, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        }else{
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int defaultImage;

        switch (getItemViewType(position)){
            case VIEW_TYPE_TODAY:
                defaultImage = Utility.getArtResourceForWeatherCondition(weatherId);
                break;

            default:
                defaultImage = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        if(Utility.usingLocalGraphics(mContext)){
            holder.iconView.setImageResource(defaultImage);
        }else{
            Glide.with(mContext)
                    .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                    .error(defaultImage)
                    .crossFade()
                    .into(holder.iconView);
        }

        // this enables better animations. even if we lose state due to a device rotation,
        // the animator can use this to re-find the original view
        ViewCompat.setTransitionName(holder.iconView, "iconView" + position);

        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);

        holder.dateView.setText(Utility.getFriendlyDayString(mContext, dateInMillis));

        String description = Utility.getStringForWeatherCondition(mContext, weatherId);
        holder.descriptionView.setText(description);
        holder.descriptionView.setContentDescription(mContext.getString(R.string.content_description_forecast, description));

        // Read high temperature from cursor
        double high = mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        String highString = Utility.formatTemperature(mContext, high);
        holder.highTempView.setText(highString);
        holder.highTempView.setContentDescription(mContext.getString(R.string.content_description_high_temp, highString));

        // Read low temperature from cursor
        double low = mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        String lowString = Utility.formatTemperature(mContext, low);
        holder.lowTempView.setText(lowString);
        holder.lowTempView.setContentDescription(mContext.getString(R.string.content_description_low_temp, lowString));

        mICM.onBindViewHolder(holder, position);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE;
    }

    @Override
    public int getItemCount() {
        if(mCursor == null) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof ForecastAdapterViewHolder ) {
            ForecastAdapterViewHolder vfh = (ForecastAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }
}