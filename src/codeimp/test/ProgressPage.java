<<<<<<< HEAD:src/codeimp/test/ProgressPage.java
package codeimp.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import codeimp.graders.EmptyClass;
import codeimp.graders.InheritedRatio;
import codeimp.graders.LCOM2;
import codeimp.graders.LCOM5;
import codeimp.graders.SharedMethods;
import codeimp.graders.SharedMethodsInChildren;
import codeimp.graders.TCC;
import codeimp.wizards.CodeImpProgressBar;

public class ProgressPage extends WizardPage {

	protected abstract class CancellableThread extends Thread {
		protected volatile boolean isCancelled = false;
		protected CodeImpProgressBar progressBar = null;

		CancellableThread(CodeImpProgressBar progressBar) {
			this.progressBar = progressBar;
		}

		public abstract void run();

		public void cancel() {
			isCancelled = true;
		}
	}

	private Composite container = null;
	protected int processBarStyle = SWT.SMOOTH; // process bar style
	private Display display = null;
	private CancellableThread thread = null;
	private CodeImpProgressBar progressBar = null;

	public ProgressPage(String pageName) {
		super(pageName);
		setTitle(pageName);
	}

	public ProgressPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
				true, false));
		GridLayout gLayout = new GridLayout();
		container.setLayout(gLayout);
		display = container.getDisplay();

		Composite progressBarComposite = new Composite(container, SWT.NONE);
		progressBarComposite.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));
		progressBarComposite.setLayout(new FillLayout());

		// progressBar = new ProgressBar(progressBarComposite, processBarStyle);
		progressBar = new CodeImpProgressBar(progressBarComposite,
				processBarStyle, display);
		progressBar.setParentPage(this);

		Composite minorProgressComposite = new Composite(container,
				processBarStyle);
		minorProgressComposite.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));
		minorProgressComposite.setLayout(new FillLayout());
		final CodeImpProgressBar minorProgressBar = new CodeImpProgressBar(
				minorProgressComposite, processBarStyle, display);

		setControl(container);

		thread = new CancellableThread(progressBar) {

			@Override
			public void run() {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot wsRoot = workspace.getRoot();
				IProject project = wsRoot.getProject("CodeImpGradersTestData");
				if (project == null) {
					System.out.println("Data project have not be opened.");
					return;
				}
				IJavaProject jProject = JavaCore.create(project);
				IJavaElement[] packages = null;
				try {
					IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
					for(IPackageFragmentRoot root:roots) {
						if(root.isExternal()) {
							continue;
						}
						System.out.println("Root: " + root.getElementName());
						packages = root.getChildren();
						for(IJavaElement e:packages) {
							System.out.println("Child: " + e.getElementName());
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
					return;
				}
				System.out.println("packages length: " + packages.length);
				progressBar.setMaximum(packages.length);
				int count = 1;
				for (IJavaElement pkg : packages) {
					try {
						if (((IPackageFragment) pkg).getKind() == IPackageFragmentRoot.K_SOURCE) {
							ICompilationUnit[] units = ((IPackageFragment) pkg)
									.getCompilationUnits();
							minorProgressBar.setMaximum(units.length);
							minorProgressBar.setSelection(0);
							int minorCount = 0;
							for (ICompilationUnit unit : units) {
								IResource res = unit.getCorrespondingResource();
								if(!(res instanceof IFile)) {
									System.out.println("No file is found");
									return;
								}
								System.out.println("Start " + unit.getElementName());
								IType[] types = unit.getAllTypes();
								for(IType type:types) {
									if(isCancelled) {
										return;
									}
									if(type.getElementName().equals("Child1")) {
									LCOM2 grader1 = new LCOM2(type, (IFile) res);
									System.out.println("LCOM2: " + grader1.getScore());
									LCOM5 grader2 = new LCOM5(type, (IFile) res);
									System.out.println("LCOM5: " + grader2.getScore());
									TCC grader3 = new TCC(type, (IFile) res);
									System.out.println("TCC: " + grader3.getScore());									
									InheritedRatio grader4 = new InheritedRatio(type);
									System.out.println("InheritedRatio: " + grader4.getScore());
									SharedMethodsInChildren grader5 = new SharedMethodsInChildren(type);
									System.out.println("SharedMethodsInChildren: " + grader5.getScore());
									SharedMethods grader6 = new SharedMethods(type);
									System.out.println("SharedMethods: " + grader6.getScore());
									EmptyClass grader7 = new EmptyClass(type);
									System.out.println("EmptyClass: " + grader7.getScore());
									}
								}
								minorCount++;
								minorProgressBar.setSelection(minorCount);
								
							}							
						}
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
					progressBar.setSelection(count);
					count++;
				}
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						setPageComplete(true);
					}
				});
				
			}

		};
		thread.start();

		setPageComplete(false);
	}

	@Override
	public boolean canFlipToNextPage() {
		if (progressBar.getSelection() < progressBar.getMaximum()) {
			return false;
		} else {
			return super.canFlipToNextPage();
		}
	}

	public void tryCancel() {
		if (thread != null) {
			thread.cancel();
		}
	}

}
=======
package codeimp.test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import codeimp.graders.EmptyClass;
import codeimp.graders.InheritedRatio;
import codeimp.graders.LCOM2;
import codeimp.graders.LCOM5;
import codeimp.graders.SharedMethods;
import codeimp.graders.SharedMethodsInChildren;
import codeimp.graders.TCC;
import codeimp.wizards.CodeImpProgressBar;

public class ProgressPage extends WizardPage {

	protected abstract class CancellableThread extends Thread {
		protected volatile boolean isCancelled = false;
		protected CodeImpProgressBar progressBar = null;

		CancellableThread(CodeImpProgressBar progressBar) {
			this.progressBar = progressBar;
		}

		public abstract void run();

		public void cancel() {
			isCancelled = true;
		}
	}

	private Composite container = null;
	protected int processBarStyle = SWT.SMOOTH; // process bar style
	private Display display = null;
	private CancellableThread thread = null;
	private CodeImpProgressBar progressBar = null;

	public ProgressPage(String pageName) {
		super(pageName);
		setTitle(pageName);
	}

	public ProgressPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
				true, false));
		GridLayout gLayout = new GridLayout();
		container.setLayout(gLayout);
		display = container.getDisplay();

		Composite progressBarComposite = new Composite(container, SWT.NONE);
		progressBarComposite.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));
		progressBarComposite.setLayout(new FillLayout());

		// progressBar = new ProgressBar(progressBarComposite, processBarStyle);
		progressBar = new CodeImpProgressBar(progressBarComposite,
				processBarStyle, display);
		progressBar.setParentPage(this);

		Composite minorProgressComposite = new Composite(container,
				processBarStyle);
		minorProgressComposite.setLayoutData(new GridData(GridData.FILL,
				GridData.CENTER, true, false));
		minorProgressComposite.setLayout(new FillLayout());
		final CodeImpProgressBar minorProgressBar = new CodeImpProgressBar(
				minorProgressComposite, processBarStyle, display);

		setControl(container);

		thread = new CancellableThread(progressBar) {

			@Override
			public void run() {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot wsRoot = workspace.getRoot();
				IProject project = wsRoot.getProject("CodeImpGradersTestData");
				if (project == null) {
					System.out.println("Data project have not be opened.");
					return;
				}
				IJavaProject jProject = JavaCore.create(project);
				IJavaElement[] packages = null;
				try {
					IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
					for(IPackageFragmentRoot root:roots) {
						if(root.isExternal()) {
							continue;
						}
						System.out.println("Root: " + root.getElementName());
						packages = root.getChildren();
						for(IJavaElement e:packages) {
							System.out.println("Child: " + e.getElementName());
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
					return;
				}
				System.out.println("packages length: " + packages.length);
				progressBar.setMaximum(packages.length);
				int count = 1;
				for (IJavaElement pkg : packages) {
					try {
						if (((IPackageFragment) pkg).getKind() == IPackageFragmentRoot.K_SOURCE) {
							ICompilationUnit[] units = ((IPackageFragment) pkg)
									.getCompilationUnits();
							minorProgressBar.setMaximum(units.length);
							minorProgressBar.setSelection(0);
							int minorCount = 0;
							for (ICompilationUnit unit : units) {
								IResource res = unit.getCorrespondingResource();
								if(!(res instanceof IFile)) {
									System.out.println("No file is found");
									return;
								}
								System.out.println("Start " + unit.getElementName());
								IType[] types = unit.getAllTypes();
								for(IType type:types) {
									if(isCancelled) {
										return;
									}
									if(type.getElementName().equals("Child1")) {
									LCOM2 grader1 = new LCOM2(type, (IFile) res);
									System.out.println("LCOM2: " + grader1.getScore());
									LCOM5 grader2 = new LCOM5(type, (IFile) res);
									System.out.println("LCOM5: " + grader2.getScore());
									TCC grader3 = new TCC(type, (IFile) res);
									System.out.println("TCC: " + grader3.getScore());									
									InheritedRatio grader4 = new InheritedRatio(type);
									System.out.println("InheritedRatio: " + grader4.getScore());
									SharedMethodsInChildren grader5 = new SharedMethodsInChildren(type);
									System.out.println("SharedMethodsInChildren: " + grader5.getScore());
									SharedMethods grader6 = new SharedMethods(type);
									System.out.println("SharedMethods: " + grader6.getScore());
									EmptyClass grader7 = new EmptyClass(type);
									System.out.println("EmptyClass: " + grader7.getScore());
									}
								}
								minorCount++;
								minorProgressBar.setSelection(minorCount);
								
							}							
						}
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
					progressBar.setSelection(count);
					count++;
				}
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						setPageComplete(true);
					}
				});
				
			}

		};
		thread.start();

		setPageComplete(false);
	}

	@Override
	public boolean canFlipToNextPage() {
		if (progressBar.getSelection() < progressBar.getMaximum()) {
			return false;
		} else {
			return super.canFlipToNextPage();
		}
	}

	public void tryCancel() {
		if (thread != null) {
			thread.cancel();
		}
	}

}
>>>>>>> origin/master:src/codeimp/test/ProgressPage.java
