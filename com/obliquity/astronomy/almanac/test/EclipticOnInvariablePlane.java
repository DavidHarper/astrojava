/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2014 David Harper at obliquity.com
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Vector;

public class EclipticOnInvariablePlane {
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final DecimalFormat dfmt = new DecimalFormat("0.0000000");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final char TAB = '\t';

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		
		boolean earthCentre = false;
		boolean sunCentre = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
			
			if (args[i].equalsIgnoreCase("-suncentre"))
				sunCentre = true;
			
			if (args[i].equalsIgnoreCase("-earthcentre"))
				earthCentre = true;
		}

		if (filename == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		Date date = null;
		
		try {
			date = datefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = datefmt.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;

		double jdstep = 0.0;

		jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(filename, jdstart - 1.0,
					jdfinish + 1.0);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}

		Vector invariablePlaneX = new Vector();
		Vector invariablePlaneY = new Vector();
		Vector invariablePlaneZ = new Vector();
		
		calculateInvariablePlaneVectors(3.85263363 * Math.PI/180.0, 23.00888303 * Math.PI/180.0, invariablePlaneX, invariablePlaneY, invariablePlaneZ);
		
		Vector earthPosition = new Vector();
		Vector earthVelocity = new Vector();
		
		Vector moonPosition = new Vector();
		Vector moonVelocity = new Vector();
		
		Vector sunPosition = new Vector();
		Vector sunVelocity = new Vector();
		
		double mu = 1.0 / (1.0 + ephemeris.getEMRAT());

		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.EMB,
						earthPosition, earthVelocity);

				if (earthCentre) {
					ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.MOON,
						moonPosition, moonVelocity);
				
					moonPosition.multiplyBy(mu);
					moonVelocity.multiplyBy(mu);

					earthPosition.subtract(moonPosition);
					earthVelocity.subtract(moonVelocity);
				}
				
				if (sunCentre) {
					ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.SUN, sunPosition, sunVelocity);

					earthPosition.subtract(sunPosition);
					earthVelocity.subtract(sunVelocity);
				}
				
				Vector L = earthPosition.vectorProduct(earthVelocity);
				
				L.normalise();
				
				double x = invariablePlaneX.scalarProduct(L);
				double y = invariablePlaneY.scalarProduct(L);
				double z = invariablePlaneZ.scalarProduct(L);
				
				System.out.println(dfmt.format(t) + TAB + dfmt.format(x) + TAB + dfmt.format(y) + TAB + dfmt.format(z));
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

	}

	private static void calculateInvariablePlaneVectors(double node, double incl,
			Vector X, Vector Y,	Vector Z) {
		double cn = Math.cos(node);
		double sn = Math.sin(node);
		double ci = Math.cos(incl);
		double si = Math.sin(incl);
		
		X.setComponents(cn, sn, 0.0);
		Y.setComponents(-sn*ci, cn*ci, si);
		Z.setComponents(sn*si, -cn*si, ci);
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-step\t\tStep size (days)");
	}

}
