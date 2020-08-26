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

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static long iteratorcounter=0;
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

        File file=new File("/storage/emulated/0/scatter/save.txt");
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
                //photo.setPosition(new Vector3(pos[0], pos[2], pos[1]));
                //photo.setRotation(new Quaternion(-rot[0], -rot[2], -rot[1], rot[3]));
                photo.setPosition(new Vector3(pos[0], pos[1], pos[2]));
                photo.setRotation(new Quaternion(rot[3], rot[0], rot[1], rot[2]));

                //Setting the screen coordinates of the pixels
                float[] x = JsonArraytoArray(jsonObect.getJSONArray("x"));
                float[] y = JsonArraytoArray(jsonObect.getJSONArray("y"));
                float[] pixelid = JsonArraytoArray(jsonObect.getJSONArray("index"));
                for (int i = 0; i < pixelid.length; i++) {
                    photo.pixel_locations.put((int) pixelid[i], new Vector3(x[i], y[i], 1));
                }
                photos.add(photo);
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
          //testing();
        ProcessPhotos();
        }
    });

    //Function to find the pixel world points
    private void ProcessPhotos(){
        double x_val[]=new double[100];
        double y_val[]=new double[100];
        double z_val[]=new double[100];
        Log.d("Photossize",String.valueOf(photos.size()));

        if(photos.size()<2) return;
        Log.d("Testing","test");

        int indexcount=0;
        for( int ID=0;ID<100;ID++){
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
            FunctionNtoM func = new Errorfunction(photos,ID,IDcount);
            UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
            optimizer.setFunction(func, null);
            //optimizer.setVerbose();
            optimizer.initialize(new double[]{10,11,10}, 1e-12, 1e-12);
            UtilOptimize.process(optimizer, 10000);

            if(optimizer.isConverged()){
                Log.d("Resultfinal_Conver","converged");
            }
            else{
                Log.d("Resultfinal_Conver","notconverged");
            }
            double found[] = optimizer.getParameters();

            x_val[indexcount]=found[0];
            y_val[indexcount]=found[1];
            z_val[indexcount]=found[2];
            indexcount++;
            Log.d("Resultfinal_itercount",String.valueOf(iteratorcounter));
            Log.d("Resultfinal_error", String.valueOf(optimizer.getFunctionValue()));
            Log.d("Resultfinal_x_y_z",String.valueOf(found[0])+","+String.valueOf(found[1])+","+String.valueOf(found[2])+","+String.valueOf(ID));
            iteratorcounter=0;
        }
        saveData(new File("/storage/emulated/0/scatter/worldpoints.txt"),x_val,y_val,z_val);
    }

    //Testing the LM algorithm using randomly generated data
    private void testing() {
        double a = 50;
        double b = 98;

        // randomly generate points along the line
        Random rand = new Random(230);
        List<Point2D> points = new ArrayList<Point2D>();
        Random rn=new Random();
        for (int i = 0; i < 200; i++) {
            //double x=rand.nextDouble()/Math.PI/4.0-Math.PI/8.0;
            double x=i*10;
            //double y=a*Math.cos(b*x)+b*Math.sin(a*x);//+rand.nextDouble()*0.1;
            double y=a*x+b;//+rand.nextDouble()*0.1;
            int num=rn.nextInt(10)-5;
            y+=num;
            Log.d("randomnoisevalue",String.valueOf(num));
            Point2D point2D=new Point2D();
            point2D.x=x;
            point2D.y=y;
            points.add(point2D);
        }

        FunctionNtoM func = new FunctionLineDistanceEuclidean(points);
        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
        optimizer.setFunction(func, null);
        optimizer.initialize(new double[]{0,0}, 1e-12, 1e-12);
        UtilOptimize.process(optimizer, 1000);
        double found[] = optimizer.getParameters();
        Log.d("Resultfinal_itercount",String.valueOf(iteratorcounter));
        Log.d("Resultfinal_error =:", String.valueOf(optimizer.getFunctionValue()));
        Log.d("Resultfinal_x",String.valueOf(found[0]));
        Log.d("Resultfinal_y",String.valueOf(found[1]));
        iteratorcounter=0;

    }


    //Testing the LM algorithm using randomly generated data with apache library
    private void testing_apcahe() {
        double a = 100;
        double b = 102;

        // randomly generate points along the line
        Random rand = new Random(230);
        List<Point2D> points = new ArrayList<Point2D>();

        for (int i = 0; i < 200; i++) {
            double x=rand.nextDouble()/Math.PI/4.0-Math.PI/8.0;
            double y=a*Math.cos(b*x)+b*Math.sin(a*x);//+rand.nextDouble()*0.1;
            Point2D point2D=new Point2D();
            point2D.x=x;
            point2D.y=y;
            points.add(point2D);
        }

        FunctionNtoM func = new FunctionLineDistanceEuclidean(points);
        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);
        optimizer.setFunction(func, null);
        optimizer.initialize(new double[]{90,96}, 1e-12, 1e-6);
        UtilOptimize.process(optimizer, 10000);
        double found[] = optimizer.getParameters();
        Log.d("Resultfinal_itercount",String.valueOf(iteratorcounter));
        Log.d("Resultfinal_error =:", String.valueOf(optimizer.getFunctionValue()));
        Log.d("Resultfinal_x",String.valueOf(found[0]));
        Log.d("Resultfinal_y",String.valueOf(found[1]));
        iteratorcounter=0;

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

    //Convert pixel screen coordinate to world space coordinates
    private Vector3 PixelToWorldSpace(float xIndex, float yIndex, float depth, Photos snapshot,int id){
        float[] projectionmat=snapshot.projection_mat;
        int width=snapshot.Width;
        int height=snapshot.Height;

        float zFar=100.0f;
        float zNear=0.01f;

        float corrected_depth=(zFar*zNear)/(zFar-depth*zFar+depth*zNear);
        corrected_depth=corrected_depth*2;

        float x=xIndex/(float)width;
        float y=yIndex/(float)height;
        x-=0.5f;
        y-=0.5f;

        x*=2.0f;
        y*=2.0f;

        x=x/(float)projectionmat[0];
        y=y/(float)projectionmat[5];
        Vector3 v=new Vector3();
        v.x=x*corrected_depth;
        v.y=y*corrected_depth;
        v.z=corrected_depth;

        Log.d("v.x",String.valueOf(v.x));
        Log.d("v.y",String.valueOf(v.y));
        Log.d("v.z",String.valueOf(v.z));

        Matrix t=new Matrix();
        t.makeTrs(snapshot.Position,snapshot.Rotation, Vector3.one());


        v=t.transformPoint(v);
        Log.d("tranf_v.x",String.valueOf(t.data[0])+","+String.valueOf(snapshot.Rotation.x)+","+String.valueOf(id));
        Log.d("tranf_v.y",String.valueOf(v.y));
        Log.d("tranf_v.z",String.valueOf(v.z));

        return v;
    }

    private double magnitude(Vector3 v){
        double x=Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z);
        return x;
    }


}






//
//            MultivariateJacobianFunction distancetopoint=new MultivariateJacobianFunction() {
//                @Override
//                public Pair<RealVector, RealMatrix> value(RealVector point) {
//                    Vector3 c=new Vector3((float)point.getEntry(0),(float)point.getEntry(1),(float)point.getEntry(2));
//                    RealVector value=new ArrayRealVector(idcount);
//                    RealMatrix jacobian=new Array2DRowRealMatrix(idcount,idcount);
//                    int index = 0;
//                    for (Photos photo : MainActivity.photos) {
//                        if (photo.pixel_locations.get(id) != null) {
//                            Vector3 v=photo.pixel_locations.get(id);
//                            Vector3 projected_point = PixelToWorldSpace(v.x, v.y, 0.05f, photo,id);
//
//                            Vector3 p = photo.Position;
//                            Vector3 d = Vector3.subtract(projected_point, photo.Position);
//
//                            Vector3 temp1 = (Vector3.cross(Vector3.subtract(c, p), d));
//                            double temp2 = magnitude(temp1) / magnitude(d);
//                            value.setEntry(index,temp2);
//                            jacobian.setEntry(index,0,(c.x)/temp2);
//                            jacobian.setEntry(index,1,(c.y)/temp2);
//                            jacobian.setEntry(index,2,(c.z)/temp2);
//                            //Log.d("Result_v",String.valueOf((v.x))+","+String.valueOf(v.y)+","+String.valueOf(v.z));
//
//                            //Log.d("Result_position",String.valueOf((photo.Position.x))+","+String.valueOf(photo.Position.y)+","+String.valueOf(photo.Position.z));
//
//                           // Log.d("Result_project",String.valueOf((projected_point.x))+","+String.valueOf(projected_point.y)+","+String.valueOf(projected_point.z));
//
//                            // Log.d("Result_d",String.valueOf((d.x))+","+String.valueOf(d.y)+","+String.valueOf(d.z));
//
//                            //index++;
//                        }
//                    }
//                    return new Pair<RealVector, RealMatrix>(value,jacobian);
//                }
//            };
//
//            double[] prescribeddistance=new double[IDcount];
//            Arrays.fill(prescribeddistance,0.0000001);
//            LeastSquaresProblem problem=new LeastSquaresBuilder().start(new double[]{0,0,0}).model(distancetopoint).target(prescribeddistance).lazyEvaluation(false).maxEvaluations(1000).maxIterations(1000).build();
//
//
//            LeastSquaresOptimizer.Optimum optimizer=new LevenbergMarquardtOptimizer().optimize(problem);
//            Vector3 fittedcenter=new Vector3((float)optimizer.getPoint().getEntry(0),(float)optimizer.getPoint().getEntry(1),(float)optimizer.getPoint().getEntry(2));
//            Log.d("Result_fittedpoint",String.valueOf(fittedcenter.x)+","+String.valueOf(fittedcenter.y)+","+String.valueOf(fittedcenter.z));
//            Log.d("Result_RMS",String.valueOf(optimizer.getRMS()));
//            x_val[indexcount]=fittedcenter.x;
//            y_val[indexcount]=fittedcenter.y;
//            z_val[indexcount]=fittedcenter.z;
//
//            indexcount++;


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

