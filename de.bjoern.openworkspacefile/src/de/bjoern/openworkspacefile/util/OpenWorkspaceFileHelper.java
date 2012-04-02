package de.bjoern.openworkspacefile.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import de.bjoern.openworkspacefile.Activator;

/**
 * Helper class providing methods to find resources and open editors.
 * 
 * @author funhoff
 */
public class OpenWorkspaceFileHelper {

	/**
	 * Path separator.
	 */
	public static final String PATH_SEPARATOR = "/";

	/**
	 * Private constructor.
	 * 
	 * @since Creation date: 22.03.2012
	 */
	private OpenWorkspaceFileHelper() {
		// prevent instantiation
	}

	/**
	 * Opens the the editor with the given file and selects the first occuring
	 * of the given string.
	 * 
	 * @param activePage
	 *            The page in which the editor will be opened.
	 * @param file
	 *            The file to open.
	 * @since Creation date: 14.03.2012
	 */
	public static void openEditor(final IWorkbenchPage activePage, final IFile file) {
		openEditor(activePage, file, null);
	}

	/**
	 * Opens the the editor with the given file and selects the first occuring
	 * of the given string.
	 * 
	 * @param activePage
	 *            The page in which the editor will be opened.
	 * @param file
	 *            The file to open.
	 * @param findString
	 *            String to find and select in the editor. Can be
	 *            <code>null</code>.
	 * @since Creation date: 02.04.2012
	 */
	public static void openEditor(final IWorkbenchPage activePage, final IFile file, final String findString) {

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					IEditorPart editor = IDE.openEditor(activePage, file);
					if (findString != null && !findString.isEmpty()) {
						IFindReplaceTarget target = (IFindReplaceTarget) editor.getAdapter(IFindReplaceTarget.class);
						target.findAndSelect(0, findString, true, false, false);
					}
				}

				catch (PartInitException e) {
					showAndLogErrorMessage("The editor could not be openend.", e);
				}
			}
		});

	}

	/**
	 * Shows and logs an error message with the given text to the user.
	 * 
	 * @param message
	 *            The error message text to show.
	 * @param trowable
	 *            Throwable to log. Can be <code>null</code>.
	 * @since Creation date: 13.03.2012
	 */
	public static void showAndLogErrorMessage(final String message, Throwable trowable) {
		Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, trowable));
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "An error occured during opening the workspace file", message);
			}
		});
	}

	/**
	 * Returns the corresponding workspace file of the given repository URI. Can
	 * be <code>null</code> if file not found.
	 * 
	 * @param repositoryURI
	 *            The URI of the repository source.
	 * @return The corresponding workspace file if found, otherwise returns
	 *         <code>null</code>.
	 * @since Creation date: 14.03.2012
	 */
	public static IFile getWorkspaceFile(String repositoryURI) {
		String uri = "";
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		String[] split = repositoryURI.split(PATH_SEPARATOR);
		for (int i = split.length - 1; i >= 0; i--) {
			if (i == split.length - 1) {
				uri = split[i] + uri;
			}
			else {
				uri = split[i] + PATH_SEPARATOR + uri;
			}
			Path path = new Path(uri);

			if (workspaceRoot.exists(path)) {
				IFile fileForLocation = workspaceRoot.getFile(path);
				IFile[] filesForLocationURI = workspaceRoot.findFilesForLocationURI(fileForLocation.getLocationURI());
				return getNotResourceFile(filesForLocationURI);
			}
		}
		return getWorkspaceFileFallBackStrategy(split[split.length - 1], repositoryURI);
	}

	/**
	 * Returns on of the given files. Should not be the resource file.
	 * 
	 * @param files
	 *            Files to choose the 'right' on.
	 * @return The {@link IFile} not representing the resource file.
	 * @since Creation date: 22.03.2012
	 */
	private static IFile getNotResourceFile(IFile[] files) {
		Map<IFile, Integer> fileSeparatorCountMap = new HashMap<IFile, Integer>();
		if (files.length == 1) {
			return files[0];
		}
		else {
			for (int i = 0; i < files.length; i++) {
				String[] filePathSplit = files[i].getFullPath().toString().split("/");
				int separatorCount = filePathSplit.length;
				fileSeparatorCountMap.put(files[i], separatorCount);
			}
			Set<IFile> keySet = fileSeparatorCountMap.keySet();
			IFile fileToReturn = null;
			int lowestCount = Integer.MAX_VALUE;
			for (Iterator<IFile> iterator = keySet.iterator(); iterator.hasNext();) {
				IFile file = iterator.next();
				int separatorCount = fileSeparatorCountMap.get(file);
				if (separatorCount < lowestCount) {
					fileToReturn = file;
					lowestCount = separatorCount;
				}
			}
			return fileToReturn;
		}
	}

	/**
	 * Searches the workspace for the most likely file.
	 * 
	 * @param filename
	 *            The file name to search.
	 * @param repositoryURI
	 *            The URI of the repository source.
	 * @return The corresponding workspace file if found, otherwise returns
	 *         <code>null</code>.
	 */
	private static IFile getWorkspaceFileFallBackStrategy(String filename, String repositoryURI) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		try {
			IResource[] members = workspaceRoot.members();
			IFile[] filesFromResources = getWorkspaceFileFromResources(members, filename);
			if (filesFromResources != null && filesFromResources.length > 0) {
				if (filesFromResources.length == 1) {
					return filesFromResources[0];
				}
				else {
					return getMostLikelyFile(filesFromResources, repositoryURI);
				}
			}
		}
		catch (CoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The fall back strategy failed", e));
		}
		return null;
	}

	/**
	 * Returns the depending {@link IFile}s if the given filename is found in
	 * one of the given {@link IResource}s.
	 * 
	 * @param resources
	 *            The resources to search in.
	 * @param filename
	 *            The searched file name.
	 * @return The corresponding {@link IFile}s if found. Otherwise
	 *         <code>null</code> is returned.
	 * @throws CoreException
	 */
	private static IFile[] getWorkspaceFileFromResources(IResource[] resources, String filename) throws CoreException {
		Vector<IFile> foundFilesVector = new Vector<IFile>();
		for (int i = 0; i < resources.length; i++) {
			IFile[] files = getWorkspaceFileFromSingleResource(resources[i], filename);
			if (files != null && files.length > 0) {
				for (int j = 0; j < files.length; j++) {
					foundFilesVector.add(files[j]);
				}
			}
		}
		if (!foundFilesVector.isEmpty()) {
			return foundFilesVector.toArray(new IFile[foundFilesVector.size()]);
		}
		return null;
	}

	/**
	 * Returns the depending {@link IFile} if the given filename is found in the
	 * given {@link IResource}.
	 * 
	 * @param resource
	 *            The resource to search in.
	 * @param filename
	 *            The searched file name.
	 * @return The corresponding {@link IFile} if found. Otherwise
	 *         <code>null</code> is returned.
	 * @throws CoreException
	 */
	private static IFile[] getWorkspaceFileFromSingleResource(IResource resource, String filename) throws CoreException {
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			if (filename.equals(file.getName())) {
				return new IFile[]{file};
			}
		}
		else if (resource instanceof IFolder) {
			IFolder folder = (IFolder) resource;
			IResource[] members = folder.members();
			if (members.length > 0) {
				return getWorkspaceFileFromResources(members, filename);
			}
		}
		else if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			if (project.isOpen()) {
				IResource[] members = project.members();
				if (members.length > 0) {
					return getWorkspaceFileFromResources(members, filename);
				}
			}
		}
		return null;
	}

	/**
	 * Returns the most likely searched file.
	 * 
	 * @param files
	 *            The file to choose the most likely one.
	 * @param repositoryURI
	 *            The URI of the repository source.
	 * @return The most likely searched file.
	 */
	private static IFile getMostLikelyFile(IFile[] files, String repositoryURI) {
		Map<IFile, Integer> filePathIdentitiesMap = new HashMap<IFile, Integer>();
		for (int i = 0; i < files.length; i++) {
			filePathIdentitiesMap.put(files[i], 0);
			String[] repositorySplit = repositoryURI.split(PATH_SEPARATOR);
			String[] foundFileSplit = files[i].getFullPath().toString().split(PATH_SEPARATOR);
			int inspectDeep = (repositorySplit.length > foundFileSplit.length) ? foundFileSplit.length : repositorySplit.length;
			for (int j = 0; j < inspectDeep; j++) {
				if (repositorySplit[repositorySplit.length - 1 - j].equals(foundFileSplit[foundFileSplit.length - 1 - j])) {
					int oldDeep = filePathIdentitiesMap.get(files[i]);
					filePathIdentitiesMap.put(files[i], ++oldDeep);
				}
				else {
					break;
				}
			}
		}

		return evaluateMostLikelyFile(filePathIdentitiesMap);
	}

	/**
	 * Returns the file with the highest path identity deep in the given map.
	 * 
	 * @param filePathIdentitiesMap
	 *            The Map containing file and path identity deep.
	 * @return The {@link IFile} with the highest path identity deep.
	 */
	private static IFile evaluateMostLikelyFile(Map<IFile, Integer> filePathIdentitiesMap) {
		IFile mostLikelyFile = null;
		int mostLikelyFileDeep = 0;
		Set<IFile> keySet = filePathIdentitiesMap.keySet();
		for (Iterator<IFile> iterator = keySet.iterator(); iterator.hasNext();) {
			IFile file = iterator.next();
			int deep = filePathIdentitiesMap.get(file);
			if (deep > mostLikelyFileDeep) {
				mostLikelyFileDeep = deep;
				mostLikelyFile = file;
			}
		}
		return mostLikelyFile;
	}

}
