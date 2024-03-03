/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2024 David Harper at obliquity.com
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

package com.obliquity.astronomy.almanac.lunaroccultation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.obliquity.astronomy.almanac.Star;

public class XZCatalogueOfZodiacalStars {
	private Vector<Star> catalogue = new Vector<Star>();
	private Map<Integer, Star> catalogueByXZNumber = new HashMap<Integer, Star>();
	private Map<Integer, Star> catalogueByHDNumber = new HashMap<Integer, Star>();
	
	public static void main(String[] args) {
		double limitingMagnitude = args.length > 0 ? Double.parseDouble(args[0]) : 6.5;
		
		try {
			XZCatalogueOfZodiacalStars catalogue = new XZCatalogueOfZodiacalStars(limitingMagnitude);
			System.out.println("The catalogue contains " + catalogue.size() + " stars");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	public XZCatalogueOfZodiacalStars(double limitingMagnitude) throws IOException {
		InputStream is = getClass().getResourceAsStream("/starcatalogues/xz80q.dat");

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		if (limitingMagnitude > 20.0)
			limitingMagnitude = 20.0;
		
		loadCatalogue(br, limitingMagnitude);
		
		is.close();
	}
	
	private void loadCatalogue(BufferedReader br, double limitingMagnitude) throws IOException {		
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			if (getVisualMagnitude(line) <= limitingMagnitude) {
				Star star = parseLine(line);
				
				catalogue.add(star);
				
				int catID = star.getCatalogueNumber();
				catalogueByXZNumber.put(catID, star);
				
				int hdID = star.getHDNumber();
				if (hdID > 0)
					catalogueByHDNumber.put(hdID, star);
			}
		}

		br.close();
	}
	
	private double getVisualMagnitude(String line) {
		return getDoubleField(line, 20, 24);
	}
	
	private Star parseLine(String line) {
		int catnum = getIntegerField(line, 1, 6);
		
		double vmag = getDoubleField(line, 20, 24);
		
		int rah = getIntegerField(line, 26, 27);
		int ram = getIntegerField(line, 28, 29);
		double ras = getDoubleField(line, 30, 36);
		
		double ra = ((double)rah + ((double)ram)/60.0 + ras/3600.0) * Math.PI/12.0;
		
		double pmRA = getDoubleField(line, 37, 44) * 15.0/100.0;
		
		char decSign = line.charAt(44);
		
		int decd = getIntegerField(line, 46, 47);
		int decm = getIntegerField(line, 48, 49);
		double decs = getDoubleField(line, 50, 55);
		
		double dec = ((double)decd + ((double)decm)/60.0 + decs) * Math.PI/180.0;
		
		if (decSign == '-')
			dec = -dec;
		
		double pmDec = getDoubleField(line, 56, 63)/100.0;
		
		double parallax = (double)getIntegerField(line, 64, 66);
		
		double rv = getDoubleField(line, 68, 73);
		
		int hdnum = getIntegerField(line, 167, 172);
		
		return new Star(catnum, hdnum, ra, dec, pmRA, pmDec, parallax, vmag, rv);
	}
	
	private int getIntegerField(String line, int startpos, int endpos) {
		String field = line.substring(startpos-1, endpos).trim();
		return field.length() > 0 ? Integer.parseInt(field) : -1;
	}
	
	private double getDoubleField(String line, int startpos, int endpos) {
		String field = line.substring(startpos-1, endpos).trim();
		return field.length() > 0 ? Double.parseDouble(line.substring(startpos-1, endpos)) : 0.0;
	}
	
	public int size() {
		return catalogue.size();
	}
}
