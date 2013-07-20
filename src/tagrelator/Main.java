package tagrelator;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;


import tagrelator.collect.flickr.FlickrCollector;
import tagrelator.pmi.PMI;
import tagrelator.pmi.PMIvalue;
import tagrelator.pmi.RawCounts;
import tagrelator.pmi.SOCPMI;
import tagrelator.read.CorpusReader;
import tagrelator.read.FlickrReader;


/**the entry point of the runnable .jar which processes the command line options and calls the according objects and methods
 *    
 * */
public class Main {

	
	public static void main(String[] args) {
		
		if(args.length==0){
			help();
		}
		else{
			//output results to file
			boolean tofile = false;
			//demo mode
			boolean demo = false;
			//collect mode
			boolean collect = false;
			//collect all WordCounts
			boolean collWC = false;
			//repair collect
			boolean repCollect = false;
			//print help
			boolean help = false;
			//compute only pmi
			boolean onlypmi = false;
			//compute similarity
			boolean sim = false;
			//just do statistics on the corpus
			boolean stats = false;
			//use Flickr data
			boolean useFlickr = false;
			//use Corpus data
			boolean useANC = false;
			//use BNC as Corpus
			boolean useBNC = false;
			//perform selftests
			boolean test = false;
			//read from corpus obj
			boolean readCobj = false;
			//amount of context words
			Integer context = 0;
			//a plain text file with wordpairs to process
			String pairFile = "";
			//parameter for the Flickr Collector which gives the amount of samples to process
			Integer sampleSize = 0;
			//filename of a store which was created by the Flickr Collector
			String flickrStoreToRead = "";
			//either a folder with corpus files processed by the ANCprocessor
			String corpusStoreToRead = "";
			//filename where to write the output of Flickr Collector
			String flickrStoreToWrite = "";
			//fileame where to write the output of a CorpusReader read
			String corpusStoreToWrite = ""; 
			//delta parameter for SOC-PMI computation
			Double delta = 0.0;
			//gamma parameter for SOC-PMI computation
			Double gamma = 0.0;
			//set the boostfactor of the corpussize
			int boost = 1;
			//Flickr Api key
			String apikey = null;
			//Flickr shared secret
			String shsec = null;
			
			////////////////////////////////////////////////////////////////
			//process commadline parameters
			////////////////////////////////////////////////////////////////
			for(int i = 0; i<args.length; i++){
					
					//--------------------------
					//		main options
					//--------------------------
				
					//start the Flickr Collector 
					if(args[i].equals( "-coll" )){
						//System.out.println("found -coll option");
						collect = true;
					}
					// equal out differences in samplesize, which occured due to errors in collecting 
					if(args[i].equals("-Repair")){
						repCollect = true;
						if((args.length > i+1) && args[i+1] != null){
							flickrStoreToRead = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing corpus folder for Repair Collect");
						}
						
					}
					//compute only pairwise pmi 
					else if(args[i].equals( "-pmi")){
						onlypmi = true;
					}
					//compute Similarity
					else if(args[i].equals( "-sim")){
						sim = true;
					}
					//performs a full standard run with default files
					else if(args[i].equals( "-demo")){
						demo = true;
					}
					//performs a full standard run with default files
					else if(args[i].equals( "-collWC")){
						collWC = true;
					}
					else if(args[i].equals("-stats")){
						stats = true;
					}
					else if(args[i].equals("-apik")){
						if((args.length > i+1) && args[i+1] != null ){
							apikey = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -apik");
						}
					}
					else if(args[i].equals("-secr")){
						if((args.length > i+1) && args[i+1] != null ){
							shsec = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option --secr");
						}
					}
					//------------------------------
					//		data selection options
					//------------------------------
					//use flickr data
					else if(args[i].equals( "-f")){
						useFlickr = true;
						}
					//use ANC corpus data
					else if(args[i].equals( "-anc")){
						useANC = true;
					}
					//use ANC corpus data
					else if(args[i].equals( "-bnc")){
						useBNC = true;
					}
					//get filename of file with wordpairs
					else if(args[i].equals( "-p" )){
						if((args.length > i+1) && args[i+1] != null){
							pairFile = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -p");
						}
					}
					//switch on reading from BNC/ANC corpus object
					else if(args[i].equals( "-co")){
						readCobj = true;
					}
					//Flickr corpus folder to read, optional
					else if(args[i].equals( "-if" )){
						if((args.length > i+1) && args[i+1] != null ){
							flickrStoreToRead = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -if");
						}
					}
					//BNC/ANC corpus store to read, optional
					else if(args[i].equals( "-ic" )){
						if((args.length > i+1) && args[i+1] != null ){
							corpusStoreToRead = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -ic");
							}
					}
					//context size when computing ANC/BNC
					else if(args[i].equals( "-cxt")){
						if((args.length > i+1) && args[i+1] != null){
							context = Integer.parseInt(args[i+1]);
							if(context==0){
								throw new IllegalArgumentException("Error: value 0 for option -cxt not allowed. has to be >0");
							}
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -cxt"); 
						}
					}
					//get samplesize
					else if(args[i].equals( "-s" )){
						if((args.length > i+1) && args[i+1] != null){
							sampleSize = Integer.parseInt(args[i+1]);		
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -s");
						}
					}
					//------------------------------------
					//		output options
					//------------------------------------
					else if(args[i].equals( "-tf")){
						//System.out.println("found -tf option");
						tofile = true;
					}
					//Flickr store to write to (optional)
					else if(args[i].equals( "-of" )){
						if((args.length > i+1) && args[i+1] != null){
							flickrStoreToWrite = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -of");
						}
					}
					//Corpus store to write to (optional)
					else if(args[i].equals( "-oc" )){
						if((args.length > i+1) && args[i+1] != null){
							corpusStoreToWrite = args[i+1];
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -oc");
						}
					}
					//------------------------------------
					//		parameters
					//------------------------------------
					//delta parameter
					else if(args[i].equals( "-delta" )){
						if((args.length > i+1) && args[i+1] != null){
							delta = Double.parseDouble(args[i+1]);
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -delta");
						}
					}
					//gamma parameter
					else if(args[i].equals( "-gamma" )){
						if((args.length > i+1) && args[i+1] != null){
							gamma = Double.parseDouble(args[i+1]);
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -gamma");
						}
					}
					//corpus size boost factor
					else if(args[i].equals( "-boost" )){
						if((args.length > i+1) && args[i+1] != null){
							boost = Integer.parseInt(args[i+1]);
						}
						else{
							throw new IllegalArgumentException("Error: missing Parameter for option -boost");
						}
					}
					
					//print help
					else if(args[i].equals( "-h")){
						help = true;
					}
					else if(args[i].equals( "-help")){
						help = true;
					}
					else if(args[i].equals( "--help")){
						help = true;
					}
					else if(args[i].equals( "-test")){
						test=true;
					}
						
				}//end for, collected all options
			
			
			//if no corpus option was set, use BNC and Flickr
			if(useANC==false && useBNC==false && useFlickr==false){
				useBNC = true;
				useFlickr = true;
			}
			
			
			///////////////////////////////////////////////////////
			//compute similarity
			///////////////////////////////////////////////////////
			if(sim){
				/**
				System.out.println("0.0 <-> NaN "+ new Double(0.0).compareTo(Double.NaN));
				System.out.println("NaN <-> NaN "+ new Double(0.0).compareTo(Double.NaN));
				System.out.println("1.0 <-> NaN "+ new Double(1.0).compareTo(Double.NaN));
				System.out.println("-1.0 <-> NaN "+ new Double(-1.0).compareTo(Double.NaN));
				System.out.println("-1.0 <-> 1.0 "+ new Double(-1.0).compareTo(1.0));
				System.out.println("NaN <-> 0.0 "+ new Double(Double.NaN).compareTo(0.0));
				System.out.println("NaN <-> 1.0 "+ new Double(Double.NaN).compareTo(1.0));
				System.out.println("NaN <-> -1.0 "+ new Double(Double.NaN).compareTo(-1.0));
				*/
				
				if(delta == 0.0 || gamma == 0.0){
					System.err.println("Error: provide delta and gamma paramter with options, -delta and -gamma");
				}
				else{
				WordPair[] cWpairs = new WordPair[1];
				WordPair[] fWpairs = new WordPair[1];
				
				TagRelator tagRel = new TagRelator(delta, gamma);
				try{
					if(useANC){
						if(boost > 1){
							System.out.println("boost only affects Flickr Corpus");
						}
						if(readCobj){
							cWpairs = tagRel.simFromCorpusObj(pairFile, corpusStoreToRead, false);
						}
						else{
							cWpairs = tagRel.simFromCorpusFiles(pairFile, corpusStoreToRead, context, corpusStoreToWrite, false);
						}
						
					}
					else if(useBNC){
						if(boost > 1){
							System.out.println("boost only affects Flickr Corpus");
						}
						if(readCobj){
							cWpairs = tagRel.simFromCorpusObj(pairFile, corpusStoreToRead, true);
						}
						else{
							cWpairs = tagRel.simFromCorpusFiles(pairFile, corpusStoreToRead, context, corpusStoreToWrite, true);
						}
					}
					
					if(useFlickr){
							tagRel.setBoost(boost);
							fWpairs = tagRel.simFromFlickr(pairFile, flickrStoreToRead);
					}
					
					PrintStream ps = System.out;
					
					if( tofile==true){
						try{
							ps = new PrintStream("result.csv");
						}
						catch(FileNotFoundException e){
							e.printStackTrace();
						}
					}
					else{
						ps = System.out;
					}
					
					if(useFlickr && (useANC|| useBNC)){
						ps.println("word1, word2, user rating, Corpus, SOC-PMI A, SOC-PMI B, sim, Flickr, SOC-PMI A, SOC-PMI B, sim");
						for(int i=0; i<fWpairs.length; i++){
							WordPair cPair = cWpairs[i];
							WordPair fPair = fWpairs[i];
							ps.println( cPair.getWordA()+", "+cPair.getWordB()+ ", "+cPair.getRating()+", Corpus "+cPair.getSocpmiA().floatValue()+", "+cPair.getSocpmiB().floatValue()+", "+cPair.getSim().floatValue()+
									",\tFlickr, "+fPair.getSocpmiA().floatValue()+", "+fPair.getSocpmiB().floatValue()+", "+fPair.getSim().floatValue() );
							
						}
					}
					
					if(useFlickr && !(useANC && useBNC)){
						ps.println("word1, word2, user rating, Flickr, SOC-PMI A, SOC-PMI B, sim");
						for(int i=0; i<fWpairs.length; i++){
							
							WordPair fPair = fWpairs[i];
							ps.println( fPair.getWordA()+", "+fPair.getWordB()+ ", "+fPair.getRating()+",\tFlickr, "+fPair.getSocpmiA().floatValue()+", "+fPair.getSocpmiB().floatValue()+", "+fPair.getSim().floatValue() );	
						}
					}
					
					if(!(useFlickr) && (useANC || useBNC)){
						ps.println("word1, word2, user rating, Corpus, SOC-PMI A, SOC-PMI B, sim");
						for(int i=0; i<cWpairs.length; i++){
							WordPair cPair = cWpairs[i];
							ps.println( cPair.getWordA()+", "+cPair.getWordB()+ ", "+cPair.getRating()+",\tCorpus, "+cPair.getSocpmiA().floatValue()+", "+cPair.getSocpmiB().floatValue()+", "+cPair.getSim().floatValue()+" , "+ cPair.getActBetaA()+" , "+ cPair.getActBetaB());	
						}
					}
					
					if(tofile){
						System.out.print("\nwrote results to results.csv");
					}
				}
				catch(FileNotFoundException e){
					//System.err.println("no wordpair file");
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
				}
			}
			
			/////////////////////////////////////////////////////////////////////
			//compute only pairwise pmi
			/////////////////////////////////////////////////////////////////////
			else if(onlypmi){
				
				TagRelator tagRel = new TagRelator(delta, gamma);
				WordPair[] fWpairs = new WordPair[0];
				WordPair[] cWpairs = new WordPair[0];
				try{
					if(useANC){
						
							cWpairs = tagRel.readTermsFile(pairFile);
							
							//read corpus
							CorpusReader cRead = new CorpusReader();
							RawCounts cCounts = new RawCounts();
							if(readCobj){
								cCounts = cRead.readFromObj(corpusStoreToRead);
							}
							else{
								cCounts =	cRead.readFromANCfiles(tagRel.wordpPairsToTermSet(cWpairs), corpusStoreToRead, context, corpusStoreToWrite);
							}
							System.out.println("computing PMI values on Corpus data");
							
							PMI cPMI = new PMI();
							for(int a=0; a<cWpairs.length; a++){
								
								cWpairs[a] = cPMI.pmiPair(cWpairs[a], cCounts);
							}
					}
					else if(useBNC){
						
						cWpairs = tagRel.readTermsFile(pairFile);
						
						//read corpus
						CorpusReader cRead = new CorpusReader();
						RawCounts cCounts = new RawCounts();
						if(readCobj){
							cCounts = cRead.readFromObj(corpusStoreToRead);
							System.out.println("cSize :"+cCounts.getCorpusSize());
						}
						else{
							cCounts =	cRead.readBNC(tagRel.wordpPairsToTermSet(cWpairs), corpusStoreToRead, context, corpusStoreToWrite);
							System.out.println("cSize :"+cCounts.getCorpusSize());
						}
						System.out.println("computing PMI values on Corpus data");
						
						PMI cPMI = new PMI();
						for(int a=0; a<cWpairs.length; a++){
							
							cWpairs[a] = cPMI.pmiPair(cWpairs[a], cCounts);
						}
					}
					if(useFlickr){
						
							fWpairs = tagRel.readTermsFile(pairFile);
							//read corpus
							FlickrReader fRead = new FlickrReader();
							RawCounts 	fCounts = fRead.readFlickrFolder(flickrStoreToRead);
							
							System.out.println("\ncomputing PMI values on Flickr data");
							
							PMI fPMI = new PMI(boost);
							for(int a=0; a<fWpairs.length; a++){
								
								fWpairs[a] = fPMI.pmiPair(fWpairs[a], fCounts);
							}
						
					}
	
					PrintStream ps = System.out;				
					if( tofile==true){
						try{ ps = new PrintStream("result.csv");}
						catch(FileNotFoundException e){ e.printStackTrace();}
					}
					else{
						ps = System.out;
					}
					
					if(useFlickr && (useANC||useBNC)){
						ps.println("word1, word2, user rating, Corpus, PMI, Flickr, PMI");
						for(int i=0; i<fWpairs.length; i++){
							WordPair cPair = cWpairs[i];
							WordPair fPair = fWpairs[i];
							ps.println( cPair.getWordA()+", "+cPair.getWordB()+ ", "+cPair.getRating()+", Corpus "+cPair.getPmi().floatValue()+",\tFlickr, "+fPair.getPmi().floatValue() );
							
						}
					}
					
					if(useFlickr && !(useANC && useBNC)){
						ps.println("word1, word2, user rating, Flickr, PMI");
						for(int i=0; i<fWpairs.length; i++){
							
							WordPair fPair = fWpairs[i];
							ps.println( fPair.getWordA()+", "+fPair.getWordB()+ ", "+fPair.getRating()+",\tFlickr, "+fPair.getPmi().floatValue());	
						}
					}
					
					if(!(useFlickr) && (useANC || useBNC)){
						ps.println("word1, word2, user rating, Corpus, PMI");
						for(int i=0; i<cWpairs.length; i++){
							WordPair cPair = cWpairs[i];
							ps.println( cPair.getWordA()+", "+cPair.getWordB()+ ", "+cPair.getRating()+",\tCorpus, "+cPair.getPmi().floatValue());	
						}
					}
					
					if(tofile){
						System.out.print("\nwrote results to results.csv");
					}
				}
				catch(FileNotFoundException e){
					System.err.println("no wordpair file");
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
				
			}
			///////////////////////////////////////////////////////////////////////
			//run selftest methods
			///////////////////////////////////////////////////////////////////////
			else if(test){
				PMI aPMI = new PMI();
				aPMI.selftest("pmitest.txt");
				
				System.out.println("\n\nyou can perform your own test if you have data by putting it in pmitest.txt in the working directory. " +
						"the expected form of the file is:\nfirst line is size of the corpus in words\nfollowing lines:" +
						"\na word, independent frequency of the word, a context word, independent frequency of the context word, the joint frequency of them, the pmi value");
				
				System.out.println("\n\nthe Selftests on beta and SOC-PMI take as input the replication of the walk through example data in the paper:" +
						"\nSecond Order Co-occurrence PMI for Determining the Semantic Similarity of Words" +
						"\nhttp://hnk.ffzg.hr/bibl/lrec2006/pdf/242_pdf.pdf\ndata is hard coded in the class");
				
				
				SOCPMI aSocPmi = new SOCPMI();
				aSocPmi.betatest();
				System.out.println();
				aSocPmi.selftest();
				
			}
			/////////////////////////////////////////////////////////////////
			//demo mode
			/////////////////////////////////////////////////////////////////
			else if(demo){
				//System.out.println("found -demo option");
				
				TagRelator tagRel = new TagRelator(4.5, 3.0);
				WordPair[] cWpairs = new WordPair[0];
				WordPair[] fWpairs = new WordPair[0];
				
				try{
					cWpairs = tagRel.simFromCorpusObj("", "demoCcounts.obj",true);
					fWpairs = tagRel.simFromFlickr("", "");
				}
				catch(FileNotFoundException e){
					System.err.println("no wordpair file");
					e.printStackTrace();
				}
				catch(IOException e){
					e.printStackTrace();
				}
				
				
				PrintStream ps = System.out;
				
				if( tofile==true){
					try{
						ps = new PrintStream("result.csv");
					}
					catch(FileNotFoundException e){
						e.printStackTrace();
					}
				}
				else{
					ps = System.out;
				}
				
				ps.println("word1, word2, user rating, Corpus, SOC-PMI A, SOC-PMI B, sim, Flickr, SOC-PMI A, SOC-PMI B, sim");
				for(int i=0; i<fWpairs.length; i++){
					WordPair cPair = cWpairs[i];
					WordPair fPair = fWpairs[i];
					ps.println( cPair.getWordA()+", "+cPair.getWordB()+ ", "+cPair.getRating()+", Corpus "+cPair.getSocpmiA().floatValue()+", "+cPair.getSocpmiB().floatValue()+", "+cPair.getSim().floatValue()+
							",\tFlickr, "+fPair.getSocpmiA().floatValue()+", "+fPair.getSocpmiB().floatValue()+", "+fPair.getSim().floatValue() );
					
				}
				
				if(tofile){
					System.out.print("\nwrote results to results.csv");
				}
			}//end if demo
			//////////////////////////////////////////////////////////////////////////////////////
			//collect some samples for Flickr 
			//////////////////////////////////////////////////////////////////////////////////////
			else if(collect){
				
				if(apikey == null || shsec == null){
					System.err.println("provide Flickr API key with -apik, and Flickr shared secret with -secr options, for access to Flickr");
				}
				else{
					FlickrCollector myCollector = new FlickrCollector(apikey, shsec);
					
					if(repCollect){
						myCollector.setQuiet(true);
						myCollector.repairCollect(flickrStoreToRead);
					}
					else{
						TagRelator myRelator = new TagRelator(1.0,1.0);
						
						TreeSet<String> terms = new TreeSet<String>();
						
						try{
							terms = myRelator.wordpPairsToTermSet(myRelator.readTermsFile(pairFile));
							myCollector.collectV2(terms, sampleSize, flickrStoreToRead, flickrStoreToWrite);
						}
						catch(FileNotFoundException e){
							System.err.println("no wordpair file");
							e.printStackTrace();
						}
						catch(IOException e){
							e.printStackTrace();
						}
					}
				}
			}//end if coll
			////////////////////////////////////////
			//repair and Stats
			//////////////////////////////////////
			//collect word frequencies for all words in corpus 
			else if(collWC){
				if(apikey == null || shsec == null){
					System.err.println("provide Flickr API key with -apik, and Flickr shared secret with -secr options, for access to Flickr");
				}
				else{
					FlickrCollector myCollector = new FlickrCollector(apikey, shsec);
					
					myCollector.allWfreqs(flickrStoreToRead);
				}
			}
			else if(stats){
				FlickrReader myFReader = new FlickrReader();
				try{
					myFReader.corpusStats(flickrStoreToRead);
				}catch(IOException e){
					System.err.println("corpus statisitics caused ");
					e.printStackTrace();
				}
			}
			else if(help){
				help();
			}
			else{
				System.out.println("no valid Argument pattern found, sorry");
			}
		
		};//end else
		
		System.out.print("...done");
	}//end main
	
	//print the help
	private static void help(){
		System.out.println("This software is computing similarity values for wordpairs.\nFor that it uses tagdata from Flickr and Textcorpus data\nthe measures used are PMI and SOC-PMI.");
		
		System.out.println("\nusage:" +
				"\n-demo : performs a full run on the default files and prints the results for wordsimilarity, SOC-PMI\n"+ 
				"\n-sim : compute similarity values for the word pairs, SOC-PMI values included" +
				"\n-pmi : compute pairwise PMI values for the wordpairs"+ 
				"\n-h|-help|--help : print this text" +
				"\n-test : performs the selftest methods for PMI and SOC-PMI, read more in the javadoc" +
				"\n-coll : collect sample data on Flickr, -s option has to be given for this " +
				"\n the options above are mutally exclusive\n" +
				"\n-apik : the Flickr API key needed for collecting" +
				"\n-secr : the Flickr shared secret needed for collecting" +
				"\n-of filename : if you want save the result of collect in a new file " +
				"\n-s X : X is the number of photo samples per term to collect from Flickr in this run," +
				"\n       if an existing store is loaded the new samples are added" +
				"\n-cxt X : mandatory when corpus files are used, use X context words before and after a word match" +
				"\n\noptional:" +
				"\n-f/-anc/-bnc : use Flickr / ANC / BNC corpus data,  default is Flickr and BNC data"+
				"\n-p filename : argument that specifies the file containing the wordpairs" +
				"\n-if filename : a Flickr store to open" +
				"\n-ic filename : if u want to change the folder of the preprocessed corpus, or give a filename when used with -co,read javadoc on ANCprocessor for more info" +
				"\n-co : the Corpus counts for the wordpairs are computed from the files in the Corpus folder, and then written to an .obj file" +
				"\n      you can read the counts from this object which is much faster, by setting this option." +
				"\n      using matching wordpairs is in your responsability, -cxt has no impact in this read mode" +
				"\n the last three options should work with, -sim, -pmi, -collect\n" +
				"\n-tf set this option to write the results of -sim, -pmi, -demo to the file result.csv instead of console");
	} 

	
	/**prints the results for one term   System.out.println("\nthe PMI values\n");
	 * */
	public static void printPMI(String term, TreeSet<PMIvalue> pmiVals, RawCounts rawC){
		
		HashMap<String, HashMap<String, Integer>> rawContext = rawC.getContextFreqs();
		HashMap<String, Integer> rawWfr = rawC.getWordFreqs();
		
		Iterator<PMIvalue> it = pmiVals.iterator();
		System.out.println("term "+term+"\ncontext word\t\t\t\tPMI");
		
		while(it.hasNext()){
			PMIvalue pmiVal = it.next();
			System.out.println("-"+pmiVal.getWord()+"-\t\t\t\t"+pmiVal.getPmiVal()+"\t\tcontext freq = "+rawContext.get(term).get(pmiVal.getWord())+" indep freq = "+rawWfr.get(pmiVal.getWord()));
		}
		System.out.println("");
	}
	
	/**prints a whole result
	 * */
	public static void printAllPMI(TreeSet<String> terms, TreeMap<String, TreeSet<PMIvalue>> pmiVals, RawCounts rawC){
		
		Iterator<String> tIt = terms.iterator();
		
		while(tIt.hasNext()){
			String term = tIt.next();
			if(!(pmiVals.containsKey(term))){
				System.out.println("no PMI values for "+term );
			}
			else{
				
				printPMI(term, pmiVals.get(term), rawC);
				
			}
		}
		
	}
}
