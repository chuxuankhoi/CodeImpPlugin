package codeimp.test.all;

public class Brother1 extends Superclass {
	
	protected double brother1Attr1;
	
	protected void pullup1(String param1) {
		System.out.println(param1);
	}
	
	public double brother1Method1(double value) {
		return brother1Attr1 + value;
	}
}
