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

public class TerrestrialObserver extends EarthCentre {
	protected EarthRotationModel erm = null;
	protected double latitude = 0.0, longitude = 0.0, height = 0.0;

	public TerrestrialObserver(JPLEphemeris ephemeris, EarthRotationModel erm,
			double latitude, double longitude, double height) {
		super(ephemeris);
		this.erm = erm;
		this.latitude = latitude;
		this.longitude = longitude;
		this.height = height;		
	}
	
	public TerrestrialObserver(JPLEphemeris ephemeris, EarthRotationModel erm,
			Place place) {
		super(ephemeris);
		this.erm = erm;
		this.latitude = place.getLatitude();
		this.longitude = place.getLongitude();
		this.height = place.getHeight();
	}
	
	public TerrestrialObserver(EarthCentre ec, EarthRotationModel erm,
			Place place) {
		super(ec.getEphemeris());
		this.erm = erm;
		this.latitude = place.getLatitude();
		this.longitude = place.getLongitude();
		this.height = place.getHeight();
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		super.getStateVector(time, sv);
		geocentreToTopocentre(time, sv.getPosition(), sv.getVelocity());
	}

	public void getPosition(double time, Vector p) throws JPLEphemerisException {
		super.getPosition(time, p);
		geocentreToTopocentre(time, p, null);
	}

	private void geocentreToTopocentre(double time, Vector position,
			Vector velocity) {
		final double FL = 1.0 / 298.257; /* Flattening of the earth */
		final double RE = 6378.14; /* Radius of Earth in km. */
		final double OMEGA = 7.2921151467e-5; /*
												 * Earth's rotation in
												 * radians/second
												 */

		double f2 = (1.0 - FL) * (1.0 - FL);
		double cphi = Math.cos(latitude);
		double sphi = Math.sin(latitude);
		double c = 1.0 / Math.sqrt(cphi * cphi + f2 * sphi * sphi);
		double s = f2 * c;

		/* Get geocentric coordinates including elevation correction */

		double pcospd = (RE * c + height * 0.001) * cphi * reciprocalAU;
		double psinpd = (RE * s + height * 0.001) * sphi * reciprocalAU;

		/* Get the position vector of the observer in AU */

		double LST = erm.greenwichApparentSiderealTime(time) + longitude;

		Vector pTopo = new Vector(pcospd * Math.cos(LST), pcospd
				* Math.sin(LST), psinpd);

		Vector vTopo = (velocity != null) ? new Vector(-OMEGA * pcospd
				* Math.sin(LST) * 86400.0, OMEGA * pcospd * Math.cos(LST)
				* 86400.0, 0.0) : null;

		Matrix pMatrix = erm.precessionMatrix(ephemeris.getEpoch(), time);
		Matrix nMatrix = erm.nutationMatrix(time);

		pMatrix.transpose();
		nMatrix.transpose();

		pTopo.multiplyBy(nMatrix);
		pTopo.multiplyBy(pMatrix);
		position.add(pTopo);

		if (velocity != null && vTopo != null) {
			vTopo.multiplyBy(nMatrix);
			vTopo.multiplyBy(pMatrix);
			velocity.add(vTopo);
		}
	}
}
