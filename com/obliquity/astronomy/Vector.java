package com.obliquity.astronomy;

import java.lang.Math;

public class Vector implements java.lang.Cloneable {
    protected double x = 0.0, y = 0.0, z = 0.0;

    public Vector() {
	this(0.0, 0.0, 0.0);
    }

    public Vector(double X, double Y, double Z) {
	x = X;
	y = Y;
	z = Z;
    }

    public Vector(Vector that) {
	copy(that);
    }

    public Vector(double[] V) {
	this(V[0], V[1], V[2]);
    }

    public void copy(Vector that) {
	this.x = that.x;
	this.y = that.y;
	this.z = that.z;
    }

    public Object clone() {
	Vector that = new Vector(this);
	return (Object)that;
    }

    public Vector copyOf() {
	return new Vector(this);
    }

    public void clear() {
	x = y = z = 0.0;
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

    public void subtract(Vector that) {
	this.x -= that.x;
	this.y -= that.y;
	this.z -= that.z;
    }

    public void add(Vector that) {
	this.x += that.x;
	this.y += that.y;
	this.z += that.z;
    }

    public double scalarProduct(Vector that) {
	return this.x * that.x + this.y * that.y + this.z * that.z;
    }

    public Vector vectorProduct(Vector that) {
	return new Vector(this.y * that.z - this.z * that.y,
			  this.z * that.x - this.x * that.z,
			  this.x * that.y - this.y * that.x);
    }

    public void multiplyBy(double factor) {
	this.x *= factor;
	this.y *= factor;
	this.z *= factor;
    }

    public void multiplyBy(Matrix matrix) {
	double[][] m = matrix.getComponents();

	double xNew = m[0][0] * x + m[0][1] * y + m[0][2] * z;
	double yNew = m[1][0] * x + m[1][1] * y + m[1][2] * z;
	double zNew = m[2][0] * x + m[2][1] * y + m[2][2] * z;

	x = xNew;
	y = yNew;
	z = zNew;
    }

    public java.lang.String toString() {
	return "[" + x + ", " + y + ", " + z + "]";
    }

    public java.lang.String prettyPrint(java.text.DecimalFormat fmt) {
	return "[" + fmt.format(x) + ", " + fmt.format(y) + ", " + fmt.format(z) + "]";
    }
}
