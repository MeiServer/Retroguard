/* ===========================================================================
 * $RCSfile: PatternList.java,v $
 * ===========================================================================
 *
 * RetroGuard -- an obfuscation package for Java classfiles.
 *
 * Copyright (c) 1998-2006 Mark Welsh (markw@retrologic.com)
 *
 * This program can be redistributed and/or modified under the terms of the
 * Version 2 of the GNU General Public License as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

package com.rl.obf;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used for limited pattern matching with '**' (matches across package
 * boundaries) and '*' (does not match across package boundaries) wildcards.
 *
 * @author Mark Welsh
 */
public class PatternList {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private final List<String> subs = new ArrayList<>();
	private int sc = -1;

	// Class Methods ---------------------------------------------------------
	public static PatternList create(final String pattern) {
		return new PatternList(pattern);
	}

	// Instance Methods ------------------------------------------------------
	/**
	 * Private constructor
	 * 
	 * @param pattern
	 */
	private PatternList(final String pattern) {
		final int scFirst = pattern.indexOf("**");
		final int scLast = pattern.lastIndexOf("**");
		int pos = -1;
		while (pos < pattern.length()) {
			final int oldpos = pos;
			pos = pattern.indexOf(ClassTree.PACKAGE_LEVEL, oldpos + 1);
			if (pos == -1) {
				pos = pattern.length();
			}
			if (scFirst >= 0 && oldpos + 1 <= scFirst && scFirst + 2 <= pos) {
				this.sc = this.length();
				pos = pattern.indexOf(ClassTree.PACKAGE_LEVEL, scLast + 2);
				if (pos == -1) {
					pos = pattern.length();
				}
			}
			this.subs.add(pattern.substring(oldpos + 1, pos));
		}
	}

	/**
	 * Number of segments in the list.
	 */
	public int length() {
		return this.subs.size();
	}

	/**
	 * Does a '**' wildcard segment exist?
	 */
	public boolean scExists() {
		return this.sc >= 0;
	}

	/**
	 * Index of the '**' wildcard segment.
	 */
	public int scIndex() {
		return this.sc;
	}

	/**
	 * Return the i'th segment.
	 * 
	 * @param i
	 */
	public String getSub(final int i) {
		return this.subs.get(i);
	}

	/**
	 * Return the i'th through j'th segments, joined by package separators.
	 * 
	 * @param i
	 * @param j
	 */
	public String getSub(final int i, final int j) {
		if (i < 0 || i > j || j >= this.length()) {
			throw new IllegalArgumentException();
		}
		final StringBuilder sb = new StringBuilder();
		for (int k = i; k <= j; k++) {
			sb.append(this.getSub(k));
			if (k < j) {
				sb.append(ClassTree.PACKAGE_LEVEL);
			}
		}
		return sb.toString();
	}
}
