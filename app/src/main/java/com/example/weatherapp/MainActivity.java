package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {


    private static final int PERMISSION_REQUEST_CODE = 100;
    private ProgressBar progressBar;
    private RelativeLayout rLHome;
    private TextView textCityName, textTemp, textConditions, textWindSpeed, textLastTime;
    private RecyclerView rvWeather, rvFavs;
    private ImageView imgBG, imgSearch, imgWeather, imgRefresh;
    private TextInputEditText editCityName;
    private ArrayList<WeatherModel> arr;
    private ArrayList<FavCityModel> favArr;
    private WeatherModelAdapter weatherModelAdapter;
    private FavCityAdapter favCityAdapter;
    private int PERMISSION_CODE = 1;
    double lat, lon;
    private LocationManager locationManager;
    private Location location;
    String apiKey = "902bd8f24a0db16cece5af1aa1dbdeb2";

    private static final String PREF_LAST_NOTIFICATION_DATE = "lastNotificationDate";
    String[] saveKey = {
            "CurrentWeatherData",
            "ForecastWeatherData",
            "New York",
            "Singapore",
            "Mumbai",
            "Delhi",
            "Sydney",
            "Melbourne"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.pBarLoading);
        rLHome = findViewById(R.id.RLHome);
        textCityName = findViewById(R.id.textCityName);
        textTemp = findViewById(R.id.textTemp);
        textConditions = findViewById(R.id.textConditions);
        rvWeather = findViewById(R.id.rvWeather);
        rvFavs = findViewById(R.id.rvFavs);
        imgBG = findViewById(R.id.imgBG);
        imgWeather = findViewById(R.id.imgWeather);
        imgSearch = findViewById(R.id.imgSearch);
        imgRefresh = findViewById(R.id.imgRefresh);
        editCityName = findViewById(R.id.editCityName);
        textWindSpeed = findViewById(R.id.textWindSpeed);
        textLastTime = findViewById(R.id.textLastTime);

        arr = new ArrayList<>();
        favArr = new ArrayList<>();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);

        }
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        createNotificationChannel();

        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isInternetAvailable = NetworkCheck.isNetworkAvailable(getApplicationContext());
                if (isInternetAvailable) {
                    String city = editCityName.getText().toString();
                    if (city.equals("")) {
                        Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                        editCityName.requestFocus();
                        editCityName.setError("Please Enter City Name");
                    } else {
                        updateWeather(city);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isInternetAvailable = NetworkCheck.isNetworkAvailable(getApplicationContext());
                if (isInternetAvailable) {
                    Toast.makeText(MainActivity.this, "Refreshing...", Toast.LENGTH_SHORT).show();
                    getCurrentWeather(lat, lon);
                    getForecastWeather(lat, lon);
                    favArr.clear();
                    for (int i = 2; i < saveKey.length; i++) {
                        getFavCoord(saveKey[i], i);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // Request location updates
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else {
                // Permission denied
                // Handle accordingly
            }
        }
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();


        // Do something with latitude and longitude
        Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
        getCurrentWeather(latitude, longitude);
        getForecastWeather(latitude, longitude);

        favCityAdapter = new FavCityAdapter(this, favArr);
        rvFavs.setAdapter(favCityAdapter);

        boolean isInternetAvailable = NetworkCheck.isNetworkAvailable(getApplicationContext());
        if (isInternetAvailable) {
            for (int i = 2; i < saveKey.length; i++) {
                getFavCoord(saveKey[i], i);
            }
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            for (int i = 0; i < saveKey.length; i++) {
                retrieveLastResponse(i);
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    public void OpenMap(View view) {
        Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        startActivity(intent);
    }

    private void updateWeather(String city) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + city
                + "&appid="
                + apiKey
                + "&units=metric";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("cod").equals("404")) {
                        Toast.makeText(MainActivity.this, "Please enter correct city name", Toast.LENGTH_LONG).show();
                    } else {
                        textLastTime.setText(getCurrentTime());
                        lon = response.getJSONObject("coord").getDouble("lon");
                        lat = response.getJSONObject("coord").getDouble("lat");
                        getCurrentWeather(lat, lon);
                        getForecastWeather(lat, lon);
                    }
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "Please enter correct city name", Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error", "city not found");
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private String getCurrentTime() {
        LocalDateTime dateTime = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return dateTime.format(formatter);
    }

    public void getCurrentWeather(double lat, double lon) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        Log.d("Latitude, Longiture", lat + "&&" + lon);
        String urlCurrent = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlCurrent, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                textLastTime.setText(getCurrentTime());
                saveLastResponse(response, 0);
                progressBar.setVisibility(View.GONE);
                rLHome.setVisibility(View.VISIBLE);
                updateCurrentWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error", "" + error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void updateCurrentWeather(JSONObject response) {
        try {
            Log.d("response of json", response.toString());
            String city = response.getString("name");
            textCityName.setText(city);

            String temperature = response.getJSONObject("main").getString("temp");
            textTemp.setText(temperature + "°C");

            String img = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("icon");
            Picasso.get().load("https://openweathermap.org/img/w/" + img + ".png").into(imgWeather);

            String condition = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");
            textConditions.setText(condition);

            // Create notification message based on weather condition
            String notificationMessage = "";
            if (condition.equalsIgnoreCase("Smoke")) {
                notificationMessage = "It's smokey. Suggest you to wear a mask.";
                Log.d("Smoke Main",notificationMessage);
            } else if (condition.equalsIgnoreCase("Rain")) {
                notificationMessage = "It's raining. Remember to carry an umbrella!";
                Log.d("Smoke Main",notificationMessage);
            }
            else if (condition.equalsIgnoreCase("Snow")) {
                notificationMessage = "It's snowing. Time to put your sweaters on.";
                Log.d("Smoke Main",notificationMessage);
            }
            else if (condition.equalsIgnoreCase("Clouds")) {
                notificationMessage = "It's cloudy. Weather is pleasant.";
                Log.d("Smoke Main",notificationMessage);
            }
            else if (condition.equalsIgnoreCase("Sunny")) {
                notificationMessage = "It's sunny. Remember to apply sunscreen!";
                Log.d("Smoke Main",notificationMessage);
            }
            else {
                notificationMessage = "It's a great weather.";
                Log.d("Waether THere",notificationMessage);
            }
            sendNotification(notificationMessage);

            double wSpeed = response.getJSONObject("wind")
                    .getDouble("speed");
            textWindSpeed.setText("" + wSpeed + "Km/h");

            if (img.charAt(img.length() - 1) == 'd')
                imgBG.setImageResource(R.drawable.day);
            if (img.charAt(img.length() - 1) == 'n')
                imgBG.setImageResource(R.drawable.night);


        } catch (Exception e) {
            Log.d("Update Res", "" + e.getMessage());
        }

    }

    private void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "weatherAppChannelId")
                .setSmallIcon(R.drawable.baseline_notifications_none_24)
                .setContentTitle("Weather Notification")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void getForecastWeather(double lat, double lon) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String urlForecast = "https://api.openweathermap.org/data/2.5/forecast?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlForecast, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                textLastTime.setText(getCurrentTime());
                saveLastResponse(response,1);
                arr.clear();
                updateForecastWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",""+error.getMessage());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Weather Notifications";
            String description = "Channel for Weather Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("weatherAppChannelId", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private void updateForecastWeather(JSONObject response) {
        try{
            int loop = response.getInt("cnt");
            JSONArray forecast = response.getJSONArray("list");

            for (int i = 0; i < loop; i += 1) {
                String time = forecast.getJSONObject(i)
                        .getString("dt_txt");
                double temp = forecast.getJSONObject(i)
                        .getJSONObject("main")
                        .getDouble("temp");
                String condition = forecast.getJSONObject(i)
                        .getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("icon");
                double wSpeed = forecast.getJSONObject(i)
                        .getJSONObject("wind")
                        .getDouble("speed");
                String pod = forecast.getJSONObject(i)
                        .getJSONObject("sys")
                        .getString("pod");
                arr.add(new WeatherModel(temp, condition, wSpeed,time,pod));
                if(i==0){
                    if(pod.equals("n")){
                        imgBG.setImageResource(R.drawable.night);
                    }else {
                        imgBG.setImageResource(R.drawable.day);
                    }
                }
            }
            weatherModelAdapter.notifyDataSetChanged();
        }catch (Exception e){
            Log.d("Update Res",""+e.getMessage());
        }
    }

    private void saveLastResponse(JSONObject res, int time) {

        SharedPreferences sharedPreferences = getSharedPreferences("WeatherData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try{
            String save = res.toString();
            editor.putString(saveKey[time], save);
            editor.putString("Time", getCurrentTime());
            editor.apply();
        }catch (Exception e){
            Log.d("Save Res",""+e.getMessage());
        }
    }

    private void retrieveLastResponse(int time) {
        SharedPreferences sharedPreferences = getSharedPreferences("WeatherData", Context.MODE_PRIVATE);
        String weatherData = sharedPreferences.getString(saveKey[time], "");
        String dateTime = sharedPreferences.getString("Time", "");
        textLastTime.setText(dateTime);
        try {
            JSONObject response = new JSONObject(weatherData);
            switch (time){
                case 0:
                    updateCurrentWeather(response);
                    break;
                case 1:
                    updateForecastWeather(response);
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    updateFavWeather(response);
                    break;
                default:
                    Log.d("retrieveLastResponse","Wrong time");
                    break;
            }
        }catch (Exception ex){
            Log.d("Load Res",""+ex.getMessage());
        }
    }

    public void getFavCoord(String name,int i){
        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                +name
                +"&appid="
                +apiKey
                +"&units=metric";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    textLastTime.setText(getCurrentTime());
                    double lon = response.getJSONObject("coord").getDouble("lon");
                    double lat = response.getJSONObject("coord").getDouble("lat");
                    getFavWeather(lat, lon, i);
                } catch (Exception ex) {
                    Log.d("getFavCoord","Favorite City Fetch Failed");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",""+ error.getMessage());
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void getFavWeather(double lat, double lon, int i) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        String urlCurrent = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat
                + "&lon="
                + lon
                + "&appid="
                + apiKey
                + "&units=metric";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, urlCurrent, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                saveLastResponse(response,i);
                updateFavWeather(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Fetch Error",""+error.getMessage());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void updateFavWeather(JSONObject response) {
        try{
            String city = response.getString("name");
            String temperature = response.getJSONObject("main").getString("temp");
            String img = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("icon");
            String condition = response.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");
            double wSpeed = response.getJSONObject("wind")
                    .getDouble("speed");
            favArr.add(new FavCityModel(0,city,temperature,condition,String.valueOf(wSpeed),img));
        }catch (Exception e){
            Log.d("Update Res",""+e.getMessage());
        }
        favCityAdapter.notifyDataSetChanged();
    }


}