/**
 *
 */

package com.rl.obf.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Representation of a 'methodtype' entry in the ConstantPool.
 */
public class MethodTypeCpInfo extends CpInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private int u2descriptorIndex;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 */
	public MethodTypeCpInfo() {
		super(ClassConstants.CONSTANT_MethodType);
	}

	/**
	 * Return the descriptor index.
	 */
	protected int getDescriptorIndex() {
		return this.u2descriptorIndex;
	}

	/**
	 * Set the descriptor index.
	 * 
	 * @param index
	 */
	protected void setDescriptorIndex(final int index) {
		this.u2descriptorIndex = index;
	}

	/**
	 * Check for Utf8 references to constant pool and mark them.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void markUtf8Refs(final ConstantPool pool) throws ClassFileException {
		pool.incRefCount(this.u2descriptorIndex);
	}

	/**
	 * Read the 'info' data following the u1tag byte.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void readInfo(final DataInput din) throws IOException, ClassFileException {
		this.u2descriptorIndex = din.readUnsignedShort();
	}

	/**
	 * Write the 'info' data following the u1tag byte.
	 * 
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void writeInfo(final DataOutput dout) throws IOException, ClassFileException {
		dout.writeShort(this.u2descriptorIndex);
	}

}
