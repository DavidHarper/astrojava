package com.obliquity.astronomy;

public class StateVector {
    private Vector position = null;
    private Vector velocity = null;

    public StateVector(Vector position, Vector velocity) {
	this.position = position;
	this.velocity = velocity;
    }

    public void setPosition(Vector position) {
	this.position = position;
    }

    public Vector getPosition() { return position; }

    public void setPositionComponents(double[] components) {
	if (position == null)
	    position = new Vector(components);
	else
	    position.setComponents(components);
    }

    public double[] getPositionComponents() {
	if (position == null)
	    return null;
	else
	    return position.getComponents();
    }

    public void setVelocity(Vector velocity) {
	this.velocity = velocity;
    }

    public Vector getVelocity() { return velocity; }

    public void setVelocityComponents(double[] components) {
	if (velocity == null)
	    velocity = new Vector(components);
	else
	    velocity.setComponents(components);
    }

    public double[] getVelocityComponents() {
	if (velocity == null)
	    return null;
	else
	    return velocity.getComponents();
    }
}
