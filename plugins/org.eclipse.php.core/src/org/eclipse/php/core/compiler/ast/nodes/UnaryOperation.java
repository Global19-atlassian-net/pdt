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
package org.eclipse.php.core.compiler.ast.nodes;

import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.utils.CorePrinter;
import org.eclipse.php.internal.core.compiler.ast.visitor.ASTPrintVisitor;

/**
 * Represents an unary operation expression
 * 
 * <pre>
 * e.g.
 * 
 * +$a, -3, -foo(), +-+-$a
 * </pre>
 */
public class UnaryOperation extends Expression {

	// '+'
	public static final int OP_PLUS = 0;
	// '-'
	public static final int OP_MINUS = 1;
	// '!'
	public static final int OP_NOT = 2;
	// '~'
	public static final int OP_TILDA = 3;

	private final Expression expr;
	private final int operator;

	public UnaryOperation(int start, int end, Expression expr, int operator) {
		super(start, end);

		assert expr != null;
		this.expr = expr;
		this.operator = operator;
	}

	@Override
	public void traverse(ASTVisitor visitor) throws Exception {
		final boolean visit = visitor.visit(this);
		if (visit) {
			expr.traverse(visitor);
		}
		visitor.endvisit(this);
	}

	@Override
	public String getOperator() {
		switch (getOperatorType()) {
		case OP_PLUS:
			return "+"; //$NON-NLS-1$
		case OP_MINUS:
			return "-"; //$NON-NLS-1$
		case OP_NOT:
			return "!"; //$NON-NLS-1$
		case OP_TILDA:
			return "~"; //$NON-NLS-1$
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public int getKind() {
		return ASTNodeKinds.UNARY_OPERATION;
	}

	public Expression getExpr() {
		return expr;
	}

	public int getOperatorType() {
		return operator;
	}

	/**
	 * We don't print anything - we use {@link ASTPrintVisitor} instead
	 */
	@Override
	public final void printNode(CorePrinter output) {
	}

	@Override
	public String toString() {
		return ASTPrintVisitor.toXMLString(this);
	}
}
