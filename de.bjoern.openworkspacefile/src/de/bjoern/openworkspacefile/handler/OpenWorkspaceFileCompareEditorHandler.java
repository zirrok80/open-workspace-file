package de.bjoern.openworkspacefile.handler;

import java.net.URI;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput.ResourceElement;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

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

		String selectedText = null;
		int offset = 0;
		ISelection activeSelection = activePage.getSelection();
		if (activeSelection != null & activeSelection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) activeSelection;
			selectedText = textSelection.getText();
			offset = textSelection.getOffset();
		}

		final CompareEditorInput compareEditorInput = (CompareEditorInput) activeEditor.getEditorInput();
		Object compareResult = compareEditorInput.getCompareResult();
		if (compareResult instanceof ICompareInput) {
			openCompareInput(activePage, (ICompareInput) compareResult, selectedText, offset);
		}

		return null;
	}

	/**
	 * Opens the resource of the given {@link ICompareInput} and selects the
	 * given text. If the text is <code>null</code> or empty, the resource will
	 * open at the given offset.
	 *
	 * @param activePage
	 *            The active page.
	 * @param compareInput
	 *            Input with resource to open.
	 * @param selectedText
	 *            The selected text. Can be <code>null</code>.
	 * @param offset
	 *            The offset of the text selection.
	 * @since Creation date: 27.03.2012
	 */
	private void openCompareInput(IWorkbenchPage activePage, final ICompareInput compareInput, String selectedText, int offset) {
		ITypedElement leftElement = compareInput.getLeft();
		ITypedElement rightElement = compareInput.getRight();

		openTypedElement(activePage, leftElement, selectedText, offset);

		if (isDifferentElement(leftElement, rightElement)) {
			openTypedElement(activePage, rightElement, selectedText, offset);
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
	 * Opens the given {@link ITypedElement} and selects the given text. If the
	 * text is <code>null</code> or empty, the resource will open at the given
	 * offset.
	 *
	 * @param activePage
	 *            The active page.
	 * @param element
	 *            The {@link ITypedElement} to open.
	 * @param selectedText
	 *            The text to select. Can be <code>null</code>.
	 * @param offset
	 *            The text selection offset.
	 * @since Creation date: 27.03.2012
	 */
	@SuppressWarnings("restriction")
	private void openTypedElement(IWorkbenchPage activePage, ITypedElement element, String selectedText, int offset) {
		if (element instanceof IResourceProvider) {
			IResource resource = ((IResourceProvider) element).getResource();
			if (resource instanceof IFile) {
				Job job = createJobWithFile(activePage, selectedText, offset, (IFile) resource);
				job.schedule();
			}
		}
		else if (element instanceof ResourceElement) {
			IRepositoryResource repositoryResource = ((ResourceElement) element).getRepositoryResource();
			String url = repositoryResource.getUrl();
			IFile workspaceFile = OpenWorkspaceFileHelper.getWorkspaceFile(url);
			Job job = createJobWithFile(activePage, selectedText, offset, workspaceFile);
			job.schedule();
		}
		else if (element instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement fileRevisionTypedElement = (FileRevisionTypedElement) element;
			IFileRevision fileRevision = fileRevisionTypedElement.getFileRevision();
			URI uri = fileRevision.getURI();
			IFile workspaceFile = getIFileFromURI(uri);
			Job job = createJobWithFile(activePage, selectedText, offset, workspaceFile);
			job.schedule();
		}
	}

	private IFile getIFileFromURI(URI uri) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		URI rootUri = root.getLocationURI();
		uri = rootUri.relativize(uri);
		IPath path = new Path(uri.getPath());
		return root.getFile(path);
	}

	/**
	 * Creates the job to open the given file.
	 *
	 * @param activePage
	 *            The active page.
	 * @param selectedText
	 *            The selected text.
	 * @param offset
	 *            The offset.
	 * @param file
	 *            The file to open.
	 * @return The created job.
	 * @since Creation date: 13.09.2012
	 */
	private Job createJobWithFile(IWorkbenchPage activePage, String selectedText, int offset, IFile file) {
		Job job;
		if (selectedText != null && !selectedText.isEmpty()) {
			job = new SearchAndOpenFileInWorkspaceJob(activePage, file, selectedText);
		}
		else {
			job = new SearchAndOpenFileInWorkspaceJob(activePage, file, offset);
		}
		return job;
	}

}
