package com.obliquity.astronomy;

import java.io.*;
import java.lang.*;
import java.text.*;

public class JPLEphemeris implements Serializable {
    private double []limits = null;
    private int [][]offsets = null;
    private double AU;
    private double EMRAT;
    private int numde = -1;
    private double [][]data = null;
    private int nCheby = 0;
    transient double []ChebyP = null;
    transient double []ChebyV = null;

    public static final int FIRST_COMPONENT = 0;
    public static final int MERCURY = 0;
    public static final int VENUS = 1;
    public static final int EMB = 2;
    public static final int EARTHMOONBARYCENTRE = 2;
    public static final int EARTHMOONBARYCENTER = 2;
    public static final int MARS = 3;
    public static final int JUPITER = 4;
    public static final int SATURN = 5;
    public static final int URANUS = 6;
    public static final int NEPTUNE = 7;
    public static final int PLUTO = 8;
    public static final int MOON = 9;
    public static final int SUN = 10;
    public static final int NUTATIONS = 11;
    public static final int LIBRATIONS = 12;
    public static final int LAST_COMPONENT = 12;

    public JPLEphemeris(String filename, double jdstart, double jdfinis) throws
      IOException, JPLEphemerisException {
	if (jdstart > jdfinis)
	    throw new JPLEphemerisException("Start date is greater than end date");

	File file = new File(filename);

	FileInputStream fis = new FileInputStream(file);
	DataInputStream dis = new DataInputStream(fis);

	int position = 0;

	int offset = 6 * (14 * 3 + 400);

	dis.skipBytes(offset);
	position += offset;

	limits = new double[3];

	for (int j = 0; j < 3; j++) {
	    limits[j] = dis.readDouble();
	    position += 8;
	}

	offset = 4;
	dis.skipBytes(offset);
	position += offset;

	AU= dis.readDouble();
	EMRAT = dis.readDouble();

	position += 16;

	offsets = new int[13][3];

	for (int j = 0; j < 12; j++) {
	    for (int k = 0; k < 3; k++) {
		offsets[j][k] = dis.readInt();
		position += 4;
	    }
	}

	numde = dis.readInt();
	position += 4;

	for (int k = 0; k < 3; k++) {
	    offsets[12][k] = dis.readInt();
	    position += 4;
	}

	int reclen = -1, ndata = 0;

	switch (numde) {
	case 200:
	    reclen = 1652 * 4;
	    ndata = 826;
	    break;

	case 405:
	    reclen = 2036 * 4;
	    ndata = 1018;
	    break;

	case 406:
	    reclen = 1456 * 4;
	    ndata = 728;
	    break;

	default:
	    throw new JPLEphemerisException("Ephemeris number " + numde + " not recognised");
	}

	if (jdstart < limits[0] || jdstart > limits[1])
	    throw new JPLEphemerisException("Start date is outside valid range");

	if (jdfinis < limits[0] || jdfinis > limits[1])
	    throw new JPLEphemerisException("End date is outside valid range");

	int firstrec = (int)((jdstart - limits[0])/limits[2]);
	int lastrec  = (int)((jdfinis - limits[0])/limits[2]);
	int numrecs = 0;

	offset = reclen - position;
	dis.skipBytes(offset);
	dis.skipBytes(reclen);

	numrecs = (int)Math.round((limits[1] - limits[0])/limits[2]);

	for (int j = 0; j < firstrec; j++)
	    dis.skipBytes(reclen);
	    
	numrecs = lastrec - firstrec + 1;
		
	data = new double[numrecs][ndata];

	for (int j = 0; j < numrecs; j++) {
	    for (int k = 0; k < ndata; k++)
		data[j][k] = dis.readDouble();
	}

	dis.close();

	limits[0] = data[0][0];
	limits[1] = data[numrecs-1][1];

	for (int i = 0; i < offsets.length; i++)
	    if (offsets[i][1] > nCheby)
		nCheby = offsets[i][1];
    }

    public double getAU() { return AU; }
    public double getEMRAT() { return EMRAT; }
    public int getEphemerisNumber() { return numde; }

    public double getEarliestDate() { return limits[0]; }
    public double getLatestDate() { return limits[1]; }

    public boolean isValidDate(double t) {
	if (t < limits[0] || t > limits[1])
	    return false;
	else
	    return true;
    }

    public boolean hasComponent(int k) {
	if (k < 0 || k > LIBRATIONS)
	    return false;

	if (offsets[k][1] > 0)
	    return true;
	else
	    return false;
    }

    public int getNumberOfDataRecords() {
	return data.length;
    }

    public int getLengthOfDataRecord() {
	return data[0].length;
    }

    private void initialiseChebyshevArrays() {
	ChebyP = new double[nCheby];
	ChebyV = new double[nCheby];
    }

    private void calculateChebyshevCoefficients(double x, int nOrder) {
	if (ChebyP == null || ChebyV == null)
	    initialiseChebyshevArrays();

	ChebyP[0] = 1.0;
	ChebyP[1] = x;

	ChebyV[0] = 0.0;
	ChebyV[1] = 1.0;

	for (int i = 2; i < nOrder; i++) {
	    ChebyP[i] = 2.0 * x * ChebyP[i-1] - ChebyP[i-2];
	    ChebyV[i] = 2.0 * x * ChebyV[i-1] - ChebyV[i-2] + 2.0 * ChebyP[i-1];
	}
    }


    public void calculatePositionAndVelocity(double jd, int nBody, double []pos, double []vel) 
      throws JPLEphemerisException {
	if (!hasComponent(nBody))
	    throw new JPLEphemerisException("Ephemeris does not have component " + nBody);

	if (!isValidDate(jd))
	    throw new JPLEphemerisException("Date " + jd + " is out of range");

	int nCoords = (nBody <= SUN) ? 3 : 2;

	if (pos.length < nCoords)
	    throw new JPLEphemerisException("Position array too short (length " + pos.length +
					    ") for " + nCoords + " values");

	int irec = (int)((jd - limits[0])/limits[2]);

	int ioff   = offsets[nBody][0] - 1;
	int icoeff = offsets[nBody][1];
	int isubr  = offsets[nBody][2];

	double t0 = data[irec][0];
	double dt = limits[2];

	double dsubr = (double)isubr;
	double dx = (jd - t0) * dsubr/dt;
	double vfac = 2.0 * dsubr/dt;

	int ix = (int)dx;
	dx -= (double)ix;

	ioff += ix * icoeff  * nCoords;

	dx = 2.0 * dx - 1.0;

	calculateChebyshevCoefficients(dx, icoeff);

	for (int i = 0; i < nCoords; i++, ioff += icoeff) {
	    pos[i] = data[irec][ioff];
	    for (int j = 1; j<icoeff; j++)
		pos[i] += ChebyP[j] * data[irec][ioff+j];

	    if (vel != null) {
		vel[i] = 0.0;
		for (int j = 1; j<icoeff; j++)
		    vel[i] += ChebyV[j] * data[irec][ioff+j];
	    }
	}

	if (vel != null)
	    for (int i = 0; i<nCoords; i++)
		vel[i] *= vfac;
    }
}
