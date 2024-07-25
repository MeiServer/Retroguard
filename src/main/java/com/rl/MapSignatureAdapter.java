/**
 *
 */
package com.rl;

import com.rl.obf.classfile.ClassFileException;
import com.rl.obf.classfile.NameMapper;

import de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureException;
import de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor;

public class MapSignatureAdapter extends SignatureVisitor {
	private final SignatureVisitor sv;
	private String currentClassName;
	private final NameMapper nm;

	/**
	 * Constructor
	 */
	public MapSignatureAdapter(final SignatureVisitor sv, final NameMapper nm) {
		super(0);
		this.sv = sv;
		this.nm = nm;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitFormalTypeParameter(java.lang.String)
	 */
	@Override
	public void visitFormalTypeParameter(final String name) throws SignatureException {
		this.sv.visitFormalTypeParameter(name);
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitClassBound()
	 */
	@Override
	public SignatureVisitor visitClassBound() throws SignatureException {
		this.sv.visitClassBound();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitInterfaceBound()
	 */
	@Override
	public SignatureVisitor visitInterfaceBound() throws SignatureException {
		this.sv.visitInterfaceBound();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitSuperclass()
	 */
	@Override
	public SignatureVisitor visitSuperclass() throws SignatureException {
		this.sv.visitSuperclass();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitInterface()
	 */
	@Override
	public SignatureVisitor visitInterface() throws SignatureException {
		this.sv.visitInterface();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitParameterType()
	 */
	@Override
	public SignatureVisitor visitParameterType() throws SignatureException {
		this.sv.visitParameterType();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitReturnType()
	 */
	@Override
	public SignatureVisitor visitReturnType() throws SignatureException {
		this.sv.visitReturnType();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitExceptionType()
	 */
	@Override
	public SignatureVisitor visitExceptionType() throws SignatureException {
		this.sv.visitExceptionType();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitBaseType(char)
	 */
	@Override
	public void visitBaseType(final char descriptor) throws SignatureException {
		this.sv.visitBaseType(descriptor);
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitTypeVariable(java.lang.String)
	 */
	@Override
	public void visitTypeVariable(final String name) throws SignatureException {
		this.sv.visitTypeVariable(name);
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitArrayType()
	 */
	@Override
	public SignatureVisitor visitArrayType() throws SignatureException {
		this.sv.visitArrayType();
		return this;
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitClassType(java.lang.String)
	 */
	@Override
	public void visitClassType(final String name) throws SignatureException {
		this.currentClassName = name;
		String newName = null;
		try {
			newName = this.nm.mapClass(name);
		} catch (final ClassFileException e) {
			throw new SignatureException(e);
		}
		this.sv.visitClassType(newName);
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitInnerClassType(java.lang.String)
	 */
	@Override
	public void visitInnerClassType(final String name) throws SignatureException {
		this.currentClassName += "$" + name; // Signatures use . for inner classes, but we map them using $.
		String newName = null;
		try {
			newName = this.nm.mapClass(this.currentClassName);
			if (newName.indexOf('$') == -1) {
				throw new SignatureException("Remaped inner class does not fit java inner class naming format: "
						+ this.currentClassName + " -> " + newName);
			}
			newName = newName.substring(newName.lastIndexOf('$') + 1);
		} catch (final ClassFileException e) {
			throw new SignatureException(e);
		}
		this.sv.visitInnerClassType(newName);
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitTypeArgument()
	 */
	@Override
	public void visitTypeArgument() throws SignatureException {
		this.sv.visitTypeArgument();
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitTypeArgument(char)
	 */
	@Override
	public SignatureVisitor visitTypeArgument(final char wildcard) throws SignatureException {
		this.sv.visitTypeArgument(wildcard);
		return new MapSignatureAdapter(this.sv, this.nm); // We need to return a new instance so that our current class
															// doesn't get tainted by nested types.
	}

	/**
	 * @see de.oceanlabs.mcp.retroguard.shadow.asm.signature.SignatureVisitor#visitEnd()
	 */
	@Override
	public void visitEnd() throws SignatureException {
		this.sv.visitEnd();
	}
}
