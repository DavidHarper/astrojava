package com.obliquity.astronomy.test;

import java.io.*;
import java.text.DecimalFormat;

import com.obliquity.astronomy.Matrix;
import com.obliquity.astronomy.Vector;

public class MatrixTester {
    public static void main(String args[]) {
	DecimalFormat format = new DecimalFormat("0.000000");
	format.setPositivePrefix(" ");
	Matrix a = new Matrix();

	System.out.println("a = new Matrix() =>\n" + a.prettyPrint(format));

	System.out.println();

	a = Matrix.getIdentityMatrix();

	System.out.println("a = Matrix.getIdentityMatrix() =>\n" + a.prettyPrint(format));

	System.out.println("det(a) => " + a.determinant());

	System.out.println();

	double angle = Math.PI/3.0;

	Matrix ax = Matrix.getRotationMatrix(Matrix.X_AXIS, angle);
	System.out.println("ax = Matrix.getRotationMatrix(X_AXIS, " +
			   format.format(angle) + ") =>\n" +
			   ax.prettyPrint(format));
	System.out.println("det(ax) => " + ax.determinant());

	System.out.println();

	Matrix ay = Matrix.getRotationMatrix(Matrix.Y_AXIS, angle);
	System.out.println("ay = Matrix.getRotationMatrix(Y_AXIS, " +
			   format.format(angle) + ") =>\n" +
			   ay.prettyPrint(format));
	System.out.println("det(ay) => " + ay.determinant());

	System.out.println();

	Matrix az = Matrix.getRotationMatrix(Matrix.Z_AXIS, angle);
	System.out.println("az = Matrix.getRotationMatrix(Z_AXIS, " +
			   format.format(angle) + ") =>\n" +
			   az.prettyPrint(format));
	System.out.println("det(ax) => " + az.determinant());

	System.out.println();

	System.out.println("Testing rightMultiplyBy");

	a = new Matrix(ax);

	a.rightMultiplyBy(ay);
	a.rightMultiplyBy(az);

	System.out.println("ax x ay x az =>\n" +
			   a.prettyPrint(format));

	System.out.println("det(ax . ay . az) => " + a.determinant());

	System.out.println();

	System.out.println("testing leftMultiplyBy");

	a = new Matrix(az);

	a.leftMultiplyBy(ay);
	a.leftMultiplyBy(ax);

	System.out.println("ax x ay x az =>\n" +
			   a.prettyPrint(format));

	System.out.println("det(ax . ay . az) => " + a.determinant());

	System.out.println();

	System.out.println("Testing matrix-vector multiplication");

	Vector v = new Vector(1.0, 2.0, 3.0);

	System.out.println("v=" + v.prettyPrint(format));

	System.out.println("mag(v) => " + v.magnitude());

	v.multiplyBy(a);

	System.out.println("Rotated v=" + v.prettyPrint(format));

	System.out.println("mag(v) => " + v.magnitude());

	System.out.println();

	System.out.println("Testing matrix transpose");

	System.out.println("Before:\n" + a.prettyPrint(format) + "\n");

	a.transpose();

	System.out.println("After:\n" + a.prettyPrint(format) + "\n");
    }
}
