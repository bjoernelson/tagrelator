package tagrelator.pmi;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;


public class RawCounts implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 001444405112012L;
	
	HashMap<String, Integer> wordFreqs;
	HashMap<String, HashMap<String, Integer>> contextFreqs;
	//size of the corpus in words
	long corpusSize;
	/**holds how many context words have been collected
	 * */
	int contextSize;
	
	public RawCounts( HashMap<String, HashMap<String, Integer>> someContextFreqs, HashMap<String, Integer> someWordFreqs){
		this.wordFreqs = someWordFreqs;
		this.contextFreqs = someContextFreqs;
		this.corpusSize = computeCorpusSize();
		this.contextSize = 0;
	}
	
	public RawCounts(){
		this.wordFreqs = new HashMap<String, Integer>();
		this.contextFreqs = new HashMap<String, HashMap<String, Integer>>();
		this.corpusSize = 0;
		this.contextSize = 0;
	}
	
	/////////
	//getter
	/**returns the idependent word frequencies of the word in the corpus
	 * */
	public HashMap<String, Integer> getWordFreqs(){
		return new HashMap<String, Integer>(wordFreqs);
	}
	/**returns a {@link HashMap} with search terms as keys and a {@link TreeMap} of context word counts
	 * */
	public HashMap<String, HashMap<String, Integer>> getContextFreqs(){
		return new HashMap<String, HashMap<String, Integer>>(contextFreqs);
	}
	
	/**gets the size in words of the corpus
	 * */
	public long getCorpusSize(){
		return corpusSize;
	}
	
	/**returns how many different words are in this object
	 * */
	public Integer getTypeSize(){
		return wordFreqs.keySet().size();
	}
	
	/**returns the amount of collected context
	 *on first call value is computed
	 * */
	public int getContextSize(){
		int intermed = 0;
		
		if(this.contextSize == 0 && !(contextFreqs.isEmpty()) ){
			
			Iterator<HashMap<String, Integer>> i = contextFreqs.values().iterator();
			
			while(i.hasNext()){
				
				HashMap<String, Integer> map = i.next();
				for(Integer count : map.values()){
					intermed = intermed + count;
				}
			}
			this.contextSize = intermed;
			return intermed;
		}
		else{
			//since the values held by this class are only given in the Constructor and cannot be changed afterwards,
			//value after first computation can be returned safely.
			return new Integer(contextSize);
		}
	}
	
	/////////
	//setter
	/**for the needs of the textcorpora stats corpus size must be available to
	 * be set from the outside
	 * */
	public void setCorpusSize(Integer newCsize){
		corpusSize = newCsize;
	}
	
	/**returns size of the corpus 
	 *Attention: is computed on every call*/
	private long computeCorpusSize(){
		long cSize = 0L;
		//collect frequencies from all words, add them up
		if(!wordFreqs.isEmpty()){
			
			Collection<Integer> freqList =  wordFreqs.values();
			Iterator<Integer> freqIt = freqList.iterator();
			
			while(freqIt.hasNext()){
				cSize = cSize + freqIt.next();
			}
			return cSize;
		}
		else{
			return 0L;
		}
		
	}
	
	public void clear(){
		wordFreqs.clear();
		contextFreqs.clear();
	}
	
	
}
