/* ===========================================================================
 * $RCSfile: ConstantPool.java,v $
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A representation of the data in a Java class-file's Constant Pool. Constant
 * Pool entries are managed by reference counting.
 *
 * @author Mark Welsh
 */
public class ConstantPool implements Iterable<CpInfo> {
	// Constants -------------------------------------------------------------

	// Fields ----------------------------------------------------------------
	private final ClassFile myClassFile;
	private final List<CpInfo> pool;

	// Class Methods ---------------------------------------------------------

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor, which initializes Constant Pool using an array of CpInfo.
	 * 
	 * @param classFile
	 * @param cpInfo
	 */
	public ConstantPool(final ClassFile classFile, final List<CpInfo> cpInfo) {
		this.myClassFile = classFile;
		this.pool = new ArrayList<>(cpInfo);
	}

	/**
	 * Return an Iterator of all Constant Pool entries.
	 */
	@Override
	public Iterator<CpInfo> iterator() {
		return this.pool.iterator();
	}

	/**
	 * Return the Constant Pool length.
	 */
	public int length() {
		return this.pool.size();
	}

	/**
	 * Return the specified Constant Pool entry.
	 * 
	 * @param i
	 * @throws ClassFileException
	 */
	public CpInfo getCpEntry(final int i) throws ClassFileException {
		try {
			return this.pool.get(i);
		} catch (final IndexOutOfBoundsException e) {
			throw new ClassFileException("Constant Pool index out of range.");
		}
	}

	/**
	 * Set the reference count for each element, using references from the owning
	 * ClassFile.
	 * 
	 * @throws ClassFileException
	 */
	public void updateRefCount() throws ClassFileException {
		// Reset all reference counts to zero
		this.walkPool(new PoolAction() {
			@Override
			public void defaultAction(final CpInfo cpInfo) {
				cpInfo.resetRefCount();
			}
		});

		// Count the direct references to Utf8 entries
		this.myClassFile.markUtf8Refs();

		// Count the direct references to NameAndType entries
		this.myClassFile.markNTRefs();

		// Go through pool, clearing the Utf8 entries which have no references
		this.walkPool(new PoolAction() {
			@Override
			public void utf8Action(final Utf8CpInfo cpInfo) {
				if (cpInfo.getRefCount() == 0) {
					cpInfo.clearString();
				}
			}
		});
	}

	/**
	 * Increment the reference count for the specified element.
	 * 
	 * @param i
	 * @throws ClassFileException
	 */
	public void incRefCount(final int i) throws ClassFileException {
		final CpInfo cpInfo = this.getCpEntry(i);
		if (cpInfo == null) {
			// This can happen for JDK1.2 code so remove - 981123
			// throw new ClassFileException("Illegal access to a Constant Pool element.");
			return;
		}

		cpInfo.incRefCount();
	}

	/**
	 * Remap a specified Utf8 entry to the given value and return its new index.
	 * 
	 * @param newString
	 * @param oldIndex
	 * @throws ClassFileException
	 */
	public int remapUtf8To(final String newString, final int oldIndex) throws ClassFileException {
		this.decRefCount(oldIndex);
		return this.addUtf8Entry(newString);
	}

	/**
	 * Decrement the reference count for the specified element, blanking if Utf and
	 * refs are zero.
	 * 
	 * @param i
	 * @throws ClassFileException
	 */
	public void decRefCount(final int i) throws ClassFileException {
		final CpInfo cpInfo = this.getCpEntry(i);
		if (cpInfo == null) {
			// This can happen for JDK1.2 code so remove - 981123
			// throw new ClassFileException("Illegal access to a Constant Pool element.");
			return;
		}

		cpInfo.decRefCount();
	}

	/**
	 * Add an entry to the constant pool and return its index.
	 * 
	 * @param entry
	 */
	public int addEntry(final CpInfo entry) {
		// Add new entry to end of pool
		final int index = this.pool.size();
		this.pool.add(entry);
		return index;
	}

	/**
	 * Add a string to the constant pool and return its index.
	 * 
	 * @param s
	 */
	protected int addUtf8Entry(final String s) {
		// Search pool for the string. If found, just increment the reference count and
		// return the index
		for (final ListIterator<CpInfo> iter = this.pool.listIterator(); iter.hasNext();) {
			final int i = iter.nextIndex();
			final CpInfo cpInfo = iter.next();
			if (cpInfo instanceof Utf8CpInfo) {
				final Utf8CpInfo entry = (Utf8CpInfo) cpInfo;
				if (entry.getString().equals(s)) {
					entry.incRefCount();
					return i;
				}
			}
		}

		// No luck, so try to overwrite an old, blanked entry
		for (final ListIterator<CpInfo> iter = this.pool.listIterator(); iter.hasNext();) {
			final int i = iter.nextIndex();
			final CpInfo cpInfo = iter.next();
			if (cpInfo instanceof Utf8CpInfo) {
				final Utf8CpInfo entry = (Utf8CpInfo) cpInfo;
				if (entry.getRefCount() == 0) {
					entry.setString(s);
					entry.incRefCount();
					return i;
				}
			}
		}

		// Still no luck, so append a fresh Utf8CpInfo entry to the pool
		return this.addEntry(new Utf8CpInfo(s));
	}

	/**
	 * Data walker
	 */
	class PoolAction {
		/**
		 * @param cpInfo
		 */
		public void utf8Action(final Utf8CpInfo cpInfo) {
			this.defaultAction(cpInfo);
		}

		/**
		 * @param cpInfo
		 */
		public void defaultAction(final CpInfo cpInfo) {
			// do nothing
		}
	}

	/**
	 * @param pa
	 */
	private void walkPool(final PoolAction pa) {
		for (final CpInfo cpInfo : this.pool) {
			if (cpInfo instanceof Utf8CpInfo) {
				pa.utf8Action((Utf8CpInfo) cpInfo);
			} else if (cpInfo != null) {
				pa.defaultAction(cpInfo);
			}
		}
	}
}
