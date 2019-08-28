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

    private TextView tvResult;
    private String tvResultText;
    private TextView tvAnswer;
    private String tvAnswerText;

    private int[] listOfVisibleSat;
    private int[] listOfSuppVisibleSat;

    private int pointerListOfSuppVisibleSat, dimensionListOfVisibleSat, pointerListOfVisibleSat, iterator = 0;
    private boolean b = false, z = true;

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
        TextView tvSat;
        String tvSatText = "Visible:\n";

        tvResult = findViewById(R.id.tvResult);
        tvAnswer = findViewById(R.id.tvAnsw);
        tvVariable = findViewById(R.id.tvVariable);
        tvSat = findViewById(R.id.tvSat);

        tvSat.setText(tvSatText);

        //read the parameters that have been passed from MainActivity
        Intent intent = getIntent();
        listOfVisibleSat = intent.getIntArrayExtra(MainActivity.EXTRA_NUMBER_1);
        listOfSuppVisibleSat = new int[31];
        dimensionListOfVisibleSat = intent.getIntExtra(MainActivity.EXTRA_NUMBER_2, 0);
        double Longitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_3, 0);
        double Latitude = intent.getDoubleExtra(MainActivity.EXTRA_NUMBER_4, 0);

        //show the list of visible satellites from the device
        for(pointerListOfSuppVisibleSat =0; pointerListOfSuppVisibleSat < dimensionListOfVisibleSat; pointerListOfSuppVisibleSat++)
            tvSat.append("" + listOfVisibleSat[pointerListOfSuppVisibleSat] + ", ");

        //show longitude and latitude
        tvVariableText = "Longitude: " + Longitude + "\n" + "Latitude: " + Latitude;
        tvVariable.setText(tvVariableText);

        //send a request to the server
        sendRequest(Latitude, Longitude);
    }

    public void sendRequest(final double Latitude, final double Longitude){

        //--------------------------------!!!----------------------------
        //the content of this string depends on how the own web server is set
        String url = "http://82.57.203.78/node_modules/satellites-above/call.php?argument1=" + Latitude + "&argument2=" + Longitude;
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
                            tvAnswerText = "The server did not respond! Please press Try Again!";
                            tvAnswer.setTextSize(14);
                            tvAnswer.setText(tvAnswerText);
                            tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
                        }

                        //if there is no internet connection
                        else{
                            tvAnswerText = "The device is not connected to the internet! Activate a valid connection and press Try Again!";
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
                            String[] sat = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32"};
                            tvResultText = "Supp. Vis.:\n";
                            tvResult.setText(tvResultText);

                            //check which satellites should be visible from the device
                            for (pointerListOfSuppVisibleSat = 0; pointerListOfSuppVisibleSat < 32; pointerListOfSuppVisibleSat++) {
                                if (myResponse.contains(sat[pointerListOfSuppVisibleSat])) {
                                    tvResult.append("" + (pointerListOfSuppVisibleSat + 1) + ", ");
                                    listOfSuppVisibleSat[iterator] = (pointerListOfSuppVisibleSat + 1);
                                    iterator++;
                                }
                            }

                            //check if there are enough satellites
                            if (dimensionListOfVisibleSat > 3) {

                                //check if every element of ListOfVisibleSat is also present in ListOfSuppVisibleSat
                                for (pointerListOfVisibleSat = 0; pointerListOfVisibleSat < dimensionListOfVisibleSat; pointerListOfVisibleSat++) {
                                    b = false;
                                    for (pointerListOfSuppVisibleSat = 0; pointerListOfSuppVisibleSat < iterator; pointerListOfSuppVisibleSat++) {
                                        if (listOfVisibleSat[pointerListOfVisibleSat] == listOfSuppVisibleSat[pointerListOfSuppVisibleSat])
                                            b = true;
                                    }
                                    if (!b)
                                        z = false;
                                }

                                //all visible satellites from the device are correct
                                if (z) {
                                    tvAnswerText = "True Position!";
                                    tvAnswer.setText(tvAnswerText);
                                    tvAnswer.setTextColor(Color.parseColor("#009900")); //green colour
                                }
                                //there are one or more satellites that should not be seen by the device
                                else {
                                    tvAnswerText = "False Position!";
                                    tvAnswer.setText(tvAnswerText);
                                    tvAnswer.setTextColor(Color.parseColor("#d60000")); //red colour
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
}