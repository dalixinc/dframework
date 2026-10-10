package scratch;


import junit.runner.Version;

/**
 * Way to determine exact junit version
 * 
 * @author Dale.Macdonald
 *
 */
public class GetJunitVersion {
	public static void main(String[] args) {
		System.out.println("JUnit version is: " + Version.id());
	}
}
