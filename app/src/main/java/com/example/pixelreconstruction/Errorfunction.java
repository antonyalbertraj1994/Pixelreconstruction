package com.example.pixelreconstruction;

import android.util.Log;

import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;

import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

public class Errorfunction implements FunctionNtoM {
    List<Photos> photos;
    int id,IDCount;
    public Errorfunction(List<Photos> photos,int id,int IDcount) {
        this.photos = photos;
        this.id=id;
        this.IDCount=IDcount;
    }


    @Override
    public void process(double[] input, double[] output) {
        MainActivity.iteratorcounter++;
        int index = 0;
        for (Photos photo : photos) {
            if (photo.pixel_locations.get(id) != null) {
                Vector3 v = photo.pixel_locations.get(id);
                Vector3 projected_point = PixelToWorldSpace(v.x, v.y, 0.05f, photo, id);

                Vector3 p = photo.Position;
                Vector3 d = Vector3.subtract(projected_point, photo.Position);
                Vector3 c = new Vector3((float) input[0], (float) input[1], (float) input[2]);

                Vector3 temp1 = (Vector3.cross(Vector3.subtract(c, p), d));
                double temp2 = magnitude(temp1) / magnitude(d);
                output[index] = temp2;
                Log.d("Result_v", String.valueOf((v.x)) + "," + String.valueOf(v.y) + "," + String.valueOf(v.z));

                Log.d("Result_position", String.valueOf((photo.Position.x)) + "," + String.valueOf(photo.Position.y) + "," + String.valueOf(photo.Position.z));

                Log.d("Result_project", String.valueOf((projected_point.x)) + "," + String.valueOf(projected_point.y) + "," + String.valueOf(projected_point.z));

                // Log.d("Result_d",String.valueOf((d.x))+","+String.valueOf(d.y)+","+String.valueOf(d.z));

                index++;
            }
        }
    }

    @Override
    public int getNumOfInputsN() {
        return 3;
    }

    @Override
    public int getNumOfOutputsM() {
        return IDCount;
    }

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


        Vector3 v1=t.transformPoint(v);
        Log.d("tranf_v.x",String.valueOf(t.data[0])+","+String.valueOf(snapshot.Rotation.x)+","+String.valueOf(id));
        Log.d("tranf_v.y",String.valueOf(v.y));
        Log.d("tranf_v.z",String.valueOf(v.z));

        return v1;
    }

    private double magnitude(Vector3 v){
        double x=Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z);
        return x;
    }


}
