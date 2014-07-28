package codeimp.test.all;

public class Brother3 extends Superclass {
	
	protected static final int THRESHOLD = 1;
	
	protected void pullup1(String param1) {
		System.out.println(param1);
	}
	
	public boolean brother3Method1(int value) {
		return (value > THRESHOLD);
	}
}
