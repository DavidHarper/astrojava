package com.obliquity.astronomy.almanac.test;

/*
 *  COPYRIGHT NOTICE
 *  
 *  Copyright (c) 2017 David Harper
 *  
 *  All rights reserved.
 */

public class SimplePrecession {
	// Reference: Explanatory Supplement to the Astronomical Almanac (1992), pages 103-104
	
	public static final double J2000 = 2451545.0;
	public static final double B1950 = 2433282.4235;
	public static final double B1900 = 2415020.3135;
	
	private static double SECONDS_TO_RADIANS = Math.PI/(180.0 * 3600.0);
	
	private static final double JULIAN_YEAR = 365.25;
	private static final double BESSELIAN_YEAR = 365.2422;
	
	public static double yearToJulianEpoch(double year) {
		return J2000 + (year - 2000.0) * JULIAN_YEAR;
	}
	
	public static double yearToBesselianEpoch(double year) {
		return B1950 + (year - 1950.0) * BESSELIAN_YEAR;
	}
	
	private double zeta, z, theta;
	
	private double P[][] = new double[3][3];
	
	public SimplePrecession(double fixedEpoch, double epochOfDate) {
		calculateMatrix(fixedEpoch, epochOfDate);
	}
	
	private void calculateAngles(double fixedEpoch, double epochOfDate) {
		double T = (fixedEpoch - J2000)/36525.0;
		double t = (epochOfDate - fixedEpoch)/36525.0;
		
		double aux = (2306.2181 + 1.39656 * T - 0.000139 * T * T) * t;
		
		zeta = aux + (0.30188 - 0.000344 * T) * t * t + 0.017998 * t * t * t;
	
		z = aux + (1.09468 + 0.000066 * T) * t * t + 0.018203 * t * t * t;
		
		theta = (2004.3109 - 0.85330 * T - 0.000217 * T * T) * t + (-0.42665 - 0.000217 * T) * t * t - 0.041833 * t * t * t;
		
		zeta *= SECONDS_TO_RADIANS;
		z *= SECONDS_TO_RADIANS;
		theta *= SECONDS_TO_RADIANS;
	}
	
	private void calculateMatrix(double fixedEpoch, double epochOfDate) {
		calculateAngles(fixedEpoch, epochOfDate);
		
		double cosZeta = Math.cos(zeta);
		double sinZeta = Math.sin(zeta);
		
		double cosZ = Math.cos(z);
		double sinZ = Math.sin(z);
		
		double cosTheta = Math.cos(theta);
		double sinTheta = Math.sin(theta);
		
		P[0][0] = cosZ * cosTheta * cosZeta - sinZ * sinZeta;
		P[0][1] = -cosZ * cosTheta * sinZeta - sinZ * cosZeta;
		P[0][2] = -cosZ * sinTheta;
		
		P[1][0] = sinZ * cosTheta * cosZeta + cosZ * sinZeta;
		P[1][1] = -sinZ * cosTheta * sinZeta + cosZ * cosZeta;
		P[1][2] = -sinZ * sinTheta;
		
		P[2][0] = sinTheta * cosZeta;
		P[2][1] = -sinTheta * sinZeta;
		P[2][2] = cosTheta;
	}
	
	public void precess(double[] position) {
		if (position.length < 2)
			throw new IllegalArgumentException("Expected an array of at least two doubles");
		
		double[] V1 = new double[3];
		double[] V2 = new double[3];
		
		V1[0] = Math.cos(position[0]) * Math.cos(position[1]);
		V1[1] = Math.sin(position[0]) * Math.cos(position[1]);
		V1[2] = Math.sin(position[1]);
		
		for (int i = 0; i < 3; i++) {
			V2[i] = 0.0;
			
			for (int j = 0; j < 3; j++)
				V2[i] += P[i][j] * V1[j];
		}
		
		position[0] = Math.atan2(V2[1], V2[0]);
		
		double aux = Math.sqrt(V2[0] * V2[0] + V2[1] * V2[1]);
		
		position[1] = Math.atan2(V2[2], aux);
 	}
	
	public static void main(String[] args) {
		if (args.length != 4) {
			System.err.println("Arguments: RA Dec fixedEpoch epochOfdate");
			System.exit(1);
		}
		
		double RA = Double.parseDouble(args[0]);
		double dec = Double.parseDouble(args[1]);
		double fixedEpoch = Double.parseDouble(args[2]);
		double epochOfDate = Double.parseDouble(args[3]);
		
		SimplePrecession p = new SimplePrecession(fixedEpoch, epochOfDate);

		double[] position = new double[2];
		
		position[0] = RA;
		position[1] = dec;
		
		p.precess(position);
		
		System.out.println("RA:  " + position[0] + "\nDec: " + position[1]);
	}
}
