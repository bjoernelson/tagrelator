package tagrelator.pmi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;



/**provides the computation of SOC-PMI values.<br>
 * a method for computing SOC-PMI value for a word pair W1, W2.<br>
 * roughly spoken
 * the n-best context words of W1 according to PMI-value, are taken,<br> 
 * and then the PMI-values for these context words with W2 are looked up and added up.
 * the size of n is not set by the user but a matter of the computation itself    
 * the precise formulas for SOC-PMI are to be found in the paper<br>
 * "Second Order Co-occurrence PMI for Determining the Semantic Similarity of Words" from<br>
 * @author Aminul Islam 
 * @author Diana Inkpen<br>
 * at time of programming available at <a href="http://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf">http://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf</a>
 * */
public class SOCPMI {
	
	//the beta value is computed during the computtion of SOC-PMI
	private Double beta;
	private Integer actBeta;
	private static final Double logTwo = Math.log(2.0);
	
	/**the default value for the constant parameter Delta
	 * it is the same value like referenced paper.
	 * it should be set according to corpus size. in the paper a 100mio word corpus is used 
	 * */
	public final static Double DEFDELTA = 6.5;
	private Double delta;
	/**the default value for the constant parameter Gamma
	 * its an exponent in the formula of SOC-PMI
	 * the bigger it is, the more emphasis is on high PMI values
	 * the default value is according to the referenced paper
	 * */
	public final static Double DEFGAMMA = 3.0;
	private Double gamma;
	///////////////
	//Contructor
	public SOCPMI() {
		//stays how it is
		this.beta = null;
		this.delta = new Double(DEFDELTA);
		this.gamma = new Double(DEFGAMMA);
	}
	//////////
	//getter
	/**during computation of SOC-PMI the beta value gets computed,
	 * which determines how many context words should maximally be 
	 * in the n-best list.<br>
	 * gets updated everytime computeSOCpmi() is called
	 * @return the last beta value or null if no computation has taken place yet
	 * */
	public Double getLastbeta(){
		if(beta==null){
			System.err.println("ups, seems the the computeSOCpmi method of this object hasnt been called yet. returned null.");
			return null;
		}
		
		return beta;
	}
	/**during computation of SOC-PMI the beta value is evaluated against an index
	 * actual beta is at max equal to beta
	 * if there are not enough PMI-values, or if they dont satisfy PMI>0
	 * computation stops. the amount used PMI values (actual beta) is used
	 * for the computation of the similarity value
	 * */
	public Integer getLastActbeta(){
		if(actBeta==null){
			System.err.println("ups, seems the the computeSOCpmi method of this object hasnt been called yet. returned null.");
			return null;
		}
		
		return actBeta;
	}
	
	///////////////
	//setter
	/**set your own Delta parameter
	 * */
	public void setDelta(Double newDelta){
		this.delta = new Double(newDelta);
	}
	/**set your own Gamma parameter
	 * */
	public void setGamma(Double newGamma){
		this.gamma = new Double(newGamma);
	}
	
	/**computes a SOC-PMI value for a word pair w1, w2<br>
	 * w2 is evaluated with the context words of w1
	 * @param w1 first word
	 * @param pmiW1 the PMI values for the first word  (obtainable with {@link PMI} class)
	 * @param w2 the second word
	 * @param pmiW2 the PMI values for the second word (obtainable with {@link PMI} class)
	 * @param freqW1 the independent frequency of the first word in the corpus which served as base (obtainable from the {@link RawCounts} class)
	 * @param typeSize type size of the corpus on which served as base (obtainable from {@link RawCounts} class //TODO: make true)
	 **/ 
	public Double computeSOCpmi( String w1, TreeSet<PMIvalue> pmiW1, String w2, TreeSet<PMIvalue> pmiW2,  Integer freqW1, Integer typeSize){
		
		beta = computeBeta(freqW1, typeSize);
		Integer betaCheck = 0;
		
		Double socPMIval = 0.0;
		
		//the TreeSet with the pmi values is sorted in descending order 
		//now iterate over the beta best context words, for the which the additional
		//condition holds that PMI>0.
		//now try to get the PMI(context word, w2) and add it iff PMI(context word, w2)>0 as well
		PMIvalue aPmiW1 = pmiW1.last();
		//System.out.println("first "+aPmiW1.getWord()+"= "+aPmiW1.getPmiVal());
		
		while(!(aPmiW1==null) && betaCheck <= beta.intValue() && (aPmiW1.getPmiVal()>0.0)){
			
			//the context word of w1
			String conW1 = aPmiW1.getWord();
			
			//now look it up in the PMI values for the second word
			//TODO: possibly implement PMIvalue as extends TreeSet itself
			Iterator<PMIvalue> it = pmiW2.descendingIterator();
			
			while(it.hasNext()){
				PMIvalue w2Val = it.next();
				
				if(w2Val.getWord().equals(conW1) && (w2Val.getPmiVal()>0.0)){
					//System.out.print( w2Val.getWord()+",");
					Double powPmiVal = Math.pow(w2Val.getPmiVal(), gamma);
					socPMIval = socPMIval+powPmiVal;
				}
			}
			//decrement get the next lower element
			aPmiW1 = pmiW1.lower(aPmiW1);
			//increment the check index
			betaCheck++;
			//if(!(aPmiW1==null)){
			//	System.out.println("next "+aPmiW1.getWord()+"= "+aPmiW1.getPmiVal()+" betacheck= "+betaCheck);
			//}
		}
		actBeta = betaCheck;
		return socPMIval;
	}
	
	/**replication of the walk through data in the example to test accuracy
	 *TODO:put data in .txt file
	 * */
	public void betatest(){
		
		this.delta = 0.7;
		
		System.out.println("\nselftest on beta computation. delta = 0.7, word frequency = 6, typesize = 43\nbeta="+computeBeta(6, 43));
	}
	
	/**computes the beta value
	 * */
	private Double computeBeta(Integer wordFreq, Integer typeSize){
		Double cBeta = 0.0;
		//first part of the Equation in small steps
		//logarithm of word frequency
		Double logOfwFreq = Math.log(wordFreq);
		//System.out.println("logOfwFreq="+logOfwFreq);
		//the value to the pow 2
		Double qOflogFreq = Math.pow(logOfwFreq, 2);
		//System.out.println("qOflogFreq="+qOflogFreq);
		
		//second part of the Equation in small steps
		//loagrithm to base 2 of typesize of corpus
		Double logTwoTypes = lb(typeSize.doubleValue());
		//System.out.println("logTwoTypes="+logTwoTypes);
		
		//divide by the constant parameter gamma
		Double divOflogTypes = logTwoTypes/delta;
		//System.out.println("divOflogTypes="+divOflogTypes);
		
		cBeta = qOflogFreq * divOflogTypes;
		
		this.beta = cBeta;
		//round it
		return cBeta;
	}
	
	private double lb( Double x )
	{
	  return Math.log( x ) / logTwo;
	}
	
	/**prints a whole result
	 * */
	public void selftest(){
		
		System.out.println("SOC-PMI test started\n");
		
		TreeSet<String> terms = new TreeSet<String>();
		terms.add("car");
		terms.add("automobile");
		//sadly the read read corpus methods only provide the documents only String[] of a whole document
		//but to replicate the example from the paper the context counts have be based on per sentence counting
		HashMap<String, HashMap<String, Integer>> rawContext = new HashMap<String, HashMap<String,Integer>>();
		HashMap<String, Integer> rawWfr = new HashMap<String, Integer>();
		//so counts are constructed manually
		rawWfr.put("disappear",1);rawWfr.put("worst", 1);rawWfr.put("yugoslavia",1);rawWfr.put("soak",1); 
		rawWfr.put("pursuit",1);rawWfr.put("fall",1); rawWfr.put("brightest",1);rawWfr.put("supplier",1); 
		rawWfr.put("travel",1);rawWfr.put("company",2); rawWfr.put("benefit",1);rawWfr.put("recession",2); 
		rawWfr.put("risky",1);rawWfr.put("farther",1); rawWfr.put("sign",1);rawWfr.put("car",6); 
		rawWfr.put("male",1);rawWfr.put("investment",1); rawWfr.put("accident",1);rawWfr.put("industry",10); 
		rawWfr.put("affect",1);rawWfr.put("force",1); rawWfr.put("mechanical",1);rawWfr.put("job",1); 
		rawWfr.put("claim",1);rawWfr.put("client",1); rawWfr.put("among",1);rawWfr.put("tend",1); 
		rawWfr.put("moment",1);rawWfr.put("hardest",1); rawWfr.put("engineer",3);rawWfr.put("component",2); 
	    rawWfr.put("automobile",6);rawWfr.put("manufacturer",1);rawWfr.put("emergence",1);rawWfr.put("expand",2); 
	    rawWfr.put("direct",1);rawWfr.put("driver",3); rawWfr.put("hit",1);rawWfr.put("exclude",1); 
	    rawWfr.put("largely",1);rawWfr.put("motorist",1);rawWfr.put("acreage",1); 
	    
	    HashMap<String, Integer> cCar = new HashMap<String, Integer>();
	    HashMap<String, Integer> cauto = new HashMap<String, Integer>();
	    
	    cCar.put("motorist",1); cauto.put("emergence",1);
	    cCar.put("disappear" ,1); cauto.put("direct",1);
	    cCar.put("worst",1 );cauto.put("acreage" ,1);
	    cCar.put("pursuit", 1);cauto.put("hit",1);
	    cCar.put("soak",1);cauto.put("largely",1);
	    cCar.put("travel" ,1);  cauto.put("yugoslavia",  1);
	    cCar.put("brightest" ,1);  cauto.put("supplier",  1);
	    cCar.put("fall", 1);  cauto.put("benefit",1);
	    cCar.put("risky",1); cauto.put("male",1);
	    cCar.put("company",2);  cauto.put("investment",1);
	    cCar.put("sign",1);  cauto.put("among",1); 
	    cCar.put("farther",1);  cauto.put("force" ,1);
	    cCar.put("accident",1);  cauto.put("client",1);
	    cCar.put("affect",1);  cauto.put("hardest",1); 
	    cCar.put("mechanical", 1);  cauto.put("component",2);
	    cCar.put("tend",1);  cauto.put("manufacturer",1);
	    cCar.put("claim",1);  cauto.put("expand",2); 
	    cCar.put("engineer",3);  cauto.put("industry" ,7);
	    cCar.put("moment",1);  cauto.put("recession", 1);
	    cCar.put("driver",3);
	    cCar.put("exclude",1);
	    cCar.put("recession",1);
	    cCar.put("industry",3);
	    
	    rawContext.put("car", cCar);
	    rawContext.put("automobile", cauto);
		RawCounts soctestcounts = new RawCounts(rawContext, rawWfr); 
		System.out.println("corpus size "+soctestcounts.getCorpusSize());
		System.out.println("type size "+soctestcounts.getTypeSize());
		
		System.out.println("\ncomputing PMI\n");
		//PMI values get computed
		PMI myPMI = new PMI();
		HashMap<String, TreeSet<PMIvalue>>  pmiVals = myPMI.pmiAll(soctestcounts);
		
		//printAllPMI(terms, pmiVals, soctestcounts);
		
		System.out.println("SOC-PMI computation\ndelta is set to 0.7\ngamma is set to 3.0\ntermpair:");
		
		SOCPMI mysocPMI = new SOCPMI();
		
		mysocPMI.setDelta(0.7);
		mysocPMI.setGamma(3.0);
		
		String w1 = terms.first();
		String w2 = terms.higher(terms.first());
		
		System.out.println("w1: "+w1+"\tindep freq "+soctestcounts.getWordFreqs().get(w1));
		System.out.println("w2: "+w2+"\tindep freq "+soctestcounts.getWordFreqs().get(w2));

		Double socA = mysocPMI.computeSOCpmi(w1, pmiVals.get(w1), w2, pmiVals.get(w2) , soctestcounts.getWordFreqs().get(w1), soctestcounts.getTypeSize());
		System.out.println("\nbeta "+mysocPMI.getLastbeta());
		
		System.out.println("\nSOC-PMI value: "+socA+"\n");
		
		
		System.out.println("\nw1 is "+w2);
		System.out.println("w2 is "+w1);
		
		Double socB = mysocPMI.computeSOCpmi(w2, pmiVals.get(w2), w1, pmiVals.get(w1), soctestcounts.getWordFreqs().get(w2), soctestcounts.getTypeSize());
		System.out.println("beta "+mysocPMI.getLastbeta());
		
		System.out.println("\nSOC-PMI value: "+socB+"-");
		
	}
	
	
		
}
