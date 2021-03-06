/**
 * 
 */
package org.eclipse.php.internal.core.codeassist.strategies;

import org.eclipse.dltk.core.*;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.php.core.codeassist.ICompletionContext;
import org.eclipse.php.core.codeassist.ICompletionReporter;
import org.eclipse.php.core.codeassist.ICompletionScope;
import org.eclipse.php.core.codeassist.ICompletionScope.Type;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.Logger;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.codeassist.AliasType;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.codeassist.contexts.AbstractCompletionContext;
import org.eclipse.php.internal.core.typeinference.FakeConstructor;

/**
 * This is a basic strategy that completes global classes after 'new' statement,
 * without any additional add-ons in final result
 * 
 * @author vadim.p
 * 
 */
public abstract class AbstractClassInstantiationStrategy extends TypesStrategy {

	private String enclosingClass;

	public AbstractClassInstantiationStrategy(ICompletionContext context, int trueFlag, int falseFlag) {
		super(context, trueFlag, falseFlag);
	}

	public AbstractClassInstantiationStrategy(ICompletionContext context) {
		this(context, 0, 0);
	}

	@Override
	public void apply(ICompletionReporter reporter) throws BadLocationException {

		ICompletionContext context = getContext();
		AbstractCompletionContext concreteContext = (AbstractCompletionContext) context;

		ICompletionScope scope = getCompanion().getScope().findParent(Type.CLASS, Type.INTERFACE);
		if (scope != null) {
			enclosingClass = scope.getName();
		}

		ISourceRange replaceRange = getReplacementRangeForMember(concreteContext);
		String suffix = getSuffix(concreteContext);

		IType[] types = getTypes(concreteContext);
		for (IType type : types) {
			try {
				if (PHPFlags.isNamespace(type.getFlags())) {
					ISourceRange nsReplaceRange = getReplacementRange(concreteContext);
					int extraInfo = getExtraInfo();
					if (concreteContext.isAbsoluteName()) {
						extraInfo |= ProposalExtraInfo.ABSOLUTE_NAME;
					}
					reporter.reportType(type, NamespaceReference.NAMESPACE_DELIMITER, nsReplaceRange,
							extraInfo | ProposalExtraInfo.MEMBER_IN_NAMESPACE);
					continue;
				}
			} catch (ModelException e) {
				Logger.logException(e);
			}
			if (!concreteContext.getCompletionRequestor().isContextInformationMode()) {
				// here we use fake method,and do the real work in class
				// ParameterGuessingProposal
				IMethod ctorMethod = FakeConstructor.createFakeConstructor(null, type,
						enclosingClass != null && enclosingClass.equals(type.getElementName()));
				reporter.reportMethod(ctorMethod, suffix, replaceRange, ProposalExtraInfo.FULL_NAME);
			} else {
				// if this is context information mode,we use this,
				// because the number of types' length is very small
				IMethod[] ctors = FakeConstructor.getConstructors(type, type.getElementName().equals(enclosingClass));
				if (ctors != null && ctors.length == 2) {
					if (ctors[1] != null) {
						reporter.reportMethod(ctors[1], suffix, replaceRange, ProposalExtraInfo.FULL_NAME);
					} else if (ctors[0] == null) {
						reporter.reportType(type, suffix, replaceRange);
					}
				}
			}

		}
		addAlias(reporter, suffix);
	}

	@Override
	protected void reportAlias(ICompletionReporter reporter, IDLTKSearchScope scope, ISourceModule module,
			ISourceRange replacementRange, IType type, String fullyQualifiedName, String alias, String suffix) {
		IType aliasType = new AliasType((ModelElement) type, fullyQualifiedName, alias);
		IMethod ctorMethod = FakeConstructor.createFakeConstructor(null, aliasType,
				type.getElementName().equals(enclosingClass));
		reporter.reportMethod(ctorMethod, "", replacementRange, ProposalExtraInfo.FULL_NAME); //$NON-NLS-1$
	}

	@Override
	public String getSuffix(AbstractCompletionContext abstractContext) {
		boolean insertMode = isInsertMode();

		char nextChar = ' ';
		try {
			if (insertMode) {
				nextChar = abstractContext.getNextChar();
			} else {
				ISourceRange replacementRange = getReplacementRange(abstractContext);
				nextChar = abstractContext.getChar(replacementRange.getOffset() + replacementRange.getLength());
			}

		} catch (BadLocationException e) {
			PHPCorePlugin.log(e);
		}
		return '(' == nextChar ? "" : "()"; //$NON-NLS-1$ //$NON-NLS-2$
											// //$NON-NLS-3$
	}

}
