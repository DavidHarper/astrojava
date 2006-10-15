package com.obliquity.astronomy;

public class PlanetCentre implements MovingPoint {
	private JPLEphemeris ephemeris = null;
	private int kBody = -1;
	private double AU;

	private StateVector statevector = new StateVector(new Vector(),
			new Vector());

	public PlanetCentre(JPLEphemeris ephemeris, int kBody) {
		this.ephemeris = ephemeris;
		this.kBody = kBody;
		AU = 1.0 / ephemeris.getAU();
	}

	public StateVector getStateVector(double time) throws JPLEphemerisException {
		getStateVector(time, statevector);
		return statevector;
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		Vector position = sv.getPosition();
		Vector velocity = sv.getVelocity();

		ephemeris.calculatePositionAndVelocity(time, kBody, position, velocity);

		position.multiplyBy(AU);
		velocity.multiplyBy(AU);
	}

	public Vector getPosition(double time) throws JPLEphemerisException {
		Vector position = statevector.getPosition();
		getPosition(time, position);
		return position;
	}

	public void getPosition(double time, Vector p) throws JPLEphemerisException {
		ephemeris.calculatePositionAndVelocity(time, kBody, p, null);
		p.multiplyBy(AU);
	}

	public boolean isValidDate(double time) {
		return ephemeris.isValidDate(time);
	}

	public double getEarliestDate() {
		return ephemeris.getEarliestDate();
	}

	public double getLatestDate() {
		return ephemeris.getLatestDate();
	}

	public double getEpoch() {
		return ephemeris.getEpoch();
	}
}
