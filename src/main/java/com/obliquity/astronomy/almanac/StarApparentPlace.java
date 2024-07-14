/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2020 David Harper at obliquity.com
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

package com.obliquity.astronomy.almanac;

public class StarApparentPlace {
	protected MovingPoint observer;
	protected MovingPoint sun;
	protected EarthRotationModel erm;
	
	private boolean verbose = Boolean.getBoolean("starapparentplace.verbose");

	public StarApparentPlace(MovingPoint observer, 	MovingPoint sun, EarthRotationModel erm) {
		this.observer = observer;
		this.sun = sun;
		this.erm = erm;
	}
	
	public Vector calculateApparentPlace(Star star, double positionEpoch, double fixedEpoch, double jd) throws JPLEphemerisException {
		double ra = star.getRightAscension();
		double dec = star.getDeclination();
		double parallax = star.getParallax();
		double pmRA = star.getProperMotionInRightAscension();
		double pmDec = star.getProperMotionInDeclination();
		double rv = star.getRadialVelocity();
		
		return calculateApparentPlace(ra, dec, parallax, pmRA, pmDec, rv, positionEpoch, fixedEpoch, jd);
	}
	
	private Vector calculateApparentPlace(double ra, double dec, double parallax, double pmRA, double pmDec, double rv,
			double positionEpoch, double fixedEpoch, double jd) throws JPLEphemerisException {
		Vector q = new Vector(Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec));
		
		if (verbose)
			displayVector("CATALOGUE POSITION", q);
		
		double T = (jd - positionEpoch)/36525.0;
		
		// Convert proper motions and parallax from mas/yr and mas to radians/cy and radians.
		pmRA *= Math.PI/(180.0 * 3600.0 * 1000.0) * 100.0;
		pmDec *= Math.PI/(180.0 * 3600.0 * 1000.0) * 100.0;
		parallax *= Math.PI/(180.0 * 3600.0 * 1000.0);
		
		// Convert radial velocity from km/s to AU/cy.
		rv *= 21.095;
		
		if (verbose)
			System.out.printf("pmRA = %11.9f\npmDec = %11.9f\nparallax = %11.9f\n", pmRA, pmDec, parallax);
				
		double mx = -pmRA * Math.sin(ra) - pmDec * Math.sin(dec) * Math.cos(ra) + rv * parallax * q.getX();
		double my =  pmRA * Math.cos(ra) - pmDec * Math.sin(dec) * Math.sin(ra) + rv * parallax * q.getY();
		double mz =  pmDec * Math.cos(dec) + rv * parallax * q.getZ();
		
		Vector m = new Vector(mx, my, mz);
		
		if (verbose)
			displayVector("SPACE MOTION", m);
		
		StateVector sve = observer.getStateVector(jd);
		
		Vector pEarth = sve.getPosition();
		Vector vEarth = sve.getVelocity();
		
		Vector pSun = sun.getPosition(jd);
		
		if (verbose) {
			displayVector("EARTH BARYCENTRIC POSITION", pEarth);
			displayVector("EARTH BARYCENTRIC VELOCITY", vEarth);
			displayVector("SUN   BARYCENTRIC POSITION", pSun);
		}
		
		Vector P = pEarth.copyOf();
		
		P.multiplyBy(-parallax);

		m.multiplyBy(T);
		
		P.add(m);
		
		P.add(q);
		
		if (verbose)
			displayVector("P", P);
		
		Vector E = pEarth.copyOf();
		
		E.subtract(pSun);
		
		if (verbose)
			displayVector("E = E_b - S_b", E);
		
		double emag = E.magnitude();
		
		E.normalise();
		P.normalise();
		
		if (verbose) {
			System.out.printf("|E| = %11.9f\n", emag);
			displayVector("Normalised E", E);
			displayVector("Normalised P", P);
		}
		
		double pdote = P.scalarProduct(E);
		
		Vector dp = Vector.linearCombination(E, 1.0, P, -pdote);
		
		double factor = 2.0 * 9.87e-9 * emag/(1.0 + pdote);
		
		dp.multiplyBy(factor);
		
		Vector p1 = P.copyOf();
		
		p1.add(dp);
		
		if (verbose) {
			System.out.printf("p . e = %11.9f\n", pdote);
			displayVector("dP", dp);
			displayVector("p1", p1);
		}
		
		Vector V = vEarth.copyOf();
		
		V.multiplyBy(0.005775);
		
		if (verbose)
			displayVector("V", V);
		
		double vmag = V.magnitude();
		
		double beta = 1.0/Math.sqrt(1.0 - vmag * vmag);
		
		double p1dotv = p1.scalarProduct(V);
		
		factor = 1.0 + p1dotv/(1.0 + 1.0/beta);
		
		if (verbose)
			System.out.printf("|V| = %11.9f, beta = %11.9f, p1 . V = %11.9f, factor = %11.9f\n", vmag, beta, p1dotv, factor);
		
		Vector p2 = Vector.linearCombination(p1, 1.0/beta, V, factor);
		
		p2.multiplyBy(1.0/(1.0 + p1dotv));
		
		if (verbose)
			displayVector("p2", p2);
		
		if (erm != null) {
			Matrix precession = erm.precessionMatrix(fixedEpoch, jd);
		
			Matrix nutation = erm.nutationMatrix(jd);

			precession.rightMultiplyBy(nutation);
		
			if (verbose)
				displayMatrix("PRECESSION-NUTATION", precession);
		
			p2.multiplyBy(precession);
		
			if (verbose)
				displayVector("p3", p2);
		}
		
		p2.normalise();
		
		return p2;
	}
	
	private void displayVector(String caption, Vector v) {
		System.out.println(caption);
		System.out.printf("\t[ %12.9f  %12.9f  %12.9f ]\n", v.getX(), v.getY(), v.getZ());
	}
	
	private void displayMatrix(String caption, Matrix m) {
		System.out.println(caption);
		for (int i = 0; i < 3; i++)
			System.out.printf("\t [ %12.9f  %12.9f  %12.9f ]\n", m.getComponent(i, 0), m.getComponent(i, 1), m.getComponent(i, 2));
	}
}
