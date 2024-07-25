/* ===========================================================================
 * $RCSfile: Fd.java,v $
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

import com.rl.obf.classfile.ClassFileException;

/**
 * Tree item representing a field.
 *
 * @author Mark Welsh
 */
public class Fd extends MdFd {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param isSynthetic
	 * @param name
	 * @param descriptor
	 * @param access
	 * @throws ClassFileException
	 */
	public Fd(final TreeItem parent, final boolean isSynthetic, final String name, final String descriptor,
			final int access) throws ClassFileException {
		super(parent, isSynthetic, name, descriptor, access);
	}
}
