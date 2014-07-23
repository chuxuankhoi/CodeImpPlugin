package codeimp.graders;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

public class SharedMethods extends SharedMethodsAbstract implements IGrader {
	
	public SharedMethods(IType type) {
		super(type);
	}

	@Override
	public double getScore() {
		IType[] brothers = null;
		try {
			brothers = getBrother(type);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if(brothers == null) {
			return 0;
		} else {
			return getScoreOfGroup(brothers);
		}
	}

	private IType[] getBrother(IType type) throws JavaModelException {
		ITypeHierarchy hierachy = type.newTypeHierarchy(new NullProgressMonitor());
		IType superClass = hierachy.getSuperclass(type);
		if(superClass == null) {
			return null;
		}		
		return hierachy.getSubtypes(superClass);
	}

}
