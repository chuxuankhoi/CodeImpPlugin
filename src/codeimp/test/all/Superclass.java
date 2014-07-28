package codeimp.test.all;

public class Superclass {
	
	private int superAttr1;
	private double superAttr2;
	protected boolean superAttr3;
	
	public void superMethod1() {
		if(superMethod2()) {
			System.out.println(superAttr1);
		}else
			System.out.println(superAttr2);
	}

	protected boolean superMethod2() {
		return superAttr3;
	}
	
	
}