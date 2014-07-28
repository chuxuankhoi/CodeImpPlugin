package codeimp.test.inheritance;

public class SuperClass1 extends SuperClass2 {
	public String im11() {
		doNothing();
		return getName();
	}
	
	protected String getName() {
		return this.getClass().getName();
	}
	
	private void doNothing() {
		System.out.println("Do nothing");
	}
}
