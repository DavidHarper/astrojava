/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2024 David Harper at obliquity.com
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

public class Star {
	int catalogueNumber, hdNumber;
	private double rightAscension, declination, pmRA, pmDec, parallax, visualMagnitude, radialVelocity;
	
	public Star(int catalogueNumber, int hdNumber, double rightAscension, double declination, double pmRA, double pmDec,
			double parallax, double visualMagnitude, double radialVelocity) {
		this.catalogueNumber = catalogueNumber;
		this.hdNumber = hdNumber;
		
		this.rightAscension = rightAscension;
		this.declination = declination;
		
		this.pmRA = pmRA;
		this.pmDec = pmDec;
		
		this.parallax = parallax;
		this.visualMagnitude = visualMagnitude;
		this.radialVelocity = radialVelocity;
	}
	
	public int getCatalogueNumber() {
		return catalogueNumber;
	}
	
	public int getHDNumber() {
		return hdNumber;
	}
	
	public double getRightAscension() {
		return rightAscension;
	}
	
	public double getDeclination() {
		return declination;
	}
	
	public double getProperMotionInRightAscension() {
		return pmRA;
	}
	
	public double getProperMotionInDeclination() {
		return pmDec;
	}
	
	public double getParallax() {
		return parallax;
	}
	
	public double getVisualMagnitude() {
		return visualMagnitude;
	}
	
	public double getRadialVelocity() {
		return radialVelocity;
	}
}
