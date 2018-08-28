package com.obliquity.astronomy.almanac.saturnpole;

public class StruveSaturnPoleModel extends EclipticNodeAndInclinationSaturnPoleModel {
	public StruveSaturnPoleModel() {
		super(167.9680 * Math.PI/180.0, 28.0758 * Math.PI/180.0, 0.0);
		this.sourceEpoch = erm.BesselianEpoch(1889.25);
	}

	public SaturnPolePosition getPolePosition(double epoch) {
		return nodeAndInclinationToPolePosition(epoch);
	}
}