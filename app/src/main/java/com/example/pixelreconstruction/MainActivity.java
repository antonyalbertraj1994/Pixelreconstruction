package com.example.pixelreconstruction;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static List<Photos> photos=new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photos=readjsondata(); // Read the values from the json file
        Thread p=new Thread(processworld_points); // Thread to process the 3d world points using LM algorithm
        p.start();
    }

    //Read the the values from json file
    private List<Photos> readjsondata(){
        List<Photos> photos=new ArrayList<>();

        File file=new File("/storage/emulated/0/scatter/calibmap.txt");
        try {

            FileReader fileReader=new FileReader(file);
            BufferedReader bufferedReader=new BufferedReader(fileReader);
            StringBuilder stringBuilder=new StringBuilder();
            String line=bufferedReader.readLine();

            while (line!=null) {
                Log.d("Jsonresponse", line);
                JSONObject jsonObect = new JSONObject(line);
                line = bufferedReader.readLine();

                //Setting the parameter of the photo object using the values from the JSON file
                Photos photo = new Photos();
                photo.Width = Integer.valueOf(jsonObect.getString("width"));
                photo.Height = Integer.valueOf(jsonObect.getString("height"));
                photo.setProjection_mat(JsonArraytoArray(jsonObect.getJSONArray("projectionmatrix")));
                float[] pos = JsonArraytoArray(jsonObect.getJSONArray("translation"));
                float[] rot = JsonArraytoArray(jsonObect.getJSONArray("quaternion"));
                photo.setPosition(new Vector3(pos[0], pos[2], pos[1]));
                photo.setRotation(new Quaternion(-rot[0], -rot[2], -rot[1], rot[3]));

                //Setting the screen coordinates of the pixels
                float[] x = JsonArraytoArray(jsonObect.getJSONArray("x"));
                float[] y = JsonArraytoArray(jsonObect.getJSONArray("y"));
                float[] pixelid = JsonArraytoArray(jsonObect.getJSONArray("index"));
                for (int i = 0; i < pixelid.length; i++) {
                    photo.pixel_locations.put((int) pixelid[i], new Vector3(x[i], y[i], 0));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return photos;
    }

    //Helper function to convert JSONArray to Normal Array
    private float[] JsonArraytoArray(JSONArray JsonArray){
        float[] array=new float[JsonArray.length()];
        for(int i=0;i<JsonArray.length();i++){
            try {
                array[i]=Float.valueOf(JsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    //Separate thread to run the LM algorithm and process the pixel world points
    Thread processworld_points=new Thread(new Runnable() {
        @Override
        public void run() {
        ProcessPhotos();
        }
    });

    //Function to find the pixel world points
    private void ProcessPhotos(){
        double x_val[]=new double[4];
        double y_val[]=new double[4];
        double z_val[]=new double[4];

        if(photos.size()<2) return;

        int indexcount=0;
        for(int ID=0;ID<100;ID++){
            int IDcount=0;
            for(Photos photo: photos){
                Vector3 v;
                int value;
                if(photo.pixel_locations.get(ID)!=null){
                    IDcount++;
                }
            }

            //Process only if there is more than two data points
            if(IDcount<=1) continue;

            Log.d("PixelID",String.valueOf(ID));
            // Define the function being optimized and create the optimizer
            FunctionNtoM func = new FunctionLineDistanceEuclidean(ID);
            UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
            optimizer.setFunction(func, null);

            double parameter[]=new double[3];
            parameter[0]=0;
            parameter[1]=0;
            parameter[2]=0;

            optimizer.initialize(new double[]{0,0,0}, 1e-6, 1e-6);
            UtilOptimize.process(optimizer, 10000);
            double found[] = optimizer.getParameters();

            x_val[indexcount]=found[0];
            y_val[indexcount]=found[1];
            z_val[indexcount]=found[2];
            indexcount++;

            Log.d("Resultfinal_error =:", String.valueOf(optimizer.getFunctionValue()));
            Log.d("Resultfinal_x",String.valueOf(found[0]));
            Log.d("Resultfinal_y",String.valueOf(found[1]));
            Log.d("Resultfinal_z",String.valueOf(found[2]));
        }
        saveData(new File("/storage/emulated/0/scatter/worldpoints.txt"),x_val,y_val,z_val);
    }

    //Store the 3D world points in a text file for visualization
    private void saveData(File filename,double[] x_val,double[] y_val,double[] z_val){

        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(filename);
            bw = new BufferedWriter(fw);
            for(int i=0;i<4;i++){
                bw.write(String.valueOf(x_val[i])+",");
                bw.write(String.valueOf(y_val[i])+",");
                bw.write(String.valueOf(z_val[i]));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}








//
//    private List<Photos> getcalibrationdata(){
//        List<Photos> photos=new ArrayList<>();
//
//        Photos photo1=new Photos();
//        photo1.Height=1920;
//        photo1.Width=1080;
//        photo1.pixel_locations.put(1,new Vector3(1029,536,0));
//        photo1.pixel_locations.put(2,new Vector3(680,439,0));
//        photo1.pixel_locations.put(3,new Vector3(779,260,0));
//        photo1.pixel_locations.put(4,new Vector3(909,455,0));
//        //photo1.pixel_locations.put(5,new Vector3(500,100,0));
//        //photo1.Position.set(1.0f,2.0f,3.0f);
//        float rot1_1=-0.63439536f,rot1_2=-0.12825696f,rot1_3=-0.14462604f,rot1_4=0.7484491f;
//        float pos1_1=-0.052626025f,pos1_2=-0.012273932f,pos1_3=-0.065040484f;
//
//        photo1.setPosition(new Vector3(pos1_1,pos1_3,pos1_2));
//        photo1.setRotation(new Quaternion(-rot1_1,-rot1_3,-rot1_2,rot1_4));
//
//        photo1.setProjection_mat(new float[]{3.06f,0.0f,0.0f,0.0f,0.0f,1.5327487f,0.0f,0.0f,-0.0036391115f,-0.0073796613f,-1.0002f,-1.0f,0.0f,0.0f,-0.020002f,0.0f});
//
//        Photos photo2=new Photos();
//        photo2.Height=1920;
//        photo2.Width=1080;
//        photo2.pixel_locations.put(1,new Vector3(1151,452,0));
//        photo2.pixel_locations.put(2,new Vector3(811,541,0));
//        photo2.pixel_locations.put(3,new Vector3(809,333,0));
//        photo2.pixel_locations.put(4,new Vector3(1018,444,0));
//        float rot2_1=-0.56096107f,rot2_2=-0.25094187f,rot2_3=-0.2563247f,rot2_4=0.7460888f;
//        float pos2_1=-0.15765007f,pos2_2=0.03148797f,pos2_3=-0.109593764f;
//
//        //photo2.pixel_locations.put(5,new Vector3(600,200,0));
//        photo2.setPosition(new Vector3(pos2_1,pos2_3,pos2_2));
//        photo2.setRotation(new Quaternion(-rot2_1,-rot2_3,-rot2_2,rot2_4));
//
//        photo2.setProjection_mat(new float[]{3.06f,0.0f,0.0f,0.0f,0.0f,1.5327487f,0.0f,0.0f,-0.0036391115f,-0.0073796613f,-1.0002f,-1.0f,0.0f,0.0f,-0.020002f,0.0f});
//
//
//        Photos photo3=new Photos();
//        photo3.Height=1920;
//        photo3.Width=1080;
//        photo3.pixel_locations.put(1,new Vector3(1323,682,0));
//        photo3.pixel_locations.put(2,new Vector3(1026,426,0));
//        photo3.pixel_locations.put(3,new Vector3(1214,293,0));
//        photo3.pixel_locations.put(4,new Vector3(1244,558,0));
//        //photo3.pixel_locations.put(5,new Vector3(800,200,0));
//        float rot3_1=-0.56902885f,rot3_2=-0.21606617f,rot3_3=-0.18233879f,rot3_4=0.7721878f;
//        float pos3_1=0.09597497f,pos3_2=0.013976254f, pos3_3=-0.22470102f;
//
//        photo3.setPosition(new Vector3(pos3_1,pos3_3,pos3_2));
//        photo3.setRotation(new Quaternion(-rot3_1,-rot3_3,-rot3_2,rot3_4));
//        photo3.setProjection_mat(new float[]{3.06f,0.0f,0.0f,0.0f,0.0f,1.5327487f,0.0f,0.0f,-0.0036391115f,-0.0073796613f,-1.0002f,-1.0f,0.0f,0.0f,-0.020002f,0.0f});
//
//        photos.add(photo1);
//        photos.add(photo2);
//        photos.add(photo3);
//        return photos;
//    }

//    private void optimize(){
//        double lineX = -3.9;
//        double lineY = 1.3;
//
//
//
//        // randomly generate points along the line
//
//        Random rand = new Random(234);
//        List<Point2D> points = new ArrayList<Point2D>();
//
//        for (int i = 0; i < 2; i++) {
//            double t = (rand.nextDouble() - 0.5) * 10;
//            points.add(new Point2D(lineX + t * lineY, lineY - t * lineX,10.00));
//        }
//
//        // Define the function being optimized and create the optimizer
//        FunctionNtoM func = new FunctionLineDistanceEuclidean();
//        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
//
//        optimizer.setFunction(func, null);
//
//        double parameter[]=new double[3];
//        parameter[0]=0;
//        parameter[1]=0;
//        parameter[2]=0;
//
//        optimizer.initialize(new double[]{parameter[0],parameter[1],parameter[2]}, 1e-12, 1e-12);
//        UtilOptimize.process(optimizer, 500);
//        double found[] = optimizer.getParameters();
//
//        Log.d("Final Error =:", String.valueOf(optimizer.getFunctionValue()));
//        Log.d("Actual line",String.valueOf(lineX)+","+String.valueOf(found[0]));
//        Log.d("Actual lineY",String.valueOf(lineY)+","+String.valueOf(found[1]));
//
//    }

