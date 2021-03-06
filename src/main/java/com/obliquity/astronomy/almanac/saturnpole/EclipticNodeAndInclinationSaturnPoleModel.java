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

package com.obliquity.astronomy.almanac.saturnpole;

import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.Vector;

public abstract class EclipticNodeAndInclinationSaturnPoleModel implements SaturnPoleModel {
	IAUEarthRotationModel erm = new IAUEarthRotationModel();
	
	final double node, inclination;
	
	protected EclipticNodeAndInclinationSaturnPoleModel(double node, double inclination) {
		this.node = node;
		this.inclination = inclination;
	}
	
	abstract double getSourceEpoch();
	
	protected SaturnPolePosition nodeAndInclinationToPolePosition(double targetEpoch) {
		Matrix precession = erm.precessionMatrix(getSourceEpoch(), targetEpoch);
		
		double obliquity = erm.meanObliquity(getSourceEpoch());
		
		double lngPole = node - 0.5 * Math.PI;
		
		double latPole = 0.5 * Math.PI - inclination;
		
		double xe = Math.cos(lngPole) * Math.cos(latPole);
		double ye = Math.sin(lngPole) * Math.cos(latPole);
		double ze = Math.sin(latPole);
		
		double ce = Math.cos(obliquity);
		double se = Math.sin(obliquity);
		
		double xa0 = xe;
		double ya0 = ce * ye - se * ze;
		double za0 = se * ye + ce * ze;
		
		Vector v = new Vector(xa0, ya0, za0);
		
		v.multiplyBy(precession);
		
		double ra = Math.atan2(v.getY(), v.getX());
		
		if (ra < 0.0)
			ra += 2.0 * Math.PI;
		
		double dec = Math.asin(v.getZ());
		
		return new SaturnPolePosition(ra, dec);
	}
}
