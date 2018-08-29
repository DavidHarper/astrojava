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

public abstract class RightAscensionAndDeclinationSaturnPoleModel extends AbstractSaturnPoleModel {
	IAUEarthRotationModel erm = new IAUEarthRotationModel();
	
	abstract SaturnPolePosition getPolePositionInSourceFrame(double epoch);
	
	abstract double getSourceEpoch();
	
	public SaturnPolePosition getPolePosition(double targetEpoch) {
		SaturnPolePosition pole = getPolePositionInSourceFrame(targetEpoch);
		
		Matrix precession = erm.precessionMatrix(getSourceEpoch(), targetEpoch);
		
		Vector poleVector = new Vector(Math.cos(pole.rightAscension) * Math.cos(pole.declination),
				Math.sin(pole.rightAscension) * Math.cos(pole.declination),
				Math.sin(pole.declination));
		
		poleVector.multiplyBy(precession);
			
		pole.rightAscension = Math.atan2(poleVector.getY(), poleVector.getX());
		
		if (pole.rightAscension < 0.0)
			pole.rightAscension += 2.0 * Math.PI;
		
		pole.declination = Math.asin(poleVector.getZ());
		
		return pole;
	}
}
