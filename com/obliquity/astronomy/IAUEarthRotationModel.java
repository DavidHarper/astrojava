package com.obliquity.astronomy;

public class IAUEarthRotationModel implements EarthRotationModel {
    private PrecessionAngles pAngles = new PrecessionAngles();
    private NutationAngles nAngles = new NutationAngles();

    private final double S2R = Math.PI/(180.0 * 3600.0);
    private final double H2R = Math.PI/12.0;

    private final double J2000 = 2451545.0;
    private final double JCY = 36525.0;

    public double meanObliquity(double JD) {
	double T = (JD - J2000)/JCY;

	return (84381.448 - 46.8150 * T - 0.00059 * T*T + 0.001813 * T*T*T)*S2R;
    }

    public double deltaT(double JD) {
	double T = (JD - J2000)/JCY;

	double[][] data = {
	    {-25.0,    184.4,    111.6,     31.0,       0.0   },
	    {-17.0, -31527.7,  -4773.29,  -212.0889,   -3.93731},
	    {-10.0,   6833.0,   1996.25,   186.1189,    3.87068},
	    {-3.5,    -116.0,    -88.45,   -22.3509,   -3.07831},
	    {-3.0,   -4586.4,  -4715.24, -1615.08,   -184.71   },
	    {-2.0,    -427.3,   -556.12,  -228.71,    -30.67   },
	    {-1.5,     150.7,    310.8,    204.8,      41.6    },
	    {-1.0,    -150.5,   -291.7,   -196.9,     -47.7    },
	    {-0.7,     486.0,   1896.4,   2606.9,    1204.6    },
	    {+0.2,      65.9,     96.0,     35.0,     -20.2    },
	    {+2.0,      63.4,    111.6,     31.0,       0.0    }
	};

	for (int j = 0; j < data.length; j++) {
	    if (T < data[j][0])
		return (data[j][1] + T * ( data[j][2] + T * (data[j][3] + T * data[j][4])))/86400.0;
	}

	return 0.0;
    }

    public double greenwichMeanSiderealTime(double JDUT) {
	double T = (JDUT - J2000)/JCY;

	double gmst = 67310.54841 + 8640184.812866 * T + 0.093104 * T*T - 0.0000062 * T*T*T;

	gmst %= 86400.0;

	gmst /= 3600.0;

	gmst = (gmst + 876600.0*T) % 24.0;

	if (gmst < 0.0) gmst += 24.0;

	return H2R * gmst;
    }

    public double greenwichApparentSiderealTime(double JDUT) {
	double gmst = greenwichMeanSiderealTime(JDUT);
	nutationAngles(JDUT, nAngles);
	return gmst + nAngles.getDpsi();
    }

    public PrecessionAngles precessionAngles(double jdFixed, double jdOfDate) {
	PrecessionAngles pa = new PrecessionAngles();
	precessionAngles(jdFixed, jdOfDate, pa);
	return pa;
    }

    public void precessionAngles(double jdFixed, double jdOfDate, PrecessionAngles angles) {
	double data[][]={                         
	    {+2306.2181, +1.39656, -0.000139, +0.30188, -0.000344, +0.017998},
	    {+2306.2181, +1.39656, -0.000139, +1.09468, +0.000066, +0.018203},
	    {+2004.3109, -0.85330, -0.000217, -0.42665, -0.000217, -0.041833}
	};

	double T = (jdFixed - J2000)/JCY;
	double t = (jdOfDate - jdFixed)/JCY;

	double zeta = (data[0][0] + data[0][1] * T + data[0][2] * T * T) * t +
	    (data[0][3] + data[0][4] * T) * t * t  +
	    data[0][5] * t * t * t;

	double z = (data[1][0] + data[1][1] * T + data[1][2] * T * T) * t + 
	    (data[1][3] + data[1][4] * T) * t * t +
	    data[1][5] * t * t * t; 

	double theta = (data[2][0] + data[2][1] * T + data[2][2] * T * T) * t +
	    (data[2][3] + data[2][4] * T) * t * t +
	    data[2][5] * t * t * t;
 
	angles.setAngles(zeta * S2R, z * S2R, theta * S2R);
    }

    public Matrix precessionMatrix(double jdFixed, double jdOfDate) {
	Matrix m = new Matrix();
	precessionMatrix(jdFixed, jdOfDate, m);
	return m;
    }

    public void precessionMatrix(double jdFixed, double jdOfDate, Matrix matrix) {
	precessionAngles(jdFixed, jdOfDate, pAngles);
	precessionAnglesToMatrix(pAngles, matrix);
    }

    private void precessionAnglesToMatrix(PrecessionAngles pa, Matrix pm) {
	double zeta  = pa.getZeta();
	double z     = pa.getZ();
	double theta = pa.getTheta();

	double czeta = Math.cos(zeta);
	double szeta = Math.sin(zeta);
	double cz    = Math.cos(z);
	double sz    = Math.sin(z);
	double ct    = Math.cos(theta);
	double st    = Math.sin(theta);

	pm.setComponent(0, 0,  cz * ct * czeta - sz * szeta);
	pm.setComponent(0, 1, -cz * ct * szeta - sz * czeta);
	pm.setComponent(0, 2, -cz * st);

	pm.setComponent(1, 0,  sz * ct * czeta + cz * szeta);
	pm.setComponent(1, 1, -sz * ct * szeta + cz * czeta);
	pm.setComponent(1, 2, -sz * st);
 
	pm.setComponent(2, 0,  st * czeta);
	pm.setComponent(2, 1, -st * szeta);
	pm.setComponent(2, 2,  ct);
    }

    public NutationAngles nutationAngles(double JD) {
	NutationAngles na = new NutationAngles();
	nutationAngles(JD, na);
	return na;
    }

    private double frac(double x) {
	return x - Math.floor(x);
    }

    public void nutationAngles(double JD, NutationAngles angles) {
	double data[][]={
/* IAU 1980 nutation model
 *                    Fundamental angles        Longitude         Obliquity
 *              L     L'    F     D  Omega       (sine)            (cosine)
 */
	    {  0.0,  0.0,  0.0,  0.0,  1.0, -171996.0, -174.2,  92025.0,  8.9},
	    {  0.0,  0.0,  2.0, -2.0,  2.0,  -13187.0,   -1.6,   5736.0, -3.1},
	    {  0.0,  0.0,  2.0,  0.0,  2.0,   -2274.0,   -0.2,    977.0, -0.5},
	    {  0.0,  0.0,  0.0,  0.0,  2.0,    2062.0,    0.2,   -895.0,  0.5},
	    {  0.0,  1.0,  0.0,  0.0,  0.0,    1426.0,   -3.4,     54.0, -0.1},
	    {  1.0,  0.0,  0.0,  0.0,  0.0,     712.0,    0.1,     -7.0,  0.0},
	    {  0.0,  1.0,  2.0, -2.0,  2.0,    -517.0,    1.2,    224.0, -0.6},
	    {  0.0,  0.0,  2.0,  0.0,  1.0,    -386.0,   -0.4,    200.0,  0.0},
	    {  1.0,  0.0,  2.0,  0.0,  2.0,    -301.0,    0.0,    129.0, -0.1},
	    {  0.0, -1.0,  2.0, -2.0,  2.0,     217.0,   -0.5,    -95.0,  0.3},
	    {  1.0,  0.0,  0.0, -2.0,  0.0,    -158.0,    0.0,     -1.0,  0.0},
	    {  0.0,  0.0,  2.0, -2.0,  1.0,     129.0,    0.1,    -70.0,  0.0},
	    { -1.0,  0.0,  2.0,  0.0,  2.0,     123.0,    0.0,    -53.0,  0.0},
	    {  1.0,  0.0,  0.0,  0.0,  1.0,      63.0,    0.1,    -33.0,  0.0},
	    {  0.0,  0.0,  0.0,  2.0,  0.0,      63.0,    0.0,     -2.0,  0.0},
	    { -1.0,  0.0,  2.0,  2.0,  2.0,     -59.0,    0.0,     26.0,  0.0},
	    { -1.0,  0.0,  0.0,  0.0,  1.0,     -58.0,   -0.1,     32.0,  0.0},
	    {  1.0,  0.0,  2.0,  0.0,  1.0,     -51.0,    0.0,     27.0,  0.0},
	    {  2.0,  0.0,  0.0, -2.0,  0.0,      48.0,    0.0,      1.0,  0.0},
	    { -2.0,  0.0,  2.0,  0.0,  1.0,      46.0,    0.0,    -24.0,  0.0},
	    {  0.0,  0.0,  2.0,  2.0,  2.0,     -38.0,    0.0,     16.0,  0.0},
	    {  2.0,  0.0,  2.0,  0.0,  2.0,     -31.0,    0.0,     13.0,  0.0},
	    {  2.0,  0.0,  0.0,  0.0,  0.0,      29.0,    0.0,     -1.0,  0.0},
	    {  1.0,  0.0,  2.0, -2.0,  2.0,      29.0,    0.0,    -12.0,  0.0},
	    {  0.0,  0.0,  2.0,  0.0,  0.0,      26.0,    0.0,     -1.0,  0.0},
	    {  0.0,  0.0,  2.0, -2.0,  0.0,     -22.0,    0.0,      0.0,  0.0},
	    { -1.0,  0.0,  2.0,  0.0,  1.0,      21.0,    0.0,    -10.0,  0.0},
	    {  0.0,  2.0,  0.0,  0.0,  0.0,      17.0,   -0.1,      0.0,  0.0},
	    {  0.0,  2.0,  2.0, -2.0,  2.0,     -16.0,    0.1,      7.0,  0.0},
	    { -1.0,  0.0,  0.0,  2.0,  1.0,      16.0,    0.0,     -8.0,  0.0},
	    {  0.0,  1.0,  0.0,  0.0,  1.0,     -15.0,    0.0,      9.0,  0.0},
	    {  1.0,  0.0,  0.0, -2.0,  1.0,     -13.0,    0.0,      7.0,  0.0},
	    {  0.0, -1.0,  0.0,  0.0,  1.0,     -12.0,    0.0,      6.0,  0.0},
	    {  2.0,  0.0, -2.0,  0.0,  0.0,      11.0,    0.0,      0.0,  0.0},
	    { -1.0,  0.0,  2.0,  2.0,  1.0,     -10.0,    0.0,      5.0,  0.0},
	    {  1.0,  0.0,  2.0,  2.0,  2.0,      -8.0,    0.0,      3.0,  0.0},
	    {  0.0, -1.0,  2.0,  0.0,  2.0,      -7.0,    0.0,      3.0,  0.0},
	    {  0.0,  0.0,  2.0,  2.0,  1.0,      -7.0,    0.0,      3.0,  0.0},
	    {  1.0,  1.0,  0.0, -2.0,  0.0,      -7.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  2.0,  0.0,  2.0,       7.0,    0.0,     -3.0,  0.0},
	    { -2.0,  0.0,  0.0,  2.0,  1.0,      -6.0,    0.0,      3.0,  0.0},
	    {  0.0,  0.0,  0.0,  2.0,  1.0,      -6.0,    0.0,      3.0,  0.0},
	    {  2.0,  0.0,  2.0, -2.0,  2.0,       6.0,    0.0,     -3.0,  0.0},
	    {  1.0,  0.0,  0.0,  2.0,  0.0,       6.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0,  2.0, -2.0,  1.0,       6.0,    0.0,     -3.0,  0.0},
	    {  0.0,  0.0,  0.0, -2.0,  1.0,      -5.0,    0.0,      3.0,  0.0},
	    {  0.0, -1.0,  2.0, -2.0,  1.0,      -5.0,    0.0,      3.0,  0.0},
	    {  2.0,  0.0,  2.0,  0.0,  1.0,      -5.0,    0.0,      3.0,  0.0},
	    {  1.0, -1.0,  0.0,  0.0,  0.0,       5.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0,  0.0, -1.0,  0.0,      -4.0,    0.0,      0.0,  0.0},
	    {  0.0,  0.0,  0.0,  1.0,  0.0,      -4.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  0.0, -2.0,  0.0,      -4.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0, -2.0,  0.0,  0.0,       4.0,    0.0,      0.0,  0.0},
	    {  2.0,  0.0,  0.0, -2.0,  1.0,       4.0,    0.0,     -2.0,  0.0},
	    {  0.0,  1.0,  2.0, -2.0,  1.0,       4.0,    0.0,     -2.0,  0.0},
	    {  1.0,  1.0,  0.0,  0.0,  0.0,      -3.0,    0.0,      0.0,  0.0},
	    {  1.0, -1.0,  0.0, -1.0,  0.0,      -3.0,    0.0,      0.0,  0.0},
	    { -1.0, -1.0,  2.0,  2.0,  2.0,      -3.0,    0.0,      1.0,  0.0},
	    {  0.0, -1.0,  2.0,  2.0,  2.0,      -3.0,    0.0,      1.0,  0.0},
	    {  1.0, -1.0,  2.0,  0.0,  2.0,      -3.0,    0.0,      1.0,  0.0},
	    {  3.0,  0.0,  2.0,  0.0,  2.0,      -3.0,    0.0,      1.0,  0.0},
	    { -2.0,  0.0,  2.0,  0.0,  2.0,      -3.0,    0.0,      1.0,  0.0},
	    {  1.0,  0.0,  2.0,  0.0,  0.0,       3.0,    0.0,      0.0,  0.0},
	    { -1.0,  0.0,  2.0,  4.0,  2.0,      -2.0,    0.0,      1.0,  0.0},
	    {  1.0,  0.0,  0.0,  0.0,  2.0,      -2.0,    0.0,      1.0,  0.0},
	    { -1.0,  0.0,  2.0, -2.0,  1.0,      -2.0,    0.0,      1.0,  0.0},
	    {  0.0, -2.0,  2.0, -2.0,  1.0,      -2.0,    0.0,      1.0,  0.0},
	    { -2.0,  0.0,  0.0,  0.0,  1.0,      -2.0,    0.0,      1.0,  0.0},
	    {  2.0,  0.0,  0.0,  0.0,  1.0,       2.0,    0.0,     -1.0,  0.0},
	    {  3.0,  0.0,  0.0,  0.0,  0.0,       2.0,    0.0,      0.0,  0.0},
	    {  1.0,  1.0,  2.0,  0.0,  2.0,       2.0,    0.0,     -1.0,  0.0},
	    {  0.0,  0.0,  2.0,  1.0,  2.0,       2.0,    0.0,     -1.0,  0.0},
	    {  1.0,  0.0,  0.0,  2.0,  1.0,      -1.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0,  2.0,  2.0,  1.0,      -1.0,    0.0,      1.0,  0.0},
	    {  1.0,  1.0,  0.0, -2.0,  1.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  0.0,  2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  2.0, -2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0, -2.0,  2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0, -2.0,  2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0, -2.0, -2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0,  2.0, -2.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  1.0,  0.0,  0.0, -4.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  2.0,  0.0,  0.0, -4.0,  0.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  0.0,  2.0,  4.0,  2.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  0.0,  2.0, -1.0,  2.0,      -1.0,    0.0,      0.0,  0.0},
	    { -2.0,  0.0,  2.0,  4.0,  2.0,      -1.0,    0.0,      1.0,  0.0},
	    {  2.0,  0.0,  2.0,  2.0,  2.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0, -1.0,  2.0,  0.0,  1.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  0.0, -2.0,  0.0,  1.0,      -1.0,    0.0,      0.0,  0.0},
	    {  0.0,  0.0,  4.0, -2.0,  2.0,       1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  0.0,  0.0,  2.0,       1.0,    0.0,      0.0,  0.0},
	    {  1.0,  1.0,  2.0, -2.0,  2.0,       1.0,    0.0,     -1.0,  0.0},
	    {  3.0,  0.0,  2.0, -2.0,  2.0,       1.0,    0.0,      0.0,  0.0},
	    { -2.0,  0.0,  2.0,  2.0,  2.0,       1.0,    0.0,     -1.0,  0.0},
	    { -1.0,  0.0,  0.0,  0.0,  2.0,       1.0,    0.0,     -1.0,  0.0},
	    {  0.0,  0.0, -2.0,  2.0,  1.0,       1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  2.0,  0.0,  1.0,       1.0,    0.0,      0.0,  0.0},
	    { -1.0,  0.0,  4.0,  0.0,  2.0,       1.0,    0.0,      0.0,  0.0},
	    {  2.0,  1.0,  0.0, -2.0,  0.0,       1.0,    0.0,      0.0,  0.0},
	    {  2.0,  0.0,  0.0,  2.0,  0.0,       1.0,    0.0,      0.0,  0.0},
	    {  2.0,  0.0,  2.0, -2.0,  1.0,       1.0,    0.0,     -1.0,  0.0},
	    {  2.0,  0.0, -2.0,  0.0,  1.0,       1.0,    0.0,      0.0,  0.0},
	    {  1.0, -1.0,  0.0, -2.0,  0.0,       1.0,    0.0,      0.0,  0.0},
	    { -1.0,  0.0,  0.0,  1.0,  1.0,       1.0,    0.0,      0.0,  0.0},
	    { -1.0, -1.0,  0.0,  2.0,  1.0,       1.0,    0.0,      0.0,  0.0},
	    {  0.0,  1.0,  0.0,  1.0,  0.0,       1.0,    0.0,      0.0,  0.0}
	};

	final double SREV = 360.0*3600.0;

	double T = (JD - J2000)/JCY;              

/* Calculation of the fundamental arguments
 * 
 *  L  = Mean longitude of Moon - mean longitude of Moon's perigee
 *  LP = Mean longitude of Sun  - mean longitude of Sun's perigee
 *  F  = Mean longitude of Moon - mean longitude of Moon's node
 *  D  = Mean longitude of Moon - mean longitude of Sun 
 *       ie mean elongation of Moon from Sun
 *  OM = Longitude of mean ascending node of the lunar orbit on the
 *       ecliptic measured from the mean equinox of date
 */
	double L = ((+0.064 * T + 31.310) * T + 715922.633) * T + 485866.733
                                                    + frac(1325.0 * T) * SREV ;
	L %= SREV;

	double LP = ((-0.012 * T - 0.577) * T + 1292581.224) * T + 1287099.804 
                                                     + frac(99.0 * T) * SREV ;
	LP %= SREV;

	double F = ((+0.011 * T - 13.257) * T + 295263.137) * T + 335778.877
	    + frac(1342.0 * T) * SREV;

	F %= SREV;

	double D = ((+0.019 * T - 6.891) * T + 1105601.328) * T + 1072261.307
	    + frac(1236.0 * T) * SREV;

	D %= SREV;

	double OM = ((0.008 * T + 7.455) * T - 482890.539) * T + 450160.280
	    - frac(5.0 * T) * SREV;

	OM %= SREV;

	double dpsi = 0.0;
	double deps = 0.0;

	for (int i = data.length - 1; i >= 0 ; i--) {
	    double arg =  data[i][0] * L
		+ data[i][1] * LP
		+ data[i][2] * F
		+ data[i][3] * D
		+ data[i][4] * OM ;

	    arg = (arg % SREV) * S2R;

	    dpsi += (data[i][5] + data[i][6] * T) * Math.sin(arg);
	    deps += (data[i][7] + data[i][8] * T) * Math.cos(arg);
	}

	dpsi *= 1.0e-4 * S2R;
	deps *= 1.0e-4 * S2R;

	angles.setAngles(dpsi, deps);
    }

    public Matrix nutationMatrix(double t) {
	Matrix m = new Matrix();
	nutationMatrix(t, m);
	return m;
    }

    public void nutationMatrix(double t, Matrix matrix) {
	nutationAngles(t, nAngles);
	double eps0 = meanObliquity(t);
	nutationAnglesToMatrix(nAngles, eps0, matrix);
    }

    private void nutationAnglesToMatrix(NutationAngles na, double eps0, Matrix nm) {
	double dpsi = na.getDpsi();
	double deps = na.getDeps();

	double eps = eps0 + deps;

	double ce0   = Math.cos(eps0);
	double se0   = Math.sin(eps0);
	double cdpsi = Math.cos(dpsi);
	double sdpsi = Math.sin(dpsi);
	double ce    = Math.cos(eps);
	double  se   = Math.sin(eps);

	nm.setComponent(0, 0,  cdpsi);
	nm.setComponent(0, 1, -sdpsi * ce0);
	nm.setComponent(0, 2, -sdpsi * se0);
	
	nm.setComponent(1, 0,  sdpsi * ce);
	nm.setComponent(1, 1,  cdpsi * ce * ce0 + se * se0);
	nm.setComponent(1, 2,  cdpsi * ce * se0 - se * ce0);
	
	nm.setComponent(2, 0,  sdpsi * se);
	nm.setComponent(2, 1,  cdpsi * se * ce0 - ce * se0);
	nm.setComponent(2, 2,  cdpsi * se * se0 + ce * ce0);
    }
}
