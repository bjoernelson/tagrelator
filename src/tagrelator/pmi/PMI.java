package tagrelator.pmi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.TreeSet;

import tagrelator.WordPair;
import tagrelator.read.CorpusReader;
import tagrelator.read.FlickrReader;

/**provides the computation of PMI values.<br>
 * a method for computing PMI values for a single word<br>
 * a method for computing PMI values for all word all at once<br>
 * a method for computing a mean PMI value for a pair of words, using the words as each others context<p>
 * the computation follows the Equation for PMI in the paper<br>
 * "Second Order Co-occurrence PMI for Determining the Semantic Similarity of Words" from<br>
 * @author Aminul Islam 
 * @author Diana Inkpen<br>
 * at time of programming available at <a href="http://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf">http://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf</a>
 * */
public class PMI {
	/**java provides no function to compute logarithm to base 2, this is a workaround
	 * */
	private static final double logTwo = Math.log(2.0); 
	/**multiply the corpussize  with this factor
	 * useful for Flickr Data since real Corpussize is unknown 
	 * */
	int boostFac;
	//////////////////
	//Constructor
	/**since the PMI class is only for computation needs, it has no member varables, and the constructor does basically nothing
	 * */
	public PMI(){
		//since its only for computing nothing has to be initiated
		this.boostFac = 1;
	}
	
	public PMI(int aBoost){
		//since its only for computing nothing has to be initiated
		this.boostFac = aBoost;
	}
	
	/**computes the PMI values for one word and all of its context words
	 * @param aWord the word as String
	 * @param contextFreqs the joint frequencies of the word and its context words,<br> 
	 * be sure to put the the right contextFreq for aWord in there  
	 * @param wordFreqs the independent word frequencies of all words in the corpus
	 * @param corpusSize the size of the used corpus in words
	 * @return a TreeMap sorted in descending oder by PMI values
	 * */
	public TreeSet<PMIvalue> pmiSingle( String aWord, HashMap<String, Integer> contextFreqs , HashMap<String, Integer> wordFreqs, long corpusSize ){
		
		//results
		TreeSet<PMIvalue> pmiVals = new TreeSet<PMIvalue>();
		
		//independent frequency of the searchterm
		Integer wFreq = wordFreqs.get(aWord);
		
		if(wFreq==null){
			System.err.println("PMI: the Searchterm "+aWord+" is not in word frequencies. empty result returned");
			
			return pmiVals;
		}
		
		//iterate over context words and compute PMI values for them
		Iterator<String> cWordIt = contextFreqs.keySet().iterator();
		
		while(cWordIt.hasNext()){
			
			//get context word as string
			String cWord = cWordIt.next();
			//get independent frequency of context word
			Integer cWordFreq = wordFreqs.get(cWord);
			//get joint frequency of word and context word
			Integer jointFreq = contextFreqs.get(cWord);
			
			//for errors and wrong usage
			if(cWordFreq==null){
				System.err.println("the context word "+cWord+" is not in word frequencies. assuming count 1");
				cWordFreq = 1;
			}
			if(jointFreq==null){
				//should not happen
				System.err.println("the context word "+cWord+" is not in context frequencies. no PMI value for it in result");
			}
			else{
				//compute the PMI value
				double pmiVal = pmiFormula(aWord, cWord, wFreq, cWordFreq, jointFreq, corpusSize);
				//System.out.println(cWord+" = "+pmiVal);
				Double pmiValDou = new Double(pmiVal);
				if( pmiValDou.equals(Double.NaN) || new Double(pmiVal).equals(Double.NEGATIVE_INFINITY) || pmiValDou.equals(Double.POSITIVE_INFINITY) ){
					//do nothing
				}
				else{
					pmiVals.add(new PMIvalue(cWord, pmiVal));
				}
				
			}
		}//end while
		
		//System.out.println("bubu i "+i+"\naaand "+pmiVals.size() );
		
		return pmiVals;
	}
	
	/**computes PMI values for words that are keys in the context map of the {@link RawCounts} parameter.
	 * @param someRawCounts Frequency counts which can be read in by the {@link FlickrReader} and {@link CorpusReader} classes from the according files 
	 * @return a map from words to Sets of {@link PMIvalue}s , which contain the context word and the computed PMI value of type {@link Double} 
	 * */
	public HashMap<String, TreeSet<PMIvalue>> pmiAll(RawCounts someRawCounts){
		System.out.println("computing PMI values");
		//results
		HashMap<String, TreeSet<PMIvalue>> pmiVals = new HashMap<String, TreeSet<PMIvalue>>(); 
		
		//get iterator over keyset of the context freqs
		//keys are the words
		Iterator<String> termIt  = someRawCounts.getContextFreqs().keySet().iterator();
		int c = 0;
		//iterate through words and compute the PMI values for the context words
		while(termIt.hasNext()){
			
			//get the word
			String term = termIt.next();
			//compute PMI value of the word and its context words
			TreeSet<PMIvalue> tPMIs = pmiSingle(term, someRawCounts.getContextFreqs().get(term), someRawCounts.getWordFreqs(), (someRawCounts.getCorpusSize()*boostFac));
			
			pmiVals.put(term, tPMIs);
			if(c==50){
				System.out.print(".");
				c = 0;
			}
			c++;
		}
		System.out.println("done");
		return new HashMap<String, TreeSet<PMIvalue>>(pmiVals);
	}
	
	
	/**computes the mean PMI value of a pair of words, in that it computes the PMI 
	 * for w2 as context for w1 and the other way around, resulting PMI is half of the sum of the two
	 * @param aPair a {@link WordPair} object
	 * @param someCounts a {@link RawCounts} object, which at least has to contain the independent frequencies of the words in the wordpair and their joint frequencies. 
	 * @return the methods sets the pmi member variable of the {@link WordPair} object and returns it
	 * */
	public WordPair pmiPair(WordPair aPair, RawCounts someCounts){
		String w1 = aPair.getWordA();
		String w2 = aPair.getWordB();
		
		Integer freqW1 = someCounts.getWordFreqs().get(w1);
		Integer freqW2 = someCounts.getWordFreqs().get(w2);
		
		Integer jointW1;
		Integer jointW2;
		
		//test if context count contain the words of the wordpair
		boolean w1IsInContext = someCounts.getContextFreqs().containsKey(w1);
		boolean w2IsInContext = someCounts.getContextFreqs().containsKey(w2);
		
		//handling data sparsity
		if(freqW1==null){
			//avoid division by 0
			freqW1 = 1;
			//if w1 did not occur, joint is 0, and it makes the PMI back to 0 
			jointW1 = 0;
		}
		else if( w1IsInContext && w2IsInContext){
			//w2 in context of w1
			jointW1 = someCounts.getContextFreqs().get(w1).get(w2);
		}
		else{ jointW1=0; }
		
		//to be totally secure
		if(jointW1==null){ jointW1=0; }
		
		
		if(freqW2==null){
			//avoid division by 0
			freqW2=1;
			//ensures that pmi will 0
			jointW2= 0;
		}
		else if(w1IsInContext && w2IsInContext){
			//w1 in context of w2
			jointW2 = someCounts.getContextFreqs().get(w2).get(w1);
		}else{ jointW2 =0; }
		
		//to be totally secure
		if(jointW2==null){ jointW2=0; }
		
		Double pmiW1 = pmiFormula(w1, w2, freqW1, freqW2, jointW1, someCounts.getCorpusSize()); 
		Double pmiW2 = pmiFormula(w2, w1, freqW2, freqW1, jointW2, someCounts.getCorpusSize());
		
		Double pmi = (pmiW1+pmiW2)/2;
		
		aPair.setPmi(pmi);
		
		return aPair;
		
	}
	
	/**implementation of the formula for the PMI, have a look in class description, or source code, to get more information about the formula
	 * */
	private Double pmiFormula( String aterm, String aContextword, Integer freqTerm, Integer freqCont, Integer aJointFreq,long cSize ){
		
		//following the Equation for PMI in the paper
		//Second Order Co-occurrence PMI for Determining the Semantic Similarity of Words
		//from Aminul Islam and Diana Inkpen
		//at time of programmin gavailable at http://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf
		//tested and correct
		
		Long numerator = aJointFreq.longValue() * cSize;
		
		Long denominator = freqCont.longValue() * freqTerm.longValue();
		
		Double division = numerator.doubleValue() / denominator.doubleValue();
		
		/**correct but not after the paper
		Double numerator = jointFreq.doubleValue() / corpusSize.doubleValue();
		
		Double denomC = cWordFreq.doubleValue() / corpusSize.doubleValue();				
		
		Double denomT = sTfreq.doubleValue() / corpusSize.doubleValue();
		
		Double denominator = denomC * denomT;
		
		
		//Double division =  numerator.doubleValue() / denominator.doubleValue();
		Double division = numerator / denominator;
		*/
		Double pmiVal = lb(division);
		
		return new Double(pmiVal);
		
	}
	
	/**computes logarithm to base 2 of x
	 * */
	private double lb( Double x )
	{
	  return Math.log( x ) / logTwo;
	}
	
	/**to validate the correctness of the PMI computation<br>
	 * results get printed out on console<br>
	 * @param fileName name of a plaintext file containing a comma separated list which is structured as follows<br>
	 * the first line of the file should be size in words of the corpus from which the data stems<br>
	 * following lines<br>
	 * word, idependent frequency of word, context word, idependent frequency of context word, joint frequency, pmi value<br> 
	 * */
	public void selftest(String fileName){
		
		System.out.println("testing PMI computation with "+fileName);
		
		try{
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);
			
			Integer corpusSize = Integer.parseInt(br.readLine());
			String aLine = br.readLine();
			
			while(!(aLine==null)){
				
				String[] aLineArr = aLine.split(",");
				
				HashMap<String, Integer> wFreqs = new HashMap<String, Integer>();
				HashMap<String, Integer> cFreqs = new HashMap<String, Integer>();
			
				wFreqs.put(aLineArr[0], Integer.parseInt(aLineArr[1]));
				wFreqs.put(aLineArr[2], Integer.parseInt(aLineArr[3]));
				
				cFreqs.put(aLineArr[2], Integer.parseInt(aLineArr[4]));
				
				Double expectPMI = Double.parseDouble(aLineArr[5]);
				
				TreeSet<PMIvalue> compPMI = pmiSingle(aLineArr[0], cFreqs, wFreqs, corpusSize); 
				
				System.out.println("word: "+aLineArr[0]+"\tcontext word: "+aLineArr[2]+"\texpected PMI="+expectPMI+"\tcomputed PMI="+compPMI.first().getPmiVal());
				
				aLine = br.readLine();
			}
			
		}
		catch(FileNotFoundException e){
			System.err.println("could not find test file:");
			e.printStackTrace();
		}
		catch(IOException e){
			System.err.println("Error when reading testfile");
			e.printStackTrace();
		}
	}
	
}
