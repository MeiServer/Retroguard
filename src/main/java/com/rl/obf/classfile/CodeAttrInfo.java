/* ===========================================================================
 * $RCSfile: CodeAttrInfo.java,v $
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Representation of an attribute.
 *
 * @author Mark Welsh
 */
public class CodeAttrInfo extends AttrInfo {
	// Constants -------------------------------------------------------------
	@SuppressWarnings("hiding")
	public static final int CONSTANT_FIELD_SIZE = 12;

	// Fields ----------------------------------------------------------------
	private int u2maxStack;
	private int u2maxLocals;
	private int u4codeLength;
	private byte[] code;
	private List<ExceptionInfo> exceptionTable;
	protected List<AttrInfo> attributes;

	// Class Methods ---------------------------------------------------------
	/**
	 * Number of bytes following an opcode
	 * 
	 * @param opcode
	 */
	private static int opcodeBytes(final int opcode) {
		switch (opcode) {
		case 0xAA:
		case 0xAB:
		case 0xC4:
			return -1; // variable length opcode
		case 0x10:
		case 0x12:
		case 0x15:
		case 0x16:
		case 0x17:
		case 0x18:
		case 0x19:
		case 0x36:
		case 0x37:
		case 0x38:
		case 0x39:
		case 0x3A:
		case 0xBC:
			return 1;
		case 0x11:
		case 0x13:
		case 0x14:
		case 0x84:
		case 0x99:
		case 0x9A:
		case 0x9B:
		case 0x9C:
		case 0x9D:
		case 0x9E:
		case 0x9F:
		case 0xA0:
		case 0xA1:
		case 0xA2:
		case 0xA3:
		case 0xA4:
		case 0xA5:
		case 0xA6:
		case 0xA7:
		case 0xA8:
		case 0xB2:
		case 0xB3:
		case 0xB4:
		case 0xB5:
		case 0xB6:
		case 0xB7:
		case 0xB8:
		case 0xBB:
		case 0xBD:
		case 0xC0:
		case 0xC1:
		case 0xC6:
		case 0xC7:
			return 2;
		case 0xC5:
			return 3;
		case 0xB9:
		case 0xBA:
		case 0xC8:
		case 0xC9:
			return 4;
		default:
			return 0;
		}
	}

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param cf
	 * @param attrNameIndex
	 * @param attrLength
	 */
	protected CodeAttrInfo(final ClassFile cf, final int attrNameIndex, final int attrLength) {
		super(cf, attrNameIndex, attrLength);
	}

	/**
	 * Return the length in bytes of the attribute.
	 */
	@Override
	protected int getAttrInfoLength() {
		int length = CodeAttrInfo.CONSTANT_FIELD_SIZE + this.u4codeLength
				+ this.exceptionTable.size() * ExceptionInfo.CONSTANT_FIELD_SIZE;
		for (final AttrInfo at : this.attributes) {
			length += AttrInfo.CONSTANT_FIELD_SIZE + at.getAttrInfoLength();
		}
		return length;
	}

	/**
	 * Return the String name of the attribute; over-ride this in sub-classes.
	 */
	@Override
	protected String getAttrName() {
		return ClassConstants.ATTR_Code;
	}

	/**
	 * Trim attributes from the classfile ('Code', 'Exceptions', 'ConstantValue' are
	 * preserved, all others except those in the {@code List<String>} are killed).
	 */
	@Override
	protected void trimAttrsExcept(final List<String> keepAttrs) {
		// Traverse all attributes, removing all except those on 'keep' list
		final List<AttrInfo> delAttrs = new ArrayList<>();
		for (final AttrInfo at : this.attributes) {
			if (keepAttrs.contains(at.getAttrName())) {
				at.trimAttrsExcept(keepAttrs);
			} else {
				delAttrs.add(at);
			}
		}

		this.attributes.removeAll(delAttrs);
	}

	/**
	 * Check for references in the 'info' data to the constant pool and mark them.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void markUtf8RefsInInfo(final ConstantPool pool) throws ClassFileException {
		for (final AttrInfo at : this.attributes) {
			at.markUtf8Refs(pool);
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
		this.u2maxStack = din.readUnsignedShort();
		this.u2maxLocals = din.readUnsignedShort();
		this.u4codeLength = din.readInt();
		this.code = new byte[this.u4codeLength];
		din.readFully(this.code);
		final int u2exceptionTableLength = din.readUnsignedShort();
		this.exceptionTable = new ArrayList<>(u2exceptionTableLength);
		for (int i = 0; i < u2exceptionTableLength; i++) {
			this.exceptionTable.add(ExceptionInfo.create(din));
		}
		final int u2attributesCount = din.readUnsignedShort();
		this.attributes = new ArrayList<>(u2attributesCount);
		for (int i = 0; i < u2attributesCount; i++) {
			this.attributes.add(AttrInfo.create(din, this.cf, AttrSource.CODE));
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
		dout.writeShort(this.u2maxStack);
		dout.writeShort(this.u2maxLocals);
		dout.writeInt(this.u4codeLength);
		dout.write(this.code);
		dout.writeShort(this.exceptionTable.size());
		for (final ExceptionInfo ex : this.exceptionTable) {
			ex.write(dout);
		}
		dout.writeShort(this.attributes.size());
		for (final AttrInfo at : this.attributes) {
			at.write(dout);
		}
	}

	/**
	 * Do necessary name remapping.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	protected void remap(final ClassFile cf, final NameMapper nm) throws ClassFileException {
		for (final AttrInfo at : this.attributes) {
			at.remap(cf, nm);
		}
	}

	/**
	 * Walk the code, finding .class and Class.forName to update.
	 * 
	 * @param cpToFlag
	 * @throws ClassFileException
	 */
	protected FlagHashtable walkFindClassStrings(final FlagHashtable cpToFlag) throws ClassFileException {
		return this.walkClassStrings(cpToFlag, Collections.<Integer, Integer>emptyMap());
	}

	/**
	 * Walk the code, updating .class and Class.forName strings.
	 * 
	 * @param cpUpdate
	 * @throws ClassFileException
	 */
	protected void walkUpdateClassStrings(final Map<Integer, ?> cpUpdate) throws ClassFileException {
		this.walkClassStrings(null, cpUpdate);
	}

	/**
	 * Walk the code, updating .class and Class.forName strings. Note that class
	 * literals MyClass.class are stored directly in the constant pool in 1.5
	 * (change from 1.4), not referenced by Utf8 name, so .option MapClassString is
	 * not necessary for them. Still needed for Class.forName("MyClass") though.
	 * 
	 * @param cpToFlag
	 * @param cpUpdate
	 * @throws ClassFileException
	 */
	private FlagHashtable walkClassStrings(final FlagHashtable cpToFlag, final Map<Integer, ?> cpUpdate)
			throws ClassFileException {
		int opcodePrev = -1;
		int ldcIndex = -1;
		for (int i = 0; i < this.code.length; i++) {
			final int opcode = this.code[i] & 0xFF;
			if (opcode == 0x12 && i + 1 < this.code.length) // ldc
			{
				ldcIndex = this.code[i + 1] & 0xFF;
				final CpInfo ldcCpInfo = this.cf.getCpEntry(ldcIndex);
				if (!(ldcCpInfo instanceof StringCpInfo)) {
					ldcIndex = -1;
				}
			} else if (opcode == 0x13 && i + 2 < this.code.length) // ldc_w
			{
				ldcIndex = ((this.code[i + 1] & 0xFF) << 8) + (this.code[i + 2] & 0xFF);
				final CpInfo ldcCpInfo = this.cf.getCpEntry(ldcIndex);
				if (!(ldcCpInfo instanceof StringCpInfo)) {
					ldcIndex = -1;
				}
			}
			if ((opcodePrev == 0x12 || opcodePrev == 0x13) && ldcIndex != -1) // ldc or ldc_w and is a StringCpInfo
			{
				boolean isClassForName = false;
				if (opcode == 0xB8 && i + 2 < this.code.length) // invokestatic
				{
					final int invokeIndex = ((this.code[i + 1] & 0xFF) << 8) + (this.code[i + 2] & 0xFF);
					final CpInfo cpInfo = this.cf.getCpEntry(invokeIndex);
					if (cpInfo instanceof MethodrefCpInfo) {
						final MethodrefCpInfo entry = (MethodrefCpInfo) cpInfo;
						final ClassCpInfo classEntry = (ClassCpInfo) this.cf.getCpEntry(entry.getClassIndex());
						final String className = this.cf.getUtf8(classEntry.getNameIndex());
						final NameAndTypeCpInfo ntEntry = (NameAndTypeCpInfo) this.cf
								.getCpEntry(entry.getNameAndTypeIndex());
						final String name = this.cf.getUtf8(ntEntry.getNameIndex());
						final String descriptor = this.cf.getUtf8(ntEntry.getDescriptorIndex());
						if ("class$".equals(name)
								&& ("(Ljava/lang/String;)Ljava/lang/Class;".equals(descriptor)
										|| "(Ljava/lang/String;Z)Ljava/lang/Class;".equals(descriptor))
								|| "java/lang/Class".equals(className) && "forName".equals(name)
										&& "(Ljava/lang/String;)Ljava/lang/Class;".equals(descriptor)) {
							isClassForName = true;
							// Update StringCpInfo index in ldc to new one
							final Object o = cpUpdate.get(new Integer(ldcIndex));
							if (o instanceof Integer) {
								final Integer oi = (Integer) o;
								final int remapStringIndex = oi.intValue();
								switch (opcodePrev) {
								case 0x13: // ldc_w
									this.code[i - 2] = 0;
									//$FALL-THROUGH$
								case 0x12: // ldc
									this.code[i - 1] = (byte) remapStringIndex;
									break;
								default: // error
									throw new RuntimeException(
											"Internal error: " + ".class or Class.forName remap of non-ldc/ldc_w");
								}
							}
						}
					}
				}
				if (cpToFlag != null) {
					cpToFlag.updateFlag(this.cf.getCpEntry(ldcIndex), ldcIndex, isClassForName);
				}
			}
			final int bytes = this.getOpcodeBytes(opcode, i);
			i += bytes;
			opcodePrev = opcode;
		}
		return cpToFlag;
	}

	/**
	 * Compute length of opcode arguments at offset
	 * 
	 * @param opcode
	 * @param i
	 * @throws ClassFileException
	 */
	private int getOpcodeBytes(final int opcode, final int i) throws ClassFileException {
		int bytes = CodeAttrInfo.opcodeBytes(opcode);
		if (bytes < 0) // variable length instructions
		{
			switch (opcode) {
			case 0xAA: // tableswitch
				bytes = 3 - i % 4; // 0-3 byte pad
				bytes += 4; // default value
				final int low = ((this.code[i + 1 + bytes] & 0xFF) << 24)
						+ ((this.code[i + 1 + bytes + 1] & 0xFF) << 16) + ((this.code[i + 1 + bytes + 2] & 0xFF) << 8)
						+ (this.code[i + 1 + bytes + 3] & 0xFF);
				bytes += 4; // low value
				final int high = ((this.code[i + 1 + bytes] & 0xFF) << 24)
						+ ((this.code[i + 1 + bytes + 1] & 0xFF) << 16) + ((this.code[i + 1 + bytes + 2] & 0xFF) << 8)
						+ (this.code[i + 1 + bytes + 3] & 0xFF);
				bytes += 4; // high value
				if (high >= low) {
					bytes += (high - low + 1) * 4; // jump offsets
				}
				break;
			case 0xAB: // lookupswitch
				bytes = 3 - i % 4; // 0-3 byte pad
				bytes += 4; // default value
				final int npairs = ((this.code[i + 1 + bytes] & 0xFF) << 24)
						+ ((this.code[i + 1 + bytes + 1] & 0xFF) << 16) + ((this.code[i + 1 + bytes + 2] & 0xFF) << 8)
						+ (this.code[i + 1 + bytes + 3] & 0xFF);
				bytes += 4; // npairs value
				if (npairs >= 0) {
					bytes += npairs * 8; // match / offset pairs
				}
				break;
			case 0xC4: // wide
				final int wideOpcode = this.code[i + 1] & 0xFF;
				switch (wideOpcode) {
				case 0x15: // iload
				case 0x16: // lload
				case 0x17: // fload
				case 0x18: // dload
				case 0x19: // aload
				case 0x36: // istore
				case 0x37: // lstore
				case 0x38: // fstore
				case 0x39: // dstore
				case 0x3A: // astore
				case 0xA9: // ret
					bytes = 3;
					break;
				case 0x84: // iinc
					bytes = 5;
					break;
				default:
					throw new ClassFileException("Illegal wide opcode");
				}
				break;
			default:
				throw new ClassFileException("Illegal variable length opcode");
			}
		}
		return bytes;
	}
}
