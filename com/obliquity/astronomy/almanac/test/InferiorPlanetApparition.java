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
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.obliquity.astronomy.almanac.*;

public class InferiorPlanetApparition {
	public enum Horizon { EAST, WEST, BOTH }
	
	public static final double TWOPI = 2.0 * Math.PI;

	private static final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datetimefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd/HH:mm");

	static {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		datetimefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	public static void main(String[] args) {
		String filename = null;
		String startdate = null;
		String enddate = null;
		String longitude = null;
		String latitude = null;
		boolean civil = false;
		boolean graph = false;
		Horizon horizon = Horizon.BOTH;
		boolean useJD = false;

		int kBody = -1;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-venus"))
				kBody = JPLEphemeris.VENUS;

			if (args[i].equalsIgnoreCase("-mercury"))
				kBody = JPLEphemeris.MERCURY;

			if (args[i].equalsIgnoreCase("-civil"))
				civil = true;

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];

			if (args[i].equalsIgnoreCase("-graph"))
				graph = true;

			if (args[i].equalsIgnoreCase("-east"))
				horizon = Horizon.EAST;

			if (args[i].equalsIgnoreCase("-west"))
				horizon = Horizon.WEST;
			
			if (args[i].equalsIgnoreCase("-jd"))
				useJD = true;
		}

		if (filename == null || kBody < 0 || startdate == null || latitude == null || longitude == null) {
			showUsage();
			System.exit(1);
		}

		Date startDate = null;

		try {
			startDate = parseDate(startdate);
		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(1);
			;
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) startDate.getTime()) / MILLISECONDS_PER_DAY;

		double jdfinish = 0.0;

		if (enddate != null) {
			Date endDate = null;

			try {
				endDate = parseDate(enddate);
			} catch (ParseException e) {
				e.printStackTrace();
				System.exit(1);
			}

			jdfinish = UNIX_EPOCH_AS_JD
					+ ((double) endDate.getTime()) / MILLISECONDS_PER_DAY + 1.0;
		} else
			jdfinish = jdstart + 1.0;

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

		MovingPoint planet = null;

		planet = new PlanetCentre(ephemeris, kBody);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace apPlanet = new ApparentPlace(earth, planet, sun, erm);

		ApparentPlace apSun = new ApparentPlace(earth, sun, sun, erm);

		double lat = Double.parseDouble(latitude) * Math.PI / 180.0;
		double lon = Double.parseDouble(longitude) * Math.PI / 180.0;

		Place place = new Place(lat, lon, 0.0, 0.0);

		InferiorPlanetApparition ipa = new InferiorPlanetApparition();
		
		if (graph) {
			try {
				final InferiorPlanetApparitionData[] data = ipa.calculateInferiorPlanetApparitionData(apPlanet, apSun, place, jdstart, jdfinish, civil, horizon);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						createUI(data);
					}
				});
				} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}

		} else {
			try {
				ipa.run(apPlanet, apSun, place, jdstart, jdfinish, civil, horizon, useJD);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void createUI(InferiorPlanetApparitionData[] data) {
		InferiorPlanetApparitionPanel panel = new InferiorPlanetApparitionPanel(data);
		
		JFrame frame = new JFrame("Inferior Planet Apparition");
		
		frame.getContentPane().add(panel);
		frame.setSize(900, 800);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public void run(ApparentPlace apPlanet, ApparentPlace apSun, Place place,
			double jdstart, double jdfinish, boolean civil, Horizon horizon, boolean useJD)
			throws JPLEphemerisException {
		InferiorPlanetApparitionData[] data = calculateInferiorPlanetApparitionData(
				apPlanet, apSun, place, jdstart, jdfinish, civil, horizon);

		for (InferiorPlanetApparitionData ipaData : data)
			displayAspect(ipaData, useJD, System.out);
	}

	public InferiorPlanetApparitionData[] calculateInferiorPlanetApparitionData(
			ApparentPlace apPlanet, ApparentPlace apSun, Place place,
			double jdstart, double jdfinish, boolean civil, Horizon horizon)
			throws JPLEphemerisException {
		LocalVisibility lv = new LocalVisibility();

		List<InferiorPlanetApparitionData> ipaData = new ArrayList<InferiorPlanetApparitionData>();

		for (double jd = jdstart; jd < jdfinish; jd += 1.0) {
			RiseSetType riseSetType = civil ? RiseSetType.CIVIL_TWILIGHT : RiseSetType.UPPER_LIMB;
			
				RiseSetEvent[] civilTwilights = lv.findRiseSetEvents(apSun,
						place, jd, riseSetType);

				for (RiseSetEvent rse : civilTwilights) {
					if (keepRiseSetEvent(rse.type, horizon)) {
						double date = rse.date;

						HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apPlanet,
										place, date);
						
						HorizontalCoordinates hcSun = lv.calculateApparentAltitudeAndAzimuth(apSun,
								place, date);

						if (hc.altitude > 0.0) {
							AlmanacData almanacData = AlmanacData.calculateAlmanacData(apPlanet, apSun, date,
										AlmanacData.OF_DATE, new AlmanacData());
							
							ipaData.add(new InferiorPlanetApparitionData(
								riseSetType, rse.type, hc, almanacData, hcSun.azimuth));
					}
				}
			}
		}

		InferiorPlanetApparitionData[] data = new InferiorPlanetApparitionData[ipaData
				.size()];

		ipaData.toArray(data);

		return data;
	}
	
	private boolean keepRiseSetEvent(RiseSetEventType rseType, Horizon horizon) {
		switch (horizon) {
		case EAST:
			return rseType == RiseSetEventType.RISE;
			
		case WEST:
			return rseType == RiseSetEventType.SET;
			
		default:
			return true;
		}
	}

	private final String SEPARATOR = " ";

	private void displayAspect(InferiorPlanetApparitionData ipaData, boolean useJD,
			PrintStream ps) {
		String prefix = eventPrefix(ipaData);

		displayAspect(prefix, ipaData.almanacData.julianDate,
				ipaData.horizontalCoordinates, ipaData.almanacData, ipaData.sunAzimuth, useJD, ps);
	}

	private String eventPrefix(InferiorPlanetApparitionData ipaData) {
		switch (ipaData.riseSetType) {
		case UPPER_LIMB:
			return ipaData.riseSetEventType == RiseSetEventType.RISE ? "SUNRISE"
					: "SUNSET ";

		case CIVIL_TWILIGHT:
			return ipaData.riseSetEventType == RiseSetEventType.RISE ? "CIVIL_E"
					: "CIVIL_S";

		default:
			return null;
		}
	}

	private void displayAspect(String prefix, double date,
			HorizontalCoordinates hc, AlmanacData data, double sunAzimuth, boolean useJD, PrintStream ps) {
		double positionAngle = reduceAngle(
				data.positionAngleOfBrightLimb - hc.parallacticAngle) * 180.0
				/ Math.PI;

		final String fmt0 = "%-8s";
		final String fmt1 = "%7.2f";
		final String fmt2 = "%5.2f";
		final String fmt3 = "   %13.5f";
		
		ps.printf(fmt0, prefix);
		ps.print(SEPARATOR);
		
		if (useJD)
			ps.printf(fmt3, date);
		else
			ps.print(dateToString(date));
		
		ps.print(SEPARATOR);
		ps.printf(fmt1, 180.0 * hc.altitude / Math.PI);
		ps.print(SEPARATOR);
		ps.printf(fmt1, 180.0 * hc.azimuth / Math.PI);
		ps.print(SEPARATOR);
		ps.printf(fmt2, data.magnitude);
		ps.print(SEPARATOR);
		ps.printf(fmt2, data.semiDiameter);
		ps.print(SEPARATOR);
		ps.printf(fmt2, data.illuminatedFraction);
		ps.print(SEPARATOR);
		ps.printf(fmt1, positionAngle);
		ps.print(SEPARATOR);
		ps.printf(fmt1, 180.0 * sunAzimuth / Math.PI);
		ps.println();
	}

	private final DecimalFormat dfmt1 = new DecimalFormat("0000");
	private final DecimalFormat dfmt2 = new DecimalFormat("00");

	private String dateToString(double t) {
		AstronomicalDate ad = new AstronomicalDate(t);
		ad.roundToNearestSecond();
		return dfmt1.format(ad.getYear()) + "-" + dfmt2.format(ad.getMonth())
				+ "-" + dfmt2.format(ad.getDay()) + " "
				+ dfmt2.format(ad.getHour()) + ":"
				+ dfmt2.format(ad.getMinute());
	}

	private static Date parseDate(String str) throws ParseException {
		if (str != null) {
			try {
				return datetimefmtIn.parse(str);
			} catch (ParseException e) {
				return datefmtIn.parse(str);
			}
		} else
			return new Date();
	}

	// Reduce an angle to the range (-PI, PI]
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWOPI;

		while (x <= -Math.PI)
			x += TWOPI;

		return x;
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-longitude\tLongitude, in degrees");
		System.err.println("\t-latitude\tLatitude, in degrees");

		System.err.println();

		System.err.println("MANDATORY EXCLUSIVE PARAMETERS");

		System.err.println("\t-venus\tDisplay data for Venus");
		System.err.println("\t-mercury\tDisplay data for Mercury");

		System.err.println();

		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-enddate\tEnd date [DEFAULT: startdate + 1.0]");
		System.err.println("\t-civil\tShow altitude at start/end of civil twilight");
		System.err.println("\t-graph\tDisplay the a[[arition graphically");
		
		System.err.println("\t-east\tShow only events at sunrise/morning civil twilight");
		System.err.println("\t-west\tShow only events at sunset/evening civil twilight");
		
		System.err.println("\t-jd\tDisplay date and time as Julian Day Number");
	}

}
