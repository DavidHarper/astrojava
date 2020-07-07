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

import java.io.*;
import java.util.*;

import javax.swing.JFileChooser;

import com.obliquity.astronomy.almanac.*;

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
