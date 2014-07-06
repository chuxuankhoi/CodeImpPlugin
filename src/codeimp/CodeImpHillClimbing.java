/**
 * 
 */
package codeimp;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.graders.IGrader;
import codeimp.graders.LCOM1Score;
import codeimp.graders.LongMethodGrader;
import codeimp.refactoring.CodeImpRefactoring;
import codeimp.refactoring.CodeImpRefactoringManager;
import codeimp.refactoring.RefactoringPair;
import codeimp.undo.CodeImpUndoManager;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImpHillClimbing extends CodeImpAbstract {

	public CodeImpHillClimbing(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}

	public void runImprovement() {
		try {
			if (codeSelection == null || sourceFile == null) {
				printLog("runImprovement - Lack of information about the experiment.");
				return;
			}
			double curScore = calCurrentScore();
			if (curScore <= 0) {
				printLog("Plug-in cannot judge the source code.");
				return;
			}
			double oldScore = curScore;
			if (undoMan == null)
				undoMan = new CodeImpUndoManager();
			CodeImpRefactoringManager refManager = CodeImpRefactoringManager.getManager();
			String[] actionList = refManager.getActionsList();
			if (actionList == null || actionList.length == 0) {
				printLog("No refactoring action found.");
				return;
			}
			IJavaElement[] rootElements = CodeImpUtils.identifyElements(
					codeSelection.getText(), sourceFile);
			if (rootElements == null) {
				printLog("Unexpected returned.");
				return;
			}
			if (rootElements.length == 0) {
				printLog("No item identified.");
				return;
			}
			System.out.println("Refatored elements:");
			for (int i = 0; i < rootElements.length; i++) {
				System.out.println("\t" + rootElements[i].getElementName()
						+ " - " + rootElements[i].getElementType());
			}

			for (int i = 0; i < actionList.length; i++) {
				for (int j = 0; j < rootElements.length; j++) {
					RefactoringPair[] pairs = refManager.getRefactoringPairs(
							rootElements[j], actionList[i], sourceFile);
					if (pairs == null) {
						continue;
					}
					for (int k = 0; k < pairs.length; k++) {
						CodeImpRefactoring refactoring = new CodeImpRefactoring(
								pairs[i], sourceFile.getProject());
						refactoring.process(undoMan);
						curScore = calCurrentScore();
						if (curScore < oldScore) {
							oldScore = curScore;
							continue;
						} else {
							undoMan.performUndo(null, null);
							if (curScore > oldScore) {
								break;
							}
						}
					}
				}
			}
			printLog("Improvement completed. Final score: " + curScore);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected double scoreElement(IJavaElement element)
			throws JavaModelException {
		// TODO Select appropriate scoring function for element based on its
		// type
		IGrader grader = null;
		if (element instanceof IType) {
			if (CodeImpUtils.isInProject((IType) element,
					sourceFile.getProject())) {
				grader = new LCOM1Score((IType) element, sourceFile);

			}
		} else if (element instanceof IMethod) {
			if (CodeImpUtils.isInProject((IMethod) element,
					sourceFile.getProject())) {
				grader = new LongMethodGrader((IMethod) element);
			}
		}

		return grader == null ? 0 : grader.getScore();
	}

}
