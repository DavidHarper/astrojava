package com.obliquity.astronomy;

import java.lang.Math;

public class ThreeVector {
    public double x = 0.0, y = 0.0, z = 0.0;

    public ThreeVector() {
	this(0.0, 0.0, 0.0);
    }

    public ThreeVector(double X, double Y, double Z) {
	x = X;
	y = Y;
	z = Z;
    }

    public ThreeVector(double[] V) {
	this(V[0], V[1], V[2]);
    }

    public double magnitude() {
	return Math.sqrt(x * x + y * y + z * z);
    }

    public void normalise() {
	double r = magnitude();

	if (r == 0.0)
	    return;

	x /= r;
	y /= r;
	z /= r;
    }

    public double[] toArray() {
	double[] V = new double[3];
	V[0] = x;
	V[1] = y;
	V[2] = z;
	return V;
    }

    public void toArray(double[] V) {
	V[0] = x;
	V[1] = y;
	V[2] = z;
    }

    public void setComponents(double[] V) {
	x = V[0];
	y = V[1];
	z = V[2];
    }
}
