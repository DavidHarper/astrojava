package com.obliquity.astronomy.almanac.saturnpole;

public class StruveSaturnPoleModel extends EclipticNodeAndInclinationSaturnPoleModel {
	private final double sourceEpoch;
	
	public StruveSaturnPoleModel() {
		super(167.9680 * Math.PI/180.0, 28.0758 * Math.PI/180.0);
		this.sourceEpoch = erm.BesselianEpoch(1889.25);
	}
	
	double getSourceEpoch() {
		return sourceEpoch;
	}

	public SaturnPolePosition getPolePosition(double epoch) {
		return nodeAndInclinationToPolePosition(epoch);
	}
}
