package com.example.pixelreconstruction;

import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.Dictionary;
import java.util.Enumeration;
import java.io.Serializable;
import java.util.Hashtable;

public class Photos {
    public Dictionary<Integer, Vector3> pixel_locations=new Hashtable<>();

    public float[] projection_mat;
    public Vector3 Position;
    public Quaternion Rotation;

    public int Width;
    public int Height;

    public void setPosition(Vector3 position) {
        Position = position;
    }

    public void setRotation(Quaternion rotation) {
        Rotation = rotation;
    }

    public void setProjection_mat(float[] projection_mat) {
        this.projection_mat = projection_mat;
    }
}
