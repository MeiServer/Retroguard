/* ===========================================================================
 * $RCSfile: InnerClassesAttrInfo.java,v $
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
public class InnerClassesAttrInfo extends AttrInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private List<InnerClassesInfo> classes;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param cf
	 * @param attrNameIndex
	 * @param attrLength
	 */
	protected InnerClassesAttrInfo(final ClassFile cf, final int attrNameIndex, final int attrLength) {
		super(cf, attrNameIndex, attrLength);
	}

	/**
	 * Return the String name of the attribute; over-ride this in sub-classes.
	 */
	@Override
	protected String getAttrName() {
		return ClassConstants.ATTR_InnerClasses;
	}

	/**
	 * Return the list of inner classes data.
	 */
	protected List<InnerClassesInfo> getInfo() {
		return this.classes;
	}

	/**
	 * Check for Utf8 references in the 'info' data to the constant pool and mark
	 * them.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void markUtf8RefsInInfo(final ConstantPool pool) throws ClassFileException {
		for (final InnerClassesInfo cl : this.classes) {
			cl.markUtf8Refs(pool);
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
		final int u2numberOfClasses = din.readUnsignedShort();
		this.classes = new ArrayList<>(u2numberOfClasses);
		for (int i = 0; i < u2numberOfClasses; i++) {
			this.classes.add(InnerClassesInfo.create(din));
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
		dout.writeShort(this.classes.size());
		for (final InnerClassesInfo cl : this.classes) {
			cl.write(dout);
		}
	}

	/**
	 * Do necessary name remapping.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void remap(final ClassFile cf, final NameMapper nm) throws ClassFileException {
		for (final InnerClassesInfo cl : this.classes) {
			cl.remap(cf, nm);
		}
	}
}
