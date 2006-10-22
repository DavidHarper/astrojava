package com.obliquity.astronomy;

public class MoonCentre implements MovingPoint {
	protected JPLEphemeris ephemeris = null;

	protected StateVector statevector = new StateVector(new Vector(),
			new Vector());
	protected StateVector moonstatevector = new StateVector(new Vector(),
			new Vector());

	protected double mu = 0.0;

	protected double reciprocalAU;

	public MoonCentre(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;
		double emrat = ephemeris.getEMRAT();
		mu = emrat / (1.0 + emrat);
		reciprocalAU = 1.0 / ephemeris.getAU();
	}

	public StateVector getStateVector(double time) throws JPLEphemerisException {
		getStateVector(time, statevector);
		return statevector;
	}

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException {
		Vector position = sv.getPosition();
		Vector velocity = sv.getVelocity();
		Vector moonposition = moonstatevector.getPosition();
		Vector moonvelocity = moonstatevector.getVelocity();

		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.EMB,
				position, velocity);
		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.MOON,
				moonposition, moonvelocity);

		moonposition.multiplyBy(mu);
		moonvelocity.multiplyBy(mu);

		position.add(moonposition);
		position.multiplyBy(reciprocalAU);
		velocity.add(moonvelocity);
		velocity.multiplyBy(reciprocalAU);
	}

	public Vector getPosition(double time) throws JPLEphemerisException {
		Vector position = statevector.getPosition();
		getPosition(time, position);
		return position;
	}

	public void getPosition(double time, Vector p) throws JPLEphemerisException {
		Vector moonposition = moonstatevector.getPosition();

		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.EMB, p, null);
		ephemeris.calculatePositionAndVelocity(time, JPLEphemeris.MOON,
				moonposition, null);

		moonposition.multiplyBy(mu);
		p.add(moonposition);
		p.multiplyBy(reciprocalAU);
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
	
	public int getBodyCode() { return JPLEphemeris.MOON; }
	
	public JPLEphemeris getEphemeris() {
			return ephemeris;
	}
}
