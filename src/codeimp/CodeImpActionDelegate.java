/**
 * 
 */
package codeimp;

import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpActionDelegate implements IWorkbenchWindowActionDelegate {
	IWorkbenchWindow window = null;
	IFile curEditorFile = null;
	ITextSelection textSelection = null;

	/**
	 * Pair of IJavaElement and action which are used to refactor the element
	 * 
	 * @author chuxuankhoi
	 * 
	 */
	private class RefactoringPair {
		public IJavaElement element;
		public String action; // get from IJavaRefactorings
		public Object addition;

		public RefactoringPair(IJavaElement elem, String act, Object add) {
			element = elem;
			action = act;
			addition = add;
		}
	}

	ArrayList<RefactoringPair> smellElements;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		MessageDialog.openInformation(window.getShell(),
				"Have not completed yet", "CodeImp is under construction");

		// Check Java perspective and editor
		if (!isPerspective("Java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking current perspective ...",
					"Current version have not support the language.");
			return;
		}

		// Check opening file to make sure that it is a java file
		getCurrentEditorFile();
		if (!curEditorFile.getFileExtension().equals("java")) {
			MessageDialog.openInformation(window.getShell(),
					"Checking file extension ...",
					"Current version have not support the file extension.");
		}

		// Get the selected code in the current editor
		getSelectedCode();
		if (textSelection.getText().equals("")) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text is selected.");
			return;
		}

		// Extract selected code to find IJavaElements
		IJavaElement[] extractedResults = null;
		try {
			extractedResults = extractSelectedCode();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return;
		}
		if (extractedResults == null) {
			MessageDialog.openInformation(window.getShell(),
					"Extracting selected text ...", "Fail to extract code.");
			return;
		}
		if (extractedResults.length == 0) {
			MessageDialog.openInformation(window.getShell(),
					"Extracting selected text ...", "No element found.");
			return;
		}
		System.out.println("Number of elements found: "
				+ extractedResults.length);

		// TODO Analyse code smell and build smellElements - JDeodorant or
		// FindBugs
		if (smellElements == null)
			smellElements = new ArrayList<RefactoringPair>();
		if (extractedResults[0] instanceof IField) {
			RefactoringPair refactoringElem = new RefactoringPair(
					extractedResults[0], IJavaRefactorings.RENAME_FIELD,
					"newName");
			smellElements.add(refactoringElem);
		}

		// TODO Display the analysis and confirm the changed blocks - Eclipse
		// facility

		// Call refactoring module to change the code
		for (RefactoringPair pair : smellElements) {
			refactorElement(pair);
		}

		// Clean refactored items
		smellElements.clear();
	}

	private IJavaElement[] extractSelectedCode() throws JavaModelException {
		ArrayList<IJavaElement> retList = new ArrayList<IJavaElement>();

		// Extract code and recognize the elements
		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(curEditorFile);
		String sourceCode = cu.getSource();
		String selectedCode = textSelection.getText();
		String[] words = selectedCode.split("[ \t\\x0b\n(){}'\";,.]");
		int oldOffset = textSelection.getOffset();
		int offset = 0;
		int length = 0;
		for (String word : words) {
			if (word.equals(""))
				continue;
			offset = sourceCode.indexOf(word, oldOffset);
			length = word.length();
			IJavaElement[] elements = cu.codeSelect(offset, length);
			for (IJavaElement element : elements) {
				retList.add(element);
			}
			oldOffset = offset;
		}

		// Return found array
		IJavaElement[] retArray = new IJavaElement[retList.size()];
		retList.toArray(retArray);
		return retArray;
	}

	/**
	 * @param pair
	 */
	private void refactorElement(RefactoringPair pair) {
		if (pair == null) {
			return;
		}
		if (pair.element == null || pair.action == null
				|| pair.addition == null) {
			MessageDialog.openInformation(window.getShell(), "Refactoring ...",
					"Error when creating pair.");
			return;
		}
		System.out.println("Refator " + pair.element.getElementName() + " by "
				+ pair.action);
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);
		JavaRefactoringDescriptor descriptor = createDescriptor(
				pair, contribution);

		RefactoringStatus status = new RefactoringStatus();
		try {
			Refactoring refactoring = descriptor.createRefactoring(status);
			IProgressMonitor monitor = new NullProgressMonitor();
			refactoring.checkInitialConditions(monitor);
			refactoring.checkFinalConditions(monitor);
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Depend on the action user expected, the function creates a descriptor 
	 * which contains enough information for the refactoring
	 * @param pair
	 * @param contribution
	 * @return
	 */
	private JavaRefactoringDescriptor createDescriptor(RefactoringPair pair,
			RefactoringContribution contribution) {
		JavaRefactoringDescriptor descriptor;
		descriptor = (JavaRefactoringDescriptor) contribution.createDescriptor();
		// next lines are essential for any refactoring
		descriptor.setProject(curEditorFile.getProject().getName());
		
		// following lines depend on the specific refactoring
		switch (pair.action) {
		case IJavaRefactorings.RENAME_FIELD:
		case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
		case IJavaRefactorings.RENAME_COMPILATION_UNIT:
		case IJavaRefactorings.RENAME_ENUM_CONSTANT:
		case IJavaRefactorings.RENAME_JAVA_PROJECT:
		case IJavaRefactorings.RENAME_METHOD:
		case IJavaRefactorings.RENAME_PACKAGE:
		case IJavaRefactorings.RENAME_SOURCE_FOLDER:
		case IJavaRefactorings.RENAME_TYPE:
		case IJavaRefactorings.RENAME_TYPE_PARAMETER:
			((RenameJavaElementDescriptor) descriptor).setJavaElement(pair.element);
			((RenameJavaElementDescriptor) descriptor).setNewName((String) pair.addition);
			break;
		case IJavaRefactorings.CHANGE_METHOD_SIGNATURE:
		case IJavaRefactorings.CONVERT_ANONYMOUS:
		case IJavaRefactorings.CONVERT_LOCAL_VARIABLE:
		case IJavaRefactorings.CONVERT_MEMBER_TYPE:
		case IJavaRefactorings.COPY:
		case IJavaRefactorings.DELETE:
		case IJavaRefactorings.ENCAPSULATE_FIELD:
		case IJavaRefactorings.EXTRACT_CLASS:
		case IJavaRefactorings.EXTRACT_CONSTANT:
		case IJavaRefactorings.EXTRACT_INTERFACE:
		case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
		case IJavaRefactorings.EXTRACT_METHOD:
		case IJavaRefactorings.EXTRACT_SUPERCLASS:
		case IJavaRefactorings.GENERALIZE_TYPE:
		case IJavaRefactorings.INFER_TYPE_ARGUMENTS:
		case IJavaRefactorings.INLINE_CONSTANT:
		case IJavaRefactorings.INLINE_LOCAL_VARIABLE:
		case IJavaRefactorings.INLINE_METHOD:
		case IJavaRefactorings.INTRODUCE_FACTORY:
		case IJavaRefactorings.INTRODUCE_INDIRECTION:
		case IJavaRefactorings.INTRODUCE_PARAMETER:
		case IJavaRefactorings.INTRODUCE_PARAMETER_OBJECT:
		case IJavaRefactorings.MOVE:
		case IJavaRefactorings.MOVE_METHOD:
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
		case IJavaRefactorings.PULL_UP:
		case IJavaRefactorings.PUSH_DOWN:
		default:
			break;
		}
		
		return descriptor;
	}

	private boolean isPerspective(String expectedPerspective) {
		String curPerspective = window.getActivePage().getPerspective()
				.getLabel();
		if (curPerspective.equals(expectedPerspective)) {
			return true;
		}
		return false;
	}

	private void getCurrentEditorFile() {
		IFile curFile = null;
		IEditorPart editorPart = window.getActivePage().getActiveEditor();
		if (editorPart != null) {
			IFileEditorInput input = (IFileEditorInput) editorPart
					.getEditorInput();
			curFile = input.getFile();
		}
		curEditorFile = curFile;
	}

	private void getSelectedCode() {
		IWorkbenchPartSite site = window.getPartService().getActivePart()
				.getSite();
		if (site == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No workbench part site.");
			return;
		}
		ISelectionProvider provider;
		ISelection selection;
		provider = site.getSelectionProvider();
		if (provider == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection provider.");
			return;
		}
		selection = provider.getSelection();
		if (selection == null) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No selection is get.");
			return;
		}
		if (!(selection instanceof ITextSelection)) {
			MessageDialog.openInformation(window.getShell(),
					"Getting selected text ...", "No text selection is get.");
			return;
		}
		textSelection = (ITextSelection) selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	@Override
	public void dispose() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.
	 * IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
