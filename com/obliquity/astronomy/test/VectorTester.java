package com.obliquity.astronomy.test;

import java.text.DecimalFormat;

import com.obliquity.astronomy.Vector;

public class VectorTester {
	public static void main(String args[]) {
		DecimalFormat format = new DecimalFormat("0.000000");
		format.setPositivePrefix(" ");

		Vector a = new Vector(1.0, 2.0, 3.0);
		Vector b = new Vector(1.0, 4.0, 9.0);

		System.out.println("a=" + a);
		System.out.println("b=" + b);

		System.out.println();

		System.out.println("a.magnitude() => " + a.magnitude());

		System.out.println();

		Vector c = (Vector) a.clone();

		System.out.println("c = a.clone() => " + c);

		c = new Vector(a);

		System.out.println("c = new Vector(a) => " + c);

		c.clear();

		System.out.println("c.clear() => " + c);

		c.copy(a);

		System.out.println("c.copy(a) => " + c);

		System.out.println();

		c.add(b);

		System.out.println("c.add(b) => " + c);

		c.subtract(b);

		System.out.println("c.subtract(b) => " + c);

		System.out.println();

		System.out.println("c.scalarProduct(b) => " + c.scalarProduct(b));

		Vector d = c.vectorProduct(b);

		System.out.println();

		System.out.println("d = c.vectorProduct(b) => " + d);

		System.out.println("d.scalarProduct(b) => " + d.scalarProduct(b));
		System.out.println("d.scalarProduct(c) => " + d.scalarProduct(c));

	}
}
