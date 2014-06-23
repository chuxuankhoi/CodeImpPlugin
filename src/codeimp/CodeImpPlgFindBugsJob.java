/**
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * 
 * Extended by Chu Xuan Khoi
 * Last modified: June, 2014
 */
package codeimp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.builder.WorkItem;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

public class CodeImpPlgFindBugsJob extends FindBugsJob {

	private final List<WorkItem> resources;
	private final IResource resource;
	private CodeImpPlgFindBugsWorker worker;
	
	public CodeImpPlgFindBugsJob(String name, IResource resource,
			List<WorkItem> resources, IWorkbenchPart targetPart) {
		super(name, resource);
		this.resources = resources;
		this.resource = resource;
	}

	@Override
	protected boolean supportsMulticore() {
		return MutexSchedulingRule.MULTICORE;
	}

	@Override
	protected void runWithProgress(IProgressMonitor monitor)
			throws CoreException {
		worker = new CodeImpPlgFindBugsWorker(resource, monitor);

		worker.work(resources);
		System.out.println("Results:");
		for (WorkItem item : resources) {
			System.out.println("\t" + item.getName());
		}
	}

	public ArrayList<RefactoringPair> getReport() {
		if(worker != null) {
			return worker.getReport();
		} else {
			return null;
		}
	}
}
