/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2014 David Harper at obliquity.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 */

package com.obliquity.astronomy;

import java.lang.Math;

public class Vector implements java.lang.Cloneable {
	public static final int X_AXIS = 1;
	public static final int Y_AXIS = 2;
	public static final int Z_AXIS = 3;
	
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
		return (Object) that;
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

	public double[] getComponents() {
		return toArray();
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
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
		return new Vector(this.y * that.z - this.z * that.y, this.z * that.x
				- this.x * that.z, this.x * that.y - this.y * that.x);
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
	
	public void linearCombination(Vector va, double xa, Vector vb, double xb) {
		this.x = xa * va.x + xb * vb.x;
		this.y = xa * va.y + xb * vb.y;
		this.z = xa * va.z + xb * vb.z;
	}
	
	public void rotate(double angle, int axis) {
		if (axis != X_AXIS && axis != Y_AXIS && axis != Z_AXIS)
			return;
		
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		
		double xNew, yNew, zNew;
		
		switch (axis) {
		case X_AXIS:
			yNew = y * cosAngle + z * sinAngle;
			zNew = -y * sinAngle + z * cosAngle;
			y = yNew;
			z = zNew;
			break;
			
		case Y_AXIS:
			zNew = z * cosAngle + x * sinAngle;
			xNew = -z * sinAngle + x * cosAngle;
			z = zNew;
			x = xNew;
			break;
			
		case Z_AXIS:
			xNew = x * cosAngle + y * sinAngle;
			yNew = -x * sinAngle + y * cosAngle;
			x = xNew;
			y = yNew;
			break;
		}
	}

	public java.lang.String toString() {
		return "[" + x + ", " + y + ", " + z + "]";
	}

	public java.lang.String prettyPrint(java.text.DecimalFormat fmt) {
		return "[" + fmt.format(x) + ", " + fmt.format(y) + ", "
				+ fmt.format(z) + "]";
	}
}
