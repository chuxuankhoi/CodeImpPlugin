/**
 * 
 */
package codeimp;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import de.tobject.findbugs.reporter.Reporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;

/**
 * @author chuxuankhoi
 *
 */
public class CodeImpPlgReporter extends Reporter {
	
	private final ArrayList<BugInstance> bugsList = new ArrayList<BugInstance>();

	public CodeImpPlgReporter(IJavaProject project, Project findBugsProject,
			IProgressMonitor monitor) {
		super(project, findBugsProject, monitor);
	}

	/* (non-Javadoc)
	 * @see de.tobject.findbugs.reporter.Reporter#doReportBug(edu.umd.cs.findbugs.BugInstance)
	 */
	@Override
	protected void doReportBug(BugInstance bug) {
		bugsList.add(bug);
		super.doReportBug(bug);
	}

	public ArrayList<RefactoringPair> getRefactoringPairs() {
		// TODO Auto-generated method stub
		return null;
	}

}
