import com.obliquity.astronomy.*;
import java.util.Random;

import java.io.*;
import java.text.*;

public class TestApparentPlace {
    public static void main(String args[]) {
    	if (args.length < 5) {
    		System.err.println("Usage: TestApparentPlace filename kBody startdate enddate step");
    		System.exit(1);
    	}

    	String filename = args[0];

    	int kBody = parseBody(args[1]);
    	
    	if (kBody < 0) {
    		System.err.println("Unknown body name: \"" + args[1] + "\"");
    		System.exit(1);
    	}

    	boolean randomdates = Boolean.getBoolean("random");
   	
    	double jdstart = Double.parseDouble(args[2]);
    	double jdfinish = Double.parseDouble(args[3]);
    	
    	double jdstep = 0.0;
    	int kSteps = 0;
    	
    	if (randomdates)
    		kSteps = Integer.parseInt(args[4]);
    	else
    		jdstep = Double.parseDouble(args[4]);
    	
    	boolean timingTest = Boolean.getBoolean("timingtest");

    	JPLEphemeris ephemeris = null;

    	try {
    		ephemeris = new JPLEphemeris(filename, jdstart - 1.0, jdfinish + 1.0);
    	}
    	catch (JPLEphemerisException jee) {
    		jee.printStackTrace();
    		System.err.println("JPLEphemerisException ... " + jee);
    		System.exit(1);
    	}
        catch (IOException ioe) {
        	ioe.printStackTrace();
            System.err.println("IOException ... " + ioe);
            System.exit(1);
        }

        MovingPoint planet = null;
        
        if (kBody == JPLEphemeris.MOON)
        	planet = new MoonCentre(ephemeris);
        else
        	planet = new PlanetCentre(ephemeris, kBody);
        
        EarthCentre earth = new EarthCentre(ephemeris);

        MovingPoint sun = null;

        if (kBody == JPLEphemeris.SUN)
        	sun = planet;
        else
        	sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

        double tEarliest = ephemeris.getEarliestDate() + 1.0;
        double tLatest   = ephemeris.getLatestDate() - 1.0;
        double tSpan = tLatest - tEarliest;

        boolean silent = Boolean.getBoolean("silent");
        
        Random random = randomdates ? new Random() : null;

        EarthRotationModel erm = new IAUEarthRotationModel();

        PrintStream ps = silent ? null : System.err;
        
        long startTime = System.currentTimeMillis();
        int nSteps = 0;
        
        ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);

        try {
        	if (randomdates) {
        		double dt = jdfinish - jdstart;
        		for (int i = 0; i < kSteps; i++) {
        			double t = jdstart + dt * random.nextDouble();
        			ap.calculateApparentPlace(t);
        			if (!timingTest)
        				displayApparentPlace(t, ap, System.out);
        			nSteps++;
        		}
        	} else {
        		for (double t = jdstart; t <= jdfinish; t += jdstep) {
        			ap.calculateApparentPlace(t);
        			if (!timingTest)
        				displayApparentPlace(t, ap, System.out);
        			nSteps++;
        		}
        	}
        }
        catch (JPLEphemerisException jplee) {
        	jplee.printStackTrace();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Executed " + nSteps + " steps in " + duration + " ms");
    }
    
    private static int parseBody(String bodyname) {
    	if (bodyname.equalsIgnoreCase("sun"))
    		return JPLEphemeris.SUN; 
    	
    	if (bodyname.equalsIgnoreCase("moon"))
    		return JPLEphemeris.MOON;
    	
    	if (bodyname.equalsIgnoreCase("mercury"))
    		return JPLEphemeris.MERCURY;
    	
    	if (bodyname.equalsIgnoreCase("venus"))
    		return JPLEphemeris.VENUS;
    	
    	if (bodyname.equalsIgnoreCase("mars"))
    		return JPLEphemeris.MARS;
    	
    	if (bodyname.equalsIgnoreCase("jupiter"))
    		return JPLEphemeris.JUPITER;
    	
    	if (bodyname.equalsIgnoreCase("saturn"))
    		return JPLEphemeris.SATURN;
    	
    	if (bodyname.equalsIgnoreCase("uranus"))
    		return JPLEphemeris.URANUS;
    	
    	if (bodyname.equalsIgnoreCase("neptune"))
    		return JPLEphemeris.NEPTUNE;
    	
    	if (bodyname.equalsIgnoreCase("pluto"))
    		return JPLEphemeris.PLUTO;
    	
    	return -1;
    }

    private static final DecimalFormat dfmta = new DecimalFormat("00.000");
    private static final DecimalFormat dfmtb = new DecimalFormat("00.00");
    private static final DecimalFormat ifmt = new DecimalFormat("00");
    private static final DecimalFormat dfmtc = new DecimalFormat("0.0000000");

    private static void displayApparentPlace(double t, ApparentPlace ap, PrintStream ps) {
    	double ra = ap.getRightAscension() * 12.0/Math.PI;
    	double dec = ap.getDeclination() * 180.0/Math.PI;

    	char decsign = (dec < 0.0) ? 'S' : 'N';
	
    	if (ra < 0.0)
    		ra += 24.0;
    		
    	if (dec < 0.0)
    		dec = -dec;

    	int rah  = (int)ra;
    	ra -= (double)rah;
    	ra *= 60.0;
    	int ram = (int)ra;
    	ra -= (double)ram;
    	ra *= 60.0;
	
    	int decd = (int)dec;
    	dec -= (double)decd;
    	dec *= 60.0;
    	int decm = (int)dec;
    	dec -= (double)decm;
    	dec *= 60.0;
	
    	ps.print(dfmtb.format(t));
    	ps.print("  ");
    	ps.print(ifmt.format(rah) + " " + ifmt.format(ram) + " " + dfmta.format(ra));
    	ps.print("  ");
    	ps.print(decsign + " " + ifmt.format(decd) + " " + ifmt.format(decm) + " " + dfmtb.format(dec));
    	ps.print("  ");
    	ps.print(dfmtc.format(ap.getGeometricDistance()));
    	ps.print("  ");
    	ps.print(dfmtc.format(ap.getLightPathDistance()));
    	ps.println();
    }    		
 }
