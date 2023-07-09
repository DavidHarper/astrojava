package com.obliquity.astronomy.almanac.test;

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

import com.obliquity.astronomy.almanac.chebyshev.Chebyshev;
import com.obliquity.astronomy.almanac.chebyshev.Evaluatable;

public class TestChebyshev {
	public static void main(String[] args) {
		int N = 20;
		
		if (args.length > 0)
			N = Integer.parseInt(args[0]);
		
		TestChebyshev tester = new TestChebyshev();
		
		tester.run(N);
	}
	
	public void run(int N) {		
		testFunction(new Sine(0.5 * Math.PI), "sine function (half-cycle)", N);
				
		testFunction(new Cosine(0.5 * Math.PI), "cosine function (half-cycle)", N);
				
		testFunction(new Sine(Math.PI), "sine function (full-cycle)", N);
		
		testFunction(new Cosine(Math.PI), "cosine function (full-cycle)", N);
		
		testFunction(new Sine(2.0 * Math.PI), "sine function (double-cycle)", N);

		testFunction(new Cosine(2.0 * Math.PI), "cosine function (double-cycle)", N);
	}
	
	private void testFunction(Evaluatable e, String name, int N) {
		double[] T = new double[N];
		double[] coeffs = new double[N];

		System.out.println("\nFUNCTION: " + name);
		
		Chebyshev.calculateChebyshevCoefficients(e, coeffs);
		
		System.out.println("\nCoefficients:");
		
		for (int i = 0; i < coeffs.length; i++)
			System.out.printf("a_%d = %17.14f\n", i, coeffs[i]);
		
		System.out.println("\nTesting accuracy of approximation:");
		
		double dx = 1.0/32.0;
		
		for (double x = -1.0; x <= 1.0; x += dx) {
			Chebyshev.calculateChebyshevPolynomials(x, T);
			
			double approx = coeffs[0];
		
			for (int i = 1; i < coeffs.length; i++)
				approx += coeffs[i] * T[i];
			
			double exact = e.evaluate(x);
			
			double error = approx - exact;
			
			System.out.printf("x = %17.14f : %17.14f vs %17.14f  [%17.14f]\n", x, exact, approx, error);
		}
	}
	
	public class Sine implements Evaluatable {
		private final double factor;
		
		public Sine(double factor) {
			this.factor = factor;
		}
		
		public double evaluate(double argument) {
			return Math.sin(factor * argument);
		}
	}
	
	public class Cosine implements Evaluatable {
		private final double factor;
		
		public Cosine(double factor) {
			this.factor = factor;
		}
		
		public double evaluate(double argument) {
			return Math.cos(factor * argument);
		}
	}

}
