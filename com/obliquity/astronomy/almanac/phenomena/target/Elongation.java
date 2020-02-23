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

package com.obliquity.astronomy.almanac.phenomena.target;

import com.obliquity.astronomy.almanac.ApparentPlace;

import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.phenomena.PhenomenaException;
import com.obliquity.astronomy.almanac.phenomena.TargetFunction;

public class Elongation implements TargetFunction {
	private ApparentPlace apTarget1 = null, apTarget2 = null;
	
	public Elongation(ApparentPlace apTarget1, ApparentPlace apTarget2) throws PhenomenaException {
		this.apTarget1 = apTarget1;
		this.apTarget2 = apTarget2;
		
		if (apTarget1.getObserver().getBodyCode() != apTarget2.getObserver().getBodyCode())
			throw new PhenomenaException("Observer is not the same for both targets");
	}

	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= 2.0 * Math.PI;
		
		while (x <= -Math.PI)
			x+= 2.0 * Math.PI;
		
		return x;
	}
	
	private double calculateElongation(double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double dec1 = apTarget1.getDeclinationOfDate();
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		double dec2 = apTarget2.getDeclinationOfDate();
		
		double x = Math.sin(dec1) * Math.sin(dec2)
				+ Math.cos(dec1) * Math.cos(dec2) * Math.cos(ra1 - ra2);
		
		x = Math.acos(x);
		
		double dra = reduceAngle(ra2 - ra1);
		
		return dra < 0.0 ? -x : x;
	}

	public double valueAtTime(double t) throws JPLEphemerisException {
		return calculateElongation(t);
	}

}
