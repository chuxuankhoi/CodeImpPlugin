package codeimp.test.inheritance;

public class SubClass00 extends GivenClass {
	
	public void sharedMethod() {
		System.out.println("Shared");
	}

	@Override
	public void m1() {
		if (f2 == 0) {
			System.out.println("No output");
		} else {
			System.out.println("f2 is " + f2);
		}
	}
}
