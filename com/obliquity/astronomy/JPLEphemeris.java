package com.obliquity.astronomy;

import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

/**
 * This class encapsulates a JPL planetary ephemeris such as DE200
 * or DE405. It provides methods for evaluating the position and
 * velocity of any object in the ephemeris at any time instant which
 * is withing the range of the ephemeris.
 * <P>
 * An object of this class may be created directly from the binary
 * JPL ephemeris files which are distributed by Myles Standish at
 * the JPL ftp site: ftp://ssd.jpl.nasa.gov/pub/eph/export/unix
 * <P>
 * The user may specify a time span for which the ephemeris object
 * should be able to return positions and velocities. Only the data
 * records need to cover this time span will be loaded from the file.
 * <P>
 * This class is serializable, which means that once an instance has
 * been created from a binary ephemeris file, it may be saved to disk
 * and re-created via the ObjectInputStream.readObject method. This
 * allows a subset of a JPL ephemeris to be serialized as part of a
 * JAR package or retrieved by an applet or other network-aware
 * Java application.
 */

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
    private double []pos = new double[3];
    private double []vel = new double[3];
    private Map mapConstants = new HashMap();

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

    private static final double EPOCH = 2451545.0;

    /**
     * Constructs a new JPLEphemeris object for a specified time span from a binary
     * JPL ephemeris file.
     *
     * @param filename The name of the binary JPL ephemeris file from which the data
     *                 will be loaded.
     *
     * @param jdstart The earliest time (expressed as a Julian Date in Barycentric Dynamical Time)
     *                at which the ephemeris must be able to return positions and
     *                velocities. If zero, use the lower date limit implicit in the ephemeris file.
     *
     * @param jfinis The latest time (expressed as a Julian Date in Barycentric Dynamical Time)
     *                at which the ephemeris must be able to return positions and
     *                velocities. If zero, use the upper date limit implicit in the ephemeris file.
     */

    public JPLEphemeris(String filename, double jdstart, double jdfinis) throws
      IOException, JPLEphemerisException {
	if (jdstart > jdfinis)
	    throw new JPLEphemerisException("Start date is greater than end date");

	File file = new File(filename);

	FileInputStream fis = new FileInputStream(file);
	DataInputStream dis = new DataInputStream(fis);

	int position = 0;

	int offset = 6 * 14 * 3;

	dis.skipBytes(offset);

	position += offset;

	byte cnam[] = new byte[2400];

	dis.readFully(cnam);

	position += 2400;

	limits = new double[3];

	for (int j = 0; j < 3; j++) {
	    limits[j] = dis.readDouble();
	    position += 8;
	}

	int ncon = dis.readInt();

	position += 4;

	AU = dis.readDouble();
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

	if (jdstart == 0.0)
	    jdstart = limits[0];

	if (jdfinis == 0.0)
	    jdfinis = limits[1] - limits[2];

	if (jdstart < limits[0] || jdstart > limits[1])
	    throw new JPLEphemerisException("Start date is outside valid range");

	if (jdfinis < limits[0] || jdfinis > limits[1])
	    throw new JPLEphemerisException("End date is outside valid range");

	int firstrec = (int)((jdstart - limits[0])/limits[2]);
	int lastrec  = (int)((jdfinis - limits[0])/limits[2]);
	int numrecs = 0;

	offset = reclen - position;
	dis.skipBytes(offset);

	position = 0;

	for (int iconst = 0; iconst < ncon; iconst++) {
	    double cval = dis.readDouble();
	    position += 8;

	    String cname = new String(cnam, iconst * 6, 6, "UTF-8").trim();

	    mapConstants.put(cname, new Double(cval));
	}

	offset = reclen - position;
	dis.skipBytes(offset);

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

    /**
     * Constructs a new JPLEphemeris object from a binary JPL ephemeris file.
     *
     * @param filename The name of the binary JPL ephemeris file from which the data
     *                 will be loaded.
     */

    public JPLEphemeris(String filename) throws IOException, JPLEphemerisException {
	this(filename, 0.0, 0.0);
    }

    /**
     * Return the value of the AU parameter contained within the JPL ephemeris.
     * This is the length of the astronomical unit expresssed in kilometres.
     *
     * @return The length of an astronomical unit,, in kilometres.
     */
    public double getAU() { return AU; }

    /**
     * Return the value of the Earth/Moon mass ratio contained within the JPL
     * ephemeris.
     *
     * @return The Earth/Moon mass ratio.
     */
    public double getEMRAT() { return EMRAT; }

    /**
     * Return the value of the named constant as a Double object.
     *
     * @param cname the name of the constant.
     * @return the value of the constant as a Double object.
     */
    public Double getConstant(String cname) { return (Double)mapConstants.get(cname); }

    /**
     * Return the entry set of the constants map.
     *
     * @return the entry set of the constants map.
     */
    public Set getConstantsEntrySet() { return mapConstants.entrySet(); }

    /**
     * Return the DE ephemeris number of the JPL ephemeris which was used to
     * create this object.
     *
     * @return The DE ephemeris number.
     */
    public int getEphemerisNumber() { return numde; }

    /**
     * Return the earliest date for which this object can evaluate positions
     * and velocities. Note that it may be earlier than the argument specified
     * in the constructor, because the ephemeris data are stored as Chebyshev
     * coefficients taken directly from the binary JPL ephemeris file.
     *
     * @return The earliest date for which this ephemeris is valid.
     */
    public double getEarliestDate() { return limits[0]; }

    /**
     * Return the latest date for which this object can evaluate positions
     * and velocities. Note that it may be later than the argument specified
     * in the constructor, because the ephemeris data are stored as Chebyshev
     * coefficients taken directly from the binary JPL ephemeris file.
     *
     * @return The latest date for which this ephemeris is valid.
     */
    public double getLatestDate() { return limits[1]; }

    /**
     * Indicates whether the ephemeris can evaluate positions and velocities for
     * the specified date.
     *
     * @param t The date which is to be tested.
     *
     * @return True of the ephemeris covers the specified date, false otherwise.
     *         If this function returns false, then an exception will be thrown
     *         if the user attempts to evaluate positions and velocities at the
     *         specified date.
     */
    public boolean isValidDate(double t) {
	if (t < limits[0] || t > limits[1])
	    return false;
	else
	    return true;
    }

    /**
     * Return the epoch of the reference system of this ephemeris.
     *
     * @return The epoch of the reference system of this ephemeris.
     */
    public double getEpoch() { return EPOCH; }

    /**
     * Indicates whether the ephemeris contains Chebyshev coefficients for the
     * component identified by the specified code.
     *
     * @param k The code which specifies the planet or quantity (such as librations)
     *          to be tested. This will normally be one of the public constants
     *          such as JPLEphemeris.LIBRATIONS. All ephemerides contain Chebyshev
     *          coefficients for the Sun, Moon and planets; they do not all contain
     *          coefficients for nutations and librations.
     *
     * @return True if the ephemeris contains Chebyshev coefficients for the specified
     *         component; false otherwise. Note that if the user attempts to evaluate
     *         the ephemeris for a component which is not present, an exception will
     *         be thrown.
     */
    public boolean hasComponent(int k) {
	if (k < 0 || k > LIBRATIONS)
	    return false;

	if (offsets[k][1] > 0)
	    return true;
	else
	    return false;
    }

    /**
     * Returns the number of data records which this object contains.
     *
     * @return The number of data records.
     */
    public int getNumberOfDataRecords() {
	return data.length;
    }

    /**
     * Returns the length of the data records in this object.
     *
     * @return The number of double-precision values in each data record.
     */
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

    /**
     * This is the main routine which most client programs will call in order to
     * evaluate the ephemeris for a specified component at a specified date.
     *
     * @param jd The time argument, expressed as a Julian Date in Barycentric
     *           Dynamical Time, at which the ephemeris is to be evaluated.
     *
     * @param body The code representing the component (Sun, Moon, planet, nutation,
     *             libration) which is to be evaluated. This should be one of the
     *             public constants such as JPLEphemeris.JUPITER.
     *
     * @param pos The output array into which the position vector components will
     *            be stored. The length of this array must be at least three for
     *            planetary positions, and at least two for nutations and librations.
     *
     * @param vel The output array into which the velocity vector components will
     *            be stored. This argument may be null, indicating that the velocity
     *            is not required. However, if it is non-null, then its length must
     *            be at least three for planetary positions and at least two for
     *            nutations and librations.
     */
    public void calculatePositionAndVelocity(double jd, int nBody, Vector position, Vector velocity) 
      throws JPLEphemerisException {
	if (position == null)
	    throw new JPLEphemerisException("Position vector is null");

	if (!hasComponent(nBody))
	    throw new JPLEphemerisException("Ephemeris does not have component " + nBody);

	if (!isValidDate(jd))
	    throw new JPLEphemerisException("Date " + jd + " is out of range");

	int nCoords = (nBody <= SUN) ? 3 : 2;

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

	    if (velocity != null) {
		vel[i] = 0.0;
		for (int j = 1; j<icoeff; j++)
		    vel[i] += ChebyV[j] * data[irec][ioff+j];
	    }
	}

	position.setComponents(pos);

	if (velocity != null) {
	    for (int i = 0; i<nCoords; i++)
		vel[i] *= vfac;

	    velocity.setComponents(vel);
	}
    }
}
