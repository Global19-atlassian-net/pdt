/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
package org.eclipse.php.internal.core.phpModel.parser;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.internal.core.phpModel.IPHPLanguageModel;
import org.eclipse.php.internal.core.phpModel.phpElementData.CodeData;
import org.eclipse.php.internal.core.phpModel.phpElementData.PHPClassData;
import org.eclipse.php.internal.core.phpModel.phpElementData.PHPFileData;
import org.eclipse.php.internal.core.phpModel.phpElementData.PHPClassData.PHPInterfaceNameData;

public class PHPProjectModel extends FilterableCompositePhpModel implements IPhpProjectModel, IAdaptable {

	private static String ID = "PHPProjectModel"; //$NON-NLS-1$

	private PHPUserModel userModel;
	private IPHPLanguageModel languageModel;
	private final Set<IProjectModelListener> listeners = new HashSet<IProjectModelListener>(2);
	private IProject currentProject;

	public PHPProjectModel() {
		super();
	}

	/**
	 * Initialize a non-project model
	 * this means we initialize the default model
	 */
	public void initialize() {
		languageModel = new PHPLanguageModelManager().setDefaultLanguageModel();
		addModel(languageModel);
	}

	public void initialize(IProject project) {
		currentProject = project;

		userModel = new PHPUserModel();
		userModel.initialize(project);
		addModel(userModel);

		languageModel = new PHPLanguageModelManager();
		languageModel.initialize(project);
		addModel(languageModel);

		if (project != null && project.isAccessible()) {
			PHPIncludePathModelManager includePathModel = new PHPIncludePathModelManager();
			includePathModel.initialize(project);
			addModel(includePathModel);
		}

		addModelListenrs();
	}

	public IProject getProject() {
		return currentProject;
	}

	public String getID() {
		return ID;
	}

	private void addModelListenrs() {
		IPhpModel[] models = getModels();
		for (IPhpModel phpModel : models) {
			if (phpModel instanceof IProjectModelListener) {
				addProjectModelListener((IProjectModelListener) phpModel);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public PHPUserModel getPHPUserModel() {
		return userModel;
	}

	public IPHPLanguageModel getPHPLanguageModel() {
		return languageModel;
	}

	public Object getExternalResource(PHPFileData fileData) {
		IPhpModel[] models = getModels();
		for (IPhpModel element : models) {
			if (element instanceof ExternalFilesModel) {
				Object rv = ((ExternalFilesModel) element).getExternalResource(fileData);
				if (rv != null) {
					return rv;
				}
			}
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public CodeData getActiveConstructor(String className, String fileName) {
		PHPClassData classData = getClass(fileName, className);
		CodeData constructorData = null;
		if (classData != null) {
			constructorData = getUserActiveConstructor(classData);
			if (constructorData == null) {
				constructorData = classData.getConstructor();
			}
		}
		return constructorData;
	}

	private CodeData getUserActiveConstructor(PHPClassData classData) {
		return getUserActiveConstructor(classData, new HashSet());
	}

	private CodeData getUserActiveConstructor(PHPClassData classData, HashSet subClasses) {
		if (classData == null) {
			return null;
		}
		if (classData.hasConstructor()) {
			return classData.getConstructor();
		}
		// search in parent class
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(classData.getName());
			String fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			return getUserActiveConstructor(getClass(fileName, superClass), subClasses);
		}
		return null;
	}

	public CodeData getClassFunctionData(String fileName, String className, String functionName) {
		return getClassFunctionData(fileName, className, functionName, new HashSet());
	}

	private CodeData getClassFunctionData(String fileName, String className, String functionName, HashSet subClasses) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return null;
		}

		CodeData[] functions = classData.getFunctions();
		int index = ModelSupport.getFirstMatch(functions, functionName, true);
		if (index >= 0) {
			return functions[index];
		}
		// search in parent class
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(className);
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			return getClassFunctionData(fileName, superClass, functionName, subClasses);
		}
		return null;
	}

	public CodeData[] getClassFunctions(String fileName, String className, String startsWith) {
		return getClassFunctions(fileName, className, startsWith, new HashSet());
	}

	private CodeData[] getClassFunctions(String fileName, String className, String startsWith, HashSet subClasses) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return null;
		}

		CodeData[] functions = ModelSupport.getCodeDataStartingWith(classData.getFunctions(), startsWith);

		CodeData[] superFunctions = new CodeData[0];
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(className);
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			superFunctions = ModelSupport.getFilteredCodeData(getClassFunctions(fileName, superClass, startsWith, subClasses), ModelSupport.PROTECTED_ACCESS_LEVEL_FILTER);
		}
		return ModelSupport.removeRepeatedNames(ModelSupport.mergeNoDuplicates(functions, superFunctions));
	}

	public CodeData getClassVariablesData(String fileName, String className, String startsWith) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return null;
		}

		if (startsWith.startsWith("$")) { //$NON-NLS-1$
			startsWith = startsWith.substring(1);
		}

		CodeData[] variables = classData.getVars();
		for (CodeData element : variables) {
			if (element.getName().equals(startsWith)) {
				return element;
			}
		}

		String superClass = classData.getSuperClassData().getName();
		if (superClass != null) {
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			return getClassVariablesData(fileName, superClass, startsWith);
		}

		return null;
	}

	public CodeData[] getClassVariables(String fileName, String className, String startsWith) {
		return getClassVariables(fileName, className, startsWith, new HashSet());
	}

	private CodeData[] getClassVariables(String fileName, String className, String startsWith, HashSet subClasses) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return null;
		}

		if (startsWith.startsWith("$")) { //$NON-NLS-1$
			startsWith = startsWith.substring(1);
		}
		CodeData[] variables = ModelSupport.getCodeDataStartingWith(classData.getVars(), startsWith);

		CodeData[] superVariables = null;
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(className);
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			superVariables = ModelSupport.getFilteredCodeData(getClassVariables(fileName, superClass, startsWith, subClasses), ModelSupport.PROTECTED_ACCESS_LEVEL_FILTER);
		}
		return ModelSupport.merge(variables, superVariables);
	}

	public CodeData getClassConstsData(String fileName, String className, String constName) {
		return getClassConstsData(fileName, className, constName, new HashSet());
	}

	private CodeData getClassConstsData(String fileName, String className, String constName, HashSet subClasses) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return null;
		}
		CodeData[] consts = classData.getConsts();
		for (CodeData element : consts) {
			if (element.getName().equals(constName)) {
				return element;
			}
		}
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(className);
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			CodeData classConstsData = getClassConstsData(fileName, superClass, constName, subClasses);
			if (classConstsData != null) {
				return classConstsData;
			}
		}

		final PHPInterfaceNameData[] interfacesNamesData = classData.getInterfacesNamesData();
		for (PHPInterfaceNameData interfaceNameData : interfacesNamesData) {
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			final CodeData classConstsData = getClassConstsData(fileName, interfaceNameData.getName(), constName);
			if (classConstsData != null) {
				return classConstsData;
			}
		}

		return null;
	}

	public CodeData[] getClassConsts(String fileName, String className, String startsWith) {
		return getClassConsts(fileName, className, startsWith, new HashSet());
	}

	private CodeData[] getClassConsts(String fileName, String className, String startsWith, HashSet subClasses) {
		PHPClassData classData = getClass(fileName, className);
		if (classData == null) {
			return new CodeData[0];
		}

		CodeData[] variables = ModelSupport.getCodeDataStartingWith(classData.getConsts(), startsWith);

		Collection<CodeData> superConsts = new ArrayList();
		String superClass = classData.getSuperClassData().getName();
		if (superClass != null && !subClasses.contains(superClass)) {
			subClasses.add(className);
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			superConsts.addAll(Arrays.asList(getClassConsts(fileName, superClass, startsWith, subClasses)));
		}
		PHPInterfaceNameData[] interfacesNamesData = classData.getInterfacesNamesData();
		for (PHPInterfaceNameData interfaceNameData : interfacesNamesData) {
			String interfaceName = interfaceNameData.getName();
			fileName = classData.isUserCode() ? classData.getUserData().getFileName() : ""; //$NON-NLS-1$
			superConsts.addAll(Arrays.asList(getClassConsts(fileName, interfaceName, startsWith)));
		}
		return ModelSupport.merge(variables, superConsts.toArray(new CodeData[superConsts.size()]));
	}

	public String getSuperClassName(String fileName, String className) {
		PHPClassData classData = getClass(fileName, className);
		if (classData != null) {
			return classData.getSuperClassData().getName();
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public CodeData[] getKeywordData() {
		return languageModel.getKeywordData();
	}

	public CodeData[] getKeywordData(String startsWith) {
		return ModelSupport.getCodeDataStartingWith(getKeywordData(), startsWith);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public CodeData[] getVariables(String fileName, PHPCodeContext context, String startsWith, boolean showVariablesFromOtherFiles) {
		return ModelSupport.removeRepeatedNames(super.getVariables(fileName, context, startsWith, showVariablesFromOtherFiles));
	}

	public CodeData[] getArrayVariables(String fileName, String arrayName, String startsWith, boolean showObjectsFromOtherFiles) {
		if (arrayName.equals("_SERVER") || arrayName.equals("HTTP_SERVER_VARS")) { //$NON-NLS-1$ //$NON-NLS-2$
			return ModelSupport.getCodeDataStartingWith(languageModel.getServerVariables(), startsWith);
		}
		if (arrayName.equals("_SESSION") || arrayName.equals("HTTP_SESSION_VARS")) { //$NON-NLS-1$ //$NON-NLS-2$
			return ModelSupport.getCodeDataStartingWith(languageModel.getSessionVariables(), startsWith);
		}
		if (arrayName.equals("GLOBALS")) { //$NON-NLS-1$
			CodeData[] defaultArrayVariables = ModelSupport.getCodeDataStartingWith(languageModel.getPHPVariables(), startsWith);
			CodeData[] userArrayVariables = userModel.getGlobalVariables(fileName, startsWith, showObjectsFromOtherFiles);
			return ModelSupport.removeRepeatedNames(ModelSupport.merge(defaultArrayVariables, userArrayVariables));
		}
		return PHPCodeDataFactory.EMPTY_CODE_DATA_ARRAY;
	}

	//	public PHPConstantData getConstant(String constantName, boolean caseSensitive) {
	//		PHPConstantData constantData = userModel.getConstants(constantName, caseSensitive);
	//		if (constantData == null) {
	//			CodeData[] constants = languageModel.getConstants(constantName, caseSensitive);
	//			if (constants != null && constants.length > 0) {
	//				constantData = (PHPConstantData) constants[0];
	//			}
	//		}
	//		return constantData;
	//	}

	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public void addFileToModel(IFile file) {
		fireFileDataAdded(file);
	}

	public void removeFileFromModel(IFile file) {
		fireFileDataRemoved(file);
	}

	public void fileWasChanged(IFile file, IDocument sDocument) {
		fireFileChanged(file, sDocument);
	}

	/**
	 * Project Model Listeners methods
	 */
	public synchronized void addProjectModelListener(IProjectModelListener l) {
		listeners.add(l);
	}

	public synchronized void removeProjectModelListener(IProjectModelListener l) {
		listeners.remove(l);
	}

	public synchronized IProjectModelListener[] getProjectModelListeners() {
		return listeners.toArray(new IProjectModelListener[listeners.size()]);
	}

	private void fireFileChanged(IFile file, IDocument sDocument) {
		final IProjectModelListener[] projectModelListeners = getProjectModelListeners();
		for (IProjectModelListener projectModelListener : projectModelListeners) {
			projectModelListener.fileChanged(file, sDocument);
		}
	}

	private void fireFileDataRemoved(IFile file) {
		final IProjectModelListener[] projectModelListeners = getProjectModelListeners();
		for (IProjectModelListener projectModelListener : projectModelListeners) {
			projectModelListener.fileRemoved(file);
		}
	}

	private void fireFileDataAdded(IFile file) {
		final IProjectModelListener[] projectModelListeners = getProjectModelListeners();
		for (IProjectModelListener projectModelListener : projectModelListeners) {
			projectModelListener.fileAdded(file);
		}
	}

	public boolean isBuildNeeded() {
		return userModel.isBuildNeeded();
	}
}