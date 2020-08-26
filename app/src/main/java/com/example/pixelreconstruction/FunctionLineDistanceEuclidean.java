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
        List<Point2D> data;

        public FunctionLineDistanceEuclidean(List<Point2D> point2D) {
            //this.id=id;
            this.data = point2D;
        }

        /**

         * Number of parameters used to define the line.

         */
        @Override
        public int getNumOfInputsN() {
            return 2;
        }


        /**

         * Number of output error functions.  Two for each point.

         */

        @Override

        public int getNumOfOutputsM() {
            return data.size();
        }


        @Override
        public void process(double[] input, double[] output) {
            MainActivity.iteratorcounter++;
            Log.d("process_size",String.valueOf(input.length)+","+String.valueOf(output.length));
            for(int i=0;i<data.size();i++){
                double y=input[0]*data.get(i).x+input[1];//+rand.nextDouble()*0.1;

                //double y=input[0]*Math.cos(input[1]*data.get(i).x)+input[1]*Math.sin(input[0]*data.get(i).x);
                output[i]=data.get(i).y-y;
            }
        }

        private double magnitude(Vector3 v){
            double x=Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z);
            return x;
        }


    }

