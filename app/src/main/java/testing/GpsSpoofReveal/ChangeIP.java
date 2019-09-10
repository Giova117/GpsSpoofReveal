package testing.GpsSpoofReveal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class ChangeIP extends AppCompatActivity {

    private String IP = null;
    private String [] actIPtext = new String [10];
    private String IPtext = "";

    private AutoCompleteTextView actIP;
    private Button bSaveIP;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_ip);
        readFile();

        actIP = findViewById(R.id.actIP);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, actIPtext);
        actIP.setAdapter(adapter);

        bSaveIP = findViewById(R.id.bSaveIP);

        //inserts the new IP
        bSaveIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IPtext = String.valueOf(actIP.getText());
                if(checkIP(IPtext) && IPtext.compareTo("1.1.1.1") != 0){
                    actIP.setText("IP: " + IPtext + " saved!");
                    IPtext = IPtext + "\n";
                    for(int i=0; i < 9; i++){
                        IPtext = IPtext + actIPtext[i] + "\n";
                    }
                    writeFile();
                }else
                    actIP.setText("Not a valid IP!");
            }
        });

    }

    //reads the IP file
    public void readFile(){
        FileInputStream fis = null;
        int maxNum = 0;

        try{
            fis = openFileInput("ListIP");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            while(((IP = br.readLine()) != null) && (maxNum < 10)){
                sb.append(IP).append("\n");
                actIPtext [maxNum] = IP;
                maxNum ++;
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
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

    //save the IP entered in the IP file (maximum 10 IP ==> with each new IP the last one is deleted)
    public void writeFile(){
        FileOutputStream fos = null;
        try{
            fos = openFileOutput("ListIP", MODE_PRIVATE);
            fos.write(IPtext.getBytes());
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
    }

    //check if the entered IP is valid
    boolean checkIP (String IP){
        int temp1 = 0, temp2 = 0, temp3, i=0;
        while(i < IP.length()){
            while(IP.charAt(i)>= '0' && IP.charAt(i) <= '9' ){
                i++;
                temp1++;
                if(i == IP.length() && temp2 == 3 && temp1 >= 1 && temp1 <=3) {
                    temp3 = Integer.parseInt(IP.substring(i - temp1));
                    return temp3 <= 255;
                }
                if(i == IP.length())
                    return false;
            }
            if(IP.charAt(i) == '.' && temp1 >= 1 && temp1 <=3 && temp2 < 4){
                temp3 = Integer.parseInt(IP.substring(i - temp1, i));
                if(temp3 > 255)
                    return false;
                temp1 = 0;
                temp2++;
                i++;
            }else{
                return false;
            }
        }
        return false;
    }


}
