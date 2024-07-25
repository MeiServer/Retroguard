/**
 *
 */

package com.rl.obf.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Representation of a 'methodhandle' entry in the ConstantPool.
 */
public class InvokeDynamicCpInfo extends CpInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private int u2bootstrapMethodAttrIndex;
	private int u2nameAndTypeIndex;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 */
	public InvokeDynamicCpInfo() {
		super(ClassConstants.CONSTANT_InvokeDynamic);
	}

	/**
	 * Return the bootstrap-method-attr index.
	 */
	protected int getBootstrapMethodAttrIndex() {
		return this.u2bootstrapMethodAttrIndex;
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
		this.u2bootstrapMethodAttrIndex = din.readUnsignedShort();
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
		dout.writeShort(this.u2bootstrapMethodAttrIndex);
		dout.writeShort(this.u2nameAndTypeIndex);
	}
}
