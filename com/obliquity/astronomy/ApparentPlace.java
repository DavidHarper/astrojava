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

package com.obliquity.astronomy;

public class ApparentPlace {
	protected MovingPoint observer;
	protected MovingPoint target;
	protected MovingPoint sun;
	protected EarthRotationModel erm;

	protected Vector dc = null;
	protected double pl = 0.0;
	protected double gd = 0.0;
	protected double ra;
	protected double dec;

	protected Matrix precess = new Matrix();
	protected Matrix nutate = new Matrix();
	
	protected boolean isValid = false;

	private final static double EPSILON = 1.0e-9;

	public ApparentPlace(MovingPoint observer, MovingPoint target,
			MovingPoint sun, EarthRotationModel erm) {
		this.observer = observer;
		this.target = target;
		this.sun = sun;
		this.erm = erm;
	}

	public Vector getDirectionCosines() throws IllegalStateException {
		if (isValid)
			return dc;	
		else
			throw new IllegalStateException();
	}

	public double getLightPathDistance() throws IllegalStateException {
		if (isValid)
			return pl;
		else
			throw new IllegalStateException();
	}

	public double getGeometricDistance() throws IllegalStateException {
		if (isValid)
			return gd;
		else
			throw new IllegalStateException();
	}

	public double getRightAscension() throws IllegalStateException {
		if (isValid)
			return ra;
		else
			throw new IllegalStateException();
	}

	public double getDeclination() throws IllegalStateException {
		if (isValid)
			return dec;
		else
			throw new IllegalStateException();
	}
	
	public MovingPoint getTarget() {
		return target;
	}
	
	public MovingPoint getObserver() {
		return observer;
	}
	
	public MovingPoint getSun() {
		return sun;
	}
	
	public EarthRotationModel getEarthRotationModel() {
		return erm;
	}

	public void calculateApparentPlace(double t) throws JPLEphemerisException {
		StateVector svObserver = null;
		Vector EB = null;
		Vector SB = new Vector();
		Vector QB = new Vector();

		Vector P = new Vector();
		Vector Q = new Vector();
		Vector E = new Vector();

		double c = 173.1446;
		double factor = 2.0 * 9.87e-9;

		svObserver = observer.getStateVector(t);
		EB = svObserver.getPosition();

		if (sun != null)
			sun.getPosition(t, SB);

		E.copy(EB);
		E.subtract(SB);

		double EE = E.magnitude();

		double dtau;

		double tau = 0.0;

		do {
			target.getPosition(t - tau, QB);

			sun.getPosition(t - tau, SB);

			P.copy(QB);
			P.subtract(EB);

			double PP = P.magnitude();

			Q.copy(QB);
			Q.subtract(SB);

			double QQ = Q.magnitude();

			if (tau == 0.0)
				gd = PP;

			pl = PP;

			if (target != sun)
				pl += factor * Math.log((EE + PP + QQ) / (EE - PP + QQ));

			double newtau = pl / c;

			dtau = newtau - tau;

			tau = newtau;
		} while (Math.abs(dtau) > EPSILON);

		P.normalise();
		Q.normalise();
		E.normalise();

		if (target != sun) {
			Vector pa = new Vector(E);
			pa.multiplyBy(P.scalarProduct(Q));

			Vector pb = new Vector(Q);
			pb.multiplyBy(E.scalarProduct(P));

			pa.subtract(pb);

			double pfactor = (factor / EE) / (1.0 + Q.scalarProduct(E));

			pa.multiplyBy(pfactor);

			P.add(pa);
		}

		Vector V = svObserver.getVelocity();
		V.multiplyBy(1.0 / c);

		double VV = V.magnitude();

		double beta = Math.sqrt(1.0 - VV * VV);

		double denominator = 1.0 + P.scalarProduct(V);

		double factora = beta / denominator;

		double factorb = (1.0 + P.scalarProduct(V) / (1.0 + beta))
				/ denominator;

		P.multiplyBy(factora);
		V.multiplyBy(factorb);

		P.add(V);

		P.normalise();

		if (erm != null) {
			double ut = t - erm.deltaT(t);

			erm.precessionMatrix(target.getEpoch(), ut, precess);
			erm.nutationMatrix(ut, nutate);

			P.multiplyBy(precess);
			P.multiplyBy(nutate);
		}

		dc = new Vector(P);

		double x = dc.getX();
		double y = dc.getY();
		double z = dc.getZ();

		ra = Math.atan2(y, x);
		dec = Math.atan2(z, Math.sqrt(x * x + y * y));
		
		isValid = true;
	}
}
