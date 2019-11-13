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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Vector;

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
			
			NereidIntegration nereid1 = new NereidIntegration(chebyshevFilename, ephemeris);
			
			NereidJacobson2009 nereid2 = new NereidJacobson2009(ephemeris);
			
			tester.run(nereid1, nereid2);
		} catch (JPLEphemerisException | IOException e) {
			e.printStackTrace();
		}
	}

	private void run(NereidIntegration nereid1, NereidJacobson2009 nereid2) throws IOException, JPLEphemerisException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		Vector P1 = new Vector(), V1 = new Vector(), P2 = new Vector(), V2 = new Vector();
		
		String format = "%12.3f  %12.3f  %12.3f    %12.3f\n";
		
		while (true) {
			System.out.print("> ");
			
			String line = br.readLine();
			
			if (line == null)
				return;
			
			double jd = Double.parseDouble(line);
			
			nereid1.calculatePlanetocentricPositionAndVelocity(jd, P1, V1);
			
			nereid2.calculatePlanetocentricPositionAndVelocity(jd, P2, V2);
			
			System.out.printf(format, P1.getX(), P1.getY(), P1.getZ(), P1.magnitude());
			System.out.printf(format, P2.getX(), P2.getY(), P2.getZ(), P2.magnitude());
			
			System.out.println();
			
			System.out.printf(format, V1.getX(), V1.getY(), V1.getZ(), V1.magnitude());
			System.out.printf(format, V2.getX(), V2.getY(), V2.getZ(), V2.magnitude());
			
			System.out.println();
		}
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of JPL ephemeris file");
		System.err.println("\t-coeffs\t\tName of file containing Chebyshev data");
	}
}
