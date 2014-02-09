package com.obliquity.astronomy.test;

import java.io.*;
import java.util.*;

import javax.swing.JFileChooser;

import com.obliquity.astronomy.*;

public class JPLReader {
	private static String[] planetNames = { "Mercury", "Venus", "EMB", "Mars",
			"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Moon", "Sun",
			"Nutations", "Librations" };

	public static void main(String[] args) {
		File[] files = null;

		if (args.length > 0) {
			files = new File[args.length];
			for (int i = 0; i < args.length; i++)
				files[i] = new File(args[i]);
		} else {
			JFileChooser chooser = new JFileChooser();

			chooser.setMultiSelectionEnabled(true);

			File cwd = new File(System.getProperty("user.home"));
			chooser.setCurrentDirectory(cwd);

			int returnVal = chooser.showOpenDialog(null);

			if (returnVal == JFileChooser.APPROVE_OPTION)
				files = chooser.getSelectedFiles();
			else
				System.exit(1);
		}

		if (files == null || files.length == 0)
			System.exit(0);
		
		Arrays.sort(files);

		for (int i = 0; i < files.length; i++) {
			JPLEphemeris ephemeris = null;

			try {
				System.out.println("----- Loading file " + files[i].getName() + " -----");
				ephemeris = new JPLEphemeris(files[i]);
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
		
		System.out.println("================================================================================");
	}
}
