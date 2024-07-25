/* ===========================================================================
 * $RCSfile: RefCpInfo.java,v $
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

/**
 * Representation of a 'ref'-type entry in the ConstantPool.
 *
 * @author Mark Welsh
 */
abstract public class RefCpInfo extends CpInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private int u2classIndex;
	private int u2nameAndTypeIndex;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param tag
	 */
	protected RefCpInfo(final int tag) {
		super(tag);
	}

	/**
	 * Return the class index.
	 */
	protected int getClassIndex() {
		return this.u2classIndex;
	}

	/**
	 * Return the name-and-type index.
	 */
	protected int getNameAndTypeIndex() {
		return this.u2nameAndTypeIndex;
	}

	/**
	 * Set the name-and-type index.
	 * 
	 * @param index
	 */
	protected void setNameAndTypeIndex(final int index) {
		this.u2nameAndTypeIndex = index;
	}

	/**
	 * Return the method's class string name.
	 * 
	 * @param cf
	 * @throws ClassFileException
	 */
	public String getClassName(final ClassFile cf) throws ClassFileException {
		final ClassCpInfo entry = (ClassCpInfo) cf.getCpEntry(this.u2classIndex);
		return entry.getName(cf);
	}

	/**
	 * Return the method's string name.
	 * 
	 * @param cf
	 * @throws ClassFileException
	 */
	public String getName(final ClassFile cf) throws ClassFileException {
		final NameAndTypeCpInfo ntCpInfo = (NameAndTypeCpInfo) cf.getCpEntry(this.u2nameAndTypeIndex);
		return cf.getUtf8(ntCpInfo.getNameIndex());
	}

	/**
	 * Return the method's string descriptor.
	 * 
	 * @param cf
	 * @throws ClassFileException
	 */
	public String getDescriptor(final ClassFile cf) throws ClassFileException {
		final NameAndTypeCpInfo ntCpInfo = (NameAndTypeCpInfo) cf.getCpEntry(this.u2nameAndTypeIndex);
		return cf.getUtf8(ntCpInfo.getDescriptorIndex());
	}

	/**
	 * Check for N+T references to constant pool and mark them.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void markNTRefs(final ConstantPool pool) throws ClassFileException {
		pool.incRefCount(this.u2nameAndTypeIndex);
	}

	/**
	 * Read the 'info' data following the u1tag byte.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void readInfo(final DataInput din) throws IOException, ClassFileException {
		this.u2classIndex = din.readUnsignedShort();
		this.u2nameAndTypeIndex = din.readUnsignedShort();
	}

	/**
	 * Write the 'info' data following the u1tag byte.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void writeInfo(final DataOutput dout) throws IOException, ClassFileException {
		dout.writeShort(this.u2classIndex);
		dout.writeShort(this.u2nameAndTypeIndex);
	}
}
