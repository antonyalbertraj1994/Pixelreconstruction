package com.example.pixelreconstruction;

import android.util.Log;

import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;

import org.ddogleg.optimization.functions.FunctionNtoM;

import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.ArrayList;
import java.util.List;



    /**

     * Computes the distance a point is from a line in 2D.

     * The line is defined using the tangent from origin equation.

     *

     * @author Peter Abeles

     */

    public class FunctionLineDistanceEuclidean implements FunctionNtoM {
        // Data which the line is being fit too
        int id;

        public FunctionLineDistanceEuclidean(int id) {
            this.id=id;
            //this.data = data;
        }

        /**

         * Number of parameters used to define the line.

         */

        @Override
        public int getNumOfInputsN() {

//            int index = 0;
//            for (Photos photo : MainActivity.photos) {
//                Vector3 v = new Vector3();
//                if (photo.pixel_locations.get(this.id) != null) {
//                    index++;
//                }
//            }
            return 3;
        }



        /**

         * Number of output error functions.  Two for each point.

         */

        @Override

        public int getNumOfOutputsM() {

//            int index = 0;
//            for (Photos photo : MainActivity.photos) {
//                Vector3 v = new Vector3();
//                if (photo.pixel_locations.get(this.id) != null) {
//                    index++;
//                }
//            }
            return 3;
        }


        @Override
        public void process(double[] input, double[] output) {
            Log.d("process_size",String.valueOf(input.length)+","+String.valueOf(output.length));
            int index = 0;
            for (Photos photo : MainActivity.photos) {
                if (photo.pixel_locations.get(this.id) != null) {
                    Vector3 v=photo.pixel_locations.get(this.id);
                    Vector3 projected_point = PixelToWorldSpace(v.x, v.y, 0.05f, photo,this.id);

                    Vector3 p = photo.Position;
                    Vector3 d = Vector3.subtract(projected_point, photo.Position);
                    Vector3 c = new Vector3((float) input[0], (float) input[1], (float) input[2]);

                    Vector3 temp1 = (Vector3.cross(Vector3.subtract(c, p), d));
                    double temp2 = magnitude(temp1) / magnitude(d);
                    output[index] = temp2;
                    Log.d("Result_v",String.valueOf((v.x))+","+String.valueOf(v.y)+","+String.valueOf(v.z));

                    Log.d("Result_position",String.valueOf((photo.Position.x))+","+String.valueOf(photo.Position.y)+","+String.valueOf(photo.Position.z));

                    Log.d("Result_project",String.valueOf((projected_point.x))+","+String.valueOf(projected_point.y)+","+String.valueOf(projected_point.z));

                   // Log.d("Result_d",String.valueOf((d.x))+","+String.valueOf(d.y)+","+String.valueOf(d.z));

                    index++;
                }
            }
            // tangent equation

//            // compute the residual error for each point in the data set
//
//            for( int i = 0; i < data.size(); i++ ) {
//
//                Point2D p = data.get(i);
//
//                double t = slopeX * ( p.x - lineX ) + slopeY * ( p.y - lineY );
//
//                t /= slopeX * slopeX + slopeY * slopeY;
//
//
//
//                double closestX = lineX + t*slopeX;
//
//                double closestY = lineY + t*slopeY;
//
//
//
//                output[i*2]   = p.x-closestX;
//
//                output[i*2+1] = p.y-closestY;
//
//            }
            Log.d("Result_errorfunction",String.valueOf(index));
        }

        private double magnitude(Vector3 v){
            double x=Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z);
            return x;
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


            v=t.transformPoint(v);
            Log.d("tranf_v.x",String.valueOf(t.data[0])+","+String.valueOf(snapshot.Rotation.x)+","+String.valueOf(id));
            Log.d("tranf_v.y",String.valueOf(v.y));
            Log.d("tranf_v.z",String.valueOf(v.z));

            return v;
        }
    }

