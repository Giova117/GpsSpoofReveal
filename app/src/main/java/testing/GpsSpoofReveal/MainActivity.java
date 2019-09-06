package testing.GpsSpoofReveal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;

//import android.location.GnssMeasurement;
//import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.location.cts.nano.Ephemeris;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    //private GnssMeasurementsEvent mGnssMeasurementEvent;
    //private GnssMeasurementsEvent.Callback mGnssMeasurementEventCallback;
    private GnssStatus.Callback mGnssStatusCallback;
    private LocationManager mLocationManager;
    private LocationListener listener;
    private GnssNavigationMessage.Callback mNavCallback;

    /*This class is used to derive the navigation message from raw data.
    * This class was developed by Google*/
    private GpsNavigationMessageStore objNavMess;
    private Ephemeris.GpsNavMessageProto NavigationMessages;

    private TextView tvGpsInfo;
    private TextView tvGpsCoo;
    private TextView tvList;
    private Button bStart;
    private Button bReset;
    private String tvGpsCooText = "";
    private Chronometer chronometer;
    private String  currentTime;
    private int SecretVariable = 0, counter;

    //these variables contain the position of the device
    private double Longitude;
    private double Latitude;

    //these variables are used to pass parameters to the activity Analyze
    public static final String EXTRA_NUMBER_1 = "testing.GpsSpoofReveal.EXTRA_NUMBER_1";
    public static final String EXTRA_NUMBER_2 = "testing.GpsSpoofReveal.EXTRA_NUMBER_2";
    public static final String EXTRA_NUMBER_3 = "testing.GpsSpoofReveal.EXTRA_NUMBER_3";
    public static final String EXTRA_NUMBER_4 = "testing.GpsSpoofReveal.EXTRA_NUMBER_4";
    public static final String EXTRA_NUMBER_5 = "testing.GpsSpoofReveal.EXTRA_NUMBER_5";
    public static final String EXTRA_NUMBER_6 = "testing.GpsSpoofReveal.EXTRA_NUMBER_6";
    public static final String EXTRA_NUMBER_7 = "testing.GpsSpoofReveal.EXTRA_NUMBER_7";
    public static final String EXTRA_NUMBER_8 = "testing.GpsSpoofReveal.EXTRA_NUMBER_8";
    public static final String EXTRA_NUMBER_9 = "testing.GpsSpoofReveal.EXTRA_NUMBER_9";
    public static final String EXTRA_NUMBER_10 = "testing.GpsSpoofReveal.EXTRA_NUMBER_10";

    private Integer satelliteCount, sat_id, constellationType, azimuthDegrees, elevationDegrees, cn0DbHz, pointer = 0, numOfNavMessSat = 0;
    private boolean hasAlmanac, hasEphemeris;
    private String v = "If you acquire data for more than an hour the application may report an error in the navigation message!";
    private int[] [] listOfSat;
    /*format of listOfSat [32] [7]
    * maximum number of GPS satellites is 32 even if it is impossible for the device to see them all at the same time.
    *
    * listOfSat[X] [0] = satellite ID
    * listOfSat[X] [1] = azimuth
    * listOfSat[X] [2] = elevation
    * listOfSat[X] [3] = cn   (Carrier to Noise)
    * listOfSat[X] [4] = hasEphemeris?
    * listOfSat[X] [5] = hasAlmanac?
    * listOfSat[X] [6] = this bit is set to one if the satellite has at least one rawData or has no ephemeris or almanac
    *
    * */
    private int[] [] listOfNavMessSat;
    /*format of listOfNavMessSat [32] [7]
     * maximum number of GPS satellites is 32 even if it is impossible for the device to see them all at the same time.
     *
     * listOfNavMessSat[X] [0] = satellite ID
     * listOfNavMessSat[X] [1] = hasSubmessage1?
     * listOfNavMessSat[X] [2] = hasSubmessage2?
     * listOfNavMessSat[X] [3] = hasSubmessage3?
     * listOfNavMessSat[X] [4] = hasSubmessage4?
     * listOfNavMessSat[X] [5] = hasSubmessage5?
     * listOfNavMessSat[X] [6] = this bit is set to 0 when the Type field of the rawData is != 1, in the GPS constellation this cannot be ==> error, probable spoofing
     *
     * */
    private String[] [] rawData;
    /*format of rawData [32] [6]
     * maximum number of GPS satellites is 32 even if it is impossible for the device to see them all at the same time.
     *
     * rawData[X] [0] = satellite ID
     * rawData[X] [1] = rawData1 or null if it is missing
     * rawData[X] [2] = rawData2 or null if it is missing
     * rawData[X] [3] = rawData3 or null if it is missing
     * rawData[X] [4] = rawData4 or null if it is missing
     * rawData[X] [5] = rawData5 or null if it is missing
     *
     * */
    private boolean startStop = false, infoReset = false, findPosition = false;
    private final int MY_PERMISSIONS_REQUEST_POSITION = 10;
    private String Ephemeris;
    private String Ionosphere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGpsInfo = findViewById(R.id.tvGpsInfo);
        tvGpsCoo = findViewById(R.id.tvGpsCoo);
        tvList = findViewById(R.id.tvList);

        bStart = findViewById(R.id.bStart);
        bReset = findViewById(R.id.bReset);
        //initialize chronometer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");

        listOfSat = new int[32] [7];
        listOfNavMessSat = new int[32] [7];
        rawData = new String[32] [6];

        tvGpsInfo.setText(v);


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

                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(View view) {

                    //start the GPS acquisition
                    if(!startStop) {
                        infoReset = true;
                        Toast.makeText(getApplicationContext(), "Start GPS acquisition!", Toast.LENGTH_SHORT).show();
                        startChronometer();
                        currentTime = "" + Calendar.getInstance().getTime();
                        tvList.setTextColor(Color.parseColor("#d60000")); //red colour
                        listener = new LocationListener() {

                            //this method is run when the device detects a change of position
                            @Override
                            public void onLocationChanged(Location location) {

                                //the position (longitude and latitude) is updated
                                Longitude = location.getLongitude();
                                Latitude = location.getLatitude();
                                findPosition = true;
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
                        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);   //it is possible to ignore this error, permission checking is already performed
                        mLocationManager.registerGnssNavigationMessageCallback(mNavCallback);
                        //mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementEventCallback);
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, listener);  //it is possible to ignore this error, permission checking is already performed
                        startStop = true;
                    }

                    //stop the GPS acquisition
                    else{
                        NavigationMessages = new  Ephemeris.GpsNavMessageProto ();
                        NavigationMessages = objNavMess.createDecodedNavMessage();
                        try {
                            Ephemeris = Arrays.toString(NavigationMessages.ephemerids);

                            /*contains information about the ionosphere.
                             *It takes about 12.5 minutes to download them. */
                            Ionosphere = String.valueOf(NavigationMessages.iono);

                        }catch (Exception e){
                            e.printStackTrace();
                        }
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
            mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
            mLocationManager.unregisterGnssNavigationMessageCallback(mNavCallback);
        }

        //reset the activity
        if (startStop)
            Toast.makeText(getApplicationContext(), "Stop GPS acquisition!", Toast.LENGTH_SHORT).show();
        tvGpsInfo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        startStop = false;
        infoReset = false;
        v = "If you acquire data for more than an hour the application may report an error in the navigation message!";
        bReset.setBackgroundResource(R.drawable.info);
        bStart.setBackgroundResource(R.drawable.start);

        tvGpsInfo.setText(v);
        tvGpsCooText = "";
        tvGpsCoo.setText(tvGpsCooText);
        String tvListText = "";
        tvList.setText(tvListText);
        resetChronometer();
        numOfNavMessSat = 0;
        counter = 0;
        pointer =0;
        listOfSat = new int[32] [7];
        listOfNavMessSat = new int[32] [7];
        rawData = new String[32][6];
        super.onStop();
    }

    //read data received from GPS satellites
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void dataFromSatellite () {

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        objNavMess = new GpsNavigationMessageStore();

        //Raw Data
        mNavCallback = new GnssNavigationMessage.Callback() {
            @Override
            public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                super.onGnssNavigationMessageReceived(event);
                if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) //analyze only GPS L1 satellites
                {
                    byte Svid = (byte) event.getSvid();
                    byte Type = (byte) event.getType();
                    byte SubmessageId = (byte) event.getSubmessageId();
                    byte[] Data = event.getData();
                    boolean find = false;
                    boolean allSatHasNav = true;


                    for (int i = 0; i < numOfNavMessSat; i++) {
                        if (listOfNavMessSat[i][0] == Svid) {
                            find = true;
                            if (Type == 1) {
                                if (listOfNavMessSat[i][SubmessageId] == 0) {
                                    listOfNavMessSat[i][SubmessageId] = 1;
                                    rawData[i][SubmessageId] = Arrays.toString(Data);
                                    v = "NavMess: ID " + listOfNavMessSat[i][0] + ", Submessages: [ " + listOfNavMessSat[i][1] + " " + listOfNavMessSat[i][2] + " " + listOfNavMessSat[i][3] + " " + listOfNavMessSat[i][4] + " " + listOfNavMessSat[i][5] + " ]\n\n" + v;
                                    objNavMess.onNavMessageReported(Svid, Type, SubmessageId, Data);
                                }
                            } else {
                                listOfNavMessSat[i][6] = 1;
                            }
                        }
                    }

                    if (!find) {
                        listOfNavMessSat[numOfNavMessSat][0] = Svid;
                        if (Type == 1) {
                            listOfNavMessSat[numOfNavMessSat][SubmessageId] = 1;
                            v = "NavMess: ID " + listOfNavMessSat[numOfNavMessSat][0] + ", Submessages: [ " + listOfNavMessSat[numOfNavMessSat][1] + " " + listOfNavMessSat[numOfNavMessSat][2] + " " + listOfNavMessSat[numOfNavMessSat][3] + " " + listOfNavMessSat[numOfNavMessSat][4] + " " + listOfNavMessSat[numOfNavMessSat][5] + " ]\n\n" + v;
                            rawData[numOfNavMessSat][0] = "" + Svid;
                            rawData[numOfNavMessSat][SubmessageId] = Arrays.toString(Data);

                            objNavMess.onNavMessageReported(Svid, Type, SubmessageId, Data);
                        } else {
                            listOfNavMessSat[numOfNavMessSat][6] = 1;
                        }

                        for (int i = 0; i < pointer; i++) {
                            if (listOfSat[i][0] == Svid)
                                listOfSat[i][6] = 1; //this satellite has at least one RawData
                        }

                        for (int i = 0; i < pointer; i++)
                            if (listOfSat[i][6] == 0)
                                allSatHasNav = false;


                        if (allSatHasNav && findPosition)
                            tvList.setTextColor(Color.parseColor("#009900")); //green colour
                        else
                            tvList.setTextColor(Color.parseColor("#d60000")); //red colour

                        numOfNavMessSat++;
                    }


                    counter++;
                    if (counter == 5) {
                        counter = 0;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                tvGpsInfo.setText(v);
                            }
                        });
                    }
                }
            }
        };

        //satellites ID, Carrier to Noise, azimuth degrees, elevation degrees, hasEphemeris and hasAlmanac
        mGnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                satelliteCount = status.getSatelliteCount();
                for (int i = 0; i < satelliteCount; i++) {
                    sat_id = status.getSvid(i);
                    constellationType = status.getConstellationType(i);
                    azimuthDegrees = (int) status.getAzimuthDegrees(i);
                    elevationDegrees = (int) status.getElevationDegrees(i);
                    cn0DbHz = (int) status.getCn0DbHz(i);
                    hasEphemeris = status.hasEphemerisData(i);
                    hasAlmanac = status.hasAlmanacData(i);

                    //add only constellation type GPS
                    if (constellationType == GnssStatus.CONSTELLATION_GPS) {
                        if (checkList(sat_id, azimuthDegrees, elevationDegrees, cn0DbHz, hasEphemeris, hasAlmanac)) {
                            v = "ID " + sat_id + ", Azimuth " + azimuthDegrees + ", Elevation " + elevationDegrees + ", C/N " + cn0DbHz + ", hasEphemeris " + hasEphemeris + ", hasAlmanac " + hasAlmanac + "\n\n" + v;
                            tvGpsInfo.setText(v);
                        }
                    }
                }
            }
        };
    }
        /*This class allows the reading of the main parameters of the received signal, including ReceivedSvTimeNanos, ReceivedSvTimeUncertaintyNanos etc.
        * A further method to detect spoofing is to relate Cn0DbHz and AgcLevelDb. Much scientific literature has been written about this.
        * Unfortunately, this application has not been implemented as it came out of the scope of this work.
        *   mGnssMeasurementEventCallback = new GnssMeasurementsEvent.Callback() {
        *       @Override
        *       public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
        *           super.onGnssMeasurementsReceived(eventArgs);
        *               Log.d("master", "" + eventArgs.getMeasurements());
        *       }
        *   }; */


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
    public boolean checkList(int val, int azimuth, int elevation, int cn, boolean hasEphemeris, boolean hasAlmanac){
        boolean find = false;
        for(int k = 0; k < pointer; k++){
            if(val==listOfSat[k] [0]){
                find = true;
                listOfSat[k] [1] = azimuth;
                listOfSat[k] [2] = elevation;
                listOfSat[k] [3] = cn;
                listOfSat[k] [4] = hasEphemeris ? 1:0;
                listOfSat[k] [5] = hasAlmanac ? 1:0;
                listOfSat[k] [6] = ((!hasEphemeris || !hasAlmanac) ? 1:0);  //if satellite has no eph. or alm., set the navigatioMessages bit to 1
            }
        }
        if(!find){
            listOfSat[pointer] [0] = val;

            tvList.append("" + val + ", ");
            pointer++;
            return true;
        }
        return false;
    }

    public void openActivity2(){
        Intent intent = new Intent(this, Analyze.class);

        //create a correct variable for passing the listOfSat Matrix

        //pass the following parameters to activity Analyze
        intent.putExtra(EXTRA_NUMBER_1, listOfSat);
        intent.putExtra(EXTRA_NUMBER_2, pointer);
        intent.putExtra(EXTRA_NUMBER_3, Longitude);
        intent.putExtra(EXTRA_NUMBER_4, Latitude);
        intent.putExtra(EXTRA_NUMBER_5, listOfNavMessSat);
        intent.putExtra(EXTRA_NUMBER_6, numOfNavMessSat);
        intent.putExtra(EXTRA_NUMBER_7, Ephemeris);
        intent.putExtra(EXTRA_NUMBER_8, rawData);
        intent.putExtra(EXTRA_NUMBER_9, currentTime);
        intent.putExtra(EXTRA_NUMBER_10, Ionosphere);

        startActivity(intent);  //start the activity Analyze
    }

    //shows information in the console
    public void displayInfo(int SecretVariable){

        //easter egg
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
            v = "---INFO---\n\n Version: 1.3\n Made by: Giovanni Carollo\nContacts: " +
                    "giovanni.carollo.3@studenti.unipd.it\nThis application is a part of the GpsSpoofReveal project. " +
                    "GpsSpoofReveal was created as a thesis project for a three-year degree in computer engineering for the University of Padua. " +
                    "Right now GpsSpoofReveal is still in a beta phase. The layout may not be displayed correctly on some devices.\n\n---INFO---\n";
            tvGpsInfo.setText(v);
            tvGpsInfo.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.unipd); //unipd logo image
        }
    }
}