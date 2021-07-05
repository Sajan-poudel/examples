package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btn_camera, btn_folder;
    ImageView imageView;
    TextView txtOCR, timerTxt, imageDetails;
    private String[] filenameArr = {"licensed_car0.jpeg", "licensed_car103.jpeg", "licensed_car121.jpeg", "licensed_car16.jpeg", "licensed_car202.jpeg", "licensed_car236.jpeg", "licensed_car4.jpeg", "licensed_car46.jpeg", "licensed_car79.jpeg", "big.jpg", "karnataka.jpeg","bulgaria.jpg"};
    private String filename = "licensed_car16.jpeg";
    TessBaseAPI tessBaseAPI;
    public static final String TESS_DATA = "/tessdata/";
    private int desireImageHeight = 64;
    private int[] desireHeightArr = {32, 40, 64, 128};
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private String FileName = "eng.traineddata";
    private int imageHeight, imageWidth;
    String csv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_camera = findViewById(R.id.Btn_camera);
        btn_folder = findViewById(R.id.Btn_folder);
        imageView = findViewById(R.id.imageDisplay);
        txtOCR = findViewById(R.id.TxtOCr);
        timerTxt = findViewById(R.id.timerTxt);
        imageDetails = findViewById(R.id.image_details);
        Bitmap bitmap = loadBitmapFromAssets(getBaseContext(), filename, desireImageHeight);
        String details = "original  width :" + imageWidth + "  height : " + imageHeight + "\n" + "reduced  width : " + bitmap.getWidth() + "  height : " + bitmap.getHeight();
        imageDetails.setText(details);
        imageView.setImageBitmap(bitmap);
        long start = System.nanoTime();
        String str = getOcrText(bitmap);
        long end = System.nanoTime();
        timerTxt.setText("time taken : " +  ((end - start)/1000000.0) + "milliseconds");
        txtOCR.setText(str);
        csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCsvFile.csv");
        csvAppend();

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetectorActivity.class);
                startActivity(intent);
            }
        });

        btn_folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Photo_folder_activity.class);
                startActivity(intent);
            }
        });
    }

    protected Bitmap loadBitmapFromAssets(Context context, String name, int desireImageHeight){
        try{
            InputStream in = context.getAssets().open(name);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            imageHeight = bitmap.getHeight();
            imageWidth = bitmap.getWidth();
            float factor = imageWidth/(imageHeight * 1.0f);
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(factor * desireImageHeight), desireImageHeight, true);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getOcrText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.setDebug(true);
        tessBaseAPI.setVariable("load_system_dawg", "F");
        tessBaseAPI.setVariable("load_freq_dawg", "F");
        tessBaseAPI.setVariable("load_punc_dawg", "F");
        tessBaseAPI.setVariable("load_number_dawg", "F");
        tessBaseAPI.setVariable("load_unambig_dawg", "F");
        tessBaseAPI.setVariable("load_bigram_dawg", "F");
        tessBaseAPI.setVariable("load_fixed_length_dawgs", "F");
        tessBaseAPI.init(dataPath, "eng");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890abcdefg"+"hijklmnopqrstuvwxyz"+"ABCDEFGHIJKLMNO"+"PQRSTUVWXYZ");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-=[]}{" + ";:'\"\\|~`,./<>?");
        tessBaseAPI.setImage(bitmap);
        String returnString = "no result";
        try {
            returnString = tessBaseAPI.getUTF8Text();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnString;
    }

    private void prepareTesData(){
        try {
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if(!dir.mkdir())
                    Toast.makeText(getApplicationContext(), "the folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
            }
            String pathToDataFile = dir + "/" + FileName;
            if(!(new File(pathToDataFile)).exists()){
                InputStream in = getAssets().open(FileName);
                OutputStream out = new FileOutputStream(pathToDataFile);
                byte [] buffer = new byte[1024];
                int len;
                while((len = in.read(buffer)) > 0){
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void csvAppend(){
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(csv));
            List<String[]> data = new ArrayList<String[]>();
            Bitmap bt;
            String s;
            data.add(new String[]{"fileName", "originalWidth", "originalHeight", "reducedWidth", "reducedHeight", "detectedOcr"});
            for(int dh : desireHeightArr){
                for(String fname : filenameArr){
                    bt = loadBitmapFromAssets(getBaseContext(), fname, dh);
                    s = getOcrText(bt);
                    data.add(new String[]{
                            fname, String.valueOf(imageWidth), String.valueOf(imageHeight), String.valueOf(bt.getWidth()), String.valueOf(bt.getHeight()), s
                    });
                }
            }
            writer.writeAll(data);
            writer.close();
        } catch (Exception e) {
            Log.d("csv", "cannot create csv file");
            e.printStackTrace();
        }
    }
}