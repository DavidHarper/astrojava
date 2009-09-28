package com.obliquity.astronomy.test;

import java.io.*;
import java.text.DecimalFormat;

import com.obliquity.astronomy.*;

public class NeptuneBarycentricElements {
	private static final DecimalFormat dfmt1 = new DecimalFormat("0000000.00");
	private static final DecimalFormat dfmt2 = new DecimalFormat("############.000");
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err
					.println("Usage: NeptuneBarycentricElements filename [interval]");
			System.exit(1);
		}

		String filename = args[0];

		JPLEphemeris ephemeris = null;

		try {
			System.err.println("Loading file " + filename + " ...");
			ephemeris = new JPLEphemeris(filename);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}
		
		double GMB = calculateMassOfSunAndPlanets(ephemeris);

		double dt = args.length > 1 ? Double.parseDouble(args[1]) : 10.0;

		try {
			calculateElementsForDateRange(ephemeris, dt, GMB);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
	
	private static double calculateMassOfSunAndPlanets(JPLEphemeris ephemeris) {
		String[] names = {"GMS", "GM1", "GM2", "GMB", "GM4", "GM5", "GM6", "GM7" };
		
		double sum = 0.0;
		
		for (int i = 0; i < names.length; i++) {
			Double mass = ephemeris.getConstant(names[i]);
			sum += mass.doubleValue();
		}
		
		return sum;
	}

	private static void calculateElementsForDateRange(JPLEphemeris ephemeris,
			double dt, double GMB) throws JPLEphemerisException {
		double tEarliest = ephemeris.getEarliestDate();
		double tLatest = ephemeris.getLatestDate();

		for (double t = tEarliest; t < tLatest; t += dt)
			calculateElements(ephemeris, t, GMB);
	}

	private static Vector position = new Vector();
	private static Vector velocity = new Vector();

	private static void calculateElements(JPLEphemeris ephemeris, double t, double GMB)
			throws JPLEphemerisException {
		ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.NEPTUNE,
				position, velocity);

		System.out.print(dfmt1.format(t));
		System.out.print(' ');
		System.out.print(dfmt2.format(position.getX()));
		System.out.print(' ');
		System.out.print(dfmt2.format(position.getY()));
		System.out.print(' ');
		System.out.print(dfmt2.format(position.getZ()));
		System.out.print(' ');
		System.out.print(dfmt2.format(velocity.getX()));
		System.out.print(' ');
		System.out.print(dfmt2.format(velocity.getY()));
		System.out.print(' ');
		System.out.print(dfmt2.format(velocity.getZ()));
		System.out.println();
	}

}
