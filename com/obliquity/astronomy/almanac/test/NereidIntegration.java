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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.StateVector;
import com.obliquity.astronomy.almanac.Vector;

/*
 * This class encapsulates an orbit model for Nereid in the form
 * of sets of Chebyshev coefficients spanning a range of dates.
 */

public class NereidIntegration implements MovingPoint {
	class NereidChebyshevData {
		public double jdStart, jdEnd;
		double[][] coeffs;
	}
	
	public static final int BODY_CODE = 1802;
	
	private NereidChebyshevData[] chebyshevData = null;
	private double jdEarliest = Double.NaN, jdLatest = Double.NaN;
	private double[] ChebyP, ChebyV;
	
	/*
	 * JPL ephemeris object for calculating the position of Neptune.
	 */
	
	private JPLEphemeris ephemeris;
	private double AU;
	private PlanetCentre neptune;
	
	public NereidIntegration(String filename, JPLEphemeris ephemeris) throws IOException {
		this(new FileInputStream(filename), ephemeris);
	}
	
	public NereidIntegration(File file, JPLEphemeris ephemeris) throws IOException {
		this(new FileInputStream(file), ephemeris);
	}
	
	public NereidIntegration(InputStream is, JPLEphemeris ephemeris) throws IOException {
		this.ephemeris = ephemeris;
		AU = 1.0 / ephemeris.getAU();
		neptune = new PlanetCentre(ephemeris, JPLEphemeris.NEPTUNE);

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		List<NereidChebyshevData> dataList = new ArrayList<NereidChebyshevData>();
		
		while (true) {
			NereidChebyshevData nextData = readChebyshevData(br);
			
			if (nextData != null)
				dataList.add(nextData);
			else
				break;
		}
		
		chebyshevData = dataList.toArray(chebyshevData);
		
		jdEarliest = chebyshevData[0].jdStart;
		jdLatest = chebyshevData[chebyshevData.length - 1].jdEnd;
		
		int maxCoeffs = 0;
		
		for (NereidChebyshevData data : chebyshevData) {
			int nCoeffs = data.coeffs[0].length;
			
			if (nCoeffs > maxCoeffs)
				maxCoeffs = nCoeffs; 
		}
		
		ChebyP = new double[maxCoeffs];
		ChebyV = new double[maxCoeffs];
	}
	
	private NereidChebyshevData readChebyshevData(BufferedReader br) throws IOException {
		String line = br.readLine();
		
		if (line == null)
			return null;
		
		String[] words = line.split("\\s+");
		
		if (words == null || words.length < 3)
			return null;
		
		try {
			NereidChebyshevData data = new NereidChebyshevData();

			data.jdStart = Double.parseDouble(words[0]);
			data.jdEnd = Double.parseDouble(words[1]);
			int nCoeffs = Integer.parseInt(words[2]);
			
			data.coeffs = new double[3][nCoeffs];
			
			for (int i = 0; i < nCoeffs; i++) {
				line = br.readLine();
				
				if (line == null)
					return null;
				
				words = line.split("\\s+");
				
				if (words == null || words.length < 3)
					return null;

				for (int j = 0; j < 3; j++)
					data.coeffs[i][j] = Double.parseDouble(words[j]);
			}
			
			return data;
		}
		catch (NumberFormatException nfe) {
			return null;
		}	
	}
	
	public void calculatePlanetocentricPositionAndVelocity(double time, Vector position, Vector velocity) throws JPLEphemerisException {
		if (position == null)
			throw new JPLEphemerisException("Input position vector was null");
		
		if (!isValidDate(time))
			throw new JPLEphemerisException("Date is outside valid range");
		
		NereidChebyshevData data = findChebyshevDataForTime(time);
		
		if (data == null)
			throw new JPLEphemerisException("Failed to find Chebyshev data for date");
		
		double x = 1.0 * 2.0 * (time - data.jdStart)/(data.jdEnd - data.jdStart);
		
		int nCoeffs = data.coeffs[0].length;
		
		ChebyP[0] = 1.0;
		ChebyP[1] = x;
		
		if (velocity != null) {
			ChebyV[0] = 0.0;
			ChebyV[1] = 1.0;
		}
		
		for (int k = 2; k < nCoeffs; k++) {
			ChebyP[k] = 2.0 * x * ChebyP[k-1] - ChebyP[k-2];
			
			if (velocity != null)
				ChebyV[k] = 2.0 * x * ChebyV[k-1] - ChebyV[k-2] + 2.0 * ChebyP[k-1];
		}
		
		double[] pos = new double[3];
		double[] vel = (velocity != null) ? new double[3] : null;
		
		for (int i = 0; i < 3; i++) {
			pos[i] = 0.5 * data.coeffs[i][0];
			
			for (int k = 1; k < nCoeffs; k++)
				pos[i] += data.coeffs[i][k] * ChebyP[k];
			
			if (velocity != null) {
				vel[i] = 0.0;
				
				for (int k = 1; k < nCoeffs; k++)
					vel[i] += data.coeffs[i][k] * ChebyV[k];
			}
		}
		
		position.setComponents(pos);
		
		if (velocity != null)
			velocity.setComponents(vel);
	}
	
	private NereidChebyshevData findChebyshevDataForTime(double time) {
		for (NereidChebyshevData data : chebyshevData) {
			if (data.jdStart <= time && time <= data.jdEnd)
				return data;
		}
		
		return null;
	}

	private Vector nereidPosition = new Vector(), nereidVelocity = new Vector();
	
	private StateVector nereidStateVector = new StateVector(nereidPosition, nereidVelocity);
	
	private Vector neptunePosition = new Vector(), neptuneVelocity = new Vector();
	
	private StateVector neptuneStateVector = new StateVector(neptunePosition, neptuneVelocity);

	public StateVector getStateVector(double time)
			throws JPLEphemerisException {
		neptune.getStateVector(time, neptuneStateVector);
		
		getStateVector(time, nereidStateVector);
		
		nereidStateVector.add(neptuneStateVector);
		
		return nereidStateVector;
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		neptune.getStateVector(time, neptuneStateVector);
		
		Vector position = sv.getPosition();
		Vector velocity = sv.getVelocity();
		
		calculatePlanetocentricPositionAndVelocity(time, position, velocity);
		
		position.multiplyBy(AU);
		velocity.multiplyBy(AU);
		
		sv.add(neptuneStateVector);
	}

	public Vector getPosition(double time) throws JPLEphemerisException {
		neptune.getPosition(time, neptunePosition);
		
		getPosition(time, nereidPosition);

		nereidPosition.add(neptunePosition);
		
		return nereidPosition;
	}

	public void getPosition(double time, Vector p)
			throws JPLEphemerisException {
		neptune.getPosition(time, neptunePosition);
		
		calculatePlanetocentricPositionAndVelocity(time, p, null);
		
		p.multiplyBy(AU);
		
		p.add(neptunePosition);
	}

	public boolean isValidDate(double time) {
		return jdEarliest <= time && time <= jdLatest && ephemeris.isValidDate(time);
	}

	public double getEarliestDate() {
		return Math.max(jdEarliest, ephemeris.getEarliestDate());
	}

	public double getLatestDate() {
		return Math.min(jdLatest, ephemeris.getLatestDate());
	}

	public double getEpoch() {
		return 2451545.0;
	}

	public int getBodyCode() {
		return BODY_CODE;
	}

	public JPLEphemeris getEphemeris() {
		return ephemeris;
	}
}
