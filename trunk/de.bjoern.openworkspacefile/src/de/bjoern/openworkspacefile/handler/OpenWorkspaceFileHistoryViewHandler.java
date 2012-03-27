package de.bjoern.openworkspacefile.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import de.bjoern.openworkspacefile.util.OpenWorkspaceFileHelper;
import de.bjoern.openworkspacefile.util.SearchAndOpenFileInWorkspaceJob;

/**
 * Handler to open the source of the history view.
 * 
 * @author funhoff
 */
public class OpenWorkspaceFileHistoryViewHandler extends AbstractHandler {

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

		ISelection activeMenuSelection = HandlerUtil.getActiveMenuSelection(event);
		if (activeMenuSelection instanceof IStructuredSelection) {
			openActiveMenuSelection(activePage, activeMenuSelection);
		}
		return null;
	}

	/**
	 * Opens the active menu selection.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param activeMenuSelection
	 *            The active menu selection to open.
	 * @since Creation date: 26.03.2012
	 */
	private void openActiveMenuSelection(IWorkbenchPage activePage, ISelection activeMenuSelection) {
		IStructuredSelection structuredSelection = (IStructuredSelection) activeMenuSelection;
		Object firstElement = structuredSelection.getFirstElement();
		if (firstElement instanceof SVNChangedPathData) {
			openSVNChangedPathData(activePage, (SVNChangedPathData) firstElement);
		}
		else {
			openHistoryViewSource(activePage);
		}
	}

	/**
	 * Opens the selected {@link SVNChangedPathData}.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param data
	 *            The data to open.
	 * @since Creation date: 26.03.2012
	 */
	private void openSVNChangedPathData(IWorkbenchPage activePage, SVNChangedPathData data) {
		String filePath = data.resourcePath + OpenWorkspaceFileHelper.PATH_SEPARATOR + data.resourceName;
		Job job = new SearchAndOpenFileInWorkspaceJob(activePage, filePath);
		job.schedule();
	}

	/**
	 * Opens the source of the history view, if it is an instance of
	 * {@link IFile}.
	 * 
	 * @param activePage
	 *            The active page.
	 * @since Creation date: 26.03.2012
	 */
	private void openHistoryViewSource(IWorkbenchPage activePage) {
		IViewPart view = activePage.findView(IHistoryView.VIEW_ID);
		if (view != null && view instanceof IHistoryView) {
			IHistoryView historyView = (IHistoryView) view;
			Object input = historyView.getHistoryPage().getInput();
			if (input != null && input instanceof IFile) {
				IFile file = (IFile) input;
				Job job = new SearchAndOpenFileInWorkspaceJob(activePage, file.getFullPath().toString());
				job.schedule();
			}
		}
	}

}
