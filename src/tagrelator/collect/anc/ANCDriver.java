package tagrelator.collect.anc;

public class ANCDriver {

	/**
	 * @param args argume is path to OANC folder
	 */
	public static void main(String[] args) {
		
		String oancFolder = "";
		
		if(args[0] != null){
			oancFolder = args[0];
		}
		
		ANCprocessor myproc = new ANCprocessor(oancFolder, "","","");
		myproc.process();
		
	}

}
