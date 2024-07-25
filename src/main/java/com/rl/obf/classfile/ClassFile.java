/* ===========================================================================
 * $RCSfile: ClassFile.java,v $
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is a representation of the data in a Java class-file (*.class). A
 * ClassFile instance representing a *.class file can be generated using the
 * static create(DataInput) method, manipulated using various operators, and
 * persisted back using the write(DataOutput) method.
 *
 * @author Mark Welsh
 */
public class ClassFile implements ClassConstants {
	// Constants -------------------------------------------------------------
	public static final String SEP_REGULAR = "/";
	public static final String SEP_INNER = "$";
	private static final String CLASS_FORNAME_NAME_DESCRIPTOR = "forName(Ljava/lang/String;)Ljava/lang/Class;";
	private static final String[] DANGEROUS_CLASS_SIMPLENAME_DESCRIPTOR_ARRAY = {
			"getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;",
			"getField(Ljava/lang/String;)Ljava/lang/reflect/Field;",
			"getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
			"getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;" };
	private static final String LOG_DANGER_CLASS_PRE = "     Your class ";
	private static final String LOG_DANGER_CLASS_MID = " calls the java/lang/Class method ";
	private static final String LOG_CLASS_FORNAME_MID = " uses '.class' or calls java/lang/Class.";
	private static final String[] DANGEROUS_CLASSLOADER_SIMPLENAME_DESCRIPTOR_ARRAY = {
			"defineClass(Ljava/lang/String;[BII)Ljava/lang/Class;",
			"findLoadedClass(Ljava/lang/String;)Ljava/lang/Class;",
			"findSystemClass(Ljava/lang/String;)Ljava/lang/Class;", "loadClass(Ljava/lang/String;)Ljava/lang/Class;",
			"loadClass(Ljava/lang/String;Z)Ljava/lang/Class;" };
	private static final String LOG_DANGER_CLASSLOADER_PRE = "     Your class ";
	private static final String LOG_DANGER_CLASSLOADER_MID = " calls the java/lang/ClassLoader method ";

	// Fields ----------------------------------------------------------------
	private int u4magic;
	private int u2minorVersion;
	private int u2majorVersion;
	private ConstantPool constantPool;
	private int u2accessFlags;
	private int u2thisClass;
	private int u2superClass;
	private List<Integer> u2interfaces;
	private List<FieldInfo> fields;
	private List<MethodInfo> methods;
	private List<AttrInfo> attributes;

	private CpInfo cpIdString = null;

	// Class Methods ---------------------------------------------------------
	/**
	 * Create a new ClassFile from the class file format data in the DataInput
	 * stream.
	 *
	 * @param din
	 * @throws IOException
	 * @throws ClassFileException
	 */
	public static ClassFile create(final DataInput din) throws IOException, ClassFileException {
		if (din == null) {
			throw new IOException("No input stream was provided.");
		}
		final ClassFile cf = new ClassFile();
		cf.read(din);
		return cf;
	}

	/**
	 * Parse a method descriptor into a list of parameter names and a return type,
	 * in same format as the Class.forName() method returns.
	 *
	 * @param descriptor
	 * @throws ClassFileException
	 */
	public static List<String> parseMethodDescriptor(final String descriptor) throws ClassFileException {
		String descriptorPart = descriptor;
		final List<String> names = new ArrayList<>();
		if (descriptorPart.charAt(0) != '(') {
			throw new ClassFileException("Illegal method descriptor: " + descriptor);
		}

		descriptorPart = descriptorPart.substring(1);
		String type = "";
		boolean foundParamEnd = false;
		int returnParamCnt = 0;
		while (descriptorPart.length() > 0) {
			switch (descriptorPart.charAt(0)) {
			case '[':
				type = type + "[";
				descriptorPart = descriptorPart.substring(1);
				break;

			case 'B':
			case 'C':
			case 'D':
			case 'F':
			case 'I':
			case 'J':
			case 'S':
			case 'Z':
			case 'V':
				names.add(ClassFile.translateType(type + descriptorPart.substring(0, 1)));
				descriptorPart = descriptorPart.substring(1);
				type = "";
				if (foundParamEnd) {
					returnParamCnt++;
				}
				break;

			case ')':
				descriptorPart = descriptorPart.substring(1);
				foundParamEnd = true;
				break;

			case 'L': {
				final int pos = descriptorPart.indexOf(';') + 1;
				names.add(ClassFile.translateType(type + descriptorPart.substring(0, pos)));
				descriptorPart = descriptorPart.substring(pos);
				type = "";
				if (foundParamEnd) {
					returnParamCnt++;
				}
				break;
			}

			default:
				throw new ClassFileException("Illegal method descriptor: " + descriptor);
			}
		}

		if (returnParamCnt != 1) {
			throw new ClassFileException("Illegal method descriptor: " + descriptor);
		}

		return names;
	}

	/**
	 * Translate a type specifier from the internal JVM convention to the
	 * Class.forName() one.
	 *
	 * @param inName
	 * @throws ClassFileException
	 */
	public static String translateType(final String inName) throws ClassFileException {
		String outName = null;
		switch (inName.charAt(0)) {
		case '[':
			// For array types, Class.forName() inconsistently uses the internal type name
			// but with '/' --> '.'
			outName = ClassFile.translate(inName);
			break;

		case 'B':
			outName = Byte.TYPE.getName();
			break;

		case 'C':
			outName = Character.TYPE.getName();
			break;

		case 'D':
			outName = Double.TYPE.getName();
			break;

		case 'F':
			outName = Float.TYPE.getName();
			break;

		case 'I':
			outName = Integer.TYPE.getName();
			break;

		case 'J':
			outName = Long.TYPE.getName();
			break;

		case 'S':
			outName = Short.TYPE.getName();
			break;

		case 'Z':
			outName = Boolean.TYPE.getName();
			break;

		case 'V':
			outName = Void.TYPE.getName();
			break;

		case 'L': {
			final int pos = inName.indexOf(';');
			outName = ClassFile.translate(inName.substring(1, pos));
			break;
		}

		default:
			throw new ClassFileException("Illegal field or method name: " + inName);
		}
		return outName;
	}

	/**
	 * Translate a class name from the internal '/' convention to the regular '.'
	 * one.
	 *
	 * @param name
	 */
	public static String translate(final String name) {
		return name.replace('/', '.');
	}

	/**
	 * Translate a class name from the the regular '.' convention to internal '/'
	 * one.
	 *
	 * @param name
	 */
	public static String backTranslate(final String name) {
		return name.replace('.', '/');
	}

	/**
	 * Is this class in an unsupported version of the file format?
	 */
	public boolean hasIncompatibleVersion() {
		return this.u2majorVersion > ClassConstants.MAJOR_VERSION;
	}

	/**
	 * Return major version of this class's file format.
	 */
	public int getMajorVersion() {
		return this.u2majorVersion;
	}

	// Instance Methods ------------------------------------------------------
	/**
	 * Private constructor.
	 */
	private ClassFile() {
	}

	/**
	 * Import the class data to internal representation.
	 *
	 * @param din
	 * @throws IOException
	 * @throws ClassFileException
	 */
	private void read(final DataInput din) throws IOException, ClassFileException {
		// Read the class file
		this.u4magic = din.readInt();
		this.u2minorVersion = din.readUnsignedShort();
		this.u2majorVersion = din.readUnsignedShort();

		// Check this is a valid classfile that we can handle
		if (this.u4magic != ClassConstants.MAGIC) {
			throw new ClassFileException("Invalid magic number in class file.");
		}
		// if (this.u2majorVersion > ClassConstants.MAJOR_VERSION)
		// {
		// throw new ClassFileException("Incompatible version number for class file
		// format.");
		// }

		final int u2constantPoolCount = din.readUnsignedShort();
		final List<CpInfo> cpInfo = new ArrayList<>(u2constantPoolCount);
		// Fill the constant pool, recalling the zero entry is not persisted, nor are
		// the entries following a Long or Double
		cpInfo.add(null);
		for (int i = 1; i < u2constantPoolCount; i++) {
			final CpInfo cp = CpInfo.create(din);
			cpInfo.add(cp);
			if (cp instanceof LongCpInfo || cp instanceof DoubleCpInfo) {
				i++;
				cpInfo.add(null);
			}
		}
		this.constantPool = new ConstantPool(this, cpInfo);

		this.u2accessFlags = din.readUnsignedShort();
		this.u2thisClass = din.readUnsignedShort();
		this.u2superClass = din.readUnsignedShort();
		final int u2interfacesCount = din.readUnsignedShort();
		this.u2interfaces = new ArrayList<>(u2interfacesCount);
		for (int i = 0; i < u2interfacesCount; i++) {
			this.u2interfaces.add(din.readUnsignedShort());
		}
		final int u2fieldsCount = din.readUnsignedShort();
		this.fields = new ArrayList<>(u2fieldsCount);
		for (int i = 0; i < u2fieldsCount; i++) {
			this.fields.add(FieldInfo.create(din, this));
		}
		final int u2methodsCount = din.readUnsignedShort();
		this.methods = new ArrayList<>(u2methodsCount);
		for (int i = 0; i < u2methodsCount; i++) {
			this.methods.add(MethodInfo.create(din, this));
		}
		final int u2attributesCount = din.readUnsignedShort();
		this.attributes = new ArrayList<>(u2attributesCount);
		for (int i = 0; i < u2attributesCount; i++) {
			this.attributes.add(AttrInfo.create(din, this, AttrSource.CLASS));
		}
	}

	/**
	 * Define a constant String to include in this output class file.
	 *
	 * @param id
	 */
	public void setIdString(final String id) {
		if (id != null) {
			this.cpIdString = new Utf8CpInfo(id);
		} else {
			this.cpIdString = null;
		}
	}

	/**
	 * Return the access modifiers for this classfile.
	 */
	public int getModifiers() {
		return this.u2accessFlags;
	}

	/**
	 * Return the name of this classfile.
	 *
	 * @throws ClassFileException
	 */
	public String getName() throws ClassFileException {
		return this.toName(this.u2thisClass);
	}

	/**
	 * Return the name of this class's superclass.
	 *
	 * @throws ClassFileException
	 */
	public String getSuper() throws ClassFileException {
		// This may be java/lang/Object, in which case there is no super
		if (this.u2superClass == 0) {
			return null;
		}

		return this.toName(this.u2superClass);
	}

	/**
	 * Return the names of this class's interfaces.
	 *
	 * @throws ClassFileException
	 */
	public List<String> getInterfaces() throws ClassFileException {
		final List<String> interfaces = new ArrayList<>();
		for (final int intf : this.u2interfaces) {
			interfaces.add(this.toName(intf));
		}
		return interfaces;
	}

	/**
	 * Convert a CP index to a class name.
	 *
	 * @param u2index
	 * @throws ClassFileException
	 */
	private String toName(final int u2index) throws ClassFileException {
		final CpInfo classEntry = this.getCpEntry(u2index);
		if (classEntry instanceof ClassCpInfo) {
			final ClassCpInfo entry = (ClassCpInfo) classEntry;
			return entry.getName(this);
		}

		throw new ClassFileException("Inconsistent Constant Pool in class file.");
	}

	/**
	 * Return all methods in class.
	 */
	public List<MethodInfo> getMethods() {
		return this.methods;
	}

	/**
	 * Return all fields in class.
	 */
	public List<FieldInfo> getFields() {
		return this.fields;
	}

	/**
	 * Lookup the entry in the constant pool and return as a {@code CpInfo}.
	 *
	 * @param cpIndex
	 * @throws ClassFileException
	 */
	protected CpInfo getCpEntry(final int cpIndex) throws ClassFileException {
		return this.constantPool.getCpEntry(cpIndex);
	}

	/**
	 * Remap a specified Utf8 entry to the given value and return its new index.
	 *
	 * @param newString
	 * @param oldIndex
	 * @throws ClassFileException
	 */
	public int remapUtf8To(final String newString, final int oldIndex) throws ClassFileException {
		return this.constantPool.remapUtf8To(newString, oldIndex);
	}

	/**
	 * Lookup the UTF8 string in the constant pool.
	 *
	 * @param cpIndex
	 * @throws ClassFileException
	 */
	protected String getUtf8(final int cpIndex) throws ClassFileException {
		final CpInfo utf8Entry = this.getCpEntry(cpIndex);
		if (utf8Entry instanceof Utf8CpInfo) {
			final Utf8CpInfo entry = (Utf8CpInfo) utf8Entry;
			return entry.getString();
		}

		throw new ClassFileException("Not UTF8Info");
	}

	/**
	 * List methods which can break obfuscated code, and log to a
	 * {@code List<String>}.
	 *
	 * @param list
	 */
	public List<String> listDangerMethods(final List<String> list) {
		// Need only check CONSTANT_Methodref entries of constant pool since dangerous
		// methods belong to classes 'Class' and
		// 'ClassLoader', not to interfaces.
		for (final CpInfo cpInfo : this.constantPool) {
			if (cpInfo instanceof MethodrefCpInfo) {
				try {
					// Get the method class name, simple name and descriptor
					final MethodrefCpInfo entry = (MethodrefCpInfo) cpInfo;
					final ClassCpInfo classEntry = (ClassCpInfo) this.getCpEntry(entry.getClassIndex());
					final String className = this.getUtf8(classEntry.getNameIndex());
					final NameAndTypeCpInfo ntEntry = (NameAndTypeCpInfo) this.getCpEntry(entry.getNameAndTypeIndex());
					final String name = this.getUtf8(ntEntry.getNameIndex());
					final String descriptor = this.getUtf8(ntEntry.getDescriptorIndex());

					// Check if this is on the proscribed list
					if (className.equals("java/lang/Class")) {
						if (ClassFile.CLASS_FORNAME_NAME_DESCRIPTOR.equals(name + descriptor)) {
							list.add(ClassFile.LOG_DANGER_CLASS_PRE + this.getName() + ClassFile.LOG_CLASS_FORNAME_MID
									+ ClassFile.CLASS_FORNAME_NAME_DESCRIPTOR);
						} else if (Arrays.asList(ClassFile.DANGEROUS_CLASS_SIMPLENAME_DESCRIPTOR_ARRAY)
								.contains(name + descriptor)) {
							list.add(ClassFile.LOG_DANGER_CLASS_PRE + this.getName() + ClassFile.LOG_DANGER_CLASS_MID
									+ name + descriptor);
						}
					} else if (Arrays.asList(ClassFile.DANGEROUS_CLASSLOADER_SIMPLENAME_DESCRIPTOR_ARRAY)
							.contains(name + descriptor)) {
						list.add(ClassFile.LOG_DANGER_CLASSLOADER_PRE + this.getName()
								+ ClassFile.LOG_DANGER_CLASSLOADER_MID + name + descriptor);
					}
				} catch (final ClassFileException e) {
					// ignore
				}
			}
		}
		return list;
	}

	/**
	 * Check for direct references to Utf8 constant pool entries.
	 *
	 * @throws ClassFileException
	 */
	public void markUtf8Refs() throws ClassFileException {
		// Check for references to Utf8 from outside the constant pool
		for (final FieldInfo fd : this.fields) {
			fd.markUtf8Refs(this.constantPool);
		}
		for (final MethodInfo md : this.methods) {
			md.markUtf8Refs(this.constantPool);
		}
		for (final AttrInfo at : this.attributes) {
			at.markUtf8Refs(this.constantPool);
		}

		// Now check for references from other CP entries
		for (final CpInfo cpInfo : this.constantPool) {
			if (cpInfo != null) {
				cpInfo.markUtf8Refs(this.constantPool);
			}
		}
	}

	/**
	 * Check for direct references to NameAndType constant pool entries.
	 *
	 * @throws ClassFileException
	 */
	public void markNTRefs() throws ClassFileException {
		// Now check the method and field CP entries
		for (final CpInfo cpInfo : this.constantPool) {
			if (cpInfo != null) {
				cpInfo.markNTRefs(this.constantPool);
			}
		}
	}

	/**
	 * Trim attributes from the classfile ('Code', 'Exceptions', 'ConstantValue' are
	 * preserved, all others except those in the {@code List<String>} are killed).
	 *
	 * @param extraAttrs
	 */
	public void trimAttrsExcept(final List<String> extraAttrs) {
		// Merge additional attributes with required list
		final List<String> keepAttrs = new ArrayList<>(Arrays.asList(ClassConstants.REQUIRED_ATTRS));
		keepAttrs.addAll(extraAttrs);

		// Traverse all attributes, removing all except those on 'keep' list
		for (final FieldInfo fd : this.fields) {
			fd.trimAttrsExcept(keepAttrs);
		}
		for (final MethodInfo md : this.methods) {
			md.trimAttrsExcept(keepAttrs);
		}

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
	 * Update the constant pool reference counts.
	 *
	 * @throws ClassFileException
	 */
	public void updateRefCount() throws ClassFileException {
		this.constantPool.updateRefCount();
	}

	/**
	 * Remove unnecessary attributes from the class.
	 *
	 * @param nm
	 */
	public void trimAttrs(final NameMapper nm) {
		this.trimAttrsExcept(nm.getAttrsToKeep());
	}

	/**
	 * Remap the entities in the specified ClassFile.
	 *
	 * @param nm
	 * @param log
	 * @param enableMapClassString
	 * @throws ClassFileException
	 */
	public void remap(final NameMapper nm, final PrintWriter log, final boolean enableMapClassString)
			throws ClassFileException {
		// Go through all of class's fields and methods mapping 'name' and 'descriptor'
		// references
		final ClassCpInfo cls = (ClassCpInfo) this.getCpEntry(this.u2thisClass);
		final String thisClassName = this.getUtf8(cls.getNameIndex());

		List<BootStrapMethod> bsm = null;
		for (final AttrInfo attr : this.attributes) {
			if (attr instanceof BootstrapMethodsAttrInfo) {
				bsm = ((BootstrapMethodsAttrInfo) attr).getBSM();
				break;
			}
		}

		for (final FieldInfo fd : this.fields) {
			// Remap field 'name', unless it is 'Synthetic'
			if (!fd.isSynthetic()) {
				final String name = this.getUtf8(fd.getNameIndex());
				final String remapName = nm.mapField(thisClassName, name);
				fd.setNameIndex(this.constantPool.remapUtf8To(remapName, fd.getNameIndex()));
			}

			// Remap field 'descriptor'
			final String desc = this.getUtf8(fd.getDescriptorIndex());
			final String remapDesc = nm.mapDescriptor(desc);
			fd.setDescriptorIndex(this.constantPool.remapUtf8To(remapDesc, fd.getDescriptorIndex()));
		}
		for (final MethodInfo md : this.methods) {
			// Remap method 'name', unless it is 'Synthetic'
			final String desc = this.getUtf8(md.getDescriptorIndex());
			if (!md.isSynthetic()) {
				final String name = this.getUtf8(md.getNameIndex());
				final String remapName = nm.mapMethod(thisClassName, name, desc);
				md.setNameIndex(this.constantPool.remapUtf8To(remapName, md.getNameIndex()));
			}

			// Remap method 'descriptor'
			final String remapDesc = nm.mapDescriptor(desc);
			md.setDescriptorIndex(this.constantPool.remapUtf8To(remapDesc, md.getDescriptorIndex()));
		}

		// Remap all field/method names and descriptors in the constant pool (depends on
		// class names)
		final int currentCpLength = this.constantPool.length(); // constant pool can be extended (never contracted)
																// during loop
		for (int i = 0; i < currentCpLength; i++) {
			final CpInfo cpInfo = this.getCpEntry(i);
			// If this is a CONSTANT_Fieldref, CONSTANT_Methodref or
			// CONSTANT_InterfaceMethodref get the CONSTANT_NameAndType
			// and remap the name and the components of the descriptor string.
			if (cpInfo instanceof RefCpInfo) {
				// Get the unmodified class name
				final RefCpInfo refInfo = (RefCpInfo) cpInfo;
				final ClassCpInfo classInfo = (ClassCpInfo) this.getCpEntry(refInfo.getClassIndex());
				final String className = this.getUtf8(classInfo.getNameIndex());

				// Get the current N&T reference and its 'name' and 'descriptor' utf's
				final int ntIndex = refInfo.getNameAndTypeIndex();
				final NameAndTypeCpInfo nameTypeInfo = (NameAndTypeCpInfo) this.getCpEntry(ntIndex);
				final String ref = this.getUtf8(nameTypeInfo.getNameIndex());
				final String desc = this.getUtf8(nameTypeInfo.getDescriptorIndex());

				// Get the remapped versions of 'name' and 'descriptor'
				String remapRef;
				if (cpInfo instanceof FieldrefCpInfo) {
					remapRef = nm.mapField(className, ref);
				} else {
					remapRef = nm.mapMethod(className, ref, desc);
				}
				final String remapDesc = nm.mapDescriptor(desc);

				// If a remap is required, make a new N&T (increment ref count on 'name' and
				// 'descriptor', decrement original
				// N&T's ref count, set new N&T ref count to 1), remap new N&T's utf's
				if (!remapRef.equals(ref) || !remapDesc.equals(desc)) {
					// Get the new N&T guy
					NameAndTypeCpInfo newNameTypeInfo;
					if (nameTypeInfo.getRefCount() == 1) {
						newNameTypeInfo = nameTypeInfo;
					} else {
						// Create the new N&T info
						newNameTypeInfo = (NameAndTypeCpInfo) nameTypeInfo.clone();

						// Adjust its reference counts of its utf's
						this.getCpEntry(newNameTypeInfo.getNameIndex()).incRefCount();
						this.getCpEntry(newNameTypeInfo.getDescriptorIndex()).incRefCount();

						// Append it to the Constant Pool, and point the RefCpInfo entry to the new N&T
						// data
						refInfo.setNameAndTypeIndex(this.constantPool.addEntry(newNameTypeInfo));

						// Adjust reference counts from RefCpInfo
						newNameTypeInfo.incRefCount();
						nameTypeInfo.decRefCount();
					}

					// Remap the 'name' and 'descriptor' utf's in N&T
					newNameTypeInfo
							.setNameIndex(this.constantPool.remapUtf8To(remapRef, newNameTypeInfo.getNameIndex()));
					newNameTypeInfo.setDescriptorIndex(
							this.constantPool.remapUtf8To(remapDesc, newNameTypeInfo.getDescriptorIndex()));
				}
			} else if (cpInfo instanceof MethodTypeCpInfo) {
				final MethodTypeCpInfo mtInfo = (MethodTypeCpInfo) cpInfo;
				final String desc = this.getUtf8(mtInfo.getDescriptorIndex());
				final String remapDesc = nm.mapDescriptor(desc);

				if (!remapDesc.equals(desc)) {
					mtInfo.setDescriptorIndex(this.constantPool.remapUtf8To(remapDesc, mtInfo.getDescriptorIndex()));
				}
			} else if (cpInfo instanceof InvokeDynamicCpInfo) {
				final InvokeDynamicCpInfo idc = (InvokeDynamicCpInfo) cpInfo;
				if (bsm != null) {
					do {
						final BootStrapMethod b = bsm.get(idc.getBootstrapMethodAttrIndex());
						final CpInfo _factory = this.getCpEntry(b.getFactory());
						if (_factory instanceof MethodHandleCpInfo) {
							final MethodHandleCpInfo factory = (MethodHandleCpInfo) _factory;
							final String clsName = factory.getClassName(this);
							final String name = factory.getName(this);
							// remap LambdaMetafactory#metafactory based one
							if (clsName.equals("java/lang/invoke/LambdaMetafactory") && name.equals("metafactory")) {
								// b.getArguments().get(0) <=> MethodType samMethodType (3rd argument of the
								// bootstrap method)
								// https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/LambdaMetafactory.html
								final MethodTypeCpInfo mtcp = (MethodTypeCpInfo) this.constantPool
										.getCpEntry(b.getArguments().get(0));
								final String desc = this.getUtf8(mtcp.getDescriptorIndex()); // method descriptor for
																								// actual method

								final int ntIndex = idc.getNameAndTypeIndex();
								final NameAndTypeCpInfo nameTypeInfo = (NameAndTypeCpInfo) this.getCpEntry(ntIndex);
								final String ref = this.getUtf8(nameTypeInfo.getNameIndex());
								final String ntDesc = this.getUtf8(nameTypeInfo.getDescriptorIndex()); // return
																										// descriptor

								final String[] arr = ntDesc.split("L");
								final String className = arr[arr.length - 1].split(";")[0];
								// CAUTION: 3rd argument must be a method descriptor *for an actual method*, not
								// lambda expression's
								// one
								final String remapName = nm.mapMethod(className, ref, desc);
								final String remapNtDesc = nm.mapDescriptor(ntDesc); // method descriptor *for the
																						// lambda
																						// expression*

								// If a remap is required, make a new N&T (increment ref count on 'name' and
								// 'descriptor', decrement original
								// N&T's ref count, set new N&T ref count to 1), remap new N&T's utf's
								if (!remapName.equals(ref) || !remapNtDesc.equals(ntDesc)) {
									// Get the new N&T guy
									NameAndTypeCpInfo newNameTypeInfo;
									if (nameTypeInfo.getRefCount() == 1) {
										newNameTypeInfo = nameTypeInfo;
									} else {
										// Create the new N&T info
										newNameTypeInfo = (NameAndTypeCpInfo) nameTypeInfo.clone();

										// Adjust its reference counts of its utf's
										this.getCpEntry(newNameTypeInfo.getNameIndex()).incRefCount();
										this.getCpEntry(newNameTypeInfo.getDescriptorIndex()).incRefCount();

										// Append it to the Constant Pool, and point the RefCpInfo entry to the new N&T
										// data
										idc.setNameAndTypeIndex(this.constantPool.addEntry(newNameTypeInfo));

										// Adjust reference counts from RefCpInfo
										newNameTypeInfo.incRefCount();
										nameTypeInfo.decRefCount();
									}

									// Remap the 'name' and 'descriptor' utf's in N&T
									newNameTypeInfo.setNameIndex(
											this.constantPool.remapUtf8To(remapName, newNameTypeInfo.getNameIndex()));
									newNameTypeInfo.setDescriptorIndex(this.constantPool.remapUtf8To(remapNtDesc,
											newNameTypeInfo.getDescriptorIndex()));
								}
								break;
							} else if (clsName.equals("java/lang/invoke/StringConcatFactory")
									&& name.equals("makeConcatWithConstants")) {
								// do nothing
								break;
							} else if (clsName.equals("java/lang/runtime/ObjectMethods") && name.equals("bootstrap")) {
								// do nothing
								break;
							}
							throw new UnsupportedOperationException(String
									.format("RetroGuard doesn't support this CallSite factory: %s#%s", clsName, name));
						}
					} while (false);
				} else {
					throw new IllegalArgumentException("RetroGuard can't find any BootStrapMethod at given class");
				}
			}

		}

		// Remap all class references to Utf
		for (int i = 0; i < this.constantPool.length(); i++) {
			final CpInfo cpInfo = this.getCpEntry(i);
			// If this is CONSTANT_Class, remap the class-name Utf8 entry
			if (cpInfo instanceof ClassCpInfo) {
				final ClassCpInfo classInfo = (ClassCpInfo) cpInfo;
				final String className = this.getUtf8(classInfo.getNameIndex());
				final String remapClass = nm.mapClass(className);
				final int remapIndex = this.constantPool.remapUtf8To(remapClass, classInfo.getNameIndex());
				classInfo.setNameIndex(remapIndex);
			}
		}

		// Remap all annotation type references to Utf8 classes
		for (final AttrInfo at : this.attributes) {
			at.remap(this, nm);
		}
		for (final MethodInfo md : this.methods) {
			for (final AttrInfo at : md.attributes) {
				at.remap(this, nm);
			}
		}
		for (final FieldInfo fd : this.fields) {
			for (final AttrInfo at : fd.attributes) {
				at.remap(this, nm);
			}
		}

		// If reflection, attempt to remap all class string references
		// NOTE - hasReflection wasn't picking up reflection in inner classes because
		// they call to the outer class to do
		// forName(...). Therefore removed.
		// if (hasReflection && enableMapClassString)
		if (enableMapClassString) {
			this.remapClassStrings(nm, log);
		}
	}

	/**
	 * Remap Class.forName and .class, leaving other identical Strings alone
	 *
	 * @param nm
	 * @param log
	 * @throws ClassFileException
	 */
	private void remapClassStrings(final NameMapper nm, final PrintWriter log) throws ClassFileException {
		// Visit all method Code attributes, collecting information on remap
		FlagHashtable cpToFlag = new FlagHashtable();
		for (final MethodInfo methodInfo : this.methods) {
			for (final AttrInfo attrInfo : methodInfo.attributes) {
				if (attrInfo instanceof CodeAttrInfo) {
					cpToFlag = ((CodeAttrInfo) attrInfo).walkFindClassStrings(cpToFlag);
				}
			}
		}
		// Analyse String mapping flags and generate updated Strings
		final Map<Integer, Integer> cpUpdate = new HashMap<>();
		for (final Entry<CpInfo, StringCpInfoFlags> entry : cpToFlag.entrySet()) {
			final StringCpInfo stringCpInfo = (StringCpInfo) entry.getKey();
			final StringCpInfoFlags flags = entry.getValue();
			final String name = ClassFile.backTranslate(this.getUtf8(stringCpInfo.getStringIndex()));
			// String accessed as Class.forName or .class?
			if (ClassFile.isClassSpec(name) && flags.forNameFlag) {
				final String remapName = nm.mapClass(name);
				if (!remapName.equals(name)) // skip if no remap needed
				{
					boolean simpleRemap = false;
					// String accessed in another way, so split in ConstantPool
					if (flags.otherFlag) {
						// Create a new String/Utf8 for remapped Class-name
						final int remapUtf8Index = this.constantPool.addUtf8Entry(ClassFile.translate(remapName));
						final StringCpInfo remapStringInfo = new StringCpInfo();
						remapStringInfo.setStringIndex(remapUtf8Index);
						final int remapStringIndex = this.constantPool.addEntry(remapStringInfo);
						// Default to full remap if new String would require ldc_w to access - we can't
						// cope with that yet
						if (remapStringIndex > 0xFF) {
							simpleRemap = true;
							log.println("# WARNING MapClassString: non-.class/Class.forName() string remapped");
						} else {
							log.println("# MapClassString (partial) in class " + this.getName() + ": " + name + " -> "
									+ remapName);
							// Add to cpUpdate hash for later remap in Code
							cpUpdate.put(new Integer(flags.stringIndex), new Integer(remapStringIndex));
						}
					} else
					// String only accessed as Class.forName
					{
						simpleRemap = true;
					}
					if (simpleRemap) {
						log.println("# MapClassString (full) in class " + this.getName() + ": " + name + " -> "
								+ remapName);
						// Just remap the existing String/Utf8, since it is only used for Class.forName
						// or .class, or maybe ldc_w
						// was needed (which gives improper String remap)
						final int remapIndex = this.constantPool.remapUtf8To(ClassFile.translate(remapName),
								stringCpInfo.getStringIndex());
						stringCpInfo.setStringIndex(remapIndex);
					}
				}
			}
		}
		// Visit all method Code attributes, remapping .class/Class.forName
		for (final MethodInfo methodInfo : this.methods) {
			for (final AttrInfo attrInfo : methodInfo.attributes) {
				if (attrInfo instanceof CodeAttrInfo) {
					final CodeAttrInfo codeAttrInfo = (CodeAttrInfo) attrInfo;
					codeAttrInfo.walkUpdateClassStrings(cpUpdate);
				}
			}
		}
	}

	/**
	 * Is this String a valid class specifier?
	 *
	 * @param s
	 */
	private static boolean isClassSpec(String s) {
		if (s.length() == 0) {
			return false;
		}
		int pos = -1;
		while ((pos = s.lastIndexOf('/')) != -1) {
			if (!ClassFile.isJavaIdentifier(s.substring(pos + 1))) {
				return false;
			}
			s = s.substring(0, pos);
		}
		if (!ClassFile.isJavaIdentifier(s)) {
			return false;
		}
		return true;
	}

	/**
	 * Is this String a valid Java identifier?
	 *
	 * @param s
	 */
	private static boolean isJavaIdentifier(final String s) {
		if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
			return false;
		}
		for (int i = 1; i < s.length(); i++) {
			if (!Character.isJavaIdentifierPart(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Export the representation to a DataOutput stream.
	 *
	 * @param dout
	 * @throws IOException
	 * @throws ClassFileException
	 */
	public void write(final DataOutput dout) throws IOException, ClassFileException {
		if (dout == null) {
			throw new IOException("No output stream was provided.");
		}
		dout.writeInt(this.u4magic);
		dout.writeShort(this.u2minorVersion);
		dout.writeShort(this.u2majorVersion);
		dout.writeShort(this.constantPool.length() + (this.cpIdString != null ? 1 : 0));
		for (final CpInfo cpInfo : this.constantPool) {
			if (cpInfo != null) {
				cpInfo.write(dout);
			}
		}
		if (this.cpIdString != null) {
			this.cpIdString.write(dout);
		}
		dout.writeShort(this.u2accessFlags);
		dout.writeShort(this.u2thisClass);
		dout.writeShort(this.u2superClass);
		dout.writeShort(this.u2interfaces.size());
		for (final int intf : this.u2interfaces) {
			dout.writeShort(intf);
		}
		dout.writeShort(this.fields.size());
		for (final FieldInfo fd : this.fields) {
			fd.write(dout);
		}
		dout.writeShort(this.methods.size());
		for (final MethodInfo md : this.methods) {
			md.write(dout);
		}
		dout.writeShort(this.attributes.size());
		for (final AttrInfo at : this.attributes) {
			at.write(dout);
		}
	}
}
