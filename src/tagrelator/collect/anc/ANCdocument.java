package tagrelator.collect.anc;

import java.io.Serializable;

/**Data holder Class consists of a String Array of words of the document it represents
 * */
public class ANCdocument implements Serializable{
	public final String[] document;
	
	public ANCdocument(String[] wordArr){
		this.document = wordArr;
	}
	
	public final String[] getWordArr(){
		return document;
	}
	
}
