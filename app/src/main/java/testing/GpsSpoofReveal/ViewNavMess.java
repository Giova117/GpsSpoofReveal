package testing.GpsSpoofReveal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.io.File;
import java.io.FileOutputStream;

public class ViewNavMess extends AppCompatActivity {

    private Button bSave;
    private TextView tvNavMess;
    private String tvNavMessText = "";


    private String Ephemerids;
    private String Ionosphere;
    private int numOfNavMessSat;
    private int [] [] listOfNavMessSat;
    private String [] [] rawData;

    private int Svid, start;
    private int [] satHasMess = new int[32];
    private int satHasMessLength = 0;
    private boolean find = false;
    private String Location;

    private String file = "GpsSpoofReveal";
    private String nameFile;
    private String currentTime;

    private final int MY_PERMISSIONS_REQUEST_WRITE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_nav_mess);

        bSave = findViewById(R.id.bSave);
        tvNavMess = findViewById(R.id.tvNavMess);

        //reads the parameters passed from the "Analyze" activity
        Intent intent = getIntent();

        listOfNavMessSat = (int[][]) intent.getSerializableExtra(Analyze.EXTRA_NUMBER_11);
        numOfNavMessSat = intent.getIntExtra(Analyze.EXTRA_NUMBER_12, 0);
        Ephemerids = intent.getStringExtra(Analyze.EXTRA_NUMBER_13);
        rawData = (String[][]) intent.getSerializableExtra(Analyze.EXTRA_NUMBER_14);
        Location = intent.getStringExtra(Analyze.EXTRA_NUMBER_15);
        currentTime = intent.getStringExtra(Analyze.EXTRA_NUMBER_16);
        Ionosphere = intent.getStringExtra(Analyze.EXTRA_NUMBER_17);

        //check permissions to access the device memory
        if(!(checkPermission())) {
            askPermisison();}

        //current date, first line of the result
        tvNavMessText = "" + currentTime + "\n" + Location + "\n";
        Log.d("giovanni", Ionosphere);
        if(!(Ionosphere.equals("null")))
            tvNavMessText = tvNavMessText + "\n" + "Ionosphere data:\n" + Ionosphere + "\n";

        parseResult();

        //generates a file name based on the current date
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        nameFile = "NavMess" + timeStamp + "_.txt";

        //save the file in the device memory
        bSave.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WorldReadableFiles")
            @Override
            public void onClick(View view) {
                writeFile(view);
            }});
    }

    /*This class parses the ephemeris, the raw data and the list containing the ids of the subframe of each visible satellite.
    * Standard format 1:
    *       ...
    * Satellite ID: +++
    * Ephemeris: +++
    * Raw Data: +++
    *       ...
    *This for each satellite, the satellites for which the navigation message could not be downloaded will have only raw data.
    * Standard format 2:
    *       ...
    * Satellite ID: +++
    * Raw Data: +++
    *       ...         */
    public void parseResult(){

        //generate standard format 1
        start = 1;
        if(Ephemerids.charAt(1) != ']') {
            for (int iter = 1; iter < Ephemerids.length(); iter++) {
                if (Ephemerids.charAt(iter) == 'p') {
                    iter = iter + 6;
                    if ('0' <= Ephemerids.charAt(iter) && Ephemerids.charAt(iter) <= '9') {
                        Svid = Integer.parseInt(Ephemerids.substring(iter - 1, iter + 1));
                        satHasMess[satHasMessLength++] = Svid;
                    } else {
                        Svid = Character.getNumericValue(Ephemerids.charAt(iter - 1));
                        satHasMess[satHasMessLength++] = Svid;
                    }

                    tvNavMessText = tvNavMessText + "\nSatellite ID: " + Svid + "\n";
                }

                if (Ephemerids.charAt(iter) == ',' || Ephemerids.charAt(iter) == ']') {
                    tvNavMessText = tvNavMessText + Ephemerids.substring(start, iter);
                    for (int i = 0; i < numOfNavMessSat; i++) {
                        if (listOfNavMessSat[i][0] == Svid) {
                            for (int k = 0; k < numOfNavMessSat; k++) {
                                if (listOfNavMessSat[i][0] == Integer.parseInt(rawData[k][0])) {
                                    for (int j = 1; j < 6; j++)
                                        if (listOfNavMessSat[i][j] == 1) {
                                            tvNavMessText = tvNavMessText + "\nSubmessageID: " + j + "\n" + rawData[k][j];
                                        }
                                }
                            }
                            tvNavMessText = tvNavMessText + "\n\n";
                            start = iter + 1;
                            break;
                        }
                    }
                }
            }
        }else{
            tvNavMessText = tvNavMessText + "\nSubmessages 1,2 and 3 are needed to get the navigation message!!!\n";
        }

        //generate standard format 2
        tvNavMessText = tvNavMessText + "\n\n---Raw Data without Navigation Messages---\n";
        for(int i = 0; i < numOfNavMessSat; i++){
            find = false;
            for(int k = 0; k < satHasMessLength; k++)
                if(listOfNavMessSat[i] [0] == satHasMess [k])
                    find = true;

            if(!find){
                for(int k = 0; k < numOfNavMessSat; k++){
                    if(listOfNavMessSat[i] [0] == Integer.parseInt(rawData[k] [0])){
                        tvNavMessText = tvNavMessText + "\n\nSatellite ID: " + listOfNavMessSat[i] [0];
                        for(int j = 1; j <6; j++)
                            if(listOfNavMessSat[i][j] == 1){
                                tvNavMessText = tvNavMessText + "\nSubmessageID: " + j + "\n" + rawData[k] [j];
                            }
                    }
                }
            }
        }
        tvNavMessText = tvNavMessText + "\n\n";
        tvNavMess.setText(tvNavMessText);
    }

    //check if the memory is available
    private boolean isExternalStorageWritable(){
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    //generates a new folder if it does not already exist and saves the file inside it
    public void writeFile(View v){
        if(isExternalStorageWritable() && checkPermission()){
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath() + "/" + file);
            if(!Dir.exists()){
                Dir.mkdir();
            }

            File textFile = new File(Dir, nameFile);

            try {
                FileOutputStream fos = new FileOutputStream(textFile);
                fos.write(tvNavMessText.getBytes());
                fos.close();

                Toast.makeText(getBaseContext(), "File Saved at " + file + "/" + nameFile, Toast.LENGTH_LONG).show();
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Toast.makeText(getBaseContext(), "Cannot Write to External Storage ", Toast.LENGTH_LONG).show();
        }
    }

    //verifies the access permissions to the device memory
    public boolean checkPermission(){
        return(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    //requires access permissions to the device memory
    public void askPermisison(){
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },MY_PERMISSIONS_REQUEST_WRITE);
    }
}
