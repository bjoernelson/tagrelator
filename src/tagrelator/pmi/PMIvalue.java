package tagrelator.pmi;

import java.util.Arrays;

/**a simple holder class for a context word and its corresponding PMI value
 * implements Comparable*/
public class PMIvalue implements Comparable<PMIvalue>{
	
	private final String word;
	private final double pmiVal;
	
	@Override
	public int compareTo(PMIvalue aVal) {
		
		if( this.word.equals(aVal.getWord())){
			return 0;
		}
		else if(  (Double.compare(this.pmiVal, aVal.getPmiVal())) == 0){
			return this.word.compareTo(aVal.getWord()); 
		}
		else{
			return Double.compare(this.pmiVal, aVal.getPmiVal());
		}
	}
	
	/**--------------old one------------------
	 *	@Override
	public int compareTo(PMIvalue aVal) {
		
		if(this.word.equals(aVal.getWord())){
			return 0;
		}
		if(this.pmiVal == Double.NaN){
			return 1;
		}
		if(aVal.getPmiVal() > this.pmiVal){
			return 1;
		}
		if(aVal.getPmiVal() < this.pmiVal){
			return -1;
		}
			
		if( aVal.getPmiVal().equals(this.pmiVal)){
			if(aVal.getWord().equals(this.word)){
				return 0;
			}
			else{
				for(int i = 0; i < )
					aVal.getWordasChars().equals(this.word));
				char a = "a".toCharArray()[0];
				
			}
			
		}
		
		return -1;
	}
	 */
	
	///////////////////
	//Constructor
	public PMIvalue(String aWord, double aPmiVal) {
		this.word = aWord;
		this.pmiVal = aPmiVal;
		
	}
	
	//////////
	//getter
	public final String getWord(){
		return new String(this.word);
	}
	
	public final char[] getWordasChars(){
		return this.word.toCharArray();
	}
	
	public final double getPmiVal(){
		return pmiVal;
	}
	
};
