/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2026 David Harper at obliquity.com
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

package com.obliquity.astronomy.almanac.phenomena.target;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.phenomena.TargetFunction;

public class EclipticLongitude implements TargetFunction {
	private ApparentPlace apTarget = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	public EclipticLongitude(ApparentPlace apTarget) {
		this.apTarget = apTarget;
	}

	public double valueAtTime(double t) throws JPLEphemerisException {
		apTarget.calculateApparentPlace(t);
		
		double ra = apTarget.getRightAscensionOfDate();
		
		double dec = apTarget.getDeclinationOfDate();
		
		return calculateEclipticLongitude(ra, dec, t);
	}

	private double calculateEclipticLongitude(double ra, double dec, double t) {
		double xa = Math.cos(ra) * Math.cos(dec);
		double ya = Math.sin(ra) * Math.cos(dec);
		double za = Math.sin(dec);
		
		double obliquity = erm.meanObliquity(t);
		
		double xe = xa;
		double ye = ya * Math.cos(obliquity) + za * Math.sin(obliquity);
		
		return Math.atan2(ye, xe);
	}
}
