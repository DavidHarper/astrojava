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

package com.obliquity.astronomy.almanac.phenomena;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.phenomena.Phenomenon.Type;

public class PhenomenaFinder {
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");
	
	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		boolean inRA = false;
		
		Phenomenon.Type mode = Type.CONJUNCTION;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-ephemeris":
				filename = args[++i];
				break;
				
			case "-body":
				bodyname = args[++i];
				break;
								
			case "-startdate":
				startdate = args[++i];
				break;

			case "-enddate":
				enddate = args[++i];
				break;
				
			case "-step":
				stepsize = args[++i];
				break;
				
			case "-ra":
				inRA = true;
				break;
				
			case "-conjunction":
				mode = Type.CONJUNCTION;
				break;
				
			case "-opposition":
				mode = Type.OPPOSITION;
				break;
				
			case "-quadrature-east":
				mode = Type.QUADRATURE_EAST;
				break;
				
			case "-quadrature-west":
				mode = Type.QUADRATURE_WEST;
				break;
				
			case "-greatest-elongation-east":
				mode = Type.GREATEST_ELONGATION_EAST;
				break;
				
			case "-greatest-elongation-west":
				mode = Type.GREATEST_ELONGATION_WEST;
				break;
				
			case "-stationary-east":
				mode = Type.STATIONARY_EAST;
				break;
				
			case "-stationary-west":
				mode = Type.STATIONARY_WEST;
				break;
				
			default:
				System.err.println("Unrecognised keyword \"" + args[i] + "\"");
				showUsage();
				System.exit(1);
			}
		}

		if (filename == null || bodyname == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown name for -body1: \"" + bodyname + "\"");
			System.exit(1);
		}

		Date date = null;

		try {
			date = datefmtIn.parse(startdate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		try {
			date = datefmtIn.parse(enddate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdfinish = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		double jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

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

		MovingPoint planet = (kBody == JPLEphemeris.MOON) ?
			new MoonCentre(ephemeris) : new PlanetCentre(ephemeris, kBody);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace apTarget1 = new ApparentPlace(earth, sun, sun, erm);

		ApparentPlace apTarget2 = new ApparentPlace(earth, planet, sun, erm);
		
		TargetFunction tf = null;
		
		switch (mode) {
		case CONJUNCTION:
		case OPPOSITION:
		case QUADRATURE_EAST:
		case QUADRATURE_WEST:
			try {
				LongitudeDifference ldiff = new LongitudeDifference(apTarget1, apTarget2);
				
				if (inRA)
					ldiff.setMode(LongitudeDifference.IN_RIGHT_ASCENSION);
				
				tf = ldiff;
			} catch (PhenomenaException e1) {
				e1.printStackTrace();
				System.exit(1);
			}			
			break;
			
		case GREATEST_ELONGATION_EAST:
		case GREATEST_ELONGATION_WEST:
			try {
				Elongation el = new Elongation(apTarget1, apTarget2);
				
				tf = el;
			} catch (PhenomenaException e1) {
				e1.printStackTrace();
			}			
			break;
			
		case STATIONARY_EAST:
		case STATIONARY_WEST:
			tf = new RightAscensionFunction(apTarget2);
			break;
			
		default:
			break;
		
		}
		
		PhenomenaFinder finder = new PhenomenaFinder();
		
		try {
			finder.findPhenomena(tf, jdstart, jdfinish, jdstep, mode);
		} catch (JPLEphemerisException | PhenomenaException e) {
			e.printStackTrace();
		}
	}
	
	private boolean changeOfSign(double x1, double x2) {
		if (x1 > 0.0 && x2 > 0.0)
			return false;
		
		if (x1 < 0.0 && x2 < 0.0)
			return false;
	
		// Exclude signs change between -PI and +PI
		if (Math.abs(x1) > 2.0 || Math.abs(x2) > 2.0)
			return false;
		
		return true;
	}
	
	public void findPhenomena(TargetFunction tf, double jdstart, double jdfinish,
			double jdstep, Type mode) throws JPLEphemerisException, PhenomenaException {
		if (tf instanceof LongitudeDifference) {
			LongitudeDifference ldiff = (LongitudeDifference)tf;
			findPhenomena(ldiff, jdstart, jdfinish, jdstep, mode);
		} else if (tf instanceof Elongation) {
			Elongation el = (Elongation)tf;
			findPhenomena(el, jdstart, jdfinish, jdstep, mode);
		} else if (tf instanceof RightAscensionFunction) {
			RightAscensionFunction raf = (RightAscensionFunction)tf;
			findPhenomena(raf, jdstart, jdfinish, jdstep, mode);
		}
	}

	public void findPhenomena(LongitudeDifference ldiff, double jdstart, double jdfinish,
			double jdstep, Type mode) throws JPLEphemerisException, PhenomenaException {
		switch (mode) {
		case CONJUNCTION:
			ldiff.setTargetDifference(0.0);
			break;
			
		case OPPOSITION:
			ldiff.setTargetDifference(Math.PI);
			break;
			
		case QUADRATURE_EAST:
			ldiff.setTargetDifference(0.5 * Math.PI);
			break;
			
		case QUADRATURE_WEST:
			ldiff.setTargetDifference(-0.5 * Math.PI);
			break;
			
		case GREATEST_ELONGATION_EAST:
		case GREATEST_ELONGATION_WEST:
		case STATIONARY_EAST:
		case STATIONARY_WEST:
			throw new PhenomenaException("Invalid mode for longitude difference");
			
		default:
			break;
		}
		
		findPhenomenaFromZeroOfTargetFunction(ldiff, jdstart, jdfinish, jdstep);
	}
	
	public void findPhenomenaFromZeroOfTargetFunction(LongitudeDifference ldiff, double jdstart, double jdfinish,
			double jdstep) throws JPLEphemerisException {
		double lastDX = Double.NaN;
		boolean first = true;
		
		for (double t = jdstart; t <= jdfinish; t += jdstep) {
			double dX = ldiff.valueAtTime(t);
			
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - jdstep;
					
					double tExact = ZeroFinder.findZero(ldiff, tLast, t, 1.0e-5);
					
					AstronomicalDate ad = new AstronomicalDate(tExact);
					
					System.out.printf("%5d %02d %02d %02d:%02d\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
				}
			}
			
			lastDX = dX;
			first = false;
		}		
	}
	
	public void findPhenomena(Elongation el, double jdstart, double jdfinish,
			double jdstep, Type mode) throws JPLEphemerisException, PhenomenaException {
	
		switch (mode) {
		case CONJUNCTION:
		case OPPOSITION:
		case QUADRATURE_EAST:
		case QUADRATURE_WEST:
		case STATIONARY_EAST:
		case STATIONARY_WEST:
			throw new PhenomenaException("Invalid mode for elongation");
			
		case GREATEST_ELONGATION_EAST:
			findPhenomenaFromMaximumOfTargetFunction(el, jdstart, jdfinish, jdstep);
			break;
			
		case GREATEST_ELONGATION_WEST:
			findPhenomenaFromMinimumOfTargetFunction(el, jdstart, jdfinish, jdstep);
			break;
			
		default:
			break;
		}
	}
	
	private void findPhenomenaFromMaximumOfTargetFunction(TargetFunction tf, double jdstart, double jdfinish, double jdstep) throws JPLEphemerisException {
		double values[] = new double[3];
		
		values[0] = tf.valueAtTime(jdstart);
		values[1] = tf.valueAtTime(jdstart + jdstep);
		
		for (double t = jdstart + 2.0 * jdstep; t < jdfinish; t += jdstep) {
			values[2] = tf.valueAtTime(t);
			
			if (isMidpointLargest(values)) {
				double ta = t - 2.0 * jdstep;
				double tb = t - jdstep;
				double tc = t;
				
				double tExact = ExtremumFinder.findMaximum(tf, ta, tb, tc, 1.0e-5);
				
				AstronomicalDate ad = new AstronomicalDate(tExact);
				
				System.out.printf("%5d %02d %02d %02d:%02d\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
			}
			
			values[0] = values[1];
			values[1] = values[2];
		}
	}
	
	private boolean isMidpointLargest(double[] values) {
		return values[1] > values[0] && values[1] > values[2];
	}
	
	private void findPhenomenaFromMinimumOfTargetFunction(TargetFunction tf, double jdstart, double jdfinish, double jdstep) throws JPLEphemerisException {
		double values[] = new double[3];
		
		values[0] = tf.valueAtTime(jdstart);
		values[1] = tf.valueAtTime(jdstart + jdstep);
		
		for (double t = jdstart + 2.0 * jdstep; t < jdfinish; t += jdstep) {
			values[2] = tf.valueAtTime(t);
			
			if (isMidpointSmallest(values)) {
				double ta = t - 2.0 * jdstep;
				double tb = t - jdstep;
				double tc = t;
				
				double tExact = ExtremumFinder.findMinimum(tf, ta, tb, tc, 1.0e-5);
				
				AstronomicalDate ad = new AstronomicalDate(tExact);
				
				System.out.printf("%5d %02d %02d %02d:%02d\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
			}
			
			values[0] = values[1];
			values[1] = values[2];
		}
	}
	
	private boolean isMidpointSmallest(double[] values) {
		return values[1] < values[0] && values[1] < values[2];
	}
	
	
	public void findPhenomena(RightAscensionFunction raf, double jdstart, double jdfinish,
			double jdstep, Type mode) throws JPLEphemerisException, PhenomenaException {		
		switch (mode) {
		case CONJUNCTION:
		case OPPOSITION:
		case QUADRATURE_EAST:
		case QUADRATURE_WEST:
		case GREATEST_ELONGATION_EAST:
		case GREATEST_ELONGATION_WEST:
			throw new PhenomenaException("Invalid mode for elongation");
			
		case STATIONARY_EAST:
			findPhenomenaFromMaximumOfTargetFunction(raf, jdstart, jdfinish, jdstep);
			break;
			
		case STATIONARY_WEST:
			findPhenomenaFromMinimumOfTargetFunction(raf, jdstart, jdfinish, jdstep);
			break;
			
		default:
			break;
		}
	}


	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-body\t\tName of body 1",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"OPTIONAL PARAMETERS",
				"-conjunction\tFind dates of conjunction",
				"-opposition\tFind dates of opposition",
				"-quadrature-east\tFind dates of east quadrature",
				"-quadrature-west\tFind dates of west quadrature",
				"-greatest-elongation-east\tFind dates of greatest elongation east",
				"-greatest-elongation-west\tFind dates of greatest elongation west",
				"-stationary-west\tFind dates of west stationary points in RA",
				"-stationary-east\tFind dates of east stationary points in RA"
		};
		
		for (String line : lines)
			System.err.println(line);
	}

	private static int parseBody(String bodyname) {
		if (bodyname.equalsIgnoreCase("sun"))
			return JPLEphemeris.SUN;

		if (bodyname.equalsIgnoreCase("moon"))
			return JPLEphemeris.MOON;

		if (bodyname.equalsIgnoreCase("mercury"))
			return JPLEphemeris.MERCURY;

		if (bodyname.equalsIgnoreCase("venus"))
			return JPLEphemeris.VENUS;

		if (bodyname.equalsIgnoreCase("mars"))
			return JPLEphemeris.MARS;

		if (bodyname.equalsIgnoreCase("jupiter"))
			return JPLEphemeris.JUPITER;

		if (bodyname.equalsIgnoreCase("saturn"))
			return JPLEphemeris.SATURN;

		if (bodyname.equalsIgnoreCase("uranus"))
			return JPLEphemeris.URANUS;

		if (bodyname.equalsIgnoreCase("neptune"))
			return JPLEphemeris.NEPTUNE;

		if (bodyname.equalsIgnoreCase("pluto"))
			return JPLEphemeris.PLUTO;

		return -1;
	}

}
