/* ===========================================================================
 * $RCSfile: FieldrefCpInfo.java,v $
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

package com.rl.obf.classfile;

/**
 * Representation of a 'fieldref' entry in the ConstantPool.
 *
 * @author Mark Welsh
 */
public class FieldrefCpInfo extends RefCpInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 */
	protected FieldrefCpInfo() {
		super(ClassConstants.CONSTANT_Fieldref);
	}
}
