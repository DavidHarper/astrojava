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

import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.StateVector;
import com.obliquity.astronomy.almanac.Vector;

/*
 * This class encapsulates the orbit model for Nereid defined by the mean elements
 * published by Jacobson, R.A. (2009) Astron.J. 137, 4322 (doi:10.1088/0004-6256/137/5/4322).
 * 
 */

public class Nereid implements MovingPoint {
	/*
	 * Mean orbital elements of Nereid, taken from Table 6 of Jacobson (2009).
	 * 
	 * The reference system is the Laplacian plane.  Orbital longitudes are measured
	 * from the ascending node of the Laplacian plane on the ICRF reference plane.
	 * 
	 */
	private final double EPOCH = 2451545.0,
			SEMI_MAJOR_AXIS = 5513818.0,
			ECCENTRICITY = 0.75074,
			SEMI_MINOR_AXIS = SEMI_MAJOR_AXIS * Math.sqrt(1.0 - ECCENTRICITY * ECCENTRICITY),
			LONGITUDE_OF_APSE = 256.6874,
			MEAN_LONGITUDE = 113.3797,
			INCLINATION = 7.0903,
			LONGITUDE_OF_NODE = 335.5701,
			LAMBDA_DOT = 0.9996276,
			APSE_DOT = 0.0064/365.25,
			NODE_DOT = -0.0381/365.25;
	
	/*
	 * Position of the pole of the Laplacian plane, referred to the ICRF system.
	 */
	
	private final double PLP_RA = 269.3023, PLP_DEC = 69.1166;
	
	/*
	 * Other constants.
	 */
	
	private final double TWO_PI = 2.0 * Math.PI;
	
	private final double EPSILON = 1.0/SEMI_MAJOR_AXIS;
	
	private final int MAX_KEPLER_ITERATIONS = 50;
	
	public static final int BODY_CODE = 802;
	
	/*
	 * JPL ephemeris object for calculating the position of Neptune.
	 */
	
	private JPLEphemeris ephemeris;
	private double AU;
	private PlanetCentre neptune;
	
	/*
	 * Fixed basis vectors which define the Laplacian plane.  The vectors M2 and N2 are in
	 * the Laplacian plane, such that M2 is also in the XY-plane of the ICRF system.
	 * The vector P2 is normal to the Laplacian plane.
	 */
	
	private Vector M2, N2, P2;
	
	private void constructBasisVectors() {
		Vector I = new Vector(1.0, 0.0, 0.0), J = new Vector(0.0, 1.0, 0.0), K = new Vector(0.0, 0.0, 1.0);
		
		double lpNode = Math.PI * (PLP_RA + 90.0)/180.0;
		double lpInclination = Math.PI * (90.0 - PLP_DEC)/180.0;
		
		double cosNode = Math.cos(lpNode), sinNode = Math.sin(lpNode), cosIncl = Math.cos(lpInclination), sinIncl = Math.sin(lpInclination);
		
		Vector N1 = Vector.linearCombination(I, cosNode, J, sinNode);
		Vector M1 = Vector.linearCombination(I, -sinNode, J, cosNode);
		Vector P1 = K.copyOf();
		
		N2 = N1.copyOf();
		M2 = Vector.linearCombination(M1, cosIncl, P1, sinIncl);
		P2 = Vector.linearCombination(M1, -sinIncl, P1, cosIncl);
	}
	
	public Nereid(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;
		AU = 1.0 / ephemeris.getAU();
		neptune = new PlanetCentre(ephemeris, JPLEphemeris.NEPTUNE);
		constructBasisVectors();
	}
	
	/*
	 * The high eccentricity of Nereid's orbit causes the classic Newton-Raphson method to converge
	 * extremely slowly, if at all.  Instead, we use Kurth's method, as described in Fitzpatrick, P.M.
	 * (1962)  "Methods of Celestial Mechanics", page 74.
	 * 
	 * This method returns NaN if it fails to converge.
	 */
	
	private double solveKeplersEquation(double M, double e, double eps, int maxiters) {
		double psi= 0.0, psiLast = 0.0, dpsi = 0.0;
				
		int iters = 0;
		
		do {
			iters++;
			
			psiLast = psi;
			
			psi = e * Math.sin(M + psi);
			
			dpsi = psi - psiLast; 
			
			if (iters > maxiters)
				return Double.NaN;
		} while(Math.abs(dpsi) > eps);
		
		double E = M + psi;
		
		return E;
	}
	
	public void calculatePlanetocentricPositionAndVelocity(double time, Vector position, Vector velocity) throws JPLEphemerisException {
		if (position == null)
			throw new JPLEphemerisException("Input position vector was null");
		
		if (!isValidDate(time))
			throw new JPLEphemerisException("Date is outside valid range");
		
		double apse = Math.PI * (LONGITUDE_OF_APSE + (time - EPOCH) * APSE_DOT)/180.0;
		
		double lambda = Math.PI * (MEAN_LONGITUDE + (time - EPOCH) * LAMBDA_DOT)/180.0;
		
		double meanAnomaly = (lambda - apse) % TWO_PI;
		
		double eccAnomaly = solveKeplersEquation(meanAnomaly, ECCENTRICITY, EPSILON, MAX_KEPLER_ITERATIONS);
		
		if (Double.isNaN(eccAnomaly))
			throw new JPLEphemerisException("Failed to solve Kepler's equation");
				
		double node = Math.PI * (LONGITUDE_OF_NODE + (time - EPOCH) * NODE_DOT)/180.0;
		double incl = Math.PI * INCLINATION/180.0;
		
		double cosNode = Math.cos(node), sinNode = Math.sin(node), cosIncl = Math.cos(incl), sinIncl = Math.sin(incl);
		
		// Rotate basis vectors 
		Vector N3 = Vector.linearCombination(N2, cosNode, M2, sinNode);
		Vector M3 = Vector.linearCombination(N2, -sinNode, M2, cosNode);
		Vector P3 = P2.copyOf();
				
		Vector M4 = Vector.linearCombination(M3, cosIncl, P3, sinIncl);
		Vector N4 = N3.copyOf();
		
		// Calculate argument of pericentre.
		
		apse -= node;
		
		double cosApse = Math.cos(apse), sinApse = Math.sin(apse);
		
		Vector P = Vector.linearCombination(N4, cosApse, M4, sinApse);
		Vector Q = Vector.linearCombination(N4, -sinApse, M4, cosApse);
		
		double cosE = Math.cos(eccAnomaly), sinE = Math.sin(eccAnomaly);
		
		double xw = SEMI_MAJOR_AXIS * (cosE - ECCENTRICITY);
		double yw = SEMI_MINOR_AXIS * sinE;
		
		Vector pos = Vector.linearCombination(P, xw, Q, yw);
		
		position.copy(pos);
		
		if (velocity != null) {
			double eDot = (Math.PI/180.0) * (LAMBDA_DOT - APSE_DOT)/(1.0 - ECCENTRICITY * cosE);
		
			double xwdot = -SEMI_MAJOR_AXIS * eDot * sinE;
			double ywdot = SEMI_MINOR_AXIS * eDot * cosE;
		
			Vector vel = Vector.linearCombination(P, xwdot, Q, ywdot);
			
			velocity.copy(vel);
		}
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

	/*
	 * Jacobson's mean elements were calculated by fitting a precessing
	 * ellipse to an integrated orbit over the time span 1800 to 2200,
	 * so we adopt those dates as the range of validity of the elements.
	 */
	
	private static double EARLIEST_DATE = 2451545.0 - 200.0 * 365.25, LATEST_DATE = 2451545.0 + 200.0 * 365.25;

	public boolean isValidDate(double time) {
		return time >= EARLIEST_DATE && time <= LATEST_DATE && ephemeris.isValidDate(time);
	}
	
	public double getEarliestDate() {
		return Math.max(EARLIEST_DATE, ephemeris.getEarliestDate());
	}
	
	public double getLatestDate() {
		return Math.min(LATEST_DATE,  ephemeris.getLatestDate());
	}

	/*
	 * The reference frame is ICRF, so we return J2000.
	 */
	
	public double getEpoch() {
		return 2451545.0;
	}

	/*
	 * The JPL body code for satellite 2 of planet 8 is 802.
	 */
	
	public int getBodyCode() {
		return BODY_CODE;
	}
	
	public JPLEphemeris getEphemeris() {
		return ephemeris;
	}
}
