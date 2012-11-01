import org.springframework.util.MethodInvoker;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Test {

	public void test(String s, Integer i) {
		System.out.println("s = [" + s + "], i = [" + i + "]");
	}

	public static void main(String[] args) throws Exception {
		MethodInvoker invoker = new MethodInvoker();
		invoker.setTargetObject(new Test());
		invoker.setTargetMethod("test");
		invoker.setArguments(new Object[]{Integer.valueOf(1), "Hi"});
		invoker.prepare();
		invoker.invoke();
	}
}
