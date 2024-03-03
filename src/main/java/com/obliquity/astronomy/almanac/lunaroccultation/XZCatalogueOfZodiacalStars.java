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

public class XZCatalogueOfZodiacalStars {
	private int starCount = 0;
	
	public static void main(String[] args) {
		try {
			XZCatalogueOfZodiacalStars catalogue = new XZCatalogueOfZodiacalStars();
			System.out.println("The catalogue contains " + catalogue.size() + " stars");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	public XZCatalogueOfZodiacalStars() throws IOException {
		InputStream is = getClass().getResourceAsStream("/starcatalogues/xz80q.dat");

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		loadCatalogue(br);
		
		is.close();
	}
	
	private void loadCatalogue(BufferedReader br) throws IOException {
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			starCount++;
		}
		
		br.close();
	}
	
	public int size() {
		return starCount;
	}
}
