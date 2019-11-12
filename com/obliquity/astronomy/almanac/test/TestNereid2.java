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

import java.io.IOException;

import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;

public class TestNereid2 {
	public static void main(String[] args) {
		String ephemerisFilename = null;
		String chebyshevFilename = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				ephemerisFilename = args[++i];
			else if (args[i].equalsIgnoreCase("-coeffs"))
				chebyshevFilename = args[++i];
			else {
				System.err.println("Unknown option: " + args[i]);
				showUsage();
				System.exit(2);;
			}
		}
		
		if (ephemerisFilename == null || chebyshevFilename == null) {
			showUsage();
			System.exit(1);
		}

		TestNereid2 tester = new TestNereid2();

		try {
			JPLEphemeris ephemeris = new JPLEphemeris(ephemerisFilename);
			NereidIntegration nereid = new NereidIntegration(chebyshevFilename, ephemeris);
			tester.run(nereid);
		} catch (JPLEphemerisException | IOException e) {
			e.printStackTrace();
		}
	}

	private void run(NereidIntegration nereid) {
		// TODO Auto-generated method stub
		
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of JPL ephemeris file");
		System.err.println("\t-coeffs\t\tName of file containing Chebyshev data");
	}
}
