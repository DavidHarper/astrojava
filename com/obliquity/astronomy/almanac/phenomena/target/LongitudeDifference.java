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

package com.obliquity.astronomy.almanac.phenomena;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemerisException;

public class LongitudeDifference implements TargetFunction {
	public static final int IN_LONGITUDE = 1;
	public static final int IN_RIGHT_ASCENSION = 2;
	
	private int mode = IN_LONGITUDE;
	
	private static final double TWO_PI = 2.0 * Math.PI;
	
	private ApparentPlace apTarget1 = null, apTarget2 = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private double targetDifference = 0.0;
	
	public LongitudeDifference(ApparentPlace apTarget1, ApparentPlace apTarget2, double targetDifference) throws PhenomenaException {
		this.apTarget1 = apTarget1;
		this.apTarget2 = apTarget2;
		this.targetDifference = targetDifference;
		
		if (apTarget1.getObserver().getBodyCode() != apTarget2.getObserver().getBodyCode())
			throw new PhenomenaException("Observer is not the same for both targets");
	}
	
	public LongitudeDifference(ApparentPlace apTarget1, ApparentPlace apTarget2) throws PhenomenaException {
		this(apTarget1, apTarget2, 0.0);
	}
	
	public void setMode(int mode) throws PhenomenaException {
		if (mode != IN_LONGITUDE && mode != IN_RIGHT_ASCENSION)
			throw new PhenomenaException("Invalid mode");
		
		this.mode = mode;
	}
	
	public int getMode() {
		return mode;
	}
	
	public void setTargetDifference(double targetDifference) {
		this.targetDifference = targetDifference;
	}
	
	public double getTargetDifference() {
		return targetDifference;
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
	
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWO_PI;
		
		while (x <= -Math.PI)
			x+= TWO_PI;
		
		return x;
	}

	private double calculateDifferenceInLongitude(double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double dec1 = apTarget1.getDeclinationOfDate();
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		double dec2 = apTarget2.getDeclinationOfDate();
		
		if (mode == IN_RIGHT_ASCENSION)
			return reduceAngle(ra2 - ra1 - targetDifference);
		
		double lambda1 = calculateEclipticLongitude(ra1, dec1, t);
		
		double lambda2 = calculateEclipticLongitude(ra2, dec2, t);
		
		return reduceAngle(lambda2 - lambda1 - targetDifference);	
	}

	public double valueAtTime(double t) throws JPLEphemerisException {
		return calculateDifferenceInLongitude(t);
	}
}
