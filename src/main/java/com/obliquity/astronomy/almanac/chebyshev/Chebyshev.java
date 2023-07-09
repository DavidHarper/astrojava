package com.obliquity.astronomy.almanac.chebyshev;

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

public class Chebyshev {
	public static void calculateChebyshevPolynomials(double x, double[] T) {
		if (T == null)
			throw new IllegalArgumentException("null passed when array expected");
		
		T[0] = 1.0;
		
		if (T.length > 1)
			T[1] = x;
		
		if (T.length > 2) {
			for (int j = 2; j < T.length; j++)
				T[j] = 2.0 * x * T[j-1] - T[j-2];
		}
	}
	
	public static void calculateChebyshevZeroes(double[] zeroes) {
		if (zeroes == null)
			throw new IllegalArgumentException("null passed where array expected");
		
		int N = zeroes.length;
		
		double piBy2N = Math.PI/((double)(2 * N));
		
		for (int i = 0; i < N; i++)
			zeroes[i] = Math.cos((double)(2 * i + 1) * piBy2N);
	}
	
	public static void calculateChebyshevCoefficients(Evaluatable e, double[] coeffs) {
		if (e == null)
			throw new IllegalArgumentException("null passed where Evaluatable expected");
		
		if (coeffs == null)
			throw new IllegalArgumentException("null passed where array expected");
		
		int N = coeffs.length;
		
		double[] zeroes = new double[N];
		
		calculateChebyshevZeroes(zeroes);
		
		double[][] T = new double[N][N];
		
		for (int i = 0; i < N; i++)
			calculateChebyshevPolynomials(zeroes[i], T[i]);
		
		double[] values = new double[N];
		
		for (int i = 0; i < N; i++)
			values[i] = e.evaluate(zeroes[i]);
		
		for (int i = 0; i < N; i++) {
			coeffs[i] = 0.0;
			
			for (int j = 0; j < N; j++)
				coeffs[i] += T[j][i] * values[j];
			
			coeffs[i] /= (double)N;
			
			if (i > 0)
				coeffs[i] *= 2.0; 
		}
	}

}
