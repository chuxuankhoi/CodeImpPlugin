/**
 * 
 */
package codeimp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchWindow;

import codeimp.graders.EmptyClass;
import codeimp.graders.IGrader;
import codeimp.graders.InheritedRatio;
import codeimp.graders.LCOM2;
import codeimp.graders.LCOM5;
import codeimp.graders.LongMethod;
import codeimp.graders.SharedMethods;
import codeimp.graders.SharedMethodsInChildren;
import codeimp.graders.TCC;
import codeimp.graders.test.ScoresCollection;
import codeimp.refactoring.CodeImpRefactoring;
import codeimp.refactoring.CodeImpRefactoringManager;
import codeimp.refactoring.RefactoringPair;
import codeimp.undo.CodeImpUndoManager;
import codeimp.wizards.CodeImpProgressBar;

/**
 * @author chuxuankhoi
 * 
 *         Improve selected code using hill-climbing algorithm
 * 
 */
public class CodeImpHillClimbing extends CodeImpAbstract {

	private class SharedClass {
		private HashMap<String, EffectiveRefactorings> effectiveRefactorings = null;
	}

	private Map<String, Double> results = new HashMap<String, Double>();
	private double startScore = 0;
	private CancellableThread thread = null;
	private int curAction = 0;
	private CodeImpRefactoringManager refManager = null;
	protected final SharedClass sharedData = new SharedClass();

	public CodeImpHillClimbing(ITextSelection selectedCode, IFile file,
			IWorkbenchWindow currentWindow) {
		codeSelection = selectedCode;
		sourceFile = file;
		window = currentWindow;
		refManager = CodeImpRefactoringManager.getManager();
		if (sharedData.effectiveRefactorings == null) {
			sharedData.effectiveRefactorings = new HashMap<String, CodeImpAbstract.EffectiveRefactorings>();
		}
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public synchronized void runImprovement(CodeImpProgressBar bar) {
		thread = new CancellableThread(bar) {

			@Override
			public synchronized void run() {
				if (codeSelection == null || sourceFile == null) {
					printLog("runImprovement - Lack of information about the experiment.");
					return;
				}
				sharedData.effectiveRefactorings.clear();
				int successfullRefactoring = 0; // for reporting
				int totalRefactoring = 0; // for reporting
				boolean reachOptimal = false; // avoid cases that the score is
												// increased before reaching the
												// optimal

				ScoresCollection.getActionsList().add("initialize");
				startScore = calCurrentScore();
				if (startScore <= 0) {
					printLog("Plug-in cannot judge the source code.");
					return;
				}
				double oldScore = 0;

				if (undoMan == null)
					undoMan = new CodeImpUndoManager();

				String[] actionList = refManager.getActionsList();
				if (actionList == null || actionList.length == 0) {
					printLog("No refactoring action found.");
					return;
				}
				IJavaElement[] rootElements = null;
				try {
					rootElements = CodeImpUtils.identifyElements(
							codeSelection.getText(), sourceFile);
				} catch (JavaModelException e1) {
					printLog(e1.toString());
				}
				if (rootElements == null) {
					printLog("Unexpected returned.");
					return;
				}
				if (rootElements.length == 0) {
					printLog("No item identified.");
					return;
				}

				// Start hill-climbing analysis
				bar.setMaximum(actionList.length);
				for (int i = 0; i < actionList.length; i++) {
					if (isCancelled) {
						finishAction(actionList[i], bar);
						return;
					}
					reachOptimal = false;
					curAction = i;
					ScoresCollection.getActionsList().add("get old score");
					oldScore = calCurrentScore();
					EffectiveRefactorings savedRefactoring = new EffectiveRefactorings(
							actionList[i]);
					sharedData.effectiveRefactorings.put(actionList[i],
							savedRefactoring);
					for (int j = 0; j < rootElements.length; j++) {
						if (isCancelled) {
							finishAction(actionList[i], bar);
							return;
						}
						RefactoringPair[] pairs = refManager
								.getRefactoringPairs(rootElements[j],
										actionList[i]);
						if (pairs == null) {
							continue;
						}
						// CodeImpUtils.shuffleArray(pairs);
						printLog("Pairs number: " + pairs.length);
						for (int k = 0; k < pairs.length; k++) {
							if (isCancelled) {
								finishAction(actionList[i], bar);
								return;
							}
							if (pairs[k].element instanceof IJavaElement) {
								printLog("Trying "
										+ actionList[i]
										+ " with root "
										+ rootElements[j].getElementName()
										+ " pair element: "
										+ ((IJavaElement) pairs[k].element)
												.getElementName());
							} else if (pairs[k].action == IJavaRefactorings.EXTRACT_METHOD) {
								printLog("Trying " + actionList[i]
										+ " with root "
										+ rootElements[j].getElementName()
										+ " pair of "
										+ (String) pairs[k].element);
							}
							CodeImpRefactoring refactoring = new CodeImpRefactoring(
									pairs[k], sourceFile);
							totalRefactoring++;
							boolean success = false;
							try {
								success = refactoring.process(undoMan,
										new NullProgressMonitor());
							} catch (Exception e) {
								continue;
							}
							successfullRefactoring++;
							try {
								sourceFile.refreshLocal(IFile.DEPTH_INFINITE,
										null);
							} catch (CoreException e) {
							}
							ScoresCollection.getActionsList().add(actionList[i]);
							double curScore = calCurrentScore();
							if (curScore < oldScore) {
								savedRefactoring.put(pairs[k],
										(oldScore - curScore));
								oldScore = curScore;
								reachOptimal = true;
								continue;
							} else {
								if (success) {
									IProgressMonitor undoMonitor = new NullProgressMonitor();
									try {
										undoMan.performUndo(null, undoMonitor);
									} catch (CoreException e) {
									}
								}
								if (curScore > oldScore && reachOptimal) {
									printLog("Current score is greater than the old score");
									finishAction(actionList[i], bar);
									break;
								}

							}
						}
					}
					finishAction(actionList[i], bar);
				}
				refManager.deleteTmpClass();
				ScoresCollection.getActionsList().add("finalizing analysis");
				printLog("Improvement completed. Final score: "
						+ calCurrentScore());
				printLog("Successfull refactoring: " + successfullRefactoring
						+ "/" + totalRefactoring);
				ScoresCollection.exportCSV("scores_"
						+ System.currentTimeMillis() + ".csv");
			}

		};
		thread.start();
	}

	/**
	 * Get the improvement of the refactoring action and undo all refactoring
	 * 
	 * @param action
	 */
	private void finishAction(String action, final CodeImpProgressBar bar) {
		ScoresCollection.getActionsList().add("finalizing action");
		double curScore = calCurrentScore();
		double impScore = startScore - curScore;
		results.put(action, new Double(impScore));

		while (undoMan.anythingToUndo()) {
			try {
				undoMan.performUndo(null, null);
				Thread.sleep(500);
			} catch (CoreException | InterruptedException e) {
			}
		}
		if (bar != null) {
			bar.setSelection(curAction + 1);
		}
	}

	/**
	 * @see CodeImpAbstract
	 * Weight-sum approach
	 */
	protected double scoreElement(IJavaElement element) {
		ScoresCollection.initialize();
		IGrader grader = null;
		double score = 0;
		if (element instanceof IType) {
			if (CodeImpUtils.isInProject((IType) element,
					sourceFile.getProject())) {
				grader = new LCOM2((IType) element, sourceFile);
				ScoresCollection.getList(0).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
				grader = new LCOM5((IType) element, sourceFile);
				ScoresCollection.getList(1).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
				grader = new TCC((IType) element, sourceFile);
				ScoresCollection.getList(2).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
				grader = new InheritedRatio((IType) element);
				ScoresCollection.getList(3).add(grader.getScore() * 0.5);
				score += grader == null ? 0 : (0.5 * grader.getScore());
				grader = new SharedMethodsInChildren((IType) element);
				ScoresCollection.getList(4).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
				grader = new SharedMethods((IType) element);
				ScoresCollection.getList(5).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
				grader = new EmptyClass((IType) element);
				ScoresCollection.getList(6).add(grader.getScore());
				score += grader == null ? 0 : grader.getScore();
			}
		} else if (element instanceof IMethod) {
			if (CodeImpUtils.isInProject((IMethod) element,
					sourceFile.getProject())) {
				grader = new LongMethod((IMethod) element);
				score += grader == null ? 0 : grader.getScore();
			}
		}

		return score;
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public String getPrintedResults() {
		String ret = "";
		Map<String, Double> sortedResults = CodeImpUtils.sortByComparator(
				results, CodeImpUtils.DESC);
		Iterator<Entry<String, Double>> it = sortedResults.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, Double> e = it.next();
			String result = e.getKey() + " - " + e.getValue().toString();
			ret += result;
			ret += "\n";
		}
		return ret;
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public Map<String, Double> getResults() {
		return results;
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public void cancel() {
		if (thread != null && thread.isAlive()) {
			thread.cancel();
		}
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public HashMap<String, Double> getEffectiveList(String action) {
		if (sharedData.effectiveRefactorings != null) {
			EffectiveRefactorings refactorings = sharedData.effectiveRefactorings
					.get(action);
			if (refactorings == null) {
				return null;
			}
			return refactorings.getRefactoringMap();
		} else {
			return null;
		}
	}

	/**
	 * @see CodeImpAbstract
	 */
	@Override
	public RefactoringPair[] getEffectivePairs(String action) {
		if (sharedData.effectiveRefactorings != null) {
			EffectiveRefactorings refactorings = sharedData.effectiveRefactorings
					.get(action);
			if (refactorings == null) {
				return null;
			}
			return refactorings.getRefactoringPairs();
		} else {
			return null;
		}
	}

}
