package com.example.pixelreconstruction;


import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;



import java.util.ArrayList;

import java.util.List;

import java.util.Random;

public class Point2D {
    // define a line in 2D space as the tangent from the origin

    double x,y,z;



    public Point2D(double x, double y,double z) {

        this.x = x;
        this.y = y;
        this.z=   z;

    }



    public Point2D() {

    }
}
