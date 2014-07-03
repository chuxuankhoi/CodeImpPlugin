/**
 * 
 */
package codeimp;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.graders.IGrader;
import codeimp.graders.LCOM1Score;
import codeimp.graders.LongMethodGrader;
import codeimp.refactoring.CodeImpRefactoring;
import codeimp.refactoring.RefactoringPair;
import codeimp.undo.CodeImpUndoManager;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImpHillClimbing {

	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected IUndoManager undoMan;

	private void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	public CodeImpHillClimbing(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}

	public void runImprovement() {
		try {
			undoMan = new CodeImpUndoManager();
			String[] actionList = CodeImpUtils.getRefactoringActions();
			if (actionList == null || actionList.length == 0) {
				printLog("No refactoring action found.");
				return;
			}
			double curScore = calCurrentScore();
			double oldScore = curScore;
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
					RefactoringPair[] pairs = getRefactoringPairs(
							actionList[i], rootElements[j]);
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

	/**
	 * Look for the objects that the given refactoring action can be applied to
	 * 
	 * @param action
	 *            determine the refactoring action, must be listed in
	 *            {@link IJavaRefactorings}
	 * @param rootElement
	 *            scope to look for the objects
	 * @return list of refactoring pair which describes all required items for
	 *         the refactoring action
	 */
	private RefactoringPair[] getRefactoringPairs(String action,
			IJavaElement rootElement) {
		// TODO Build list of refactoring pairs for the action
		return null;
	}

	public double calCurrentScore() throws JavaModelException {
		IJavaElement[] elements = CodeImpUtils.identifyElements(
				codeSelection.getText(), sourceFile);
		if (elements == null) {
			printLog("calCurrentScore - No element found by identifier.");
			return 0;
		}
		double score = 0;
		for (IJavaElement element : elements) {
			double elementScore = scoreElement(element);
			printLog("calCurrentScore - Element: " + element.getElementName()
					+ " - Type: " + element.getElementType() + " - Score: "
					+ elementScore);
			score += elementScore;
		}
		return score;
	}

	private double scoreElement(IJavaElement element) throws JavaModelException {
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

	public String getRefactoringHistory() {
		if (undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return null;
	}

}
