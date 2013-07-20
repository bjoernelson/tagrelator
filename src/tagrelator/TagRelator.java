package tagrelator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import org.omg.SendingContext.RunTime;

import tagrelator.collect.flickr.FlickrCollector;
import tagrelator.pmi.PMI;
import tagrelator.pmi.PMIvalue;
import tagrelator.pmi.RawCounts;
import tagrelator.pmi.SOCPMI;
import tagrelator.read.CorpusReader;
import tagrelator.read.FlickrReader;

/**provides functions that pipeline the processes involved in computing similarity values for pairs of words<br>
 * which involves computing PMI/SOC-PMI values.
 * */
public class TagRelator {
	
	private WordPair[] wordPairs;
	//filename of the file containg the wordpairs
	private static final String DefTermsFile = "combined.csv";
	//delta parameter
	private Double delta;
	//gamma parameter
	private Double gamma;
	//corpussize boost factor
	private int boost;
	
	public TagRelator(Double aDelta, Double aGamma) {
		//init to empty array
		this.wordPairs = new WordPair[0];
		
		this.delta = aDelta;
		this.gamma = aGamma;
		//default value no boosting
		this.boost = 1;
	}
	/**computes similarity values 
	 * @param wordpairFile file containg the {@link WordPair}s for which the similarity values should be computed, if "" default value is used
	 * @param pathToCorpus folder that holds preprocessed corpus files, if "" default value is used
	 * @param contextSize sets the size of the window of context words before and after a word from the wordpairs is found
	 * @return an Array of {@link WordPair} objects. the similarity value is in sim field
	 * */
	public WordPair[] simFromCorpusFiles(String wordpairFile, String pathToCorpus, Integer contextSize, String writeStore, boolean bnc) throws IOException{
		
		WordPair[] thePairs = readTermsFile(wordpairFile);
		
		TreeSet<String> terms = wordpPairsToTermSet(thePairs);
		
		//class that reads in Corpus data
		CorpusReader myCread = new CorpusReader();
		
		Integer typeSize;
		HashMap<String, TreeSet<PMIvalue>> pmiValues;
		HashMap<String, Integer> wordFreqs;
		{
			//data is read into this structure
			RawCounts theCCounts;
			if(bnc){
				theCCounts = myCread.readBNC(terms, pathToCorpus, contextSize, writeStore);
				System.out.println("BNC corpussize: "+theCCounts.getCorpusSize()+" typesize: "+theCCounts.getTypeSize()+" contextsize "+theCCounts.getContextSize());
			}
			else{
				theCCounts = myCread.readFromANCfiles(terms, pathToCorpus, contextSize, writeStore);
			}
			//compute PMI
			PMI myPMI = new PMI();
			pmiValues = myPMI.pmiAll(theCCounts);
			
			wordFreqs = theCCounts.getWordFreqs();
			typeSize = theCCounts.getTypeSize();
			
			//Iterator<PMIvalue> iter = pmiValues.get("computer").iterator();
			//while(iter.hasNext()){
			//	PMIvalue val = iter.next();
				//System.out.println("c= "+val.getWord()+"  val= "+val.getPmiVal());
			//}
		}
		Runtime.getRuntime().gc();
		
		SOCPMI mySOCpmi = new SOCPMI();
		if(bnc){
			mySOCpmi.setDelta(delta); //should be 6.5
			mySOCpmi.setGamma(gamma); 
		}
		else{
			mySOCpmi.setDelta(4.0);
		}
		//these variables get reused during SOC-PMI computation 
		String w1;
		String w2;
		
		Integer freqW1;
		Integer freqW2;
		
		TreeSet<PMIvalue> pmiW1;
		TreeSet<PMIvalue> pmiW2;
		
		
		
		System.out.println("\ncomputing SOC-PMI");
		//iterate over wordpairs and compute the SOC-PMI values
		for(int i=0; i<thePairs.length; i++){
			
			//for readaility
			w1 = thePairs[i].getWordA();
			w2 = thePairs[i].getWordB();
			
			freqW1 = wordFreqs.get(w1);
			freqW2 = wordFreqs.get(w2);
			
			//handle sparse data
			if(freqW1==null || freqW2==null){
				//if one of the words is not in the data no computation is possible, all set to 0
				thePairs[i].setSocpmiA(Double.NaN);
				thePairs[i].setSocpmiB(Double.NaN);
				thePairs[i].setBetaA(Double.NaN);
				thePairs[i].setBetaB(Double.NaN);
				thePairs[i].setActBetaA(0);
				thePairs[i].setActBetaB(0);
				thePairs[i].setSim(Double.NaN);
			}
			else{
				
				//handle that data match wordpairs
				if(!(pmiValues.containsKey(w1)) || pmiValues.get(w1).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Corpus SOC-PMI: no PMI Values for word1="+w1+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\nsorry sparse data");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else if(!(pmiValues.containsKey(w2)) || pmiValues.get(w2).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Corpus SOC-PMI: no PMI Values for word2="+w2+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\nsorry sparse data");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else{
					//if w1 have word freq, then they should pmi values
					pmiW1 = pmiValues.get(w1);
					pmiW2 = pmiValues.get(w2); 
					
					//computation of the SOC-PMI value
					thePairs[i].setSocpmiA( mySOCpmi.computeSOCpmi(w1, pmiW1, w2, pmiW2, freqW1, typeSize));
					//get some stats
					thePairs[i].setBetaA(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaA(mySOCpmi.getLastActbeta());
					
					//computation of the second PMI value
					thePairs[i].setSocpmiB( mySOCpmi.computeSOCpmi(w2, pmiW2, w1, pmiW1, freqW2, typeSize));
					//get some stats
					thePairs[i].setBetaB(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaB(mySOCpmi.getLastActbeta());
				}
			}
		}//end for
		
		wordPairs = thePairs;
		
		this.computeSim();
		
		return wordPairs;
	}
	
	/**computes similarity of wordspairs given in wordpairFile
	 * uses a saved corpus obj as base. it your responsability to have the corpus object
	 * contain matching data for the word pairs
	 * */
	public WordPair[] simFromCorpusObj(String wordpairFile, String corpusFile, boolean bnc) throws IOException{
		
		WordPair[] thePairs = readTermsFile(wordpairFile);
		
		CorpusReader myCread = new CorpusReader();
		
		
		
		//System.out.println("words = "+theANCCounts.getCorpusSize()+"  types = "+theANCCounts.getTypeSize());
		HashMap<String, TreeSet<PMIvalue>> pmiValues;
		HashMap<String, Integer> wordFreqs;
		Integer typeSize;
		//compute PMI
		
		{
			RawCounts theANCCounts = myCread.readFromObj(corpusFile);
			PMI myPMI = new PMI();
			pmiValues = myPMI.pmiAll(theANCCounts);
			wordFreqs = theANCCounts.getWordFreqs();
			typeSize = theANCCounts.getTypeSize();
		}
		Runtime.getRuntime().gc();
		
		SOCPMI mySOCpmi = new SOCPMI();
		
		//iterate over wordpairs and compute the SOC-PMI values
		for(int i=0; i<thePairs.length; i++){
			//for readaility
			String w1 = thePairs[i].getWordA();
			String w2 = thePairs[i].getWordB();
			
			Integer freqW1 = wordFreqs.get(w1);
			Integer freqW2 = wordFreqs.get(w2);
			
			//handle sparse data
			if(freqW1==null || freqW2==null){
				//if one of the words is not in the data no computation is possible, all set to 0
				thePairs[i].setSocpmiA(Double.NaN);
				thePairs[i].setSocpmiB(Double.NaN);
				thePairs[i].setBetaA(Double.NaN);
				thePairs[i].setBetaB(Double.NaN);
				thePairs[i].setActBetaA(0);
				thePairs[i].setActBetaB(0);
				thePairs[i].setSim(Double.NaN);
			}
			else{
				
				//handle that data match wordpairs
				if(!(pmiValues.containsKey(w1)) || pmiValues.get(w1).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Corpus SOC-PMI: no PMI Values for word1="+w1+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\nread from corpusfiles for the wordpairs you are trying to compute");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else if(!(pmiValues.containsKey(w2)) || pmiValues.get(w2).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Corpus SOC-PMI: no PMI Values for word2="+w2+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\nread from corpusfiles for wordpairs you are trying to compute");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else{
				
					TreeSet<PMIvalue> pmiW1 = pmiValues.get(w1);
					TreeSet<PMIvalue> pmiW2 = pmiValues.get(w2);
					if(bnc){
						mySOCpmi.setDelta(6.5);
					}
					else{
						mySOCpmi.setDelta(4.0);
					}
					//computation of the SOC-PMI value
					thePairs[i].setSocpmiA( mySOCpmi.computeSOCpmi(w1, pmiW1, w2, pmiW2, freqW1, typeSize));
					//get some stats
					thePairs[i].setBetaA(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaA(mySOCpmi.getLastActbeta());
					
					//computation of the second PMI value
					thePairs[i].setSocpmiB( mySOCpmi.computeSOCpmi(w2, pmiW2, w1, pmiW1, freqW2, typeSize));
					//get some stats
					thePairs[i].setBetaB(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaB(mySOCpmi.getLastActbeta());
				}
			}
		}
		

		
		wordPairs = thePairs;
		
		this.computeSim();
		
		return wordPairs;
	}
	
	/**computes similarity values for wordpairs based on flickr data from {@link FlickrCollector}
	 * @param wordpairFile a file with wordpairs for which similarity should be computed
	 * @param corpusPath a file contructed by {@link FlickrCollector}, it should contain data for all words in the wordpair file
	 * */
	public WordPair[] simFromFlickr(String wordpairFile, String corpusPath) throws IOException{
		
		WordPair[] thePairs = readTermsFile(wordpairFile);
		
		//TreeSet<String> terms = wordpPairsToTermSet(thePairs);
		
		FlickrReader fReader = new FlickrReader();
		RawCounts fCounts = fReader.readFlickrFolder(corpusPath); 
		
		System.out.println("Flickr corpussize: "+(fCounts.getCorpusSize()*boost)+" typesize: "+fCounts.getTypeSize()+" contextsize "+fCounts.getContextSize());
		
		//compute PMI
		PMI myPMI = new PMI(boost);
		
		HashMap<String, TreeSet<PMIvalue>> pmiValues = myPMI.pmiAll(fCounts);
		
		SOCPMI mySOCpmi = new SOCPMI();
		mySOCpmi.setDelta(delta);
		mySOCpmi.setGamma(gamma);
		
		//iterate over wordpairs and compute the SOC-PMI values
		for(int i=0; i<thePairs.length; i++){
			//for readaility
			String w1 = thePairs[i].getWordA();
			String w2 = thePairs[i].getWordB();
			
			Integer freqW1 = fCounts.getWordFreqs().get(w1);
			Integer freqW2 = fCounts.getWordFreqs().get(w2);
			
			//handle sparse data
			if(freqW1==null || freqW2==null){
				//if one of the words is not in the data no computation is possible, all set to 0
				thePairs[i].setSocpmiA(Double.NaN);
				thePairs[i].setSocpmiB(Double.NaN);
				thePairs[i].setBetaA(Double.NaN);
				thePairs[i].setBetaB(Double.NaN);
				thePairs[i].setActBetaA(0);
				thePairs[i].setActBetaB(0);
				thePairs[i].setSim(Double.NaN);
			}
			else{
				//handle that data match wordpairs
				if(!(pmiValues.containsKey(w1)) || pmiValues.get(w1).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Flickr SOC-PMI: no PMI Values for word1="+w1+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\ncollect samples for the wordpairs you are trying to compute");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else if(!(pmiValues.containsKey(w2)) || pmiValues.get(w2).isEmpty()){
					//this only occurs when w1 hasnt been collected
					System.err.println("\nError in Flickr SOC-PMI: no PMI Values for word2="+w2+". SOC-PMI for wordpair "+w1+", "+w2+" will be NaN\ncollect samples for the wordpairs you are trying to compute");
					thePairs[i].setSocpmiA(Double.NaN);
					thePairs[i].setSocpmiB(Double.NaN);
					thePairs[i].setBetaA(Double.NaN);
					thePairs[i].setBetaB(Double.NaN);
					thePairs[i].setActBetaA(0);
					thePairs[i].setActBetaB(0);
					thePairs[i].setSim(Double.NaN);
				}
				else{
					
					TreeSet<PMIvalue> pmiW1 = pmiValues.get(w1);
					TreeSet<PMIvalue> pmiW2 = pmiValues.get(w2);
					
					Integer typeSize = fCounts.getTypeSize(); 
					
					mySOCpmi.setDelta(4.0);
					//computation of the SOC-PMI value
					thePairs[i].setSocpmiA( mySOCpmi.computeSOCpmi(w1, pmiW1, w2, pmiW2, freqW1, typeSize));
					//get some stats
					thePairs[i].setBetaA(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaA(mySOCpmi.getLastActbeta());//TODO:m��h
					
					//computation of the second PMI value
					thePairs[i].setSocpmiB( mySOCpmi.computeSOCpmi(w2, pmiW2, w1, pmiW1, freqW2, typeSize));
					//get some stats
					thePairs[i].setBetaB(mySOCpmi.getLastbeta());
					thePairs[i].setActBetaB(mySOCpmi.getLastActbeta());
				}
			}
		}
		
		fCounts.clear();
		
		wordPairs = thePairs;
		
		this.computeSim();
		
		return wordPairs;
	}
	public void setBoost(int aBoost){
		this.boost = aBoost;
	}
	
	/**convenience method to get all single words out of a {@link WordPair}s Array  
	 * */
	public TreeSet<String> wordpPairsToTermSet(WordPair[] wPAirs){
		TreeSet<String> termSet = new TreeSet<String>();
		for(int i=0; i<wPAirs.length; i++){
			termSet.add(wPAirs[i].getWordA());
			termSet.add(wPAirs[i].getWordB());
		}
		return termSet;
	}
	
	//computes the similarity value of the word pairs
	private void computeSim(){
		for(int a=0; a<wordPairs.length; a++){
			//divide SOC-PMI values by actual beta that was used
			Double divA = wordPairs[a].getSocpmiA() / wordPairs[a].getActBetaA();
			Double divB = wordPairs[a].getSocpmiB() / wordPairs[a].getActBetaB();
			//similarity is the addition
			wordPairs[a].setSim((divA + divB));
		}
	} 
	
	/**reads a file of word pairs with a rating<br>
	 * expect structure word1, word2, rating<br>
	 * of just word1, word2<br>
	 * <b>Attention first lien of file is ignored<b>    
	 * */
	public WordPair[] readTermsFile (String fileName) throws IOException {
		
		if(fileName==null || fileName.equals("")){
			fileName = DefTermsFile;
			System.out.println("reading default wordpair file "+fileName);
		}
		else{
			System.out.println("reading wordpair file "+fileName);
		}
		
		File thefile = new File(fileName);
		ArrayList<WordPair> thePairs = new ArrayList<WordPair>();
		
		//try{
			FileReader fis = new FileReader(thefile);
			BufferedReader br = new BufferedReader(fis);
			
			String aLine = br.readLine();
			//two times 
			aLine = br.readLine();
			
			while(!(aLine==null)){
				
				String[] elmts = aLine.toLowerCase().split(",");
				//if three Elements 
				if(elmts.length>2){
					thePairs.add(new WordPair(elmts[0], elmts[1], Double.parseDouble(elmts[2])));
				}
				else if(elmts.length>1){
					thePairs.add(new WordPair(elmts[0], elmts[1]));
				}
				else{
					System.err.println("not enough elements in line of when trying to read word pair from file");
				}
				aLine = br.readLine();
			}
		//}
		//catch(IOException e){
			//e.printStackTrace();
		//}
		
		//System.out.println("there are "+theTerms.size()+"terms");
		br.close();
		fis.close();
		
		return thePairs.toArray(new WordPair[thePairs.size()]);
	};
	
	/**prints the contents of the wordPairs var
	 * or message no computation has taken place yet
	 * */
	public void printResults(){
		if(wordPairs.length==0){
			System.err.print("the wordpairs are empty, assuming no computation has taken place yet");
		}
		for(int i=0; i<wordPairs.length; i++){
			System.out.print(wordPairs[i].getWordA()+", "+ wordPairs[i].getWordB());
			if(wordPairs[i].isSetsocpmiA()){
				System.out.print("\tsocA "+wordPairs[i].getSocpmiA().floatValue());
			}
			if(wordPairs[i].isSetbetaA()){
				System.out.print("\t beta/act "+wordPairs[i].getBetaA().longValue()+"/");
			}
			if(wordPairs[i].isSetActBetaA()){
				System.out.print(wordPairs[i].getActBetaA());
			}
			if(wordPairs[i].isSetsocpmiB()){
				System.out.print("\t\tsoc B "+wordPairs[i].getSocpmiB().floatValue());
			}
			if(wordPairs[i].isSetBetaB()){
				System.out.print("\t beta/act "+wordPairs[i].getBetaB().longValue()+"/");
			}
			if(wordPairs[i].isSetActBetaB()){
				System.out.print(wordPairs[i].getActBetaB());
			}
			if(wordPairs[i].isSetsim()){
				System.out.print("\tsim "+wordPairs[i].getSim().floatValue());
			}
			System.out.println("");
		}
	};
	
}
