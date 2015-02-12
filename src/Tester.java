
import org.thoughtcrime.Utility;

public class Tester {

	public static void main(String[] args) {
		if (args.length == 0) {
			showUsage();
			return;
		}
		
		Utility util = new Utility(args[0]);
		
		util.printFiles();
	}

	public static void showUsage() {
		System.out.println("Usage: java -cp . Tester DIRECTORY");
	}
}
