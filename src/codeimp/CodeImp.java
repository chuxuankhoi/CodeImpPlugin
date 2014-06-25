/**
 * 
 */
package codeimp;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author chuxuankhoi
 * 
 * Improve selected code using hill-climbing algorithm
 *
 */
public class CodeImp {
	
	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected String[] refactoringHistory;
	
	protected boolean needStopThread = false;
	
	public static void printLog(String log) {
		System.out.println(System.currentTimeMillis() + " - CodeImp - " + log);
	}
	
	public CodeImp(ITextSelection selectedCode, IFile file, IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}
	
	public void runImprovement() {
		try {			
			double curScore = getCurrentScore();
			double oldScore = curScore;
			boolean needMoreImp = true;
			while(needMoreImp && !needStopThread) {
				// TODO run improvement over the elements using hill-climbing algorithm
				curScore = getCurrentScore();
				if(curScore >= oldScore) {
					needMoreImp = false;
				}
				oldScore = curScore;
			}
		} catch (Exception e) {
			printLog(e.getMessage());
		}
		
	}
	
	public String getRefactoringHistory() {
		return "";
	}
	
	public double getCurrentScore() throws JavaModelException {
		IJavaElement[] elements = extractSelectedCode();
		if(elements == null) {
			System.out.println("No element found.");
			return 0;
		}
		double score = 0;
		for(IJavaElement element:elements) {
			score += scoreElement(element);
		}
		return score;
	}
	
	private double scoreElement(IJavaElement element) {
		// TODO Auto-generated method stub
		return 0;
	}

	private IJavaElement[] extractSelectedCode() throws JavaModelException {
		if(codeSelection == null || sourceFile == null) {
			System.out.println("Error when initializing CodeImp instance.");
			return null;
		}
		ArrayList<IJavaElement> retList = new ArrayList<IJavaElement>();

		// Extract code and recognize the elements
		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(sourceFile);
		String sourceCode = cu.getSource();
		String selectedCode = codeSelection.getText();
		if(selectedCode.equals("")) {
			System.out.println("No selected code.");
			return null;
		}
		String[] words = selectedCode.split("[ \t\\x0b\n(){}'\";,.]");
		int oldOffset = codeSelection.getOffset();
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
		JavaRefactoringDescriptor descriptor = createDescriptor(pair,
				contribution);

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
	 * 
	 * @param pair
	 * @param contribution
	 * @return
	 */
	private JavaRefactoringDescriptor createDescriptor(RefactoringPair pair,
			RefactoringContribution contribution) {
		JavaRefactoringDescriptor descriptor;
		descriptor = (JavaRefactoringDescriptor) contribution
				.createDescriptor();
		// next lines are essential for any refactoring
		descriptor.setProject(sourceFile.getProject().getName());

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
			RenameJavaElementDescriptor renameDescriptor = (RenameJavaElementDescriptor) descriptor;
			renameDescriptor.setJavaElement(pair.element);
			renameDescriptor.setNewName((String) pair.addition);
			renameDescriptor.setDeprecateDelegate(true);
			renameDescriptor.setKeepOriginal(false);
			renameDescriptor.setUpdateReferences(true);
			renameDescriptor.setUpdateTextualOccurrences(true);
			break;
		case IJavaRefactorings.CHANGE_METHOD_SIGNATURE:
			break;
		case IJavaRefactorings.CONVERT_ANONYMOUS:
		case IJavaRefactorings.CONVERT_LOCAL_VARIABLE:
		case IJavaRefactorings.CONVERT_MEMBER_TYPE:
		case IJavaRefactorings.COPY:
		case IJavaRefactorings.DELETE:
		case IJavaRefactorings.ENCAPSULATE_FIELD:
			break;
		case IJavaRefactorings.EXTRACT_CLASS:
			ExtractClassDescriptor extractClassDescriptor = (ExtractClassDescriptor) descriptor;
			extractClassDescriptor.setClassName((String) pair.addition);
			break;
		case IJavaRefactorings.EXTRACT_CONSTANT:
		case IJavaRefactorings.EXTRACT_INTERFACE:
		case IJavaRefactorings.EXTRACT_LOCAL_VARIABLE:
		case IJavaRefactorings.EXTRACT_METHOD:
			break;
		case IJavaRefactorings.EXTRACT_SUPERCLASS:
			break;
		case IJavaRefactorings.GENERALIZE_TYPE:
		case IJavaRefactorings.INFER_TYPE_ARGUMENTS:
		case IJavaRefactorings.INLINE_CONSTANT:
		case IJavaRefactorings.INLINE_LOCAL_VARIABLE:
		case IJavaRefactorings.INLINE_METHOD:
		case IJavaRefactorings.INTRODUCE_FACTORY:
		case IJavaRefactorings.INTRODUCE_INDIRECTION:
		case IJavaRefactorings.INTRODUCE_PARAMETER:
		case IJavaRefactorings.INTRODUCE_PARAMETER_OBJECT:
			break;
		case IJavaRefactorings.MOVE:
			MoveDescriptor moveDescriptor = (MoveDescriptor) descriptor;
			moveDescriptor.setDestination((IJavaElement) pair.addition);
			moveDescriptor.setUpdateReferences(true);
			moveDescriptor.setUpdateQualifiedNames(true);
			if (pair.element instanceof IMember) {
				IMember[] members = { (IMember) pair.element };
				moveDescriptor.setMoveMembers(members);
			} else if (pair.element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot[] roots = { (IPackageFragmentRoot) pair.element };
				moveDescriptor.setMovePackageFragmentRoots(roots);
			} else if (pair.element instanceof IPackageFragment) {
				IPackageFragment[] packages = { (IPackageFragment) pair.element };
				moveDescriptor.setMovePackages(packages);
			} else if (pair.element instanceof IResource) {
				// Do not support
			} else {
				// Do nothing
			}
			break;
		case IJavaRefactorings.MOVE_METHOD:
			break;
		case IJavaRefactorings.MOVE_STATIC_MEMBERS:
			break;
		case IJavaRefactorings.PULL_UP:
			break;
		case IJavaRefactorings.PUSH_DOWN:
		default:
			break;
		}

		return descriptor;
	}
}
