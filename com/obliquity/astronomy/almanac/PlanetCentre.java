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

public class PlanetCentre implements MovingPoint {
	private JPLEphemeris ephemeris = null;
	private int kBody = -1;
	private double AU;

	private StateVector statevector = new StateVector(new Vector(),
			new Vector());

	public PlanetCentre(JPLEphemeris ephemeris, int kBody) {
		this.ephemeris = ephemeris;
		this.kBody = kBody;
		AU = 1.0 / ephemeris.getAU();
	}

	public StateVector getStateVector(double time) throws JPLEphemerisException {
		getStateVector(time, statevector);
		return statevector;
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		Vector position = sv.getPosition();
		Vector velocity = sv.getVelocity();

		ephemeris.calculatePositionAndVelocity(time, kBody, position, velocity);

		position.multiplyBy(AU);
		velocity.multiplyBy(AU);
	}

	public Vector getPosition(double time) throws JPLEphemerisException {
		Vector position = statevector.getPosition();
		getPosition(time, position);
		return position;
	}

	public void getPosition(double time, Vector p) throws JPLEphemerisException {
		ephemeris.calculatePositionAndVelocity(time, kBody, p, null);
		p.multiplyBy(AU);
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
	
	public int getBodyCode() { return kBody; }
	
	public JPLEphemeris getEphemeris() {
			return ephemeris;
	}
}
