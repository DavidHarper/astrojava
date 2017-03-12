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

package com.obliquity.astronomy.almanac;

import java.lang.Math;
import java.lang.IndexOutOfBoundsException;

public class Matrix {
	protected double[][] m = new double[3][3];

	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;

	public Matrix() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				m[i][j] = 0.0;
	}

	public Matrix(Matrix that) {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				this.m[i][j] = that.m[i][j];
	}

	public static Matrix getIdentityMatrix() {
		Matrix id = new Matrix();
		id.m[0][0] = id.m[1][1] = id.m[2][2] = 1.0;
		return id;
	}

	public static Matrix getRotationMatrix(int axis, double angle)
			throws IndexOutOfBoundsException {
		if (axis < X_AXIS || axis > Z_AXIS)
			throw new IndexOutOfBoundsException();

		double c = Math.cos(angle);
		double s = Math.sin(angle);

		Matrix rotate = Matrix.getIdentityMatrix();

		switch (axis) {
		case X_AXIS:
			rotate.setDiagonal(1, c);
			rotate.setDiagonal(2, c);
			rotate.setAntiSymmetricComponent(2, 1, s);
			break;

		case Y_AXIS:
			rotate.setDiagonal(0, c);
			rotate.setDiagonal(2, c);
			rotate.setAntiSymmetricComponent(0, 2, s);
			break;

		case Z_AXIS:
			rotate.setDiagonal(0, c);
			rotate.setDiagonal(1, c);
			rotate.setAntiSymmetricComponent(1, 0, s);
			break;
		}

		return rotate;
	}

	public void setComponent(int i, int j, double value) {
		m[i][j] = value;
	}

	public double getComponent(int i, int j) {
		return m[i][j];
	}

	public void setDiagonal(int i, double value) {
		m[i][i] = value;
	}

	public void setAntiSymmetricComponent(int i, int j, double value) {
		m[i][j] = value;
		m[j][i] = -value;
	}

	public void setComponents(double[][] values) {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				m[i][j] = values[i][j];
	}

	public double[][] getComponents() {
		double[][] values = new double[3][3];

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				values[i][j] = m[i][j];

		return values;
	}

	public double determinant() {
		return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) + m[0][1]
				* (m[1][2] * m[2][0] - m[1][0] * m[2][2]) + m[0][2]
				* (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
	}

	public void leftMultiplyBy(Matrix that) {
		double[][] values = new double[3][3];

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				values[i][j] = that.m[i][0] * this.m[0][j] + that.m[i][1]
						* this.m[1][j] + that.m[i][2] * this.m[2][j];

		setComponents(values);
	}

	public void rightMultiplyBy(Matrix that) {
		double[][] values = new double[3][3];

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				values[i][j] = this.m[i][0] * that.m[0][j] + this.m[i][1]
						* that.m[1][j] + this.m[i][2] * that.m[2][j];

		setComponents(values);
	}

	public void transpose() {
		double temp;

		temp = m[0][1];
		m[0][1] = m[1][0];
		m[1][0] = temp;

		temp = m[1][2];
		m[1][2] = m[2][1];
		m[2][1] = temp;

		temp = m[0][2];
		m[0][2] = m[2][0];
		m[2][0] = temp;
	}

	public void setToIdentityMatrix() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				m[i][j] = (i == j) ? 1 : 0;
	}

	public java.lang.String toString() {
		return "[" + "[" + m[0][0] + ", " + m[0][1] + ", " + m[0][2] + "], "
				+ "[" + m[1][0] + ", " + m[1][1] + ", " + m[1][2] + "], " + "["
				+ m[2][0] + ", " + m[2][1] + ", " + m[2][2] + "], " + "]";
	}

	public java.lang.String prettyPrint() {
		return "[" + m[0][0] + ", " + m[0][1] + ", " + m[0][2] + "]\n" + "["
				+ m[1][0] + ", " + m[1][1] + ", " + m[1][2] + "]\n" + "["
				+ m[2][0] + ", " + m[2][1] + ", " + m[2][2] + "]";
	}

	public java.lang.String prettyPrint(java.text.DecimalFormat fmt) {
		return "[" + fmt.format(m[0][0]) + ", " + fmt.format(m[0][1]) + ", "
				+ fmt.format(m[0][2]) + "]\n" + "[" + fmt.format(m[1][0])
				+ ", " + fmt.format(m[1][1]) + ", " + fmt.format(m[1][2])
				+ "]\n" + "[" + fmt.format(m[2][0]) + ", "
				+ fmt.format(m[2][1]) + ", " + fmt.format(m[2][2]) + "]";
	}
}
