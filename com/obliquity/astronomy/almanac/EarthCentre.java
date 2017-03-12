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

package com.obliquity.astronomy.almanac;

public class EarthCentre implements MovingPoint {
	protected JPLEphemeris ephemeris = null;

	protected StateVector statevector = new StateVector(new Vector(),
			new Vector());
	protected StateVector moonstatevector = new StateVector(new Vector(),
			new Vector());

	protected double mu = 0.0;

	protected double reciprocalAU;

	public EarthCentre(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;
		double emrat = ephemeris.getEMRAT();
		mu = 1.0 / (1.0 + emrat);
		reciprocalAU = 1.0 / ephemeris.getAU();
	}

	public StateVector getStateVector(double time) throws JPLEphemerisException {
		getStateVector(time, statevector);
		return statevector;
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		Vector position = sv.getPosition();
		Vector velocity = sv.getVelocity();
		Vector moonposition = moonstatevector.getPosition();
		Vector moonvelocity = moonstatevector.getVelocity();

		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.EMB,
				position, velocity);
		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.MOON,
				moonposition, moonvelocity);

		moonposition.multiplyBy(mu);
		moonvelocity.multiplyBy(mu);

		position.subtract(moonposition);
		position.multiplyBy(reciprocalAU);
		velocity.subtract(moonvelocity);
		velocity.multiplyBy(reciprocalAU);
	}

	public Vector getPosition(double time) throws JPLEphemerisException {
		Vector position = statevector.getPosition();
		getPosition(time, position);
		return position;
	}

	public void getPosition(double time, Vector p) throws JPLEphemerisException {
		Vector moonposition = moonstatevector.getPosition();

		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.EMB, p, null);
		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.MOON,
				moonposition, null);

		moonposition.multiplyBy(mu);
		p.subtract(moonposition);
		p.multiplyBy(reciprocalAU);
	}

	public boolean isValidDate(double time) {
		return ephemeris.isValidDate(time);
	}

	public double getEarliestDate() {
		return ephemeris.getEarliestDate();
	}

	public double getLatestDate() {
		return ephemeris.getLatestDate();
	}

	public double getEpoch() {
		return ephemeris.getEpoch();
	}
	
	public int getBodyCode() { return -1; }
	
	public JPLEphemeris getEphemeris() {
			return ephemeris;
	}
}
