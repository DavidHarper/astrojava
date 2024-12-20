/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2018 David Harper at obliquity.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 */

package com.obliquity.astronomy.almanac;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * This class encapsulates a JPL planetary ephemeris such as DE200 or DE405. It
 * provides methods for evaluating the position and velocity of any object in
 * the ephemeris at any time instant which is within the range of the
 * ephemeris.
 * <P>
 * An object of this class may be created directly from the binary JPL ephemeris
 * files which are distributed via the JPL ftp site:
 * ftp://ssd.jpl.nasa.gov/pub/eph/planets/
 * <P>
 * This version of the code can read both the big-endian (SunOS) and
 * the little-endian (Linux) binary data files. The constructor code
 * determines the binary byte order automatically by reading the ephemeris
 * number at offset 2840. If it is not within the range [0,2000] when
 * read as a big-endian integer, the file is assumed to be in little-endian
 * format, and all data are byte-reversed as they are read from the file.
 * <P>
 * The user may specify a time span for which the ephemeris object should be
 * able to return positions and velocities. Only the data records need to cover
 * this time span will be loaded from the file.
 * <P>
 * This class is serializable, which means that once an instance has been
 * created from a binary ephemeris file, it may be saved to disk and re-created
 * via the ObjectInputStream.readObject method. This allows a subset of a JPL
 * ephemeris to be serialized as part of a JAR package or retrieved by an applet
 * or other network-aware Java application.
 */

public class JPLEphemeris implements Serializable {
	private static final long serialVersionUID = -2708495076030198158L;
	private double[] limits = null;
	private int[][] offsets = null;
	private double AU;
	private double EMRAT;
	private int numde = -1;
	private double[][] data = null;
	private int nCheby = 0;
	transient double[] ChebyP = null;
	transient double[] ChebyV = null;
	private double[] pos = new double[3];
	private double[] vel = new double[3];
	private Map<String, Double> mapConstants = new HashMap<String, Double>();
	
	private final int CNAME_OFFSET = 6 * 14 * 3;
	private final int LIMITS_OFFSET = CNAME_OFFSET + 2400;
	private final int NCON_OFFSET = LIMITS_OFFSET + 3 * 8;
	private final int AU_OFFSET = NCON_OFFSET + 4;
	private final int EMRAT_OFFSET = AU_OFFSET + 8;
	private final int OFFSETS_OFFSET = EMRAT_OFFSET + 8;
	private final int NUMDE_OFFSET = OFFSETS_OFFSET + 12 * 3 * 4;
	private final int EXTRA_OFFSETS_OFFSET = NUMDE_OFFSET + 4;
	private final int EXTRA_CNAME_OFFSET = EXTRA_OFFSETS_OFFSET + 3 * 4;

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
	 * Constructs a new JPLEphemeris object for a specified time span from a
	 * binary JPL ephemeris file.
	 * 
	 * @param file
	 *            The binary JPL ephemeris file from which the data
	 *            will be loaded.
	 * 
	 * @param jdstart
	 *            The earliest time (expressed as a Julian Date in Barycentric
	 *            Dynamical Time) at which the ephemeris must be able to return
	 *            positions and velocities. If zero, use the lower date limit
	 *            implicit in the ephemeris file.
	 * 
	 * @param jfinis
	 *            The latest time (expressed as a Julian Date in Barycentric
	 *            Dynamical Time) at which the ephemeris must be able to return
	 *            positions and velocities. If zero, use the upper date limit
	 *            implicit in the ephemeris file.
	 */

	public JPLEphemeris(File file, double jdstart, double jdfinis)
			throws IOException, JPLEphemerisException {
		if (jdstart > jdfinis)
			throw new JPLEphemerisException(
					"Start date is greater than end date");

		RandomAccessFile raf = new RandomAccessFile(file, "r");

		FileChannel fc = raf.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(4);
		
		fc.position(NUMDE_OFFSET);

		fc.read(buffer);
		
		buffer.flip();

		buffer.order(ByteOrder.BIG_ENDIAN);

		numde = buffer.getInt();

		if (!isValidEphemerisNumber(numde)) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			buffer.rewind();
			
			numde = buffer.getInt();
		}
		
		// Get the number of coefficients per record for this ephemeris number.
		int ndata = getNumberOfCoefficientsPerRecord(numde);
		
		if (ndata < 0) {
			fc.close();
			raf.close();
			throw new JPLEphemerisException("Ephemeris number " + numde
					+ " not recognised");			
		}

		int reclen = 8 * ndata;
		
		// Create a new ByteBuffer which is the correct record length for this ephemeris
		ByteOrder byteOrder = buffer.order();
		
		buffer = ByteBuffer.allocate(reclen);
		
		buffer.order(byteOrder);
		
		fc.position(0);
		
		fc.read(buffer);
		
		buffer.flip();

		buffer.position(LIMITS_OFFSET);
		
		limits = new double[3];

		for (int j = 0; j < 3; j++) {
			limits[j] = buffer.getDouble();
		}

		if (jdstart == 0.0)
			jdstart = limits[0];

		if (jdfinis == 0.0)
			jdfinis = limits[1] - limits[2];

		if (jdstart < limits[0] || jdstart > limits[1]) {
			fc.close();
			raf.close();
		
			throw new JPLEphemerisException("Start date is outside valid range");
		}
		
		if (jdfinis < limits[0] || jdfinis > limits[1]) {
			fc.close();
			raf.close();
			
			throw new JPLEphemerisException("End date is outside valid range");
		}

		AU = buffer.getDouble(AU_OFFSET);
		EMRAT = buffer.getDouble(EMRAT_OFFSET);

		offsets = new int[13][3];

		buffer.position(OFFSETS_OFFSET);
		
		for (int j = 0; j < 12; j++) {
			for (int k = 0; k < 3; k++) {
				offsets[j][k] = buffer.getInt();
			}
		}

		buffer.position(EXTRA_OFFSETS_OFFSET);
		
		for (int k = 0; k < 3; k++) {
			offsets[12][k] = buffer.getInt();
		}
		
		// Read the value of NCON.  This is the number of constants in the file.
		// It may be greater than 400 for some versions.
		int ncon = buffer.getInt(NCON_OFFSET);
		
		// The block of bytes containing the names of constants is always at least
		// 2400 bytes long, but if NCON is greater than 400, we must allocate a larger
		// byte array and read the first 2400 bytes here.
		int arraySize = ncon < 400 ? 2400 : ncon * 6;

		byte cnam[] = new byte[arraySize];
		
		buffer.position(CNAME_OFFSET);
		
		buffer.get(cnam, 0, 2400);
		
		// If there are more than 400 constants, we must read the remainder of the
		// bytes containing the names of the constants.
		if (ncon > 400) {
			buffer.position(EXTRA_CNAME_OFFSET);
		
			buffer.get(cnam, 2400, arraySize - 2400);
		}
		
		int firstrec = (int) ((jdstart - limits[0]) / limits[2]);
		int lastrec = (int) ((jdfinis - limits[0]) / limits[2]);
		int numrecs = 0;
		
		
		// Read record #2, which contains the values of the constants as an array
		// of double values.
		fc.position(reclen);
	
		buffer.clear();
		
		fc.read(buffer);
		
		buffer.flip();

		for (int iconst = 0; iconst < ncon; iconst++) {
			String cname = new String(cnam, iconst * 6, 6, "UTF-8").trim();

			double cval = buffer.getDouble();

			mapConstants.put(cname, cval);
		}

		long offset = (firstrec + 2) * reclen;
		fc.position(offset);

		numrecs = (int) Math.round((limits[1] - limits[0]) / limits[2]);

		numrecs = lastrec - firstrec + 1;

		data = new double[numrecs][ndata];

		for (int j = 0; j < numrecs; j++) {
			buffer.clear();
			
			fc.read(buffer);
			
			buffer.flip();
			
			for (int k = 0; k < ndata; k++)
				data[j][k] = buffer.getDouble();
		}

		fc.close();
		raf.close();

		limits[0] = data[0][0];
		limits[1] = data[numrecs - 1][1];

		for (int i = 0; i < offsets.length; i++)
			if (offsets[i][1] > nCheby)
				nCheby = offsets[i][1];
	}
	
	private boolean isValidEphemerisNumber(int numde) {
		return numde > 0 && numde < 2000;
	}
	
	/**
	 * Return the number of double-precision coefficients per record,
	 * given the ephemeris number.
	 * 
	 * This is the NCOEFF parameter which is defined on the first line of the
	 * header.NNN file for each ephemeris.
	 * 
	 * The list of available ephemerides is given in this document:
	 * ftp://ssd.jpl.nasa.gov/pub/eph/planets/README.txt
	 * 
	 * @param ephemerisNumber
	 * The number of the ephemeris, for example 406 for DE406/LE406.
	 * 
	 * @return The number of double-precision coefficients per record, or -1 if the ephemeris number is unknown.
	 */
	private int getNumberOfCoefficientsPerRecord(int ephemerisNumber) {
		switch (ephemerisNumber) {
		case 102:
			return 773;
			
		case 200:
		case 202:
			return 826;

		case 403:
		case 405:
		case 410:
		case 413:
		case 414:
		case 418:
		case 421:
		case 422:
		case 423:
		case 430:
		case 431:
		case 440:
		case 441:
			return 1018;

		case 406:
			return 728;

		default:
			return -1;
		}
	}

	/**
	 * Constructs a new JPLEphemeris object from a binary JPL ephemeris file.
	 * 
	 * @param filename
	 *            The name of the binary JPL ephemeris file from which the data
	 *            will be loaded.
	 */
	
	public JPLEphemeris(File file)
		throws IOException, JPLEphemerisException {
		this(file, 0.0, 0.0);
	}
	
	/**
	 * Constructs a new JPLEphemeris object for a specified time span from a
	 * binary JPL ephemeris file.
	 * 
	 * @param filename
	 *            The name of the binary JPL ephemeris file from which the data
	 *            will be loaded.
	 * 
	 * @param jdstart
	 *            The earliest time (expressed as a Julian Date in Barycentric
	 *            Dynamical Time) at which the ephemeris must be able to return
	 *            positions and velocities. If zero, use the lower date limit
	 *            implicit in the ephemeris file.
	 * 
	 * @param jfinis
	 *            The latest time (expressed as a Julian Date in Barycentric
	 *            Dynamical Time) at which the ephemeris must be able to return
	 *            positions and velocities. If zero, use the upper date limit
	 *            implicit in the ephemeris file.
	 */

	public JPLEphemeris(String filename, double jdstart, double jdfinis)
			throws IOException, JPLEphemerisException {
		this(new File(filename), jdstart, jdfinis);
	}
	
	/**
	 * Constructs a new JPLEphemeris object from a binary JPL ephemeris file.
	 * 
	 * @param filename
	 *            The name of the binary JPL ephemeris file from which the data
	 *            will be loaded.
	 */

	public JPLEphemeris(String filename) throws IOException,
			JPLEphemerisException {
		this(filename, 0.0, 0.0);
	}

	/**
	 * Return the value of the AU parameter contained within the JPL ephemeris.
	 * This is the length of the astronomical unit expressed in kilometres.
	 * 
	 * @return The length of an astronomical unit,, in kilometres.
	 */
	public double getAU() {
		return AU;
	}

	/**
	 * Return the value of the Earth/Moon mass ratio contained within the JPL
	 * ephemeris.
	 * 
	 * @return The Earth/Moon mass ratio.
	 */
	public double getEMRAT() {
		return EMRAT;
	}

	/**
	 * Return the value of the named constant as a Double object.
	 * 
	 * @param cname
	 *            the name of the constant.
	 * @return the value of the constant as a Double object.
	 */
	public Double getConstant(String cname) {
		return (Double) mapConstants.get(cname);
	}

	/**
	 * Return the entry set of the constants map.
	 * 
	 * @return the entry set of the constants map.
	 */
	public Set<Map.Entry<String,Double>> getConstantsEntrySet() {
		return mapConstants.entrySet();
	}

	/**
	 * Return the DE ephemeris number of the JPL ephemeris which was used to
	 * create this object.
	 * 
	 * @return The DE ephemeris number.
	 */
	public int getEphemerisNumber() {
		return numde;
	}

	/**
	 * Return the earliest date for which this object can evaluate positions and
	 * velocities. Note that it may be earlier than the argument specified in
	 * the constructor, because the ephemeris data are stored as Chebyshev
	 * coefficients taken directly from the binary JPL ephemeris file.
	 * 
	 * @return The earliest date for which this ephemeris is valid.
	 */
	public double getEarliestDate() {
		return limits[0];
	}

	/**
	 * Return the latest date for which this object can evaluate positions and
	 * velocities. Note that it may be later than the argument specified in the
	 * constructor, because the ephemeris data are stored as Chebyshev
	 * coefficients taken directly from the binary JPL ephemeris file.
	 * 
	 * @return The latest date for which this ephemeris is valid.
	 */
	public double getLatestDate() {
		return limits[1];
	}

	/**
	 * Indicates whether the ephemeris can evaluate positions and velocities for
	 * the specified date.
	 * 
	 * @param t
	 *            The date which is to be tested.
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
	public double getEpoch() {
		return EPOCH;
	}

	/**
	 * Indicates whether the ephemeris contains Chebyshev coefficients for the
	 * component identified by the specified code.
	 * 
	 * @param k
	 *            The code which specifies the planet or quantity (such as
	 *            librations) to be tested. This will normally be one of the
	 *            public constants such as JPLEphemeris.LIBRATIONS. All
	 *            ephemerides contain Chebyshev coefficients for the Sun, Moon
	 *            and planets; they do not all contain coefficients for
	 *            nutations and librations.
	 * 
	 * @return True if the ephemeris contains Chebyshev coefficients for the
	 *         specified component; false otherwise. Note that if the user
	 *         attempts to evaluate the ephemeris for a component which is not
	 *         present, an exception will be thrown.
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
			ChebyP[i] = 2.0 * x * ChebyP[i - 1] - ChebyP[i - 2];
			ChebyV[i] = 2.0 * x * ChebyV[i - 1] - ChebyV[i - 2] + 2.0
					* ChebyP[i - 1];
		}
	}

	/**
	 * This is the main routine which most client programs will call in order to
	 * evaluate the ephemeris for a specified component at a specified date.
	 * 
	 * @param jd
	 *            The time argument, expressed as a Julian Date in Barycentric
	 *            Dynamical Time, at which the ephemeris is to be evaluated.
	 * 
	 * @param body
	 *            The code representing the component (Sun, Moon, planet,
	 *            nutation, libration) which is to be evaluated. This should be
	 *            one of the public constants such as JPLEphemeris.JUPITER.
	 * 
	 * @param pos
	 *            The output array into which the position vector components
	 *            will be stored. The length of this array must be at least
	 *            three for planetary positions, and at least two for nutations
	 *            and librations.
	 * 
	 * @param vel
	 *            The output array into which the velocity vector components
	 *            will be stored. This argument may be null, indicating that the
	 *            velocity is not required. However, if it is non-null, then its
	 *            length must be at least three for planetary positions and at
	 *            least two for nutations and librations.
	 */
	public void calculatePositionAndVelocity(double jd, int nBody,
			Vector position, Vector velocity) throws JPLEphemerisException {
		if (position == null)
			throw new JPLEphemerisException("Position vector is null");

		if (!hasComponent(nBody))
			throw new JPLEphemerisException(
					"Ephemeris does not have component " + nBody);

		if (!isValidDate(jd))
			throw new JPLEphemerisException("Date " + jd + " is out of range");

		int nCoords = (nBody <= SUN) ? 3 : 2;

		int irec = (int) ((jd - limits[0]) / limits[2]);

		int ioff = offsets[nBody][0] - 1;
		int icoeff = offsets[nBody][1];
		int isubr = offsets[nBody][2];

		double t0 = data[irec][0];
		double dt = limits[2];

		double dsubr = (double) isubr;
		double dx = (jd - t0) * dsubr / dt;
		double vfac = 2.0 * dsubr / dt;

		int ix = (int) dx;
		dx -= (double) ix;

		ioff += ix * icoeff * nCoords;

		dx = 2.0 * dx - 1.0;

		calculateChebyshevCoefficients(dx, icoeff);

		for (int i = 0; i < nCoords; i++, ioff += icoeff) {
			pos[i] = data[irec][ioff];
			for (int j = 1; j < icoeff; j++)
				pos[i] += ChebyP[j] * data[irec][ioff + j];

			if (velocity != null) {
				vel[i] = 0.0;
				for (int j = 1; j < icoeff; j++)
					vel[i] += ChebyV[j] * data[irec][ioff + j];
			}
		}

		position.setComponents(pos);

		if (velocity != null) {
			for (int i = 0; i < nCoords; i++)
				vel[i] *= vfac;

			velocity.setComponents(vel);
		}
	}
}
