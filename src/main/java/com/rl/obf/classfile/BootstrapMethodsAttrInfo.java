/**
 *
 */

package com.rl.obf.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of an attribute.
 */
public class BootstrapMethodsAttrInfo extends AttrInfo {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private List<BootStrapMethod> bsmList;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param cf
	 * @param attrNameIndex
	 * @param attrLength
	 */
	protected BootstrapMethodsAttrInfo(final ClassFile cf, final int attrNameIndex, final int attrLength) {
		super(cf, attrNameIndex, attrLength);
	}

	/**
	 * Return the String name of the attribute.
	 */
	@Override
	protected String getAttrName() {
		return ClassConstants.ATTR_BootstrapMethods;
	}

	/**
	 * Read the data following the header; over-ride this in sub-classes.
	 * 
	 * @param din
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	protected void readInfo(final DataInput din) throws IOException, ClassFileException {
		final int length = din.readUnsignedShort();
		this.bsmList = new ArrayList<>(length);
		for (int bsmCount = 0; bsmCount < length; bsmCount++) {
			final BootStrapMethod bsm = new BootStrapMethod();
			final int factory = din.readUnsignedShort();
			bsm.setFactory(factory);
			final int argCount = din.readUnsignedShort();
			for (int j = 0; j < argCount; j++) {
				bsm.addArgument(din.readUnsignedShort());
			}
			this.bsmList.add(bsm);
		}
	}

	/**
	 * Export data following the header to a DataOutput stream; over-ride this in
	 * sub-classes.
	 * 
	 * @param dout
	 * @throws IOException
	 * @throws ClassFileException
	 */
	@Override
	public void writeInfo(final DataOutput dout) throws IOException, ClassFileException {
		dout.writeShort(this.bsmList.size());
		for (final BootStrapMethod bsm : this.bsmList) {
			dout.writeShort(bsm.getFactory());
			final List<Integer> args = bsm.getArguments();
			dout.writeShort(args.size());
			for (final int arg : args) {
				dout.writeShort(arg);
			}
		}
	}

	/**
	 * The getter of BootStrapMethods
	 * 
	 * @return List of BSMs
	 */
	public List<BootStrapMethod> getBSM() {
		return Collections.unmodifiableList(this.bsmList);
	}
}
