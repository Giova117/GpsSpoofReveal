package testing.GpsSpoofReveal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//these libraries are used to make the http communication
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Analyze extends AppCompatActivity {

    private Button bTryAgain;
    private Button bIP;

    private TextView tvCN;
    private TextView tvSat;
    private TextView tvSuppSat;
    private TextView tvResult;
    private TextView tvIdSatNav;
    private TextView tvNavMessIsOk;
    private TextView tvResultNav;
    private TextView tvAnswer;
    private String tvCNText;
    private String tvSatText;
    private String tvSuppSatText;
    private String tvResultText;
    private String tvIdSatNavText;
    private String tvNavMessIsOkText;
    private String tvResultNavText;
    private String tvAnswerText;

    private int[] [] listOfVisibleSat; //same format as "listOfSat" in the "MainActivity" activity
    private int[] [] listOfSuppVisibleSat;
    /*format of listOfSuppVisibleSat [32] [3]
     * maximum number of GPS satellites is 32 even if it is impossible for the device to see them all at the same time.
     *
     * listOfSuppVisibleSat[X] [0] = satellite ID
     * listOfSuppVisibleSat[X] [1] = azimuth
     * listOfSuppVisibleSat[X] [2] = elevation
     *
     * */

    private int[] [] listOfNavMessSat; //same format as "listOfNavMessSat" in the "MainActivity" activity
    private String [] [] rawData; //same format as "rawData" in the "MainActivity" activity
    private int [] [] ephParam = new int[32][5];


    private int pointerListOfSuppVisibleSat, dimensionListOfVisibleSat, pointerListOfVisibleSat, iterator = 0, numOfNavMessSat;
    private boolean b = false, z = true, Nav = true, checkEph = true;
    private String Ephemeris, Location, Ionosphere;
    private String currentTime;

    //Set of IP present at the time of installation
    private String [] IP = new String[] {"62.211.50.101", "230.209.206.81", "10.204.225.121", "142.167.151.88", "229.234.83.159",
            "233.36.109.116", "159.14.82.41", "131.2.247.254", "101.80.75.8", "88.11.113.241"};
    private String insert = "";
    private int maxNum = 0;

    //these variables are used to pass parameters to the activity ViewNavMess
    public static final String EXTRA_NUMBER_11 = "testing.GpsSpoofReveal.EXTRA_NUMBER_11";
    public static final String EXTRA_NUMBER_12 = "testing.GpsSpoofReveal.EXTRA_NUMBER_12";
    public static final String EXTRA_NUMBER_13 = "testing.GpsSpoofReveal.EXTRA_NUMBER_13";
    public static final String EXTRA_NUMBER_14 = "testing.GpsSpoofReveal.EXTRA_NUMBER_14";
    public static final String EXTRA_NUMBER_15 = "testing.GpsSpoofReveal.EXTRA_NUMBER_15";
    public static final String EXTRA_NUMBER_16 = "testing.GpsSpoofReveal.EXTRA_NUMBER_16";
    public static final String EXTRA_NUMBER_17 = "testing.GpsSpoofReveal.EXTRA_NUMBER_17";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analize);

        bTryAgain = findViewById(R.id.bTryAgain);
        bIP = findViewById(R.id.bIP);
        Button bReturn = findViewById(R.id.bReturn);
        Button bViewNavMessData = findViewById(R.id.bViewNavMessData);

        //return button, return to activity MainActivity
        bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity1();
            }});

        bIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity3();
            }
        });

        TextView tvVariable;
        String tvVariableText;

        tvSat = findViewById(R.id.tvSat);
        tvSuppSat= findViewById(R.id.tvSuppSat);
        tvAnswer = findViewById(R.id.tvAnsw);
        tvCN = findViewById(R.id.tvCN);
        tvResult = findViewById(R.id.tvResult);
        tvVariable = findViewById(R.id.tvVariable);
        tvIdSatNav = findViewById(R.id.tvIdSatNav);
        tvNavMessIsOk = findViewById(R.id.tvNavMessIsOk);
        tvResultNav = findViewById(R.id.tvResultNav);

        //read the parameters that have been passed from MainActivity
        Intent intent = getIntent();
        listOfVisibleSat = (int[][]) intent.getSerializableExtra(MainActivity.EXTRA_NUMBER_1);
        listOfSuppVisibleSat = new int[32][3];
        dimensionListOfVisibleSat = intent.getIntExtra(MainActivity.EXTRA_NUMBER_2, 0);
        double Longitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_3, 0);
        double Latitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_4, 0);
        listOfNavMessSat = (int[][]) intent.getSerializableExtra(MainActivity.EXTRA_NUMBER_5);
        numOfNavMessSat = intent.getIntExtra(MainActivity.EXTRA_NUMBER_6, 0);
        Ephemeris = intent.getStringExtra(MainActivity.EXTRA_NUMBER_7);
        rawData = (String [] []) intent.getSerializableExtra(MainActivity.EXTRA_NUMBER_8);
        currentTime = intent.getStringExtra(MainActivity.EXTRA_NUMBER_9);
        Ionosphere = intent.getStringExtra(MainActivity.EXTRA_NUMBER_10);

        //show longitude and latitude
        Location = "Longitude: " + Longitude + "\n" + "Latitude: " + Latitude;
        tvVariableText = tvVariable.getText() +  "Longitude: " + Longitude + "\n" + "Latitude: " + Latitude;
        tvVariable.setText(tvVariableText);

        readFile();

        //view navigation message button, go to activity ViewNavMess
        bViewNavMessData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity2();
            }
        });

        //check if the ephemeris is correct
        checkEph = checkEphemeris(Ephemeris);
        if(!checkEph)
            z = false;

        //send a request to the server
        sendRequest(Latitude, Longitude);
    }

    public void sendRequest(final double Latitude, final double Longitude){

        //--------------------------------!!!----------------------------
        //the content of this string depends on how the own web server is set
        //the server IP is always the one of the first line of the IP file
        String url = "http://" + IP [0] + "/node_modules/satellites-above/call.php?argument1=" + Latitude + "&argument2=" + Longitude;
        //--------------------------------!!!----------------------------

        final OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        //sends http request
        client.newCall(request).enqueue(new Callback() {

            //This method is executed if the server has not responded or there is no internet connection
            @Override
            public void onFailure (@NonNull Call call, @NonNull IOException e){
                e.printStackTrace();

                //This method allows you to change the current view from another thread
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        //if there is an internet connection the server does not respond
                        if(isNetworkAvailable()) {
                            tvAnswerText = "Server did not respond! Please press Try Again!";
                            tvAnswer.setTextSize(14);
                            tvAnswer.setText(tvAnswerText);
                            tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                        }

                        //if there is no internet connection
                        else{
                            if(checkEph)
                                tvAnswerText = "Device is not connected to Internet! Activate a valid connection and press Try Again!";
                            else
                                tvAnswerText = "Ephemeris are not correct, use an Internet connection for better accuracy!";
                            tvAnswer.setTextSize(14);
                            tvAnswer.setText(tvAnswerText);
                            tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                        }

                        //Try again button listener
                        bTryAgain.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        readFile();
                                    }
                                });
                                tryAgain(Latitude, Longitude);
                                tvAnswerText = "";
                                tvAnswer.setTextSize(21);
                                tvAnswer.setText(tvAnswerText);
                                bTryAgain.setOnClickListener(null); //deactivate the clickListener
                            }});
                    }
                });
            }

            //run when device receives a http response
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    final String myResponse = response.body().string();
                    Analyze.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvSuppSatText = "Supp. Vis.:\n";
                            tvSatText = "Visible:\n";
                            tvCNText = "C/N0:\n";
                            tvResultText = "Result:\n";
                            tvIdSatNavText = "ID:\n";
                            tvNavMessIsOkText = "Raw Data:\n";
                            tvResultNavText = "Result:\n";

                            //it parses the server response to get the id, azimuth and elevation of the supposedly visible satellites
                            for(int c=0; c < myResponse.length(); c++){
                                if(myResponse.charAt(c) == 'r'){
                                    c = c +4;
                                    if(myResponse.charAt(c) == '0')
                                        listOfSuppVisibleSat[iterator] [0] = Character.getNumericValue(myResponse.charAt(c+1));
                                    else
                                        listOfSuppVisibleSat[iterator] [0] = Integer.parseInt(myResponse.substring(c, c+2));
                                }

                                if(myResponse.charAt(c) == 'h'){
                                    c = c +3;
                                    int i = c;
                                    while((myResponse.charAt(i) != '.'))
                                        i++;
                                    listOfSuppVisibleSat[iterator] [1] = Integer.parseInt(myResponse.substring(c, i));
                                }

                                if(myResponse.charAt(c) == 'o'){
                                    c = c + 4;
                                    int i = c;
                                    while((myResponse.charAt(i) != '.'))
                                        i++;
                                    listOfSuppVisibleSat[iterator] [2] = Integer.parseInt(myResponse.substring(c, i));
                                    iterator ++;
                                }
                            }

                            //check if there are enough satellites
                            if (dimensionListOfVisibleSat > 3) {

                                /*this variable is used to add in the Visible list all the satellites
                                that are visible but do not have a counterpart among the supposed visible ones ==> error*/
                                int [] [] lovs = new int[32][6];
                                for (int i=0; i< dimensionListOfVisibleSat; i++) {
                                    lovs[i][0] = listOfVisibleSat[i][0];
                                    lovs[i][1] = listOfVisibleSat[i][1];
                                    lovs[i][2] = listOfVisibleSat[i][2];
                                    lovs[i][3] = listOfVisibleSat[i][3];
                                }

                                /*this variable is used to add in the Supp. Vis. list all the satellites
                                that are supposed visible but do not have a counterpart among the visible ones ==> OK*/
                                int [] [] losvs = new int[32] [3];
                                for(int i=0; i<iterator; i++){
                                    losvs[i][0] = listOfSuppVisibleSat[i] [0];
                                    losvs[i][1] = listOfSuppVisibleSat[i] [1];
                                    losvs[i][2] = listOfSuppVisibleSat[i] [2];
                                }

                                //check if every element of ListOfVisibleSat is also present in ListOfSuppVisibleSat
                                for (pointerListOfVisibleSat = 0; pointerListOfVisibleSat < dimensionListOfVisibleSat; pointerListOfVisibleSat++) {
                                    b = false;

                                    for (pointerListOfSuppVisibleSat = 0; pointerListOfSuppVisibleSat < iterator; pointerListOfSuppVisibleSat++)
                                    {
                                        if (listOfVisibleSat[pointerListOfVisibleSat] [0] == listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [0]){
                                            lovs[pointerListOfVisibleSat] [0] = 0;
                                            losvs[pointerListOfSuppVisibleSat] [0] = 0;

                                            tvSatText = tvSatText + listOfVisibleSat[pointerListOfVisibleSat] [0] + ": " + listOfVisibleSat[pointerListOfVisibleSat] [1] + "° " + listOfVisibleSat[pointerListOfVisibleSat] [2] + "°\n";
                                            tvSuppSatText = tvSuppSatText + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [0] + ": " + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [1]+ "° " + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [2] + "°\n";
                                            tvCNText = tvCNText + listOfVisibleSat[pointerListOfVisibleSat] [3] + "\n";
                                            tvSat.setText(tvSatText);
                                            tvSuppSat.setText(tvSuppSatText);
                                            tvCN.setText(tvCNText);

                                            /*(suppVisibleAzimuth-3 < visibleAzimuth < suppVisibleAzimuth+3) module 360 AND
                                            * (suppVisibleElevation-3 < visibleElevation < suppVisibleElevation+3) module 360 AND
                                            *
                                            * These data lack a scientific basis but by experience they work*/
                                            if(checkOrbit(listOfVisibleSat[pointerListOfVisibleSat] [1], listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [1]) &&
                                                    checkOrbit(listOfVisibleSat[pointerListOfVisibleSat] [2], listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [2])){

                                                /*This type of control is quite useless. In the event of a spoofing attack, the AGC gain usually increases and this drastically
                                                 *decreases C / N0 making the control useless. To perform an effective check, the C / N0 should be analyzed in relation to the AGC.
                                                 *Unfortunately, at the time of development of this application, almost all Android devices do not allow reading of the AGC values.*/
                                                if((listOfVisibleSat[pointerListOfVisibleSat] [3] < 70)) {
                                                    tvResultText = tvResultText + "OK!" + "\n";
                                                    tvResult.setText(tvResultText);
                                                    b = true;
                                                }
                                                else
                                                {
                                                    tvCN.setTextColor(Color.parseColor("#d60000")); //red colour
                                                    tvResultText = tvResultText + "X" + "\n";
                                                    tvResult.setText(tvResultText);
                                                }
                                            }
                                            else
                                            {
                                                tvSat.setTextColor(Color.parseColor("#d60000")); //red colour
                                                tvResultText = tvResultText + "X" + "\n";
                                                tvResult.setText(tvResultText);
                                            }
                                        }
                                    }
                                    if (!b)
                                        z = false;
                                }
                                boolean find; //find
                                for(pointerListOfVisibleSat = 0; pointerListOfVisibleSat < dimensionListOfVisibleSat; pointerListOfVisibleSat++) {

                                    find = false;
                                    tvIdSatNavText = tvIdSatNavText + listOfVisibleSat[pointerListOfVisibleSat][0] + "\n";
                                    tvIdSatNav.setText(tvIdSatNavText);

                                    if (listOfVisibleSat[pointerListOfVisibleSat][4] == 1 && listOfVisibleSat[pointerListOfVisibleSat][5] == 1 ) {
                                        for(int i = 0; i < numOfNavMessSat; i++){
                                            if(listOfNavMessSat[i] [0] == listOfVisibleSat [pointerListOfVisibleSat] [0]) {
                                                find = true;
                                                if(listOfNavMessSat[i] [6] == 0){  //Type = 1 ==> OK!
                                                    tvNavMessIsOkText = tvNavMessIsOkText + "YES" +"\n";
                                                    tvNavMessIsOk.setText(tvNavMessIsOkText);
                                                    Nav = true;
                                                }else{ //Type = 0 ==> X
                                                    tvNavMessIsOkText = tvNavMessIsOkText + "NO" +"\n";
                                                    tvNavMessIsOk.setText(tvNavMessIsOkText);
                                                    z = false;
                                                    Nav = false;
                                                    tvNavMessIsOk.setTextColor(Color.parseColor("#d60000")); //red colour
                                                }
                                            }
                                        }
                                        if(!find) { //the satellite should have the navigation messages but is absent
                                            tvNavMessIsOkText = tvNavMessIsOkText + "MISS" +"\n";
                                            tvNavMessIsOk.setText(tvNavMessIsOkText);
                                            Nav = true;
                                        }
                                    }
                                    else{ //the satellite has no ephemeris or almanac
                                        tvNavMessIsOkText = tvNavMessIsOkText + "NO" +"\n";
                                        tvNavMessIsOk.setText(tvNavMessIsOkText);
                                        z = false;
                                        Nav = false;
                                        tvNavMessIsOk.setTextColor(Color.parseColor("#d60000")); //red colour
                                    }

                                    //add the remaining visible satellites ==> Error!
                                    if (lovs[pointerListOfVisibleSat][0] != 0) {
                                        tvSatText = tvSatText + listOfVisibleSat[pointerListOfVisibleSat][0] + ": " + listOfVisibleSat[pointerListOfVisibleSat][1] + "° " + listOfVisibleSat[pointerListOfVisibleSat][2] + "°\n";
                                        tvCNText = tvCNText + listOfVisibleSat[pointerListOfVisibleSat][3] + "\n";
                                        tvSat.setText(tvSatText);
                                        tvCN.setText(tvCNText);
                                        tvResultText = tvResultText + "X" + "\n";
                                        tvResult.setText(tvResultText);
                                    }

                                    if(Nav){
                                        tvResultNavText = tvResultNavText + "OK!" + "\n";
                                        tvResultNav.setText(tvResultNavText);
                                    }
                                    else{
                                        tvResultNavText = tvResultNavText + "X" + "\n";
                                        tvResultNav.setText(tvResultNavText);
                                    }
                                }

                                //add the remaining supposed visible satellites ==> OK
                                for(pointerListOfSuppVisibleSat = 0; pointerListOfSuppVisibleSat < iterator; pointerListOfSuppVisibleSat++)
                                    if(losvs[pointerListOfSuppVisibleSat] [0] != 0){
                                        tvSuppSatText = tvSuppSatText + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [0] + ": " + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [1]+ "° " + listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [2] + "°\n";
                                        tvSuppSat.setText(tvSuppSatText);
                                    }


                                //all visible satellites from the device are correct
                                if (z) {
                                    tvAnswerText = "True Position!";
                                    tvAnswer.setText(tvAnswerText);
                                    tvAnswer.setTextColor(Color.parseColor("#009900")); //green colour
                                    tvResult.setTextColor(Color.parseColor("#009900")); //green colour
                                    tvResultNav.setTextColor(Color.parseColor("#009900")); //green colour
                                }

                                //there are one or more satellites that should not be seen by the device or data are not correctly saved
                                else {
                                    if(dataIsCorrupted()){
                                        tvAnswer.setTextSize(14);
                                        tvAnswerText = "Data is corrupt! Restart acquisition!";
                                        tvAnswer.setText(tvAnswerText);
                                    }
                                    else{
                                        if(checkEph)
                                            tvAnswerText = "False Position! Too many visible satellites or orbits of the satellites are not correct!";
                                        else
                                            tvAnswerText = "False Position! Ephemeris are not correct, please view Navigation Message.";
                                        tvAnswer.setTextSize(14);
                                        tvAnswer.setText(tvAnswerText);
                                    }

                                    tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                                    tvResult.setTextColor(Color.parseColor("#d60000")); //red colour
                                    tvResultNav.setTextColor(Color.parseColor("#d60000")); //red colour
                                }
                            }

                            //the device has detected less than 3 satellites
                            else{
                                tvAnswerText = "There are too few satellites for a correct evaluation!";
                                tvAnswer.setTextSize(14);
                                tvAnswer.setText(tvAnswerText);
                                tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                            }
                        }
                    });
                }
            }
        });
    }

    public boolean checkOrbit(int vis, int sup){
        switch(vis) {
            case 358:
                switch(sup){
                    case 0: return true;
                    case 359: return true;
                    case 358: return true;
                    case 357: return true;
                    case 356: return true;
                    default: return false;
                }

            case 359:
                switch(sup){
                    case 1: return true;
                    case 0: return true;
                    case 359: return true;
                    case 358: return true;
                    case 357: return true;
                    default: return false;
                }

            case 0:
                switch(sup){
                    case 2: return true;
                    case 1: return true;
                    case 0: return true;
                    case 359: return true;
                    case 358: return true;
                    default: return false;
                }

            case 1:
                switch(sup){
                    case 3: return true;
                    case 2: return true;
                    case 1: return true;
                    case 0: return true;
                    case 359: return true;
                    default: return false;
                }
            default:
                return (vis > sup - 3 && vis < sup + 3);

        }
    }


    //send to the server another request
    public void tryAgain(final double Latitude,final double Longitude){
            sendRequest(Latitude, Longitude);
    }

    //execute when user press back button
    public void openActivity1(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);  //return to the activity MainActivity
    }

    public boolean checkEphemeris(String Ephemeris){
        int numOfEph = 0;
        for(int i = 0; i < Ephemeris.length(); i++){

            /*Check the eccentricity of the satellite's orbit, this control should be useless if the device is connected to the internet*/
            if(Ephemeris.charAt(i) == 'e' && Ephemeris.charAt(i+1) == ':' && Ephemeris.charAt(i-1) != 'd' && Ephemeris.charAt(i-1) != 'o')
                if(Float.parseFloat(Ephemeris.substring(i + 3, i + 8)) > 0.03)
                    return false;

            if(Ephemeris.charAt(i) == 'c' && Ephemeris.charAt(i-1) == 'd'){
                i = i+3;
                int temp = i;
                while(Ephemeris.charAt(i) >= '0' && Ephemeris.charAt(i) <= '9')
                    i++;
                ephParam [numOfEph] [1] = Integer.parseInt(Ephemeris.substring(temp, i));

            }

            if(Ephemeris.charAt(i) == 'e' && Ephemeris.charAt(i-1) == 'd' && Ephemeris.charAt(i-2) == 'o' && Ephemeris.charAt(i-3) == 'i'){
                i = i+3;
                int temp = i;
                while(Ephemeris.charAt(i) >= '0' && Ephemeris.charAt(i) <= '9')
                    i++;
                ephParam [numOfEph] [2] = Integer.parseInt(Ephemeris.substring(temp, i));
            }

            if(Ephemeris.charAt(i) == 'c' && Ephemeris.charAt(i-1) == 'o' && Ephemeris.charAt(i-2) == 't') {
                int temp = i +3;
                while (Ephemeris.charAt(i) != '.')
                    i++;
                if(Integer.parseInt(Ephemeris.substring(temp, i)) > 604500)
                    return false;
                ephParam [numOfEph] [3] = Integer.parseInt(Ephemeris.substring(temp, i));
            }

            if(Ephemeris.charAt(i) == 'e' && Ephemeris.charAt(i-1) == 'o' && Ephemeris.charAt(i-2) == 't'){
                int temp = i +3;
                while (Ephemeris.charAt(i) != '.')
                    i++;
                if(Integer.parseInt(Ephemeris.substring(temp, i)) > 604784)
                    return false;
                ephParam [numOfEph] [4] = Integer.parseInt(Ephemeris.substring(temp, i));
            }

            if(Ephemeris.charAt(i) == 'k'){
                i = i + 3;
                int temp = i;
                while(Ephemeris.charAt(i) >= '0' && Ephemeris.charAt(i) <= '9')
                    i++;
                ephParam [numOfEph] [0] = Integer.parseInt(Ephemeris.substring(temp, i));
                numOfEph++;
            }
        }

        for(int i = 0; i < numOfEph; i++) {
            if (ephParam[0][0] != ephParam[i][0]) //Week must be the same for each satellite
                return false;
            if (ephParam[i][1] != ephParam [i] [2] || ephParam [i] [1] < 0 || ephParam [i] [1] > 1023) //Iodc must be the same as Iode and between 0 and 1023
                return false;
            if(ephParam [i] [3] !=ephParam [i] [4]) //toc and toe should normally be equal
                return false;
            for(int k = i; k < numOfEph; k++)
                if(ephParam [i] [3] < ephParam [k] [3] - 3600 || ephParam [i] [3] > ephParam [k] [3] +3600) //no acquisitions longer than one hour must be made
                    return false;
        }
        return true;
    }

    public void openActivity2(){
        Intent intent2 = new Intent(this, ViewNavMess.class);

        intent2.putExtra(EXTRA_NUMBER_11, listOfNavMessSat);
        intent2.putExtra(EXTRA_NUMBER_12, numOfNavMessSat);
        intent2.putExtra(EXTRA_NUMBER_13, Ephemeris);
        intent2.putExtra(EXTRA_NUMBER_14, rawData);
        intent2.putExtra(EXTRA_NUMBER_15, Location);
        intent2.putExtra(EXTRA_NUMBER_16, currentTime);
        intent2.putExtra(EXTRA_NUMBER_17, Ionosphere);

        startActivity(intent2);  //start the activity ViewNavMess
    }

    //At the first start it generates the IP file. In subsequent starts he only reads it
    public void readFile(){
        FileOutputStream fos = null;
        FileInputStream fis = null;

        try{
            fis = openFileInput("ListIP");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String temp = "";
            if((temp = br.readLine()) != null){
                sb.append(temp).append("\n");
                IP [0] = temp;
            }

        }catch (FileNotFoundException e){
            try{
                fos = openFileOutput("ListIP", MODE_PRIVATE);
                while(maxNum < 10){
                    insert = insert + IP[maxNum] + "\n";
                    maxNum++;
                }
                fos.write(insert.getBytes());


            }catch (FileNotFoundException c){
                c.printStackTrace();
            }catch (IOException c){
                c.printStackTrace();
            }finally {
                if(fos != null){
                    try{
                        fos.close();
                    } catch (IOException c){
                        c.printStackTrace();
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally{
            if(fis !=null){
                try{
                    fis.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    //allows you to change the target IP
    public void openActivity3(){
        Intent intent3 = new Intent(this, ChangeIP.class);
        startActivity(intent3);
    }

    //check if the device is connected to internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //if the cn field is equal to 0, the satellite data have not been recorded in time. The acquisition was terminated when the satellite data had not been fully saved.
    private boolean dataIsCorrupted(){
        for(int i =0; i < dimensionListOfVisibleSat; i++ )
            if(listOfVisibleSat[i] [3] == 0)
                return true;
            return false;
    }
}