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

import java.io.IOException;

//these libraries are used to make the http communication
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Analyze extends AppCompatActivity {

    private Button bTryAgain;

    private TextView tvCN;
    private TextView tvSat;
    private TextView tvSuppSat;
    private TextView tvResult;
    private TextView tvIdSatNav;
    private TextView tvHasEphemeris;
    private TextView tvHasAlmanac;
    private TextView tvResultNav;
    private String tvCNText;
    private String tvSatText;
    private String tvSuppSatText;
    private String tvResultText;
    private String tvIdSatNavText;
    private String tvHasEphemerisText;
    private String tvHasAlmanacText;
    private String tvResultNavText;
    private TextView tvAnswer;
    private String tvAnswerText;

    private int[] [] listOfVisibleSat;
    private int[] [] listOfSuppVisibleSat;

    private int pointerListOfSuppVisibleSat, dimensionListOfVisibleSat, pointerListOfVisibleSat, iterator = 0;
    private boolean b = false, z = true, Nav = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analize);

        bTryAgain = findViewById(R.id.bTryAgain);
        Button bReturn = findViewById(R.id.bReturn);

        //return button, return to activity MainActivity
        bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity1();
            }});

        TextView tvVariable;
        String tvVariableText;

        tvSat = findViewById(R.id.tvSat);
        tvSuppSat= findViewById(R.id.tvSuppSat);
        tvAnswer = findViewById(R.id.tvAnsw);
        tvCN = findViewById(R.id.tvCN);
        tvResult = findViewById(R.id.tvResult);
        tvVariable = findViewById(R.id.tvVariable);
        tvIdSatNav = findViewById(R.id.tvIdSatNav);
        tvHasEphemeris = findViewById(R.id.tvHasEphemeris);
        tvHasAlmanac = findViewById(R.id.tvHasAlmanac);
        tvResultNav = findViewById(R.id.tvResultNav);

        //read the parameters that have been passed from MainActivity
        Intent intent = getIntent();
        listOfVisibleSat = (int[][]) intent.getSerializableExtra(MainActivity.EXTRA_NUMBER_1);
        listOfSuppVisibleSat = new int[32][3];
        dimensionListOfVisibleSat = intent.getIntExtra(MainActivity.EXTRA_NUMBER_2, 0);
        double Longitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_3, 0);
        double Latitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_4, 0);


        //show longitude and latitude
        tvVariableText = tvVariable.getText() +  "Longitude: " + Longitude + "\n" + "Latitude: " + Latitude;
        tvVariable.setText(tvVariableText);

        //send a request to the server
        sendRequest(Latitude, Longitude);
    }

    public void sendRequest(final double Latitude, final double Longitude){

        //--------------------------------!!!----------------------------
        //the content of this string depends on how the own web server is set
        String url = "http://79.44.189.146/node_modules/satellites-above/call.php?argument1=" + Latitude + "&argument2=" + Longitude;
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
                            tvAnswerText = "Device is not connected to the internet! Activate a valid connection and press Try Again!";
                            tvAnswer.setTextSize(14);
                            tvAnswer.setText(tvAnswerText);
                            tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                        }

                        //Try again button listener
                        bTryAgain.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                tryAgain(Latitude, Longitude);
                                tvAnswerText = "";
                                tvAnswer.setTextSize(21);
                                tvAnswer.setText(tvAnswerText);
                                bTryAgain.setOnClickListener(null); //deactivate the clickListener
                            }});
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {   //run when device receives a http response
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
                            tvHasEphemerisText = "Ephemeris:\n";
                            tvHasAlmanacText = "Almanac:\n";
                            tvResultNavText = "Result:\n";

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
                                int [] [] lovs = new int[32][6];
                                for (int i=0; i< dimensionListOfVisibleSat; i++) {
                                    lovs[i][0] = listOfVisibleSat[i][0];
                                    lovs[i][1] = listOfVisibleSat[i][1];
                                    lovs[i][2] = listOfVisibleSat[i][2];
                                    lovs[i][3] = listOfVisibleSat[i][3];
                                }

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

                                            if(((listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [1] - 2) < listOfVisibleSat[pointerListOfVisibleSat] [1]) &&
                                                    (listOfVisibleSat[pointerListOfVisibleSat] [1] < listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [1] +2) &&
                                                    ((listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [2] - 2) < listOfVisibleSat[pointerListOfVisibleSat] [2]) &&
                                                    (listOfVisibleSat[pointerListOfVisibleSat] [2] < listOfSuppVisibleSat[pointerListOfSuppVisibleSat] [2] +2)){
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

                                for(pointerListOfVisibleSat = 0; pointerListOfVisibleSat < dimensionListOfVisibleSat; pointerListOfVisibleSat++) {

                                    tvIdSatNavText = tvIdSatNavText + listOfVisibleSat[pointerListOfVisibleSat][0] + "\n";
                                    tvIdSatNav.setText(tvIdSatNavText);

                                    if (listOfVisibleSat[pointerListOfVisibleSat][4] == 1) {
                                        tvHasEphemerisText = tvHasEphemerisText + "YES" +"\n";
                                        tvHasEphemeris.setText(tvHasEphemerisText);
                                        Nav = true;
                                    }
                                    else{
                                        tvHasEphemerisText = tvHasEphemerisText + "NO" +"\n";
                                        tvHasEphemeris.setText(tvHasEphemerisText);
                                        z = false;
                                        Nav = false;
                                        tvHasEphemeris.setTextColor(Color.parseColor("#d60000")); //red colour
                                    }

                                    if(listOfVisibleSat[pointerListOfVisibleSat][5] == 1) {
                                        tvHasAlmanacText = tvHasAlmanacText + "YES" + "\n";
                                        tvHasAlmanac.setText(tvHasAlmanacText);
                                        Nav = true;
                                    }
                                    else{
                                        tvHasAlmanacText = tvHasAlmanacText + "NO" + "\n";
                                        tvHasAlmanac.setText(tvHasAlmanacText);
                                        z = false;
                                        Nav = false;
                                        tvHasAlmanac.setTextColor(Color.parseColor("#d60000")); //red colour
                                    }
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
                                //there are one or more satellites that should not be seen by the device
                                else {
                                    if(dataIsCorrupted()){
                                        tvAnswerText = "Data is corrupt! Restart acquisition!";
                                        tvAnswer.setText(tvAnswerText);
                                    }
                                    else{
                                    tvAnswerText = "False Position!";
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

    //send to the server another request
    public void tryAgain(final double Latitude,final double Longitude){
            sendRequest(Latitude, Longitude);
    }

    //execute when user press back button
    public void openActivity1(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);  //return to the activity MainActivity
    }

    //Check if the device is connected to internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean dataIsCorrupted(){
        for(int i =0; i < dimensionListOfVisibleSat; i++ )
            if(listOfVisibleSat[i] [2] == 0)
                return true;
            return false;
    }
}