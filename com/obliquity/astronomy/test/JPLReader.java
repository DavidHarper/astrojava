package com.obliquity.astronomy.test;

import java.io.*;
import java.util.*;

import com.obliquity.astronomy.*;

public class JPLReader {
	private static String[] planetNames = { "Mercury", "Venus", "EMB", "Mars",
			"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Moon", "Sun",
			"Nutations", "Librations" };

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: JPLReader filename [filename ...]");
			System.exit(1);
		}

		for (int i = 0; i < args.length; i++) {
			String filename = args[i];

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

			testEphemeris(ephemeris);
		}

		System.exit(0);
	}

	public static void testEphemeris(JPLEphemeris ephemeris) {
		if (ephemeris == null) {
			System.out.println("EPHEMERIS OBJECT IS NULL");
			return;
		}

		double tEarliest = ephemeris.getEarliestDate();
		double tLatest = ephemeris.getLatestDate();
		System.out.println("Ephemeris has number "
				+ ephemeris.getEphemerisNumber());
		System.out.println("Date range is " + tEarliest + " to " + tLatest);

		System.out.println("The ephemeris has "
				+ ephemeris.getNumberOfDataRecords()
				+ " records, each of length "
				+ ephemeris.getLengthOfDataRecord());

		System.out.println();

		System.out.println("The ephemeris has the following components:");
		for (int i = 0; i < planetNames.length; i++)
			if (ephemeris.hasComponent(i))
				System.out.println("[" + i + "] " + planetNames[i]);

		System.out.println();

		System.out.println("AU    = " + ephemeris.getAU());
		System.out.println("EMRAT = " + ephemeris.getEMRAT());

		if (Boolean.getBoolean("showConstants")) {
			System.out.println();
			System.out.println("CONSTANTS");

			Set<Map.Entry<String, Double>> consts = ephemeris.getConstantsEntrySet();

			for (Iterator<Map.Entry<String, Double>> iter = consts.iterator(); iter.hasNext();) {
				Map.Entry<String, Double> entry = iter.next();
				System.out.println(entry.getKey() + " = " + entry.getValue());
			}
		}
	}
}
