package com.obliquity.astronomy;

public interface MovingPoint {
	public StateVector getStateVector(double time) throws JPLEphemerisException;

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException;

	public Vector getPosition(double time) throws JPLEphemerisException;

	public void getPosition(double time, Vector v) throws JPLEphemerisException;

	public boolean isValidDate(double time);

	public double getEarliestDate();

	public double getLatestDate();

	public double getEpoch();
}
