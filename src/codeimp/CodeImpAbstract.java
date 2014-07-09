package codeimp;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.undo.CodeImpUndoManager;

public abstract class CodeImpAbstract {

	protected IWorkbenchWindow window;
	protected ITextSelection codeSelection;
	protected IFile sourceFile;
	protected IUndoManager undoMan = null;

	public CodeImpAbstract() {
		super();
	}

	protected void printLog(String log) {
		CodeImpUtils.printLog(this.getClass().getName() + " - " + log);
	}

	public abstract void runImprovement();

	public double calCurrentScore() throws JavaModelException {
		if(codeSelection == null || sourceFile == null) {
			printLog("calCurrentScore - Lack of information about the experiment.");
			return 0;
		}
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

	protected abstract double scoreElement(IJavaElement element) throws JavaModelException;

	public String getRefactoringHistory() {
		if (undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return "";
	}

}