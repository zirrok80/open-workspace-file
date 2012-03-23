package de.bjoern.openworkspacefile.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.ui.history.data.SVNChangedPathData;
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
			IStructuredSelection structuredSelection = (IStructuredSelection) activeMenuSelection;
			Object firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof SVNChangedPathData) {
				SVNChangedPathData data = (SVNChangedPathData) firstElement;
				String filePath = data.resourcePath + "/" + data.resourceName;
				Job job = new SearchAndOpenFileInWorkspaceJob(activePage, filePath);
				job.schedule();
			}
		}

		return null;
	}

}
