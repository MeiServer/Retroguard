/* ===========================================================================
 * $RCSfile: Cl.java,v $
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

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rl.NameProvider;
import com.rl.obf.classfile.ClassFile;
import com.rl.obf.classfile.ClassFileException;
import com.rl.obf.classfile.FieldInfo;
import com.rl.obf.classfile.MethodInfo;

/**
 * Tree item representing a class or interface.
 *
 * @author Mark Welsh
 */
public class Cl extends PkCl implements NameListUp, NameListDown {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	/**
	 * Owns a list of methods
	 */
	private Map<String, Md> mds = new HashMap<>();

	/**
	 * Owns a list of special methods
	 */
	private Map<String, Md> mdsSpecial = new HashMap<>();

	/**
	 * Owns a list of fields
	 */
	private Map<String, Fd> fds = new HashMap<>();

	/**
	 * Has the class been resolved already?
	 */
	private boolean isResolved = false;

	/**
	 * Has the class been scanned already?
	 */
	private boolean isScanned = false;

	/**
	 * Our superclass name
	 */
	private final String superClass;

	/**
	 * Names of implemented interfaces
	 */
	private final List<String> superInterfaces;

	/**
	 * Is this an inner class?
	 */
	private final boolean isInnerClass;

	/**
	 * NameListUp interfaces for super-class/interfaces
	 */
	private final List<NameListUp> nameListUps = new ArrayList<>();

	/**
	 * NameListDown interfaces for derived class/interfaces
	 */
	private final List<NameListDown> nameListDowns = new ArrayList<>();

	/**
	 * Are danger-method warnings suppressed?
	 */
	private boolean isNoWarn = false;

	/**
	 * Danger-method warnings
	 */
	private List<String> warningList = new ArrayList<>();

	public static int nameSpace = 0;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param isInnerClass
	 * @param name
	 * @param superClass
	 * @param superInterfaces
	 * @param access
	 */
	public Cl(final TreeItem parent, final boolean isInnerClass, final String name, final String superClass,
			final List<String> superInterfaces, final int access) {
		super(parent, name);

		if (NameProvider.oldHash) {
			this.mds = new Hashtable<>();
			this.mdsSpecial = new Hashtable<>();
			this.fds = new Hashtable<>();
		}

		this.superClass = superClass;
		this.superInterfaces = superInterfaces;
		this.isInnerClass = isInnerClass;
		this.access = access;
		// Fix: Names are only needed if they do not have a parent.
		if (parent == null && "".equals(name)) {
			throw new RuntimeException("Internal error: class must have parent if name is empty");
		}
		if (parent instanceof Cl) {
			this.sep = ClassFile.SEP_INNER;
		}
	}

	/**
	 * Is this an inner class?
	 */
	public boolean isInnerClass() {
		return this.isInnerClass;
	}

	/**
	 * Suppress warnings.
	 */
	public void setNoWarn() {
		this.isNoWarn = true;
	}

	/**
	 * Add class's warning.
	 * 
	 * @param cf
	 */
	public void setWarnings(final ClassFile cf) {
		this.warningList = cf.listDangerMethods(this.warningList);
	}

	/**
	 * Do we have non-suppressed warnings?
	 */
	public boolean hasWarnings() {
		return !this.isNoWarn && this.warningList.size() > 0;
	}

	/**
	 * Log this class's warnings.
	 * 
	 * @param log
	 */
	public void logWarnings(final PrintWriter log) {
		if (this.hasWarnings()) {
			for (final String warning : this.warningList) {
				log.println("# " + warning);
			}
		}
	}

	/**
	 * Get a method by name.
	 * 
	 * @param name
	 * @param descriptor
	 */
	public Md getMethod(final String name, final String descriptor) {
		return this.mds.get(name + descriptor);
	}

	/**
	 * Get a special method by name.
	 * 
	 * @param name
	 * @param descriptor
	 */
	public Md getMethodSpecial(final String name, final String descriptor) {
		return this.mdsSpecial.get(name + descriptor);
	}

	/**
	 * Get a field by name.
	 * 
	 * @param name
	 */
	public Fd getField(final String name) {
		return this.fds.get(name);
	}

	/**
	 * Get a {@code Collection<Md>} of methods.
	 */
	public Collection<Md> getMethods() {
		return this.mds.values();
	}

	/**
	 * Get a {@code Collection<Md>} of fields.
	 */
	public Collection<Fd> getFields() {
		return this.fds.values();
	}

	/**
	 * Return this Cl's superclass Cl
	 * 
	 * @throws ClassFileException
	 */
	public Cl getSuperCl() throws ClassFileException {
		if (this.superClass != null) {
			return this.classTree.getCl(this.superClass);
		}

		return null;
	}

	/**
	 * Return {@code List<Cl>} of this Cl's super-interfaces
	 * 
	 * @throws ClassFileException
	 */
	public List<Cl> getSuperInterfaces() throws ClassFileException {
		final List<Cl> list = new ArrayList<>();
		for (final String si : this.superInterfaces) {
			final Cl interfaceItem = this.classTree.getCl(si);
			if (interfaceItem != null) {
				list.add(interfaceItem);
			}
		}
		return list;
	}

	/**
	 * Does this internal class have the specified class or interface in its super
	 * or interface chain?
	 * 
	 * @param queryName
	 * @param checkInterfaces
	 */
	protected boolean hasAsSuperInt(final String queryName, final boolean checkInterfaces) {
		try {
			// Special case: is this java/lang/Object?
			if (this.superClass == null) {
				return false;
			}
			// Check our parents
			if (this.superClass.equals(queryName)) {
				return true;
			}
			if (checkInterfaces) {
				for (final String si : this.superInterfaces) {
					if (queryName.equals(si)) {
						return true;
					}
				}
			}
			// Nothing, so recurse up through parents
			final Cl superClInt = this.classTree.getCl(this.superClass);
			if (superClInt != null) {
				if (superClInt.hasAsSuperInt(queryName, checkInterfaces)) {
					return true;
				}
			} else {
				Class<?> superClExt = null;
				try {
					superClExt = Class.forName(ClassFile.translate(this.superClass));
				} catch (final ClassNotFoundException e) {
					// fall thru
				}
				if (superClExt != null) {
					if (Cl.hasAsSuperExt(queryName, checkInterfaces, this.classTree, superClExt)) {
						return true;
					}
				}
			}
			if (checkInterfaces) {
				for (final String si : this.superInterfaces) {
					final Cl interClInt = this.classTree.getCl(si);
					if (interClInt != null) {
						if (interClInt.hasAsSuperInt(queryName, checkInterfaces)) {
							return true;
						}
					} else {
						Class<?> interClExt = null;
						try {
							interClExt = Class.forName(ClassFile.translate(si));
						} catch (final ClassNotFoundException e) {
							// fall thru
						}
						if (interClExt != null) {
							if (Cl.hasAsSuperExt(queryName, checkInterfaces, this.classTree, interClExt)) {
								return true;
							}
						}
					}
				}
			}
		} catch (final ClassFileException e) {
			// fall thru
		}
		return false;
	}

	/**
	 * Does this class have the specified class or interface in its super or
	 * interface chain?
	 * 
	 * @param queryName
	 * @param checkInterfaces
	 * @param classTree
	 * @param clExt
	 */
	protected static boolean hasAsSuperExt(final String queryName, final boolean checkInterfaces,
			final ClassTree classTree, final Class<?> clExt) {
		try {
			// Special case: is this java/lang/Object?
			if (clExt == null || clExt.getName().equals("java.lang.Object")) {
				return false;
			}
			// Check our parents
			final String queryNameExt = ClassFile.translate(queryName);
			final Class<?> superClass = clExt.getSuperclass();
			final List<Class<?>> superInterfaces = Arrays.asList(clExt.getInterfaces());
			if (superClass != null) {
				if (queryNameExt.equals(superClass.getName())) {
					return true;
				}
			}
			if (checkInterfaces) {
				for (final Class<?> si : superInterfaces) {
					if (queryNameExt.equals(si.getName())) {
						return true;
					}
				}
			}
			// Nothing, so recurse up through parents
			if (superClass != null) {
				final Cl superClInt = classTree.getCl(ClassFile.backTranslate(superClass.getName()));
				if (superClInt != null) {
					if (superClInt.hasAsSuperInt(queryName, checkInterfaces)) {
						return true;
					}
				} else {
					final Class<?> superClExt = superClass;
					if (Cl.hasAsSuperExt(queryName, checkInterfaces, classTree, superClExt)) {
						return true;
					}
				}
			}
			if (checkInterfaces) {
				for (final Class<?> si : superInterfaces) {
					final Cl interClInt = classTree.getCl(ClassFile.backTranslate(si.getName()));
					if (interClInt != null) {
						if (interClInt.hasAsSuperInt(queryName, checkInterfaces)) {
							return true;
						}
					} else {
						final Class<?> interClExt = si;
						if (Cl.hasAsSuperExt(queryName, checkInterfaces, classTree, interClExt)) {
							return true;
						}
					}
				}
			}
		} catch (final ClassFileException e) {
			// fall thru
		}
		return false;
	}

	/**
	 * Does this class have the specified class or interface in its super or
	 * interface chain?
	 * 
	 * @param queryName
	 */
	public boolean hasAsSuperOrInterface(final String queryName) {
		return this.hasAsSuperInt(queryName, true);
	}

	/**
	 * Does this class have the specified class in its super chain?
	 * 
	 * @param queryName
	 */
	public boolean hasAsSuper(final String queryName) {
		return this.hasAsSuperInt(queryName, false);
	}

	/**
	 * Add an inner class.
	 */
	@Override
	public Cl addClass(final String name, final String superName, final List<String> interfaceNames, final int access) {
		return this.addClass(true, name, superName, interfaceNames, access);
	}

	/**
	 * Add an inner class, used when copying inner classes from a placeholder.
	 * 
	 * @param cl
	 */
	public Cl addClass(final Cl cl) {
		this.cls.put(cl.getInName(), cl);
		return cl;
	}

	/**
	 * Add a placeholder class.
	 */
	@Override
	public Cl addPlaceholderClass(final String name) {
		return this.addPlaceholderClass(true, name);
	}

	/**
	 * Add a method.
	 * 
	 * @param cf
	 * @param md
	 * @throws ClassFileException
	 */
	public Md addMethod(final ClassFile cf, final MethodInfo md) throws ClassFileException {
		return this.addMethod(md.isSynthetic(), md.getName(), md.getDescriptor(), md.getAccessFlags());
	}

	/**
	 * Add a method.
	 * 
	 * @param isSynthetic
	 * @param name
	 * @param descriptor
	 * @param accessFlags
	 * @throws ClassFileException
	 */
	public Md addMethod(final boolean isSynthetic, final String name, final String descriptor, final int accessFlags)
			throws ClassFileException {
		// Store <init> and <clinit> methods separately - needed only for reference
		// tracking
		Md md;
		if (name.charAt(0) == '<') {
			md = this.getMethodSpecial(name, descriptor);
			if (md == null) {
				md = new Md(this, isSynthetic, name, descriptor, accessFlags);
				this.mdsSpecial.put(name + descriptor, md);
			}
		} else {
			md = this.getMethod(name, descriptor);
			if (md == null) {
				md = new Md(this, isSynthetic, name, descriptor, accessFlags);
				this.mds.put(name + descriptor, md);
			}
		}
		return md;
	}

	/**
	 * Add a field.
	 * 
	 * @param cf
	 * @param fd
	 * @throws ClassFileException
	 */
	public Fd addField(final ClassFile cf, final FieldInfo fd) throws ClassFileException {
		return this.addField(fd.isSynthetic(), fd.getName(), fd.getDescriptor(), fd.getAccessFlags());
	}

	/**
	 * Add a field.
	 * 
	 * @param isSynthetic
	 * @param name
	 * @param descriptor
	 * @param access
	 * @throws ClassFileException
	 */
	public Fd addField(final boolean isSynthetic, final String name, final String descriptor, final int access)
			throws ClassFileException {
		Fd fd = this.getField(name);
		if (fd == null) {
			fd = new Fd(this, isSynthetic, name, descriptor, access);
			this.fds.put(name, fd);
		}
		return fd;
	}

	/**
	 * Prepare for resolve of a class entry by resetting flags.
	 */
	public void resetResolve() {
		this.isScanned = false;
		this.isResolved = false;
		this.nameListDowns.clear();
	}

	/**
	 * Set up reverse list of reserved names prior to resolving classes.
	 * 
	 * @throws ClassFileException
	 */
	public void setupNameListDowns() throws ClassFileException {
		// Special case: we are java/lang/Object
		if (this.superClass == null) {
			return;
		}

		// Add this class as a NameListDown to the super and each interface, if they are
		// in the JAR
		final Cl superClassItem = this.classTree.getCl(this.superClass);
		if (superClassItem != null) {
			superClassItem.nameListDowns.add(this);
		}
		for (final String si : this.superInterfaces) {
			final Cl interfaceItem = this.classTree.getCl(si);
			if (interfaceItem != null) {
				interfaceItem.nameListDowns.add(this);
			}
		}
	}

	/**
	 * Resolve a class entry - set obfuscation permissions based on super class and
	 * interfaces. Overload method and field names maximally.
	 * 
	 * @throws ClassFileException
	 */
	public void resolveOptimally() throws ClassFileException {
		// Already processed, then do nothing
		if (!this.isResolved) {
			// Get lists of method and field names in inheritance namespace
			final List<String> methods = new ArrayList<>();
			final List<String> fields = new ArrayList<>();
			this.scanNameSpaceExcept(null, methods, fields);

			// Resolve a full name space
			this.resolveNameSpaceExcept(null);

			// and move to next
			Cl.nameSpace++;
		}
	}

	/**
	 * Get lists of method and field names in inheritance namespace
	 * 
	 * @param ignoreCl
	 * @param methods
	 * @param fields
	 * @throws ClassFileException
	 */
	private void scanNameSpaceExcept(final Cl ignoreCl, final List<String> methods, final List<String> fields)
			throws ClassFileException {
		// Special case: we are java/lang/Object
		if (this.superClass == null) {
			return;
		}

		// Traverse one step in each direction in name space, scanning
		if (!this.isScanned) {
			// First step up to super classes, scanning them
			final Cl superCl = this.classTree.getCl(this.superClass);
			if (superCl != null) {
				// internal to JAR
				if (superCl != ignoreCl) {
					superCl.scanNameSpaceExcept(this, methods, fields);
				}
			} else {
				// external to JAR
				Cl.scanExtSupers(this.superClass, methods, fields);
			}
			for (final String si : this.superInterfaces) {
				final Cl interfaceItem = this.classTree.getCl(si);
				if (interfaceItem != null && interfaceItem != ignoreCl) {
					interfaceItem.scanNameSpaceExcept(this, methods, fields);
				}
			}

			// Next, scan ourself
			if (!this.isScanned) {
				this.scanThis(methods, fields);

				// Signal class has been scanned
				this.isScanned = true;
			}

			// Finally step down to derived classes, resolving them
			for (final NameListDown nameListDown : this.nameListDowns) {
				final Cl cl = (Cl) nameListDown;
				if (cl != ignoreCl) {
					cl.scanNameSpaceExcept(this, methods, fields);
				}
			}
		}
	}

	/**
	 * Get lists of method and field names in inheritance namespace
	 * 
	 * @param name
	 * @param methods
	 * @param fields
	 */
	private static void scanExtSupers(final String name, final List<String> methods, final List<String> fields) {
		Class<?> extClass = null;
		try {
			extClass = Class.forName(ClassFile.translate(name));
		} catch (final ClassNotFoundException e) {
			return;
		}

		// Get public methods and fields from supers and interfaces up the tree
		final List<Method> allPubMethods = Arrays.asList(extClass.getMethods());
		for (final Method md : allPubMethods) {
			final String methodName = md.getName();
			if (!methods.contains(methodName)) {
				methods.add(methodName);
			}
		}
		final List<Field> allPubFields = Arrays.asList(extClass.getFields());
		for (final Field fd : allPubFields) {
			final String fieldName = fd.getName();
			if (!fields.contains(fieldName)) {
				fields.add(fieldName);
			}
		}
		// Go up the super hierarchy, adding all non-public methods/fields
		while (extClass != null) {
			final List<Method> allClassMethods = Arrays.asList(extClass.getDeclaredMethods());
			for (final Method md : allClassMethods) {
				if (!Modifier.isPublic(md.getModifiers())) {
					final String methodName = md.getName();
					if (!methods.contains(methodName)) {
						methods.add(methodName);
					}
				}
			}
			final List<Field> allClassFields = Arrays.asList(extClass.getDeclaredFields());
			for (final Field fd : allClassFields) {
				if (!Modifier.isPublic(fd.getModifiers())) {
					final String fieldName = fd.getName();
					if (!fields.contains(fieldName)) {
						fields.add(fieldName);
					}
				}
			}
			extClass = extClass.getSuperclass();
		}
	}

	/**
	 * Add method and field names from this class to the lists
	 * 
	 * @param methods
	 * @param fields
	 */
	private void scanThis(final List<String> methods, final List<String> fields) {
		for (final Md md : this.mds.values()) {
			if (md.isFixed()) {
				final String name = md.getOutName();
				if (!methods.contains(name)) {
					methods.add(name);
				}
			}
		}
		for (final Fd fd : this.fds.values()) {
			if (fd.isFixed()) {
				final String name = fd.getOutName();
				if (!fields.contains(name)) {
					fields.add(name);
				}
			}
		}
	}

	/**
	 * Resolve an entire inheritance name space optimally.
	 * 
	 * @param ignoreCl
	 * @throws ClassFileException
	 */
	private void resolveNameSpaceExcept(final Cl ignoreCl) throws ClassFileException {
		// Special case: we are java/lang/Object
		if (this.superClass == null) {
			return;
		}

		// Traverse one step in each direction in name space, resolving
		if (!this.isResolved) {
			// First step up to super classes, resolving them, since we depend on them
			final Cl superCl = this.classTree.getCl(this.superClass);
			if (superCl != null && superCl != ignoreCl) {
				superCl.resolveNameSpaceExcept(this);
			}
			for (final String si : this.superInterfaces) {
				final Cl interfaceItem = this.classTree.getCl(si);
				if (interfaceItem != null && interfaceItem != ignoreCl) {
					interfaceItem.resolveNameSpaceExcept(this);
				}
			}

			// Next, resolve ourself
			if (!this.isResolved) {
				this.resolveThis();

				// Signal class has been processed
				this.isResolved = true;
			}

			// Finally step down to derived classes, resolving them
			for (final NameListDown nameListDown : this.nameListDowns) {
				final Cl cl = (Cl) nameListDown;
				if (cl != ignoreCl) {
					cl.resolveNameSpaceExcept(this);
				}
			}
		}
	}

	/**
	 * For each super interface and the super class, if it is outside DB, use
	 * reflection to merge its list of public/protected methods/fields -- while for
	 * those in the DB, resolve to get the name-mapping lists
	 * 
	 * @throws ClassFileException
	 */
	private void resolveThis() throws ClassFileException {
		// Special case: we are java/lang/Object
		if (this.superClass == null) {
			return;
		}

		final Cl superClassItem = this.classTree.getCl(this.superClass);
		if (superClassItem != null) {
			this.nameListUps.add(superClassItem);
		} else {
			this.nameListUps.add(this.getExtNameListUp(this.superClass));
		}
		for (final String si : this.superInterfaces) {
			final Cl interfaceItem = this.classTree.getCl(si);
			if (interfaceItem != null) {
				this.nameListUps.add(interfaceItem);
			} else {
				this.nameListUps.add(this.getExtNameListUp(si));
			}
		}

		// Run through each method/field in this class checking for reservations and
		// obfuscating accordingly
		nextMethod: for (final Md md : this.mds.values()) {
			final String theInName = md.getInName();
			final String theInDesc = md.getDescriptor();
			final String fullInName = md.getFullInName();
			if (!md.isFixed()) {
				// if we are a private or static or final method then dont check our children
				// for a name
				boolean checkDown = true;
				if (NameProvider.fixShadowed) {
					if (Modifier.isPrivate(md.access) || Modifier.isStatic(md.access) || Modifier.isFinal(md.access)) {
						checkDown = false;
					}
				} else {
					if (Modifier.isPrivate(md.access)) {
						checkDown = false;
					}
				}

				if (checkDown) {
					// Check for name reservation via derived classes
					for (final NameListDown nl : this.nameListDowns) {
						final String theOutName = nl.getMethodObfNameDown(this, theInName, theInDesc);
						if (theOutName != null) {
							md.setOutName(theOutName);
							if (theOutName.equals(theInName)) {
								NameProvider.verboseLog("# Method " + fullInName + " unchanged from derived class");
							} else {
								NameProvider.verboseLog(
										"# Method " + fullInName + " renamed to " + theOutName + " from derived class");
							}
							if (NameProvider.fullMap) {
								md.setOutput();
							}
							continue nextMethod;
						}
					}
				}
				// if we are a private or static method then dont check our parents for a name
				boolean checkUp = true;
				if (NameProvider.fixShadowed) {
					if (Modifier.isPrivate(md.access) || Modifier.isStatic(md.access)) {
						checkUp = false;
					}
				} else {
					if (Modifier.isPrivate(md.access)) {
						checkUp = false;
					}
				}

				if (checkUp) {
					// Check for name reservation via super classes
					for (final NameListUp nl : this.nameListUps) {
						final String theOutName = nl.getMethodOutNameUp(theInName, theInDesc);
						if (theOutName != null) {
							md.setOutName(theOutName);
							md.setIsOverride();
							if (theOutName.equals(theInName)) {
								NameProvider.verboseLog("# Method " + fullInName + " unchanged from super class");
							} else {
								NameProvider.verboseLog(
										"# Method " + fullInName + " renamed to " + theOutName + " from super class");
							}
							if (NameProvider.fullMap) {
								md.setOutput();
							}
							continue nextMethod;
						}
					}
				}
				// If no other restrictions, obfuscate it
				final String theOutName = NameProvider.getNewMethodName(md);
				if (theOutName != null) {
					md.setOutName(theOutName);
					md.setFromScriptMap();
					if (theOutName.equals(theInName)) {
						NameProvider.verboseLog("# Method " + fullInName + " unchanged from name maker");
					} else {
						NameProvider.verboseLog(
								"# Method " + fullInName + " renamed to " + theOutName + " from name maker");
					}
				} else {
					NameProvider.verboseLog("# Method " + fullInName + " null from name maker");
				}
			} else {
				if (md.isFromScriptMap()) {
					final String theOutName = md.getOutName();
					if (theOutName.equals(theInName)) {
						NameProvider.verboseLog("# Method " + fullInName + " unchanged from ScriptMap");
					} else {
						NameProvider
								.verboseLog("# Method " + fullInName + " renamed to " + theOutName + " from ScriptMap");
					}
				} else if (md.isFromScript()) {
					NameProvider.verboseLog("# Method " + fullInName + " fixed from Script");
				} else {
					NameProvider.verboseLog("# Method " + fullInName + " fixed");
				}
			}
		}
		nextField: for (final Fd fd : this.fds.values()) {
			final String theInName = fd.getInName();
			final String fullInName = fd.getFullInName();
			if (!fd.isFixed()) {
				// if we are a private or static or final field then dont check our children for
				// a name
				boolean checkDown = true;
				if (NameProvider.fixShadowed) {
					if (Modifier.isPrivate(fd.access) || Modifier.isStatic(fd.access) || Modifier.isFinal(fd.access)) {
						checkDown = false;
					}
				} else {
					if (Modifier.isPrivate(fd.access)) {
						checkDown = false;
					}
				}
				if (checkDown) {
					// Check for name reservation via derived classes
					for (final NameListDown nl : this.nameListDowns) {
						final String theOutName = nl.getFieldObfNameDown(this, theInName);
						if (theOutName != null) {
							fd.setOutName(theOutName);
							if (theOutName.equals(theInName)) {
								NameProvider.verboseLog("# Field " + fullInName + " unchanged from derived class");
							} else {
								NameProvider.verboseLog(
										"# Field " + fullInName + " renamed to " + theOutName + " from derived class");
							}
							if (NameProvider.fullMap) {
								fd.setOutput();
							}
							continue nextField;
						}
					}
				}

				// if we are a private or static field then dont check our parents for a name
				final boolean checkUp = true;
				if (NameProvider.fixShadowed) {
					if (Modifier.isPrivate(fd.access) || Modifier.isStatic(fd.access) || Modifier.isFinal(fd.access)) {
						checkDown = false;
					}
				} else {
					if (Modifier.isPrivate(fd.access)) {
						checkDown = false;
					}
				}
				if (checkUp) {
					// Check for name reservation via super classes
					for (final NameListUp nl : this.nameListUps) {
						final String theOutName = nl.getFieldOutNameUp(theInName);
						if (theOutName != null) {
							fd.setOutName(theOutName);
							fd.setIsOverride();
							if (theOutName.equals(theInName)) {
								NameProvider.verboseLog("# Field " + fullInName + " unchanged from super class");
							} else {
								NameProvider.verboseLog(
										"# Field " + fullInName + " renamed to " + theOutName + " from super class");
							}
							if (NameProvider.fullMap) {
								fd.setOutput();
							}
							continue nextField;
						}
					}
				}
				// If no other restrictions, obfuscate it
				final String theOutName = NameProvider.getNewFieldName(fd);
				if (theOutName != null) {
					fd.setOutName(theOutName);
					fd.setFromScriptMap();
					if (theOutName.equals(theInName)) {
						NameProvider.verboseLog("# Field " + fullInName + " unchanged from name maker");
					} else {
						NameProvider
								.verboseLog("# Field " + fullInName + " renamed to " + theOutName + " from name maker");
					}
				} else {
					NameProvider.verboseLog("# Field " + fullInName + " null from name maker");
				}
			} else {
				if (fd.isFromScriptMap()) {
					final String theOutName = fd.getOutName();
					if (theOutName.equals(theInName)) {
						NameProvider.verboseLog("# Field " + fullInName + " unchanged from ScriptMap");
					} else {
						NameProvider
								.verboseLog("# Field " + fullInName + " renamed to " + theOutName + " from ScriptMap");
					}
				} else if (fd.isFromScript()) {
					NameProvider.verboseLog("# Field " + fullInName + " fixed from Script");
				} else {
					NameProvider.verboseLog("# Field " + fullInName + " fixed");
				}
			}
		}
	}

	/**
	 * Get output method name from list, or null if no mapping exists.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getMethodOutNameUp(final String name, final String descriptor) throws ClassFileException {
		// Check supers
		for (final NameListUp nl : this.nameListUps) {
			final String superOutName = nl.getMethodOutNameUp(name, descriptor);
			if (superOutName != null) {
				return superOutName;
			}
		}

		// Check self
		final Md md = this.getMethod(name, descriptor);
		if (md != null) {
			if (NameProvider.fixShadowed) {
				if (!Modifier.isPrivate(md.access) && !Modifier.isStatic(md.access) && !Modifier.isFinal(md.access)) {
					return md.getOutName();
				}
			} else {
				if (!Modifier.isPrivate(md.access)) {
					return md.getOutName();
				}
			}
		}

		return null;
	}

	/**
	 * Get obfuscated method name from list, or null if no mapping exists.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getMethodObfNameUp(final String name, final String descriptor) throws ClassFileException {
		// Check supers
		for (final NameListUp nl : this.nameListUps) {
			final String superObfName = nl.getMethodObfNameUp(name, descriptor);
			if (superObfName != null) {
				return superObfName;
			}
		}

		// Check self
		final Md md = this.getMethod(name, descriptor);
		if (md != null) {
			if (NameProvider.fixShadowed) {
				if (!Modifier.isPrivate(md.access) && !Modifier.isStatic(md.access) && !Modifier.isFinal(md.access)) {
					return md.getObfName();
				}
			} else {
				if (!Modifier.isPrivate(md.access)) {
					return md.getObfName();
				}
			}
		}

		return null;
	}

	/**
	 * Get output field name from list, or null if no mapping exists.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getFieldOutNameUp(final String name) throws ClassFileException {
		// Check supers
		for (final NameListUp nl : this.nameListUps) {
			final String superOutName = nl.getFieldOutNameUp(name);
			if (superOutName != null) {
				return superOutName;
			}
		}

		// Check self
		final Fd fd = this.getField(name);
		if (fd != null) {
			if (NameProvider.fixShadowed) {
				if (!Modifier.isPrivate(fd.access) && !Modifier.isStatic(fd.access) && !Modifier.isFinal(fd.access)) {
					return fd.getOutName();
				}
			} else {
				if (!Modifier.isPrivate(fd.access)) {
					return fd.getOutName();
				}
			}
		}

		return null;
	}

	/**
	 * Get obfuscated field name from list, or null if no mapping exists.
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getFieldObfNameUp(final String name) throws ClassFileException {
		// Check supers
		for (final NameListUp nl : this.nameListUps) {
			final String superObfName = nl.getFieldObfNameUp(name);
			if (superObfName != null) {
				return superObfName;
			}
		}

		// Check self
		final Fd fd = this.getField(name);
		if (fd != null) {
			if (NameProvider.fixShadowed) {
				if (!Modifier.isPrivate(fd.access) && !Modifier.isStatic(fd.access) && !Modifier.isFinal(fd.access)) {
					return fd.getObfName();
				}
			} else {
				if (!Modifier.isPrivate(fd.access)) {
					return fd.getObfName();
				}
			}
		}

		return null;
	}

	/**
	 * Is the method reserved because of its reservation down the class hierarchy?
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getMethodObfNameDown(final Cl caller, final String name, final String descriptor)
			throws ClassFileException {
		// Check ourself for an explicit 'do not obfuscate'
		final Md md = this.getMethod(name, descriptor);
		if (md != null && md.isFixed()) {
			return md.getOutName();
		}

		// Check our supers, except for our caller (special case if we are
		// java/lang/Object)
		String theObfName = null;
		if (this.superClass != null) {
			final Cl superClassItem = this.classTree.getCl(this.superClass);
			if (superClassItem != caller) {
				NameListUp nl;
				if (superClassItem != null) {
					nl = superClassItem;
				} else {
					nl = this.getExtNameListUp(this.superClass);
				}
				theObfName = nl.getMethodObfNameUp(name, descriptor);
				if (theObfName != null) {
					return theObfName;
				}
			}
			for (final String si : this.superInterfaces) {
				final Cl interfaceItem = this.classTree.getCl(si);
				if (interfaceItem != caller) {
					NameListUp nl;
					if (interfaceItem != null) {
						nl = interfaceItem;
					} else {
						nl = this.getExtNameListUp(si);
					}
					theObfName = nl.getMethodObfNameUp(name, descriptor);
					if (theObfName != null) {
						return theObfName;
					}
				}
			}
		}

		// Check our derived classes
		for (final NameListDown nl : this.nameListDowns) {
			theObfName = nl.getMethodObfNameDown(this, name, descriptor);
			if (theObfName != null) {
				return theObfName;
			}
		}

		// No reservation found
		return null;
	}

	/**
	 * Is the field reserved because of its reservation down the class hierarchy?
	 * 
	 * @throws ClassFileException
	 */
	@Override
	public String getFieldObfNameDown(final Cl caller, final String name) throws ClassFileException {
		// Check ourself for an explicit 'do not obfuscate'
		final Fd fd = this.getField(name);
		if (fd != null && fd.isFixed()) {
			return fd.getOutName();
		}

		// Check our supers, except for our caller (special case if we are
		// java/lang/Object)
		String theObfName = null;
		if (this.superClass != null) {
			final Cl superClassItem = this.classTree.getCl(this.superClass);
			if (superClassItem != caller) {
				NameListUp nl;
				if (superClassItem != null) {
					nl = superClassItem;
				} else {
					nl = this.getExtNameListUp(this.superClass);
				}
				theObfName = nl.getFieldObfNameUp(name);
				if (theObfName != null) {
					return theObfName;
				}
			}
			for (final String si : this.superInterfaces) {
				final Cl interfaceItem = this.classTree.getCl(si);
				if (interfaceItem != caller) {
					NameListUp nl;
					if (interfaceItem != null) {
						nl = interfaceItem;
					} else {
						nl = this.getExtNameListUp(si);
					}
					theObfName = nl.getFieldObfNameUp(name);
					if (theObfName != null) {
						return theObfName;
					}
				}
			}
		}

		// Check our derived classes
		for (final NameListDown nl : this.nameListDowns) {
			theObfName = nl.getFieldObfNameDown(this, name);
			if (theObfName != null) {
				return theObfName;
			}
		}

		// No reservation found
		return null;
	}

	private static Map<String, NameListUp> extNameListUpCache = new HashMap<>();

	/**
	 * Construct, or retrieve from cache, the NameListUp object for an external
	 * class/interface
	 * 
	 * @param name
	 * @throws ClassFileException
	 */
	private NameListUp getExtNameListUp(final String name) throws ClassFileException {
		NameListUp nl = Cl.extNameListUpCache.get(name);
		if (nl == null) {
			nl = new ExtNameListUp(name);
			Cl.extNameListUpCache.put(name, nl);
		}
		return nl;
	}

	/**
	 * NameListUp for class/interface not in the database.
	 */
	class ExtNameListUp implements NameListUp {
		// Class's fully qualified name
		private Class<?> extClass;
		private List<Method> methods = null;

		/**
		 * Constructor
		 * 
		 * @param name
		 * @throws ClassFileException
		 */
		public ExtNameListUp(final String name) throws ClassFileException {
			try {
				this.extClass = Class.forName(ClassFile.translate(name));
			} catch (final ClassNotFoundException e) {
				throw new ClassFileException("ClassNotFound " + name);
			}
		}

		/**
		 * Get obfuscated method name from list, or null if no mapping exists.
		 * 
		 * @throws ClassFileException
		 */
		@Override
		public String getMethodObfNameUp(final String name, final String descriptor) throws ClassFileException {
			return this.getMethodOutNameUp(name, descriptor);
		}

		/**
		 * Get obfuscated method name from list, or null if no mapping exists.
		 * 
		 * @throws ClassFileException
		 */
		@Override
		public String getMethodOutNameUp(final String name, final String descriptor) throws ClassFileException {
			// Get list of public/protected methods
			if (this.methods == null) {
				final List<Method> allMethods = this.getAllDeclaredMethods(this.extClass);
				this.methods = new ArrayList<>();
				for (final Method md : allMethods) {
					final int modifiers = md.getModifiers();
					if (NameProvider.fixShadowed) {
						if (!Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)
								&& !Modifier.isFinal(modifiers)) {
							this.methods.add(md);
						}

					} else {
						if (!Modifier.isPrivate(modifiers)) {
							this.methods.add(md);
						}
					}
				}
			}

			// Check each public/protected class method against the named one
			nextMethod: for (final Method md : this.methods) {
				if (name.equals(md.getName())) {
					final List<String> paramAndReturnNames = ClassFile.parseMethodDescriptor(descriptor);
					final List<String> paramNames = paramAndReturnNames.subList(0, paramAndReturnNames.size() - 1);
					final String returnName = paramAndReturnNames.get(paramAndReturnNames.size() - 1);
					final List<Class<?>> paramTypes = Arrays.asList(md.getParameterTypes());
					final Class<?> returnType = md.getReturnType();
					if (paramNames.size() == paramTypes.size()) {
						for (int j = 0; j < paramNames.size(); j++) {
							if (!paramNames.get(j).equals(paramTypes.get(j).getName())) {
								continue nextMethod;
							}
						}
						if (!returnName.equals(returnType.getName())) {
							continue nextMethod;
						}

						// We have a match, and so the derived class method name must be made to match
						return name;
					}
				}
			}

			// Method is not present
			return null;
		}

		/**
		 * Get obfuscated field name from list, or null if no mapping exists.
		 */
		@Override
		public String getFieldObfNameUp(final String name) {
			return this.getFieldOutNameUp(name);
		}

		/**
		 * Get obfuscated field name from list, or null if no mapping exists.
		 */
		@Override
		public String getFieldOutNameUp(final String name) {
			// Use reflection to check class for field
			final Field field = this.getAllDeclaredField(this.extClass, name);
			if (field != null) {
				// Field must be public or protected
				final int modifiers = field.getModifiers();
				if (NameProvider.fixShadowed) {
					if (!Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)
							&& !Modifier.isFinal(modifiers)) {
						return name;
					}
				} else {
					if (!Modifier.isPrivate(modifiers)) {
						return name;
					}
				}
			}

			// Field is not present
			return null;
		}

		/**
		 * Get all methods (from supers too) regardless of access level
		 * 
		 * @param theClass
		 */
		private List<Method> getAllDeclaredMethods(Class<?> theClass) {
			final List<Method> ma = new ArrayList<>();

			// Get the public methods from all supers and interfaces up the tree
			ma.addAll(Arrays.asList(theClass.getMethods()));

			// Go up the super hierarchy, getting arrays of all methods (some redundancy
			// here, but that's okay)
			while (theClass != null) {
				ma.addAll(Arrays.asList(theClass.getDeclaredMethods()));
				theClass = theClass.getSuperclass();
			}

			return ma;
		}

		/**
		 * Get a specified field (from supers and interfaces too) regardless of access
		 * level
		 * 
		 * @param theClass
		 * @param name
		 */
		private Field getAllDeclaredField(Class<?> theClass, final String name) {
			final Class<?> origClass = theClass;

			// Check for field in supers
			while (theClass != null) {
				Field field = null;
				try {
					field = theClass.getDeclaredField(name);
				} catch (final NoSuchFieldException e) {
					// fall thru
				}
				if (field != null) {
					return field;
				}
				theClass = theClass.getSuperclass();
			}

			// Check for public field in supers and interfaces (some redundancy here, but
			// that's okay)
			try {
				return origClass.getField(name);
			} catch (final NoSuchFieldException e) {
				return null;
			}
		}
	}

	/**
	 * Construct and return the full obfuscated name of the entry.
	 */
	@Override
	public String getFullOutName() {
		final String repackageName = this.getRepackageName();

		if (repackageName != null) {
			return repackageName;
		}

		return super.getFullOutName();
	}

	public Iterator<Cl> getDownClasses() {
		final List<Cl> clsList = new ArrayList<>();
		for (final NameListDown nameListDown : this.nameListDowns) {
			final Cl cl = (Cl) nameListDown;
			clsList.add(cl);
		}
		return clsList.iterator();
	}
}
