package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

public class PlantWateringService extends IntentService {

    public static String ACTION_WATER_PLANTS = "com.example.android.mygarden.action.water_plats";
    public static String ACTION_UPDATE_PLANT_WIDGET = "com.example.android.mygarden.action.update_plant_widget";

    public PlantWateringService() {
        super("PlantWateringService");
    }

    /**
     * Starts this service to perform WaterPlants action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionWaterPlants(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);

        context.startService(intent);
    }

    /**
     * Starts this service to perform UpdatePlantWidget action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdatePlantWidget(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGET);

        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            if(ACTION_WATER_PLANTS.equals(action)) {
                handleActionWaterPlants();
            } else if(ACTION_UPDATE_PLANT_WIDGET.equals(action)) {
                handleActionUpdatePlantWidget();
            }
        }
    }

    /**
     * Updates plants
     */
    private void handleActionWaterPlants() {
        Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();

        ContentValues contentValues = new ContentValues();

        long timeNow = System.currentTimeMillis();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);

        getContentResolver().update(
                PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME+">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
    }

    /**
     * Update plant widget
     */
    private void handleActionUpdatePlantWidget() {
        // Query to get the plant most in need for watering
        Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();

        Cursor cursor = getContentResolver().query(
                PLANTS_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME
        );


        // Get plant details
        int imgRes = R.drawable.grass; //in case there is no plant in our garden
        if(cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int createtimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
            int waterTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
            int plantTypeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);

            long timeNow = System.currentTimeMillis();
            long wateredAt = cursor.getLong(waterTimeIndex);
            long createAt = cursor.getLong(createtimeIndex);

            int plantType = cursor.getInt(plantTypeIndex);

            imgRes = PlantUtils.getPlantImageRes(this, timeNow-createAt, timeNow-wateredAt, plantType);
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));

        PlantWidgetProvider.updatePlantWidgets(this, appWidgetManager, imgRes, appWidgetIds);
    }

}
