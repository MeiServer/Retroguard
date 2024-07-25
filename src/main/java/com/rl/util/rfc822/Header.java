/* ===========================================================================
 * $RCSfile: Header.java,v $
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

package com.rl.util.rfc822;

import java.io.IOException;
import java.io.Writer;

/**
 * An RFC822 'header' is a 'tag' / 'value' pair.
 *
 * @author Mark Welsh
 */
public class Header {
	// Constants -------------------------------------------------------------
	/**
	 * Maximum length of header line in a section, before break to next line
	 */
	private static final int MAX_HEADER_LINE_LENGTH = 70;

	// Fields ----------------------------------------------------------------
	private final String tag;
	private final String value;

	// Class Methods ---------------------------------------------------------
	/**
	 * Parse a header from the specified String.
	 * 
	 * @param line
	 */
	public static Header parse(final String line) {
		Header header = null;
		if (line != null) {
			final int pos = line.indexOf(':');
			if (pos != -1) {
				header = new Header(line.substring(0, pos).trim(), line.substring(pos + 1).trim());
			}
		}
		return header;
	}

	// Instance Methods ------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param tag
	 * @param value
	 */
	public Header(final String tag, final String value) {
		this.tag = tag == null ? "" : tag;
		this.value = value == null ? "" : value;
	}

	/**
	 * Return the tag.
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Return the value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Test equality of headers.
	 */
	@Override
	public boolean equals(final Object o) {
		if (o instanceof Header) {
			final Header header = (Header) o;
			if (header.getTag().equals(this.getTag()) && header.getValue().equals(this.getValue())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.tag == null ? 0 : this.tag.hashCode());
		result = prime * result + (this.value == null ? 0 : this.value.hashCode());
		return result;
	}

	/**
	 * Print String rep of this object to a java.io.Writer.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeString(final Writer writer) throws IOException {
		String prefix = this.getTag() + ": ";
		final String value = this.getValue();
		for (int index = 0; index < value.length(); prefix = " ") // continuation lines are prefixed with single space
		{
			final int start = index;
			// Compute length of value that can be appended to this line
			index += Math.min(value.length() - index, Header.MAX_HEADER_LINE_LENGTH - prefix.length());
			// Write tag or continuation space, (part of) value, EOL
			writer.write(prefix + value.substring(start, index) + "\015\012");
		}
	}

	/**
	 * Return String rep of this object.
	 */
	@Override
	public String toString() {
		return this.getTag() + ": " + this.getValue();
	}
}
