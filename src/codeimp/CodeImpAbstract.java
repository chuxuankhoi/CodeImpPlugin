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

	public double calCurrentScore() {
		if(codeSelection == null || sourceFile == null) {
			printLog("calCurrentScore - Lack of information about the experiment.");
			return 0;
		}
		IJavaElement[] elements = null;
		try {
			elements = CodeImpUtils.identifyElements(
					codeSelection.getText(), sourceFile);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
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

	protected abstract double scoreElement(IJavaElement element);

	public String getRefactoringHistory() {
		if (undoMan != null) {
			return ((CodeImpUndoManager) undoMan).getCurrentUndoListString();
		}
		return "";
	}

	/**
	 * Get refactoring actions' result in printable format
	 * @return
	 */
	public abstract String getPrintedResults();

}