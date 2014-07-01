/**
 * 
 */
package codeimp.refactoring;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author chuxuankhoi
 * 
 */
public class CodeImpRefactoring {

	private IJavaElement element;
	private String action;
	private IResource project;

	/**
	 * @param element
	 * @param action
	 * @param project
	 */
	public CodeImpRefactoring(IJavaElement element, String action,
			IResource project) {
		this.element = element;
		this.action = action;
		this.project = project;
	}

	/**
	 * 
	 * @param element
	 * @param action
	 * @throws CoreException
	 */
	public void process(IUndoManager undoMan) {
		// Generate refactoring actions
		RefactoringPair pair = new RefactoringPair();
		pair.element = element;
		pair.action = action;

		// Run the generated actions
		try {
			refactorElement(pair, undoMan);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void refactorElement(RefactoringPair pair, IUndoManager undoMan)
			throws CoreException {
		if (pair == null) {
			return;
		}
		if (pair.element == null || pair.action == null) {
			return;
		}
		System.out.println("Refator " + pair.element.getElementName() + " by "
				+ pair.action);
		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(pair.action);

		JavaRefactoringDescriptor descriptor = createDescriptor(pair,
				contribution);

		RefactoringStatus status = new RefactoringStatus();

		Refactoring refactoring = descriptor.createRefactoring(status);
		IProgressMonitor monitor = new NullProgressMonitor();
		refactoring.checkInitialConditions(monitor);
		refactoring.checkFinalConditions(monitor);
		Change change = refactoring.createChange(monitor);
		undoMan.aboutToPerformChange(change);
		Change fUndoChange = change.perform(new SubProgressMonitor(monitor, 9));
		undoMan.changePerformed(change, true);
		change.dispose();
		fUndoChange
				.initializeValidationData(new SubProgressMonitor(monitor, 1));
		undoMan.addUndo(refactoring.getName(), fUndoChange);
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
		descriptor.setProject(project.getName());

		// TODO Complete refactoring list
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
			renameDescriptor.setNewName(pair.addition == null ? pair.element
					.getElementName() + "NewName" : (String) pair.addition);
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
			extractClassDescriptor.setClassName(pair.element.getElementName()
					+ "Class");
			extractClassDescriptor.setCreateTopLevel(true);
			extractClassDescriptor.setType((IType) pair.element.getParent());
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
