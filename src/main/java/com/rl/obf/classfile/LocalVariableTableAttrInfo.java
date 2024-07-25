/* ===========================================================================
 * $RCSfile: LocalVariableTableAttrInfo.java,v $
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
import java.util.List;

/**
 * Representation of an attribute.
 *
 * @author Mark Welsh
 */
public class LocalVariableTableAttrInfo extends AttrInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private List<LocalVariableInfo> localVariableTable;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param cf
	 * @param attrNameIndex
	 * @param attrLength
	 */
	protected LocalVariableTableAttrInfo(final ClassFile cf, final int attrNameIndex, final int attrLength) {
		super(cf, attrNameIndex, attrLength);
	}

	/**
	 * Return the String name of the attribute; over-ride this in sub-classes.
	 */
	@Override
	protected String getAttrName() {
		return ClassConstants.ATTR_LocalVariableTable;
	}

	/**
	 * Check for Utf8 references in the 'info' data to the constant pool and mark
	 * them.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void markUtf8RefsInInfo(final ConstantPool pool) throws ClassFileException {
		for (final LocalVariableInfo lv : this.localVariableTable) {
			lv.markUtf8Refs(pool);
		}
	}

	/**
	 * Read the data following the header.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void readInfo(final DataInput din) throws IOException, ClassFileException {
		final int u2localVariableTableLength = din.readUnsignedShort();
		this.localVariableTable = new ArrayList<>(u2localVariableTableLength);
		for (int i = 0; i < u2localVariableTableLength; i++) {
			this.localVariableTable.add(LocalVariableInfo.create(din));
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
		dout.writeShort(this.localVariableTable.size());
		for (final LocalVariableInfo lv : this.localVariableTable) {
			lv.write(dout);
		}
	}

	/**
	 * Do necessary name remapping.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void remap(final ClassFile cf, final NameMapper nm) throws ClassFileException {
		for (final LocalVariableInfo lv : this.localVariableTable) {
			lv.remap(cf, nm);
		}
	}
}
