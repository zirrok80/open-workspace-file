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

		Job job = null;
		final CompareEditorInput compareEditorInput = (CompareEditorInput) activeEditor.getEditorInput();

		Object compareResult = compareEditorInput.getCompareResult();
		if (compareResult instanceof ICompareInput) {
			ICompareInput compareInput = (ICompareInput) compareEditorInput.getCompareResult();
			ITypedElement leftElement = compareInput.getLeft();
			if (leftElement instanceof IResourceProvider) {
				IResource resource = ((IResourceProvider) leftElement).getResource();
				if (resource instanceof IFile) {
					job = new SearchAndOpenFileInWorkspaceJob(activePage, (IFile) resource);
				}
			}
			else if (leftElement instanceof ResourceElement) {
				IRepositoryResource repositoryResource = ((ResourceElement) leftElement).getRepositoryResource();
				String url = repositoryResource.getUrl();
				job = new SearchAndOpenFileInWorkspaceJob(activePage, url);
			}
			else {
				Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "left element is not instanceof IResourceProvider"));
			}
		}
		else {
			Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "compareResult is not instanceof ICompareInput"));
		}

		if (job != null) {
			job.schedule();
		}
		else {
			OpenWorkspaceFileHelper.showAndLogErrorMessage("The resource could not be found in workspace.", null);
		}

		return null;
	}

}
