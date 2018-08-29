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
