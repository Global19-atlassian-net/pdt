/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.astview.views;

import org.eclipse.php.core.ast.nodes.AST;
import org.eclipse.php.core.ast.nodes.Program;
import org.eclipse.swt.graphics.Image;

public class WellKnownTypesProperty extends ASTAttribute {

	public static final String[] WELL_KNOWN_TYPES = { "boolean", "byte", "char", "double", "float", "int", "long",
			"short", "void", "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Class",
			"java.lang.Cloneable", "java.lang.Double", "java.lang.Error", "java.lang.Exception", "java.lang.Float",
			"java.lang.Integer", "java.lang.Long", "java.lang.Object", "java.lang.RuntimeException", "java.lang.Short",
			"java.lang.String", "java.lang.StringBuilder", "java.lang.Throwable", "java.lang.Void",
			"java.io.Serializable",

			"_.$UnknownType$" };

	private final Program fRoot;

	public WellKnownTypesProperty(Program root) {
		fRoot = root;
	}

	@Override
	public Object getParent() {
		return fRoot;
	}

	@Override
	public Object[] getChildren() {
		AST ast = fRoot.getAST();

		Binding[] res = new Binding[WELL_KNOWN_TYPES.length];
		for (int i = 0; i < WELL_KNOWN_TYPES.length; i++) {
			String type = WELL_KNOWN_TYPES[i];
			res[i] = new Binding(this, type, ast.resolveWellKnownType(type), true);
		}
		return res;
	}

	@Override
	public String getLabel() {
		return "> RESOLVE_WELL_KNOWN_TYPES"; //$NON-NLS-1$
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return 57;
	}
}
