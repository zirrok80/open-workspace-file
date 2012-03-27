package de.bjoern.openworkspacefile.handler;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput.ResourceElement;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import de.bjoern.openworkspacefile.Activator;
import de.bjoern.openworkspacefile.util.OpenWorkspaceFileHelper;
import de.bjoern.openworkspacefile.util.SearchAndOpenFileInWorkspaceJob;

/**
 * Handler to open the source of the compare editor.
 * 
 * @author funhoff
 */
public class OpenWorkspaceFileCompareEditorHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) {
			OpenWorkspaceFileHelper.showAndLogErrorMessage("Active workbench window not found.", null);
			return null;
		}
		IWorkbenchPage activePage = window.getActivePage();
		if (activePage == null) {
			OpenWorkspaceFileHelper.showAndLogErrorMessage("Active page not found.", null);
			return null;
		}
		IEditorPart activeEditor = activePage.getActiveEditor();
		if (activeEditor == null) {
			OpenWorkspaceFileHelper.showAndLogErrorMessage("Active editor not found.", null);
			return null;
		}

		final CompareEditorInput compareEditorInput = (CompareEditorInput) activeEditor.getEditorInput();

		Object compareResult = compareEditorInput.getCompareResult();
		if (compareResult instanceof ICompareInput) {
			openCompareInput(activePage, (ICompareInput) compareResult);
		}

		return null;
	}

	/**
	 * Opens the resource of the given {@link ICompareInput}.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param compareInput
	 *            Input with resource to open.
	 * @since Creation date: 27.03.2012
	 */
	private void openCompareInput(IWorkbenchPage activePage, final ICompareInput compareInput) {
		ITypedElement leftElement = compareInput.getLeft();
		ITypedElement rightElement = compareInput.getRight();

		openTypedElement(activePage, leftElement);

		if (isDifferentElement(leftElement, rightElement)) {
			openTypedElement(activePage, rightElement);
		}
	}

	/**
	 * Returns <code>true</code> if the given {@link ITypedElement} ends with
	 * the same file name.
	 * 
	 * @param fistElement
	 *            The first element to check.
	 * @param secondElement
	 *            The second element to check.
	 * @return <code>true</code> if the given {@link ITypedElement} ends with
	 *         the same file name, otherwise <code>false</code>.
	 * @since Creation date: 27.03.2012
	 */
	private boolean isDifferentElement(ITypedElement fistElement, ITypedElement secondElement) {
		String leftPath = getTypedElementPath(fistElement);
		String rightPath = getTypedElementPath(secondElement);

		String[] leftPathString = leftPath.split(OpenWorkspaceFileHelper.PATH_SEPARATOR);
		String[] rightPathString = rightPath.split(OpenWorkspaceFileHelper.PATH_SEPARATOR);

		String leftFileName = leftPathString[leftPathString.length - 1];
		String rightFileName = rightPathString[rightPathString.length - 1];

		return !leftFileName.equals(rightFileName);
	}

	/**
	 * Returns the path of the given {@link ITypedElement}.
	 * 
	 * @param element
	 *            The {@link ITypedElement} to return path from.
	 * @return The path of the given {@link ITypedElement}.
	 * @since Creation date: 27.03.2012
	 */
	private String getTypedElementPath(ITypedElement element) {
		if (element instanceof IResourceProvider) {
			return ((IResourceProvider) element).getResource().getFullPath().toString();
		}
		else if (element instanceof ResourceElement) {
			return ((ResourceElement) element).getRepositoryResource().getUrl();
		}
		return "";
	}

	/**
	 * Opens the given {@link ITypedElement}.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param element
	 *            The {@link ITypedElement} to open.
	 * @since Creation date: 27.03.2012
	 */
	private void openTypedElement(IWorkbenchPage activePage, ITypedElement element) {
		if (element instanceof IResourceProvider) {
			IResource resource = ((IResourceProvider) element).getResource();
			if (resource instanceof IFile) {
				Job job = new SearchAndOpenFileInWorkspaceJob(activePage, (IFile) resource);
				job.schedule();
			}
		}
		else if (element instanceof ResourceElement) {
			IRepositoryResource repositoryResource = ((ResourceElement) element).getRepositoryResource();
			String url = repositoryResource.getUrl();
			Job job = new SearchAndOpenFileInWorkspaceJob(activePage, url);
			job.schedule();
		}
		else {
			Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "Resource is not instanceof IResourceProvider"));
		}
	}

}
