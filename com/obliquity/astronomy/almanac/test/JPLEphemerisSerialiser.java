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

import com.obliquity.astronomy.almanac.*;

public class JPLEphemerisSerialiser {

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err
					.println("Usage: JPLEphemerisSerialiser filename start-date end-date outputfile");
			System.exit(1);
		}

		String filename = args[0];

		double jdstart = Double.parseDouble(args[1]);
		double jdfinis = Double.parseDouble(args[2]);

		String outputfilename = args[3];

		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(filename, jdstart, jdfinis);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}

		try {
			FileOutputStream ostream = new FileOutputStream(outputfilename);
			ObjectOutputStream oos = new ObjectOutputStream(ostream);
			oos.writeObject(ephemeris);
			oos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException when serialising ephemeris ... "
					+ ioe);
			System.exit(1);
		}
	}
}
