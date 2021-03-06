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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.*;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.Vector;

/*
 * This class reads asteroid ephemeris files generated by the Minor
 * Planet Center web site.  It calculates the apparent RA and Dec
 * and ecliptic longitude of the asteroid from the MPC data file
 * and outputs RA(asteroid) - RA(Sun) + 180 degrees and
 * longitude(asteroid) - longitude(Sun) + 180 degrees at each tabulated
 * date.  This allows the times of opposition in RA and longitude to
 * be determined by interpolation.
 * 
 * Reference:
 * 
 * https://minorplanetcenter.net/iau/info/MPES.pdf
 */

public class AsteroidEphemerisReader {
	public static void main(String[] args) {
		String filename = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];
			else if (args[i].equalsIgnoreCase("-help")) {
				showHelp(null);
				System.exit(0);
			}
			else {
				showHelp("Unknown option: " + args[i]);
				System.exit(1);
			}
		}

		if (filename == null) {
			showHelp("No ephemeris file specified.");
			System.exit(2);
		}
		
		AsteroidEphemerisRecord[] records = null;
		
		try {
			records = loadEphemerisRecords(System.in);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(3);
		}
		
		if (records != null) {
			double jdstart = records[0].date.getJulianDate() - 1.0;
			double jdfinish = records[records.length - 1].date.getJulianDate() + 1.0;
			
			try {
				AsteroidEphemerisReader aer = new AsteroidEphemerisReader(filename, jdstart, jdfinish);
		
				aer.run(records);
			}
			catch (JPLEphemerisException | IOException e) {
				e.printStackTrace();
			}
		}
		
		System.exit(0);
	}
	
	private static AsteroidEphemerisRecord[] loadEphemerisRecords(InputStream is) throws IOException {
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
		
		List<AsteroidEphemerisRecord> recordList = new ArrayList<AsteroidEphemerisRecord>();
		
		while (true) {
			String line = lnr.readLine();
			
			if (line == null)
				break;
			
			AsteroidEphemerisRecord record = null;
			
			try {
				record = parseRecord(line);
			}
			catch (NumberFormatException e) {
				System.err.println("Caught NumberFormatException at line " + lnr.getLineNumber());
				throw e;
			}
			
			if (record != null)
				recordList.add(record);
		}
		
		AsteroidEphemerisRecord[] records = new AsteroidEphemerisRecord[recordList.size()];
		
		recordList.toArray(records);
		
		return records;
	}
	
	// Parse a record from an MPC asteroid ephemeris.
	//
	// Example input line:
	// 2021 01 01 000000 23 07 18.5 -15 49 52
	// 123456789a123456789b123456789c12345678
	
	private static AsteroidEphemerisRecord parseRecord(String line) {
		int year   = extractInteger(line, 1, 4);
		int month  = extractInteger(line, 6, 7);
		int day    = extractInteger(line, 9, 10);
		
		int hour   = extractInteger(line, 12, 13);
		int minute = extractInteger(line, 14, 15);
		int second = extractInteger(line, 16, 17);
		
		int raHour   = extractInteger(line, 19, 20);
		int raMinute = extractInteger(line, 22, 23);
		double raSecond = extractDouble(line, 25, 28);
		
		double ra = (double)raHour + ((double)raMinute)/60.0 + raSecond/3600.0;
		
		char decSign = line.charAt(29);
		
		int decDegree = extractInteger(line, 31, 32);
		int decMinute = extractInteger(line, 34, 35);
		int decSecond = extractInteger(line, 37, 38);
		
		double dec = ((double)decDegree) + ((double)decMinute)/60.0 + ((double)decSecond)/3600.0;
		
		if (decSign == '-')
			dec = -dec;
		
		AsteroidEphemerisRecord record = new AsteroidEphemerisRecord();
		
		record.date = new AstronomicalDate(year, month, day, hour, minute, (double)second);
		
		record.ra = ra * PI/12.0;
		
		record.dec = dec * PI/180.0;
		
		return record;
	}
	
	private static int extractInteger(String line, int start, int end) {
		return Integer.parseInt(line.substring(start - 1, end));
	}
	
	private static double extractDouble(String line, int start, int end) {
		return Double.parseDouble(line.substring(start - 1, end));
	}
	
	private static void showHelp(String message) {
		if (message != null) {
			System.err.print("ERROR: ");
			System.err.println(message);
			System.err.println();
		}
		
		String[] helpText = {
				"MANDATORY OPTIONS:",
				"",
				"-ephemeris\tName of JPL ephemeris file"
		};
		
		for (String text : helpText)
			System.err.println(text);
	}
	
	private JPLEphemeris ephemeris = null;
	private ApparentPlace apSun = null;
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private static final double J2000 = 2451545.0;
	
	public AsteroidEphemerisReader(String filename, double jdstart, double jdfinish) throws IOException, JPLEphemerisException {
		ephemeris = new JPLEphemeris(filename, jdstart, jdfinish);
		
		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		apSun = new ApparentPlace(earth, sun, sun, erm);
	}
	
	public void run(AsteroidEphemerisRecord[] records) throws JPLEphemerisException {
		for (AsteroidEphemerisRecord record : records) {
			analyseAsteroidEphemerisRecord(record);
		}
	}
	
	private void analyseAsteroidEphemerisRecord(AsteroidEphemerisRecord record) throws JPLEphemerisException {
		double UT = record.date.getJulianDate();
		
		double TT = UT + erm.deltaT(UT);
		
		Vector asteroid = new Vector(cos(record.ra) * cos(record.dec), sin(record.ra) * cos(record.dec), sin(record.dec));
		
		Matrix precess = erm.precessionMatrix(J2000, UT);
		
		asteroid.multiplyBy(precess);
		
		apSun.calculateApparentPlace(TT);
		
		Vector sun = apSun.getDirectionCosinesJ2000();
		
		sun.multiplyBy(precess);
		
		double raSun = atan2(sun.getY(), sun.getX());
		
		double diffRA = (180.0/PI) * (atan2(asteroid.getY(), asteroid.getX()) - raSun + PI);
		
		while (diffRA > 180.0)
			diffRA -= 360.0;
		
		double obliquity = erm.meanObliquity(UT);
		
		Matrix ecliptic = Matrix.getRotationMatrix(Matrix.X_AXIS, -obliquity);
		
		asteroid.multiplyBy(ecliptic);
		
		sun.multiplyBy(ecliptic);
		
		double lambda = atan2(asteroid.getY(), asteroid.getX());
		
		double beta = asin(asteroid.getZ()) * 180.0/PI;
		
		double lambdaSun = atan2(sun.getY(), sun.getX());
		
		double diffLambda = (180.0/PI) * (lambda - lambdaSun + PI);
		
		while (diffLambda > 180.0)
			diffLambda -= 360.0;
		
		AstronomicalDate date = record.date;
		
		System.out.printf("%4d %02d %02d  %02d:%02d:%02.0f  %10.3f  %10.3f  %10.3f\n", date.getYear(), date.getMonth(), date.getDay(),
				date.getHour(), date.getMinute(), date.getSecond(), diffRA, diffLambda, beta);
	}
}
