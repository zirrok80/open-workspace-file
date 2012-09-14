package de.bjoern.openworkspacefile.util;

import org.apache.commons.lang3.Validate;
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
	 * File to open in editor.
	 */
	private IFile file;

	/**
	 * The String to find and select.
	 */
	private String findString;

	/**
	 * The offset to go to, if no text to find is defined.
	 */
	private int offset = 0;

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
		super("Opening workspace file");
		Validate.notNull(activePage);
		Validate.notNull(file);
		this.activePage = activePage;
		this.file = file;
	}

	/**
	 * Constructor.
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
		Validate.notEmpty(findString);
		this.findString = findString;
	}

	/**
	 * Constructor.
	 * 
	 * @param activePage
	 *            The active page.
	 * @param file
	 *            The file to open.
	 * @param offset
	 *            The offset to go to.
	 * @since Creation date: 13.09.2012
	 */
	public SearchAndOpenFileInWorkspaceJob(IWorkbenchPage activePage, IFile file, int offset) {
		this(activePage, file);
		if (offset < 0) {
			throw new IllegalArgumentException("offset must be 0 or greater.");
		}
		this.offset = offset;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (findString != null && !findString.isEmpty()) {
			OpenWorkspaceFileHelper.openEditorAndFindString(activePage, file, findString);
		}
		else {
			OpenWorkspaceFileHelper.openEditorAndGoToOffset(activePage, file, offset);
		}
		return Status.OK_STATUS;
	}

}
