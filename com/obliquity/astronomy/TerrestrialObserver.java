package com.obliquity.astronomy;

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
