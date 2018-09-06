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
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.AlmanacData;
import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.saturnpole.DourneauSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.HarperTaylorSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.IAU1989SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.Jacobson2007SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.StruveSaturnPoleModel;

public class SaturnRingAnglesTabulator {
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");
	
	private final SimpleDateFormat datefmtOut = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private ApparentPlace apSaturn = null, apSun = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();

	public SaturnRingAnglesTabulator(JPLEphemeris ephemeris) {
		MovingPoint saturn = new PlanetCentre(ephemeris, JPLEphemeris.SATURN);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		apSaturn = new ApparentPlace(earth, saturn, sun, erm);

		apSun = new ApparentPlace(earth, sun, sun, erm);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		String poleModelName = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
			
			if (args[i].equalsIgnoreCase("-saturnpolemodel"))
				poleModelName = args[++i];
		}

		if (filename == null  || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}
		
		if (poleModelName != null) {
			SaturnPoleModel poleModel = parsePoleModel(poleModelName);
			
			AlmanacData.setSaturnPoleModel(poleModel);
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

		SaturnRingAnglesTabulator tabulator = new SaturnRingAnglesTabulator(ephemeris);
		
		try {
			tabulator.run(jdstart, jdfinish, jdstep, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	private static SaturnPoleModel parsePoleModel(String poleModelName) {
		if (poleModelName == null)
			return null;
		
		switch (poleModelName.toLowerCase()) {
		case "dourneau":
			return new DourneauSaturnPoleModel();
			
		case "harpertaylor":
			return new HarperTaylorSaturnPoleModel();
			
		case "iau1989":
			return new IAU1989SaturnPoleModel();
			
		case "jacobson2007":
			return new Jacobson2007SaturnPoleModel();
			
		case "struve":
			return new StruveSaturnPoleModel();
			
		default:
			return null;
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"OPTIONAL PARAMETERS",
				"\t-step\t\tStep size in days [default: 1.0]",
				"",
				"SATURN POLE MODEL",
				"\t-saturnpolemodel MODELNAME\tUse this model for the pole of Saturn [default: iau1989]",
				"",
				"\tAvailable models: dourneau, harpertaylor, iau1989, jacobson2007, struve",
		};
		
		for (String line : lines)
			System.err.println(line);
	}
	
	public void run(double jdstart, double jdfinish, double jdstep, PrintStream ps)
			throws JPLEphemerisException {
		String anglesFormat = "  %7.3f  %7.3f  %7.3f";
		
		for (double t = jdstart; t < jdfinish; t += jdstep) {
			AlmanacData data = new AlmanacData();
			
			AlmanacData.calculateAlmanacData(apSaturn, apSun, t, AlmanacData.MEAN_OF_DATE, data);
			
			double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

			long ticks = (long) dticks;

			Date date = new Date(ticks);

			ps.print(datefmtOut.format(date));
			
			ps.printf(anglesFormat, data.saturnRingAnglesForEarth.U, data.saturnRingAnglesForEarth.B, data.saturnRingAnglesForEarth.P);
			
			ps.printf(anglesFormat, data.saturnRingAnglesForSun.U, data.saturnRingAnglesForSun.B, data.saturnRingAnglesForSun.P);
			
			ps.println();
		}
	}
}
