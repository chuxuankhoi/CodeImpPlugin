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
import codeimp.undo.CodeImpUndoManager;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImp {

	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected IUndoManager undoMan;

	public static void printLog(String log) {
		System.out.println(System.currentTimeMillis() + " - CodeImp - " + log);
	}

	public CodeImp(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
	}

	public void runImprovement() {
		try {
			// undoMan = RefactoringCore.getUndoManager();
			undoMan = new CodeImpUndoManager();
			double curScore = calCurrentScore();
			double oldScore = curScore;
			IJavaElement[] refactoredElements = CodeImpUtils
					.getRefactoredElements(codeSelection.getText(), sourceFile);
			if (refactoredElements == null) {
				System.out.println("Unexpected returned.");
				return;
			}
			if (refactoredElements.length == 0) {
				System.out.println("No item identified.");
				return;
			}
			System.out.println("Refatored elements:");
			for (int i = 0; i < refactoredElements.length; i++) {
				System.out.println("\t"
						+ refactoredElements[i].getElementName() + " - "
						+ refactoredElements[i].getElementType());
			}

			for (int i = 0; i < refactoredElements.length; i++) {
				String[] actionList = getActionsList(refactoredElements[i]);
				if (actionList == null) {
					continue;
				}
				for (int j = 0; j < actionList.length; j++) {
					CodeImpRefactoring refactoring = new CodeImpRefactoring(
							refactoredElements[i], actionList[j],
							sourceFile.getProject());
					refactoring.process(undoMan);
					curScore = calCurrentScore();
					if (curScore < oldScore) {
						oldScore = curScore;
						continue;
					} else {
						undoMan.performUndo(null, null);
						if(curScore > oldScore) {
							break;
						}
					}
				}
			}
			printLog("Improvement completed. Final score: " + curScore);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String[] getActionsList(IJavaElement iJavaElement) {
		// TODO Complete the function with other types of elements
		String[] ret = null;
		if (iJavaElement instanceof IMethod) {
			ret = new String[1];
			ret[0] = IJavaRefactorings.EXTRACT_CLASS;
		}
		return ret;
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
		// TODO Get history from undoMan
		if(undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return null;
	}

}
