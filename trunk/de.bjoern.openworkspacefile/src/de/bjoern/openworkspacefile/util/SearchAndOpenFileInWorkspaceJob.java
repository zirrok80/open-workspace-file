package de.bjoern.openworkspacefile.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Job to search for and open a file.
 * 
 * @author funhoff
 */
public class SearchAndOpenFileInWorkspaceJob extends Job {

	/**
	 * The active page.
	 */
	private IWorkbenchPage activePage;

	/**
	 * Path to a file, e.g. a repository URL path.
	 */
	private String filePath = null;

	/**
	 * File to open in editor.
	 */
	private IFile file = null;

	/**
	 * The String to find and select.
	 */
	private String findString = null;

	/**
	 * Constructor.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param filePath
	 *            The path to a file.
	 * @since Creation date: 22.03.2012
	 */
	public SearchAndOpenFileInWorkspaceJob(IWorkbenchPage activePage, String filePath) {
		super("Searching for corresponding file in workspace.");
		this.activePage = activePage;
		this.filePath = filePath;
	}

	/**
	 * Constructor.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param filePath
	 *            The path to a file.
	 * @param findString
	 *            The string to find and select.
	 * @since Creation date: 02.04.2012
	 */
	public SearchAndOpenFileInWorkspaceJob(IWorkbenchPage activePage, String filePath, String findString) {
		this(activePage, filePath);
		this.findString = findString;
	}

	/**
	 * Constructor.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param file
	 *            The file to open.
	 * @since Creation date: 22.03.2012
	 */
	public SearchAndOpenFileInWorkspaceJob(IWorkbenchPage activePage, IFile file) {
		this(activePage, "");
		this.file = file;
	}

	/**
	 * 
	 * @param activePage
	 *            The active page.
	 * @param file
	 *            The file to open.
	 * @param findString
	 *            The string to find and select.
	 * @since Creation date: 02.04.2012
	 */
	public SearchAndOpenFileInWorkspaceJob(IWorkbenchPage activePage, IFile file, String findString) {
		this(activePage, file);
		this.findString = findString;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (!filePath.isEmpty()) {
			openEditorWithFilePath();
		}
		else if (file != null) {
			openEditorWithFile();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Opens the editor based on the given file path.
	 * 
	 * @since Creation date: 22.03.2012
	 */
	private void openEditorWithFilePath() {
		IFile workspaceFile = OpenWorkspaceFileHelper.getWorkspaceFile(filePath);
		if (workspaceFile != null) {
			OpenWorkspaceFileHelper.openEditor(activePage, workspaceFile, findString);
		}
		else {
			OpenWorkspaceFileHelper.showAndLogErrorMessage("The resource could not be found in workspace.", null);
		}
	}

	/**
	 * Opens the editor with the given file.
	 * 
	 * @since Creation date: 22.03.2012
	 */
	private void openEditorWithFile() {
		OpenWorkspaceFileHelper.openEditor(activePage, file);
	}

}
