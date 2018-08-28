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
import java.util.Random;

import com.obliquity.astronomy.almanac.*;

public class JPLEphemerisSpeedTester {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: JPLEphemerisSpeedTester filename count");
			System.exit(1);
		}

		String filename = args[0];
		int nTests = Integer.parseInt(args[1]);

		JPLEphemeris ephemeris = null;

		try {
			FileInputStream istream = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(istream);
			ephemeris = (JPLEphemeris) ois.readObject();
			ois.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException when de-serialising ephemeris ... "
					+ ioe);
			System.exit(1);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			System.err
					.println("ClassNotFoundException when de-serialising ephemeris ... "
							+ cnfe);
			System.exit(1);
		}

		Random random = new Random();

		Vector pos = new Vector();
		Vector vel = new Vector();

		double tEarliest = ephemeris.getEarliestDate();
		double tLatest = ephemeris.getLatestDate();

		double tSpan = tLatest - tEarliest;

		long startTime = System.currentTimeMillis();

		for (int j = 0; j < nTests; j++) {
			int nBody = random.nextInt(JPLEphemeris.SUN);
			double t = tEarliest + tSpan * random.nextDouble();

			try {
				ephemeris.calculatePositionAndVelocity(t, nBody, pos, vel);
			} catch (JPLEphemerisException jee) {
				jee.printStackTrace();
				System.err
						.println("JPLEphemerisException from first ephemeris object ... "
								+ jee);
				System.exit(1);
			}
		}

		long finishTime = System.currentTimeMillis();
		long dt = finishTime - startTime;

		System.out.println("Completed " + nTests + " test calculations in "
				+ dt + " milliseconds");
	}
}
