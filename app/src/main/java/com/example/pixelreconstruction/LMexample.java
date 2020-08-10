package com.example.pixelreconstruction;

import android.util.Log;

import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LMexample {

    double lineX = -3.9;
    double lineY = 1.3;



    // randomly generate points along the line

    Random rand = new Random(234);
    List<Point2D> points = new ArrayList<Point2D>();

    public void optimize() {
        for (int i = 0; i < 20; i++) {
            double t = (rand.nextDouble() - 0.5) * 10;
            points.add(new Point2D(lineX + t * lineY, lineY - t * lineX,10));
        }


        // Define the function being optimized and create the optimizer
        FunctionNtoM func = new FunctionLineDistanceEuclidean(0);
        UnconstrainedLeastSquares<DMatrixRMaj> optimizer = FactoryOptimization.levenbergMarquardt(null, true);


        // Send to standard out progress information


        // if no jacobian is specified it will be computed numerically

        optimizer.setFunction(func, null);


        // provide it an extremely crude initial estimate of the line equation

        optimizer.initialize(new double[]{-0.7, 0.132}, 1e-12, 1e-12);


        // iterate 500 times or until it converges.

        // Manually iteration is possible too if more control over is required

        UtilOptimize.process(optimizer, 500);


        double found[] = optimizer.getParameters();


        // see how accurately it found the solution

        Log.d("Final Error =:", String.valueOf(optimizer.getFunctionValue()));


        // Compare the actual parameters to the found parameters

        Log.d("Actual line",String.valueOf(lineX)+","+String.valueOf(found[0]));
        Log.d("Actual lineY",String.valueOf(lineY)+","+String.valueOf(found[1]));


    }

}
