package gov.nasa.ksc.itacl.Utilities;


public class Utils {	
	public static final long THREAD_SLEEP_TIME = 100;
	
	public static void info(String string) {
		System.out.println("INFO: " + string);
	}
	
	public static void warn(String string) {
		System.out.println("WARNING: " + string);
	}
	
	public static void error(String string) {
		System.out.println("ERROR: " + string);
	}
	
	public static void log(String string) {
		System.out.print("GEN LOG: " + string);
		System.out.flush();
	}
}
