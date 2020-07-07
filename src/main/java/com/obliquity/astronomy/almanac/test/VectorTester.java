/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2018 David Harper at obliquity.com
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

package com.obliquity.astronomy.almanac.test;

import java.text.DecimalFormat;

import com.obliquity.astronomy.almanac.Vector;

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
