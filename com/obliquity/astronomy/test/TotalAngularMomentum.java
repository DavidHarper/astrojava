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

package com.obliquity.astronomy.test;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.JPLEphemeris;
import com.obliquity.astronomy.JPLEphemerisException;
import com.obliquity.astronomy.Vector;

public class TotalAngularMomentum {
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final DecimalFormat dfmt1 = new DecimalFormat("0.0000000");
	private static final DecimalFormat dfmt2 = new DecimalFormat("0.000000000000000E0");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final char TAB = '\t';

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
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
		
		double AU = 1000.0 * ephemeris.getConstant("AU");
		double day = 86400.0;
		
		double toSI = (AU * AU * AU)/(day * day);
		
		double GM[] = new double[11];
		
		GM[JPLEphemeris.SUN] = ephemeris.getConstant("GMS") * toSI;
		
		for (int i = 0; i < 9; i++) {
			if (i != JPLEphemeris.EMB)
				GM[i] = ephemeris.getConstant("GM" + (i+1)) * toSI;
		}
		
		GM[JPLEphemeris.EMB] = ephemeris.getConstant("GMB") * toSI;
		
		Vector P = new Vector();
		Vector V = new Vector();
		
		boolean debug = Boolean.getBoolean("debug");
		
		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.SUN,
						P, V);
				
				P.multiplyBy(GM[JPLEphemeris.SUN]);
				
				Vector totalJ = P.vectorProduct(V);
				
				if (debug)
					printComponents(t, dfmt1, "SUN", totalJ, dfmt2);
				
				for (int iBody = 0; iBody < 8; iBody++) {
					ephemeris.calculatePositionAndVelocity(t, iBody,
							P, V);
					
					P.multiplyBy(GM[iBody]);
					
					Vector J = P.vectorProduct(V);
					
					if (debug)
						printComponents(t, dfmt1, "BODY_" + (iBody+1), J, dfmt2);
					
					totalJ.add(J);
				}
				
				printComponents(t, dfmt1, null, totalJ, dfmt2);
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}
	}
	
	private static void printComponents(double t, DecimalFormat dfmt1, String message, Vector V, DecimalFormat dfmt2) {
		System.out.print(dfmt1.format(t));
		
		if (message != null) {
			System.out.print(TAB);
			System.out.print(message);
		}
		
		System.out.print(TAB);
		System.out.print(dfmt2.format(V.getX())); 
		System.out.print(TAB);
		System.out.print(dfmt2.format(V.getY())); 
		System.out.print(TAB);
		System.out.print(dfmt2.format(V.getZ())); 
		System.out.print(TAB);
		System.out.print(dfmt2.format(V.magnitude())); 
		System.out.println();		
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
