package com.obliquity.astronomy;

public interface EarthRotationModel {
    public double meanObliquity(double JD);

    public double deltaT(double JD);

    public double greenwichMeanSiderealTime(double JD);

    public double greenwichApparentSiderealTime(double JD);

    public PrecessionAngles precessionAngles(double jdFixed, double jdOfDate);

    public void precessionAngles(double jdFixed, double jdOfDate, PrecessionAngles angles);

    public Matrix precessionMatrix(double jdFixed, double jdOfDate);

    public void precessionMatrix(double jdFixed, double jdOfDate, Matrix matrix);

    public NutationAngles nutationAngles(double t);

    public void nutationAngles(double t, NutationAngles angles);

    public Matrix nutationMatrix(double t);

    public void nutationMatrix(double t, Matrix matrix);
}
