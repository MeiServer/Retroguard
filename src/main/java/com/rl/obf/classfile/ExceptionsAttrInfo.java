/* ===========================================================================
 * $RCSfile: ExceptionsAttrInfo.java,v $
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Representation of an attribute.
 *
 * @author Mark Welsh
 */
public class ExceptionsAttrInfo extends AttrInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private ArrayList<Integer> u2exceptionIndexTable;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param cf
	 * @param attrNameIndex
	 * @param attrLength
	 */
	protected ExceptionsAttrInfo(final ClassFile cf, final int attrNameIndex, final int attrLength) {
		super(cf, attrNameIndex, attrLength);
	}

	/**
	 * Return the String name of the attribute; over-ride this in sub-classes.
	 */
	@Override
	protected String getAttrName() {
		return ClassConstants.ATTR_Exceptions;
	}

	/**
	 * Read the data following the header.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void readInfo(final DataInput din) throws IOException, ClassFileException {
		final int u2numberOfExceptions = din.readUnsignedShort();
		this.u2exceptionIndexTable = new ArrayList<>(u2numberOfExceptions);
		for (int i = 0; i < u2numberOfExceptions; i++) {
			this.u2exceptionIndexTable.add(din.readUnsignedShort());
		}
	}

	/**
	 * Export data following the header to a DataOutput stream.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	public void writeInfo(final DataOutput dout) throws IOException, ClassFileException {
		dout.writeShort(this.u2exceptionIndexTable.size());
		for (final int ex : this.u2exceptionIndexTable) {
			dout.writeShort(ex);
		}
	}
}
