//made by me

package testing.GpsSpoofReveal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private GnssStatus.Callback mGnssStatusCallback;
    private LocationManager mLocationManager;
    private LocationListener listener;

    private TextView tvGpsInfo;
    private TextView tvGpsCoo;
    private TextView tvList;
    private Button bStart;
    private Button bReset;
    private String tvGpsCooText = "";
    private Chronometer chronometer;
    private int SecretVariable = 0;

    //these variables contain the position of the device
    private double Longitude;
    private double Latitude;

    //these variables are used to pass parameters to the activity Analyze
    public static final String EXTRA_NUMBER_1 = "testing.GpsSpoofReveal.EXTRA_NUMBER_1";
    public static final String EXTRA_NUMBER_2= "testing.GpsSpoofReveal.EXTRA_NUMBER_2";
    public static final String EXTRA_NUMBER_3= "testing.GpsSpoofReveal.EXTRA_NUMBER_3";
    public static final String EXTRA_NUMBER_4= "testing.GpsSpoofReveal.EXTRA_NUMBER_4";

    private Integer satelliteCount, sat_id, constellationType, count = 0, pointer = 0;
    private String v;
    private int[] listOfSat;
    private boolean startStop = false, infoReset = false;
    private final int MY_PERMISSIONS_REQUEST_POSITION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGpsInfo = findViewById(R.id.tvGpsInfo);
        tvGpsInfo.setMovementMethod(new ScrollingMovementMethod());
        tvGpsCoo = findViewById(R.id.tvGpsCoo);
        tvList = findViewById(R.id.tvList);

        bStart = findViewById(R.id.bStart);
        bReset = findViewById(R.id.bReset);

        //initialize chronometer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");

        listOfSat = new int[32];    //the maximum number of GPS satellites is 32 even if it is impossible for the device to see them all at the same time

    }

    //this method is executed when the application is opened
    @Override
    protected void onStart() {
        super.onStart();

        //info or reset button
        bReset.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                if(!infoReset) {
                    SecretVariable++;
                    displayInfo(SecretVariable);
                }
                else {
                    onStop();
                }
            }
        });

        //permission checking
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            bStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //start the GPS acquisition
                    if(!startStop) {
                        infoReset = true;
                        Toast.makeText(getApplicationContext(), "Start GPS acquisition!", Toast.LENGTH_SHORT).show();
                        startChronometer();
                        listener = new LocationListener() {

                            //this method is run when the device detects a change of position
                            @Override
                            public void onLocationChanged(Location location) {

                                //the position (longitude and latitude) is updated
                                Longitude = location.getLongitude();
                                Latitude = location.getLatitude();
                                tvGpsCooText = "Longitude: " + Longitude + "\nLatitude: " + Latitude;
                                tvGpsCoo.setText(tvGpsCooText);
                            }

                            @Override
                            public void onStatusChanged(String s, int i, Bundle bundle) {

                            }

                            @Override
                            public void onProviderEnabled(String s) {

                            }

                            @Override
                            public void onProviderDisabled(String s) {

                            }
                        };

                        //change the background images of the buttons
                        bReset.setBackgroundResource(R.drawable.reset);
                        bStart.setBackgroundResource(R.drawable.analyze);

                        //read data received from GPS satellites
                        dataFromSatellite();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);   //it is possible to ignore this error, permission checking is already performed
                        }
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, listener);  //it is possible to ignore this error, permission checking is already performed
                        startStop = true;
                    }

                    //stop the GPS acquisition
                    else{
                        Toast.makeText(getApplicationContext(), "Stop GPS acquisition!", Toast.LENGTH_SHORT).show();
                        resetChronometer();
                        mLocationManager.removeUpdates(listener);
                        bReset.setBackgroundResource(R.drawable.info);
                        bStart.setBackgroundResource(R.drawable.analyze);
                        startStop = false;
                        infoReset = false;
                        openActivity2();    //start the activity Analyze
                    }
                }
            });
        }
        else{

            // Permission is not granted so ask location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_POSITION);
        }

    }

    //this method is executed when the application is closed or when it changes resolution
    @Override
    protected void onStop() {

        //check the permissions and if they are verified stop receiving the position updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(listener);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
            }
        }

        //reset the activity
        if (startStop)
            Toast.makeText(getApplicationContext(), "Stop GPS acquisition!", Toast.LENGTH_SHORT).show();
        tvGpsInfo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        startStop = false;
        infoReset = false;
        v = "";
        bReset.setBackgroundResource(R.drawable.info);
        bStart.setBackgroundResource(R.drawable.start);

        tvGpsInfo.setText(v);
        tvGpsCooText = "";
        tvGpsCoo.setText(tvGpsCooText);
        String tvListText = "";
        tvList.setText(tvListText);
        resetChronometer();

        pointer =0;
        count = 0;
        listOfSat = new int[32];
        super.onStop();
    }

    //read data received from GPS satellites, in this case we only need the GPS Svid
    private void dataFromSatellite (){
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mGnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    satelliteCount = status.getSatelliteCount();
                    v = " Scan: "+ (count)+  " ,Sat-count=" +(satelliteCount)+ "\n";
                    for(int i = 0; i<satelliteCount; i++){
                        sat_id = status.getSvid(i);
                        constellationType = status.getConstellationType(i);

                        //add only constellation type GPS
                        if(constellationType == GnssStatus.CONSTELLATION_GPS){
                            v += "Sat n."+ i +", type= "+ constellationType +", id= "+ sat_id + "\n";
                            checkList(sat_id);
                        }
                    }
                    v += "\n";
                    v += tvGpsInfo.getText();
                    tvGpsInfo.setText(v);
                    count= count + 1;
                }
            };
        }
    }

    //this method is only executed when permissions are requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_POSITION) {
            // if request is cancelled, the result arrays are empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                // permission granted
                onStart();
            else
                // permission denied
                Toast.makeText(this, "Permission not allowed. Gps data not available!", Toast.LENGTH_SHORT).show();
        }
    }

    public void startChronometer()
    {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    public void resetChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
    }

    //this method checks if a visible satellite is already present in the list of visible satellites
    public void checkList(int val){
        boolean find = false;
        for(int k = 0; k < pointer; k++){
            if(val==listOfSat[k]){
                find = true;
                break;
            }
        }
        if(!find){
            listOfSat[pointer] = val;
            tvList.append("" + val + ", ");
            pointer++;
        }
    }

    public void openActivity2(){
        Intent intent = new Intent(this, Analyze.class);

        //pass the following parameters to activity Analyze
        intent.putExtra(EXTRA_NUMBER_1, listOfSat);
        intent.putExtra(EXTRA_NUMBER_2, pointer);
        intent.putExtra(EXTRA_NUMBER_3, Longitude);
        intent.putExtra(EXTRA_NUMBER_4, Latitude);

        startActivity(intent);  //start the activity Analyze
    }

    //shows information in the console
    public void displayInfo(int SecretVariable){
        if(SecretVariable == 5) {
            tvGpsInfo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            v = " ---EXPLORE---\n\n" +
                    " }--O--{\n" +
                    " [^]\n" +
                    " /ooo\\\n" +
                    " _____________:/o   o\\:_____________\n" +
                    " |=|=|=|=|=|=|:A|\"::||||::\"|A:|=|=|=|=|=|=|\n" +
                    " ^\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"!::{o}::!\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"^\n" +
                    " \\     /\n" +
                    " \\.../\n" +
                    " ____                \"---\"                ____\n" +
                    " |\\/\\/|=======|*|=======|\\/\\/|\n" +
                    " :-------\"               /-\\               \"-------:\n" +
                    " /ooo\\\n" +
                    " #|ooo|#\n" +
                    " \\___/\n\n ---SPACE---";
            tvGpsInfo.setText(v);
        }
        else{
            v = "---INFO---\n\n Version: 1.0\n Made by: Giovanni Carollo\nContacts: giovanni.carollo.3@studenti.unipd.it\nThis application is a part of the GpsSpoofReveal project. GpsSpoofReveal was created as a thesis project for a three-year degree in computer engineering for the University of Padua. Right now GpsSpoofReveal is still in a beta phase. The layout may not be displayed correctly on some devices.\n\n---INFO---\n";
            tvGpsInfo.setText(v);
            tvGpsInfo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.unipd); //unipd logo image
        }
    }
}