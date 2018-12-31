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

package com.obliquity.astronomy.almanac.test;

import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.Vector;

/*
 * This class uses data and methods from:
 * 
 * Identification of a constellation from a position,
 * Nancy G. Roman,
 * Publications of the Astronomical Society of the Pacific, 99 (July 1987), 695-699.
 */

public class ConstellationFinder {
	private static final double zoneData[][] = { { 0.0000, 24.0000, 88.0000 },
			{ 8.0000, 14.5000, 86.5000 }, { 21.0000, 23.0000, 86.1667 },
			{ 18.0000, 21.0000, 86.0000 }, { 0.0000, 8.0000, 85.0000 },
			{ 9.1667, 10.6667, 82.0000 }, { 0.0000, 5.0000, 80.0000 },
			{ 10.6667, 14.5000, 80.0000 }, { 17.5000, 18.0000, 80.0000 },
			{ 20.1667, 21.0000, 80.0000 }, { 0.0000, 3.5083, 77.0000 },
			{ 11.5000, 13.5833, 77.0000 }, { 16.5333, 17.5000, 75.0000 },
			{ 20.1667, 20.6667, 75.0000 }, { 7.9667, 9.1667, 73.5000 },
			{ 9.1667, 11.3333, 73.5000 }, { 13.0000, 16.5333, 70.0000 },
			{ 3.1000, 3.4167, 68.0000 }, { 20.4167, 20.6667, 67.0000 },
			{ 11.3333, 12.0000, 66.5000 }, { 0.0000, 0.3333, 66.0000 },
			{ 14.0000, 15.6667, 66.0000 }, { 23.5833, 24.0000, 66.0000 },
			{ 12.0000, 13.5000, 64.0000 }, { 13.5000, 14.4167, 63.0000 },
			{ 23.1667, 23.5833, 63.0000 }, { 6.1000, 7.0000, 62.0000 },
			{ 20.0000, 20.4167, 61.5000 }, { 20.5367, 20.6000, 60.9167 },
			{ 7.0000, 7.9667, 60.0000 }, { 7.9667, 8.4167, 60.0000 },
			{ 19.7667, 20.0000, 59.5000 }, { 20.0000, 20.5367, 59.5000 },
			{ 22.8667, 23.1667, 59.0833 }, { 0.0000, 2.4333, 58.5000 },
			{ 19.4167, 19.7667, 58.0000 }, { 1.7000, 1.9083, 57.5000 },
			{ 2.4333, 3.1000, 57.0000 }, { 3.1000, 3.1667, 57.0000 },
			{ 22.3167, 22.8667, 56.2500 }, { 5.0000, 6.1000, 56.0000 },
			{ 14.0333, 14.4167, 55.5000 }, { 14.4167, 19.4167, 55.5000 },
			{ 3.1667, 3.3333, 55.0000 }, { 22.1333, 22.3167, 55.0000 },
			{ 20.6000, 21.9667, 54.8333 }, { 0.0000, 1.7000, 54.0000 },
			{ 6.1000, 6.5000, 54.0000 }, { 12.0833, 13.5000, 53.0000 },
			{ 15.2500, 15.7500, 53.0000 }, { 21.9667, 22.1333, 52.7500 },
			{ 3.3333, 5.0000, 52.5000 }, { 22.8667, 23.3333, 52.5000 },
			{ 15.7500, 17.0000, 51.5000 }, { 2.0417, 2.5167, 50.5000 },
			{ 17.0000, 18.2333, 50.5000 }, { 0.0000, 1.3667, 50.0000 },
			{ 1.3667, 1.6667, 50.0000 }, { 6.5000, 6.8000, 50.0000 },
			{ 23.3333, 24.0000, 50.0000 }, { 13.5000, 14.0333, 48.5000 },
			{ 0.0000, 1.1167, 48.0000 }, { 23.5833, 24.0000, 48.0000 },
			{ 18.1750, 18.2333, 47.5000 }, { 18.2333, 19.0833, 47.5000 },
			{ 19.0833, 19.1667, 47.5000 }, { 1.6667, 2.0417, 47.0000 },
			{ 8.4167, 9.1667, 47.0000 }, { 0.1667, 0.8667, 46.0000 },
			{ 12.0000, 12.0833, 45.0000 }, { 6.8000, 7.3667, 44.5000 },
			{ 21.9083, 21.9667, 44.0000 }, { 21.8750, 21.9083, 43.7500 },
			{ 19.1667, 19.4000, 43.5000 }, { 9.1667, 10.1667, 42.0000 },
			{ 10.1667, 10.7833, 40.0000 }, { 15.4333, 15.7500, 40.0000 },
			{ 15.7500, 16.3333, 40.0000 }, { 9.2500, 9.5833, 39.7500 },
			{ 0.0000, 2.5167, 36.7500 }, { 2.5167, 2.5667, 36.7500 },
			{ 19.3583, 19.4000, 36.5000 }, { 4.5000, 4.6917, 36.0000 },
			{ 21.7333, 21.8750, 36.0000 }, { 21.8750, 22.0000, 36.0000 },
			{ 6.5333, 7.3667, 35.5000 }, { 7.3667, 7.7500, 35.5000 },
			{ 0.0000, 2.0000, 35.0000 }, { 22.0000, 22.8167, 35.0000 },
			{ 22.8167, 22.8667, 34.5000 }, { 22.8667, 23.5000, 34.5000 },
			{ 2.5667, 2.7167, 34.0000 }, { 10.7833, 11.0000, 34.0000 },
			{ 12.0000, 12.3333, 34.0000 }, { 7.7500, 9.2500, 33.5000 },
			{ 9.2500, 9.8833, 33.5000 }, { 0.7167, 1.4083, 33.0000 },
			{ 15.1833, 15.4333, 33.0000 }, { 23.5000, 23.7500, 32.0833 },
			{ 12.3333, 13.2500, 32.0000 }, { 23.7500, 24.0000, 31.3333 },
			{ 13.9583, 14.0333, 30.7500 }, { 2.4167, 2.7167, 30.6667 },
			{ 2.7167, 4.5000, 30.6667 }, { 4.5000, 4.7500, 30.0000 },
			{ 18.1750, 19.3583, 30.0000 }, { 11.0000, 12.0000, 29.0000 },
			{ 19.6667, 20.9167, 29.0000 }, { 4.7500, 5.8833, 28.5000 },
			{ 9.8833, 10.5000, 28.5000 }, { 13.2500, 13.9583, 28.5000 },
			{ 0.0000, 0.0667, 28.0000 }, { 1.4083, 1.6667, 28.0000 },
			{ 5.8833, 6.5333, 28.0000 }, { 7.8833, 8.0000, 28.0000 },
			{ 20.9167, 21.7333, 28.0000 }, { 19.2583, 19.6667, 27.5000 },
			{ 1.9167, 2.4167, 27.2500 }, { 16.1667, 16.3333, 27.0000 },
			{ 15.0833, 15.1833, 26.0000 }, { 15.1833, 16.1667, 26.0000 },
			{ 18.3667, 18.8667, 26.0000 }, { 10.7500, 11.0000, 25.5000 },
			{ 18.8667, 19.2583, 25.5000 }, { 1.6667, 1.9167, 25.0000 },
			{ 0.7167, 0.8500, 23.7500 }, { 10.5000, 10.7500, 23.5000 },
			{ 21.2500, 21.4167, 23.5000 }, { 5.7000, 5.8833, 22.8333 },
			{ 0.0667, 0.1417, 22.0000 }, { 15.9167, 16.0333, 22.0000 },
			{ 5.8833, 6.2167, 21.5000 }, { 19.8333, 20.2500, 21.2500 },
			{ 18.8667, 19.2500, 21.0833 }, { 0.1417, 0.8500, 21.0000 },
			{ 20.2500, 20.5667, 20.5000 }, { 7.8083, 7.8833, 20.0000 },
			{ 20.5667, 21.2500, 19.5000 }, { 19.2500, 19.8333, 19.1667 },
			{ 3.2833, 3.3667, 19.0000 }, { 18.8667, 19.0000, 18.5000 },
			{ 5.7000, 5.7667, 18.0000 }, { 6.2167, 6.3083, 17.5000 },
			{ 19.0000, 19.8333, 16.1667 }, { 4.9667, 5.3333, 16.0000 },
			{ 15.9167, 16.0833, 16.0000 }, { 19.8333, 20.2500, 15.7500 },
			{ 4.6167, 4.9667, 15.5000 }, { 5.3333, 5.6000, 15.5000 },
			{ 12.8333, 13.5000, 15.0000 }, { 17.2500, 18.2500, 14.3333 },
			{ 11.8667, 12.8333, 14.0000 }, { 7.5000, 7.8083, 13.5000 },
			{ 16.7500, 17.2500, 12.8333 }, { 0.0000, 0.1417, 12.5000 },
			{ 5.6000, 5.7667, 12.5000 }, { 7.0000, 7.5000, 12.5000 },
			{ 21.1167, 21.3333, 12.5000 }, { 6.3083, 6.9333, 12.0000 },
			{ 18.2500, 18.8667, 12.0000 }, { 20.8750, 21.0500, 11.8333 },
			{ 21.0500, 21.1167, 11.8333 }, { 11.5167, 11.8667, 11.0000 },
			{ 6.2417, 6.3083, 10.0000 }, { 6.9333, 7.0000, 10.0000 },
			{ 7.8083, 7.9250, 10.0000 }, { 23.8333, 24.0000, 10.0000 },
			{ 1.6667, 3.2833, 9.9167 }, { 20.1417, 20.3000, 8.5000 },
			{ 13.5000, 15.0833, 8.0000 }, { 22.7500, 23.8333, 7.5000 },
			{ 7.9250, 9.2500, 7.0000 }, { 9.2500, 10.7500, 7.0000 },
			{ 18.2500, 18.6622, 6.2500 }, { 18.6622, 18.8667, 6.2500 },
			{ 20.8333, 20.8750, 6.0000 }, { 7.0000, 7.0167, 5.5000 },
			{ 18.2500, 18.4250, 4.5000 }, { 16.0833, 16.7500, 4.0000 },
			{ 18.2500, 18.4250, 3.0000 }, { 21.4667, 21.6667, 2.7500 },
			{ 0.0000, 2.0000, 2.0000 }, { 18.5833, 18.8667, 2.0000 },
			{ 20.3000, 20.8333, 2.0000 }, { 20.8333, 21.3333, 2.0000 },
			{ 21.3333, 21.4667, 2.0000 }, { 22.0000, 22.7500, 2.0000 },
			{ 21.6667, 22.0000, 1.7500 }, { 7.0167, 7.2000, 1.5000 },
			{ 3.5833, 4.6167, 0.0000 }, { 4.6167, 4.6667, 0.0000 },
			{ 7.2000, 8.0833, 0.0000 }, { 14.6667, 15.0833, 0.0000 },
			{ 17.8333, 18.2500, 0.0000 }, { 2.6500, 3.2833, -01.7500 },
			{ 3.2833, 3.5833, -01.7500 }, { 15.0833, 16.2667, -03.2500 },
			{ 4.6667, 5.0833, -04.0000 }, { 5.8333, 6.2417, -04.0000 },
			{ 17.8333, 17.9667, -04.0000 }, { 18.2500, 18.5833, -04.0000 },
			{ 18.5833, 18.8667, -04.0000 }, { 22.7500, 23.8333, -04.0000 },
			{ 10.7500, 11.5167, -06.0000 }, { 11.5167, 11.8333, -06.0000 },
			{ 0.0000, 00.3333, -07.0000 }, { 23.8333, 24.0000, -07.0000 },
			{ 14.2500, 14.6667, -08.0000 }, { 15.9167, 16.2667, -08.0000 },
			{ 20.0000, 20.5333, -09.0000 }, { 21.3333, 21.8667, -09.0000 },
			{ 17.1667, 17.9667, -10.0000 }, { 5.8333, 8.0833, -11.0000 },
			{ 4.9167, 5.0833, -11.0000 }, { 5.0833, 5.8333, -11.0000 },
			{ 8.0833, 8.3667, -11.0000 }, { 9.5833, 10.7500, -11.0000 },
			{ 11.8333, 12.8333, -11.0000 }, { 17.5833, 17.6667, -11.6667 },
			{ 18.8667, 20.0000, -12.0333 }, { 4.8333, 4.9167, -14.5000 },
			{ 20.5333, 21.3333, -15.0000 }, { 17.1667, 18.2500, -16.0000 },
			{ 18.2500, 18.8667, -16.0000 }, { 8.3667, 8.5833, -17.0000 },
			{ 16.2667, 16.3750, -18.2500 }, { 8.5833, 9.0833, -19.0000 },
			{ 10.7500, 10.8333, -19.0000 }, { 16.2667, 16.3750, -19.2500 },
			{ 15.6667, 15.9167, -20.0000 }, { 12.5833, 12.8333, -22.0000 },
			{ 12.8333, 14.2500, -22.0000 }, { 9.0833, 9.7500, -24.0000 },
			{ 1.6667, 2.6500, -24.3833 }, { 2.6500, 3.7500, -24.3833 },
			{ 10.8333, 11.8333, -24.5000 }, { 11.8333, 12.5833, -24.5000 },
			{ 14.2500, 14.9167, -24.5000 }, { 16.2667, 16.7500, -24.5833 },
			{ 0.0000, 1.6667, -25.5000 }, { 21.3333, 21.8667, -25.5000 },
			{ 21.8667, 23.8333, -25.5000 }, { 23.8333, 24.0000, -25.5000 },
			{ 9.7500, 10.2500, -26.5000 }, { 4.7000, 4.8333, -27.2500 },
			{ 4.8333, 6.1167, -27.2500 }, { 20.0000, 21.3333, -28.0000 },
			{ 10.2500, 10.5833, -29.1667 }, { 12.5833, 14.9167, -29.5000 },
			{ 14.9167, 15.6667, -29.5000 }, { 15.6667, 16.0000, -29.5000 },
			{ 4.5833, 4.7000, -30.0000 }, { 16.7500, 17.6000, -30.0000 },
			{ 17.6000, 17.8333, -30.0000 }, { 10.5833, 10.8333, -31.1667 },
			{ 6.1167, 7.3667, -33.0000 }, { 12.2500, 12.5833, -33.0000 },
			{ 10.8333, 12.2500, -35.0000 }, { 3.5000, 3.7500, -36.0000 },
			{ 8.3667, 9.3667, -36.7500 }, { 4.2667, 4.5833, -37.0000 },
			{ 17.8333, 19.1667, -37.0000 }, { 21.3333, 23.0000, -37.0000 },
			{ 23.0000, 23.3333, -37.0000 }, { 3.0000, 3.5000, -39.5833 },
			{ 9.3667, 11.0000, -39.7500 }, { 0.0000, 1.6667, -40.0000 },
			{ 1.6667, 3.0000, -40.0000 }, { 3.8667, 4.2667, -40.0000 },
			{ 23.3333, 24.0000, -40.0000 }, { 14.1667, 14.9167, -42.0000 },
			{ 15.6667, 16.0000, -42.0000 }, { 16.0000, 16.4208, -42.0000 },
			{ 4.8333, 5.0000, -43.0000 }, { 5.0000, 6.5833, -43.0000 },
			{ 8.0000, 8.3667, -43.0000 }, { 3.4167, 3.8667, -44.0000 },
			{ 16.4208, 17.8333, -45.5000 }, { 17.8333, 19.1667, -45.5000 },
			{ 19.1667, 20.3333, -45.5000 }, { 20.3333, 21.3333, -45.5000 },
			{ 3.0000, 3.4167, -46.0000 }, { 4.5000, 4.8333, -46.5000 },
			{ 15.3333, 15.6667, -48.0000 }, { 0.0000, 2.3333, -48.1667 },
			{ 2.6667, 3.0000, -49.0000 }, { 4.0833, 4.2667, -49.0000 },
			{ 4.2667, 4.5000, -49.0000 }, { 21.3333, 22.0000, -50.0000 },
			{ 6.0000, 8.0000, -50.7500 }, { 8.0000, 8.1667, -50.7500 },
			{ 2.4167, 2.6667, -51.0000 }, { 3.8333, 4.0833, -51.0000 },
			{ 0.0000, 1.8333, -51.5000 }, { 6.0000, 6.1667, -52.5000 },
			{ 8.1667, 8.4500, -53.0000 }, { 3.5000, 3.8333, -53.1667 },
			{ 3.8333, 4.0000, -53.1667 }, { 0.0000, 1.5833, -53.5000 },
			{ 2.1667, 2.4167, -54.0000 }, { 4.5000, 5.0000, -54.0000 },
			{ 15.0500, 15.3333, -54.0000 }, { 8.4500, 8.8333, -54.5000 },
			{ 6.1667, 6.5000, -55.0000 }, { 11.8333, 12.8333, -55.0000 },
			{ 14.1667, 15.0500, -55.0000 }, { 15.0500, 15.3333, -55.0000 },
			{ 4.0000, 4.3333, -56.5000 }, { 8.8333, 11.0000, -56.5000 },
			{ 11.0000, 11.2500, -56.5000 }, { 17.5000, 18.0000, -57.0000 },
			{ 18.0000, 20.3333, -57.0000 }, { 22.0000, 23.3333, -57.0000 },
			{ 3.2000, 3.5000, -57.5000 }, { 5.0000, 5.5000, -57.5000 },
			{ 6.5000, 6.8333, -58.0000 }, { 0.0000, 1.3333, -58.5000 },
			{ 1.3333, 2.1667, -58.5000 }, { 23.3333, 24.0000, -58.5000 },
			{ 4.3333, 4.5833, -59.0000 }, { 15.3333, 16.4208, -60.0000 },
			{ 20.3333, 21.3333, -60.0000 }, { 5.5000, 6.0000, -61.0000 },
			{ 15.1667, 15.3333, -61.0000 }, { 16.4208, 16.5833, -61.0000 },
			{ 14.9167, 15.1667, -63.5833 }, { 16.5833, 16.7500, -63.5833 },
			{ 6.0000, 6.8333, -64.0000 }, { 6.8333, 9.0333, -64.0000 },
			{ 11.2500, 11.8333, -64.0000 }, { 11.8333, 12.8333, -64.0000 },
			{ 12.8333, 14.5333, -64.0000 }, { 13.5000, 13.6667, -65.0000 },
			{ 16.7500, 16.8333, -65.0000 }, { 2.1667, 3.2000, -67.5000 },
			{ 3.2000, 4.5833, -67.5000 }, { 14.7500, 14.9167, -67.5000 },
			{ 16.8333, 17.5000, -67.5000 }, { 17.5000, 18.0000, -67.5000 },
			{ 22.0000, 23.3333, -67.5000 }, { 4.5833, 6.5833, -70.0000 },
			{ 13.6667, 14.7500, -70.0000 }, { 14.7500, 17.0000, -70.0000 },
			{ 0.0000, 1.3333, -75.0000 }, { 3.5000, 4.5833, -75.0000 },
			{ 6.5833, 9.0333, -75.0000 }, { 9.0333, 11.2500, -75.0000 },
			{ 11.2500, 13.6667, -75.0000 }, { 18.0000, 21.3333, -75.0000 },
			{ 21.3333, 23.3333, -75.0000 }, { 23.3333, 24.0000, -75.0000 },
			{ 0.7500, 1.3333, -76.0000 }, { 0.0000, 3.5000, -82.5000 },
			{ 7.6667, 13.6667, -82.5000 }, { 13.6667, 18.0000, -82.5000 },
			{ 3.5000, 7.6667, -85.0000 }, { 0.0000, 24.0000, -90.0000 } };

	private static final String[] zoneName= { "UMi", "UMi", "UMi", "UMi",
			"Cep", "Cam", "Cep", "Cam", "UMi", "Dra", "Cep", "Cam", "UMi",
			"Cep", "Cam", "Dra", "UMi", "Cas", "Dra", "Dra", "Cep", "UMi",
			"Cep", "Dra", "Dra", "Cep", "Cam", "Dra", "Cep", "Cam", "UMa",
			"Dra", "Cep", "Cep", "Cas", "Dra", "Cas", "Cas", "Cam", "Cep",
			"Cam", "UMa", "Dra", "Cam", "Cep", "Cep", "Cas", "Lyn", "UMa",
			"Dra", "Cep", "Cam", "Cas", "Dra", "Per", "Dra", "Cas", "Per",
			"Lyn", "Cas", "UMa", "Cas", "Cas", "Her", "Dra", "Cyg", "Per",
			"UMa", "Cas", "UMa", "Lyn", "Cyg", "Cyg", "Cyg", "UMa", "UMa",
			"Boo", "Her", "Lyn", "And", "Per", "Lyr", "Per", "Cyg", "Lac",
			"Aur", "Lyn", "And", "Lac", "Lac", "And", "Per", "UMa", "CVn",
			"Lyn", "LMi", "And", "Boo", "And", "CVn", "And", "CVn", "Tri",
			"Per", "Aur", "Lyr", "UMa", "Cyg", "Aur", "LMi", "CVn", "And",
			"Tri", "Aur", "Gem", "Cyg", "Cyg", "Tri", "CrB", "Boo", "CrB",
			"Lyr", "LMi", "Lyr", "Tri", "Psc", "LMi", "Vul", "Tau", "And",
			"Ser", "Gem", "Vul", "Vul", "And", "Vul", "Gem", "Vul", "Vul",
			"Ari", "Sge", "Ori", "Gem", "Sge", "Tau", "Her", "Sge", "Tau",
			"Tau", "Com", "Her", "Com", "Gem", "Her", "Peg", "Tau", "Gem",
			"Peg", "Gem", "Her", "Del", "Peg", "Leo", "Ori", "Gem", "Cnc",
			"Peg", "Ari", "Del", "Boo", "Peg", "Cnc", "Leo", "Oph", "Aql",
			"Del", "CMi", "Ser", "Her", "Oph", "Peg", "Psc", "Ser", "Del",
			"Equ", "Peg", "Peg", "Peg", "CMi", "Tau", "Ori", "CMi", "Vir",
			"Oph", "Cet", "Tau", "Ser", "Ori", "Ori", "Ser", "Ser", "Aql",
			"Psc", "Leo", "Vir", "Psc", "Psc", "Vir", "Oph", "Aql", "Aqr",
			"Oph", "Mon", "Eri", "Ori", "Hya", "Sex", "Vir", "Oph", "Aql",
			"Eri", "Aqr", "Ser", "Sct", "Hya", "Oph", "Hya", "Crt", "Oph",
			"Lib", "Crv", "Vir", "Hya", "Cet", "Eri", "Crt", "Crv", "Lib",
			"Oph", "Cet", "Cap", "Aqr", "Cet", "Hya", "Eri", "Lep", "Cap",
			"Hya", "Hya", "Lib", "Sco", "Eri", "Oph", "Sgr", "Hya", "CMa",
			"Hya", "Hya", "For", "Pyx", "Eri", "Sgr", "PsA", "Scl", "For",
			"Ant", "Scl", "For", "Eri", "Scl", "Cen", "Lup", "Sco", "Cae",
			"Col", "Pup", "Eri", "Sco", "CrA", "Sgr", "Mic", "Eri", "Cae",
			"Lup", "Phe", "Eri", "Hor", "Cae", "Gru", "Pup", "Vel", "Eri",
			"Hor", "Phe", "Car", "Vel", "Hor", "Dor", "Phe", "Eri", "Pic",
			"Lup", "Vel", "Car", "Cen", "Lup", "Nor", "Dor", "Vel", "Cen",
			"Ara", "Tel", "Gru", "Hor", "Pic", "Car", "Phe", "Eri", "Phe",
			"Dor", "Nor", "Ind", "Pic", "Cir", "Ara", "Cir", "Ara", "Pic",
			"Car", "Cen", "Cru", "Cen", "Cir", "Ara", "Hor", "Ret", "Cir",
			"Ara", "Pav", "Tuc", "Dor", "Cir", "TrA", "Tuc", "Hyi", "Vol",
			"Car", "Mus", "Pav", "Ind", "Tuc", "Tuc", "Hyi", "Cha", "Aps",
			"Men", "Oct" };
	
	private static int findFirstZone(double dec) {
		for (int i = 0; i < zoneData.length; i++)
			if (dec >= zoneData[i][2])
				return i;
		
		return -1;
	}
	
	/*
	 * IMPORTANT: The input Right Ascension and Declination must be referred
	 * to the equator and equinox of B1875, as this is the coordinate system
	 * in which the Delporte constellation boundaries were defined.  If necessary,
	 * apply precession to convert your RA and Dec to B1875.
	 */
	
	public static String getZone(double ra1875, double dec1875) {
		dec1875 *= 180.0/Math.PI;
		
		int j0 = findFirstZone(dec1875);
		
		ra1875 *= 12.0/Math.PI;
		
		ra1875 %= 24.0;
		
		while (ra1875 < 0.0)
			ra1875 += 24.0;
		
		for (int j = j0; j < zoneData.length; j++)
			if (zoneData[j][0] <= ra1875 && ra1875 <= zoneData[j][1])
				return zoneName[j];
		
		// This should never happen
		return null;
	}
	
	private static Matrix precessJ2000toB1875 = null;
 
	static {
		IAUEarthRotationModel erm = new IAUEarthRotationModel();
		double epochJ2000 = erm.JulianEpoch(2000.0);
		double epochB1875 = erm.BesselianEpoch(1875.0);
		precessJ2000toB1875 = new Matrix();
		erm.precessionMatrix(epochJ2000, epochB1875, precessJ2000toB1875);
	}
	
	/*
	 * This method is provided as a convenience so that coordinates can be
	 * passed in the J2000 (ICRF) reference frame.
	 */
	
	public static String getZoneJ2000(double ra2000, double dec2000) {
		Vector dc = new Vector(Math.cos(dec2000) * Math.cos(ra2000), Math.cos(dec2000) * Math.sin(ra2000), Math.sin(dec2000));
		dc.multiplyBy(precessJ2000toB1875);

		double ra1875 = Math.atan2(dc.getY(), dc.getX());

		while (ra1875 < 0.0)
			ra1875 += 2.0 * Math.PI;

		double aux = Math.sqrt(dc.getX() * dc.getX() + dc.getY() * dc.getY());

		double dec1875 = Math.atan2(dc.getZ(), aux);
		
		return getZone(ra1875, dec1875);
	}

}
