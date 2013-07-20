package tagrelator.read;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.*;

import com.sun.corba.se.spi.orbutil.fsm.Input;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import tagrelator.collect.anc.ANCdocument;
import tagrelator.pmi.RawCounts;


/**provides some methods load corpus data, and process them into a format for further computation by the PMI and SOC-PMI classes 
 * */
public class CorpusReader {
	
	
	/**default filename for results, lives in working directory*/
	public final static String DefResultFile = "CCounts.obj";
	public final static String DefBNCResultFile = "BNCCounts.obj";
	/**holds the filename where output is written */
	public String resultFile;
	/**default filename for Ngram plain text file*/
	public final static String DefNGramFile = "w5_.txt";
	public final static String DefANCfolder = "ANCcorpus";
	public final static String DefBNCFile = "/projects/korpora/orig-korpora/bnc/Texts/texts.tgz";
	
	public CorpusReader(){
		
		this.resultFile = new String(DefResultFile);
		
	};

	/**private void readBNCweb(TreeSet<String> searchterms){
		
		try{	
			URLConnection bncConn;
			URL bncURL = new URL("http://corpus.byu.edu/bnc/");
		    bncConn = bncURL.openConnection();
			
			//enable outputting
			bncConn.setDoOutput(true);
			bncConn.setAllowUserInteraction(true);
			bncConn.setDoInput(true);
			Map<String, List<String>>  reqProps = bncConn.getRequestProperties();
			bncConn.connect();
			
			OutputStreamWriter out = new OutputStreamWriter(bncConn.getOutputStream());
			
			//prepare searchterm
			String sterm = URLEncoder.encode("mouse", "UTF-8");
			System.out.println("connected to BNC website");
			
			System.out.println("content type: " + bncConn.getContentType());
			System.out.println("content length: " + bncConn.getContentLength());
			Set<String> keys = reqProps.keySet();
			Iterator<String> it = keys.iterator();
			//bncConn.
			
			System.out.println("request properties:");
			while( it.hasNext()){
				System.out.println("-"+it.next());
			}
			
		}
		catch(IOException e){
			System.err.println("Error when connecting to BNC website: " +e.getMessage());
		}
	}
	
	private void readBNCweb2(TreeSet<String > searchterms){
		WebClient myClient = new WebClient();
		myClient.setJavaScriptEnabled(true);
		
		//WebWindow currWin = myClient.getCurrentWindow();
		try{
			
			myClient.openWindow(new URL("http://corpus.byu.edu/bnc/"), "bnc");
			
			//Page bncPage = myClient.getPage("http://corpus.byu.edu/bnc/");
			
			
		}
		catch(MalformedURLException e){
			System.err.println("2 Error when connecting to BNC website: " +e.getMessage()+"\nsorry no url, results");
		}
		
	}*/
	
	/**reads frequencies and context frequencies for the given terms from corpus files preprocessed by ANCprocessor in the folder specified.
	 * @param terms a set of terms for which context frequencies should be collected
	 * @param pathToANC to folder that holds corpus files that an ANCprocessor object computed
	 * @param aClength the number words which serve as context before and after a target word from terms is found
	 * @param aResultFile filename for the RawCounts object file that can be written to disk<br> 
	 * if null nothing is written, if "" a default is used 
	 * */
	public RawCounts readFromANCfiles( TreeSet<String> terms, String pathToANC, Integer aClength, String aResultFile){
		
		//the collected data
		//independent word frequencies
		HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
		//joint frequencies
		HashMap<String, HashMap<String, Integer>> contextFreqsAll = new HashMap<String, HashMap<String, Integer>>();
		
		//keep track of the corpusSize
		Integer corpusSize = 0;
		
		//context length of 0 is not allowed, would make no sense
		if(aClength==0){
			aClength= 1;
			System.err.println("context length of 0 is not allowed. was set to 1\ncall method readFromANCfiles() with a higher value for clength parameter to avoid this message");
		}
		
		if((pathToANC==null)||pathToANC.equals("")){
			pathToANC = new String(DefANCfolder);
		}
		//collect file to process
		File dir = new File(pathToANC);
		ArrayList<File> cfiles = searchFile(dir, ".obj");
		if(cfiles.isEmpty()){
			System.err.println("\nno corpus files in folder "+pathToANC+". emtpy Raw counts returned");
			return new RawCounts();
		}
		else{
			System.out.print("\nprocessing ANC corpusfiles\n");
			//iterate over corpus files
			for(Integer fileInd=0; fileInd<cfiles.size(); fileInd++ ){
				//documents that contain a String[] with the words 
				ANCdocument[] docs = readANCFile(cfiles.get(fileInd));
				
				//iterate over documents in the array
				for(Integer docInd =0; docInd<docs.length; docInd++){
	
					//document the String[] wordArr
					String[] words = docs[docInd].getWordArr();
					//add to wordcount
					corpusSize = corpusSize + words.length;
					
					//iterate over words
					for(Integer wordInd=0; wordInd<words.length; wordInd++){
						Integer clength = new Integer(aClength);
						
						//for simplicity and readability
						//the word in processing
						String w = words[wordInd].toLowerCase();
						
						//count independent frequency
						if(!(wordFreqs.containsKey(w))){
							wordFreqs.put(w, 0);
						}
						
						Integer newFreq = ((wordFreqs.get(w))+1);
						wordFreqs.put(w, newFreq);
						
						//if true a count of context words is performed
						if(terms.contains(w)){
							
							//ensure term is already in Map
							if(!(contextFreqsAll.containsKey(w))){
								//it is defined in the paper that the target word itself belongs to the context window
								HashMap<String, Integer> aMap = new HashMap<String, Integer>();
								aMap.put(w, 0);
								contextFreqsAll.put(w, aMap);
							}
							
							
							HashMap<String, Integer> contextFreqsTerm = contextFreqsAll.get(w);
							
							Integer newTargetFreq = contextFreqsTerm.get(w) + 1; 
							contextFreqsTerm.put(w, newTargetFreq);
							
							//TODO: this can be re-written to do it all in one pass, since target word itself belongs to context
							//count context words before searchterm
							//ensure we have enough context words before search term, decrease context otherwise
							if(wordInd<clength){
								clength=wordInd;
							}
	
							//count context words
							for(int cInd=1; cInd<=clength; cInd++){
								//for simplicity and readability
								//the context word in processing
								String cW = words[wordInd-cInd].toLowerCase();
								
								//ensure context word is in map
								if(!(contextFreqsTerm.containsKey(cW))){
									contextFreqsTerm.put(cW, 0);
								}
								//increase frequency of context word
								Integer newcFreq = ((contextFreqsTerm.get(cW))+1);
								contextFreqsTerm.put(cW, newcFreq);
								
							}
							clength = new Integer(aClength);
							//count context words after term 
							if((wordInd+clength)>=words.length){
								clength = (words.length - wordInd -1); 
							}
							for(int cInd=1; cInd<=clength; cInd++){
								String cW = words[wordInd+cInd].toLowerCase();
								
								//ensure context word is in map
								if(!(contextFreqsTerm.containsKey(cW))){
									contextFreqsTerm.put(cW, 0);
								}
								//increase frequency of context word
								Integer newcFreq = ((contextFreqsTerm.get(cW))+1);
								contextFreqsTerm.put(cW, newcFreq);
							}
							
							//put the updated counts back to contextmap
							contextFreqsAll.put(w, contextFreqsTerm);
							//System.out.print(w+", ");
						}//end if term
						
					}//end for words
					//System.out.print(".");
				}//end for docs
				System.out.print(".");
			}//for files
			System.out.print("\n");
			if(!(aResultFile==null)){
					if(!(aResultFile.equals(""))){
						resultFile = aResultFile;
					}
					
					RawCounts theCounts = new RawCounts(contextFreqsAll, wordFreqs);
					//write an RawCounts object
					writeRawCounts(theCounts, resultFile);
					return theCounts;
			}
			
			System.out.println(" ...done");
			RawCounts corpusCounts = new RawCounts(contextFreqsAll, wordFreqs);
			System.out.println("corpussize="+corpusCounts.getCorpusSize()+" typesize="+corpusCounts.getTypeSize());
			return corpusCounts;
		}
	}

	/**This method reads in the .tzip file containing BNC corpus Xml files, an d performs
	 * the counts of independent and dependent frequencies of the target words and their context words  
	 * */
	public RawCounts readBNC(TreeSet<String> terms, String pathToBNC, Integer cLength, String resultFile) throws FileNotFoundException{
			
			System.out.println("reading BNC Corpus from "+ pathToBNC);
			int c = 0;
			
			//context length of 0 is not allowed, would make no sense
			if(cLength==0){
				cLength= 1;
				System.err.println("context length of 0 is not allowed. was set to 1\ncall method readBNC() with a higher value for clength parameter to avoid this message");
			}
			//context window for counting dependent frequencies
			//as BNC xml are read by stream counting can only start when cLength words have been read in
			ArrayBlockingQueue<String> leftContext = new ArrayBlockingQueue<String>((cLength));
			ArrayBlockingQueue<String> rightContext = new ArrayBlockingQueue<String>((cLength+1));
			
			//the collected data
			//independent word frequencies
			HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
			//joint frequencies
			HashMap<String, HashMap<String, Integer>> contextFreqsAll = new HashMap<String, HashMap<String, Integer>>();
			
			//keep track of the corpusSize
			Integer corpusSize = 0;
			int filecounter = 0;
			int dircounter = 0;
			
			//stopword list in form of pos Tags
			TreeSet<String> stoptags = new TreeSet<String>();
			stoptags.add("CRD");
			stoptags.add("UNC");
			stoptags.add("ART");
			stoptags.add("PREP");
			stoptags.add("CONJ");
			stoptags.add("ORD");
			stoptags.add("PRF");
			//stoptags.add("NP0");
			//stoptags.add("PRON");
			
			if((pathToBNC==null)||pathToBNC.equals("")){
				pathToBNC = new String(DefBNCFile);
			}
			
			//access the gzipped BNC Corpus file
			FileInputStream fis = new FileInputStream(pathToBNC);
			//for processing the xml corpusfiles
			XMLInputFactory xmlFac = XMLInputFactory.newFactory(); 
			
			try {
				//decompress the gzipped file
				GZIPInputStream gzis = new GZIPInputStream( fis );
			
				//read the contained tar archive 
				TarArchiveInputStream tarIS = new TarArchiveInputStream(gzis);
				//an Entry is a file or directory in the archive
				TarArchiveEntry tarEntry = tarIS.getNextTarEntry();
				//
				XMLEventReader eventReader;
				ByteArrayInputStream byteIs;
				
				//iterate through the archive and read all contained files  
				while(tarEntry != null){
					
					//read a corpusfile
					if(tarEntry.isFile()){
						filecounter++;
						//open xml corpus file
						int size = tarIS.available();
						
						//System.out.println("file "+filecounter+"size "++
						
						//read from Stream to byte Array
						byte[] byteinput = new byte[size];
						tarIS.read(byteinput, 0, size);
						//transform back to stream for XML processing
						//TODO: find a way to work with array directly without transforming it to a stream again
						byteIs = new ByteArrayInputStream(byteinput);
						eventReader = xmlFac.createXMLEventReader(byteIs);
						
						//iterate through file
						while(eventReader.hasNext()){
							XMLEvent event = eventReader.nextEvent();
							
							if(event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("w") ){
									String pos = event.asStartElement().getAttributeByName(new QName("pos")).getValue();
									String altpos = event.asStartElement().getAttributeByName(new QName("c5")).getValue();
								if(!(stoptags.contains(pos) || stoptags.contains(altpos))){
									//for simplicity and readability
									String word = event.asStartElement().getAttributeByName(new QName("hw")).getValue();
									
									corpusSize++;
									
									//independent counts
									if(wordFreqs.containsKey(word)){
										wordFreqs.put(word, (wordFreqs.get(word)+1));} 
									else{
										wordFreqs.put(word, 1);
									}
									
									
									//////////////////////////
									//maintain context window
									if(rightContext.size() == (cLength+1)){
										
										//first word of right context is target word
										//count context words
										String rightHead = rightContext.peek(); 
										if(terms.contains(rightHead)){
											
											//ensure term is already in Map
											if(!(contextFreqsAll.containsKey(rightHead))){
												contextFreqsAll.put(rightHead, new HashMap<String, Integer>());
											}
											
											HashMap<String, Integer> contextMap = contextFreqsAll.get(rightHead);
											
											//right context
											//as defined in the paper the target word itself belongs to context
											for (String contextW : rightContext) {
												//ensure context word is in Map
												if(contextMap.containsKey(contextW)){
													//count up
													int freq = contextMap.get(contextW);
													contextMap.put(contextW, (freq+1));
												}
												else{
													//or put in map with count 1
													contextMap.put(contextW, 1);
												}
											}
											
											//left context
											for (String contextW : leftContext) {
												//ensure context word is in Map
												if(contextMap.containsKey(contextW)){
													//count up
													int freq = contextMap.get(contextW);
													contextMap.put(contextW, (freq+1));
												}
												else{
													//or put in map with count 1
													contextMap.put(contextW, 1);
												}
											}//end for left
									
										}
									
										//right context is full so swap over a word
										
										if(leftContext.size() == cLength){
											//if left context is full first remove a word
											leftContext.poll();
										}
										//remove from right context and put to left context
										leftContext.offer(rightContext.poll());
										//put new word to right context
										rightContext.offer(word);
											//System.out.println(event.asStartElement().getAttributeByName(new QName("hw")).getValue());
									}//end if
									else{
										//right context not full yet, so put word in
										rightContext.offer(word);
									}
								}//end if asStartelem
							}
						}//end while eventreader
						
						//process context residues
						while(rightContext.size() != 0 ){
							//first word of right context is target word
							//count context words
							String rightHead = rightContext.peek(); 
							if(terms.contains(rightHead)){
								
								//ensure term is already in Map
								if(!(contextFreqsAll.containsKey(rightHead))){
									contextFreqsAll.put(rightHead, new HashMap<String, Integer>());
								}
								
								HashMap<String, Integer> contextMap = contextFreqsAll.get(rightHead);
								
								//right context
								//as defined in the paper the target word itself belongs to context
								for (String contextW : rightContext) {
									//ensure context word is in Map
									if(contextMap.containsKey(contextW)){
										//count up
										int freq = contextMap.get(contextW);
										contextMap.put(contextW, (freq+1));
									}
									else{
										//or put in map with count 1
										contextMap.put(contextW, 1);
									}
								}
								
								//left context
								for (String contextW : leftContext) {
									//ensure context word is in Map
									if(contextMap.containsKey(contextW)){
										//count up
										int freq = contextMap.get(contextW);
										contextMap.put(contextW, (freq+1));
									}
									else{
										//or put in map with count 1
										contextMap.put(contextW, 1);
									}
								}//end for left
							}
							
							leftContext.poll();
							leftContext.offer(rightContext.poll());
						}//end while residues
						
						leftContext.clear();
							
						c++;
						if(c >= 50){
							System.out.print(".");
							c = 0;
						}
						
						byteIs.close();
						byteinput = null;	
					}
					if(tarEntry.isDirectory()){
						dircounter++;
					}
					//at the and of loop get the next entry
					tarEntry = tarIS.getNextTarEntry();
				}
				
			}//end try
			catch(IOException e){
				e.printStackTrace();
			}
			catch(XMLStreamException e){
				e.printStackTrace();
			}
			
		//System.out.println("files "+filecounter+" dirs "+dircounter);
		System.out.println("...done\nfiles " + filecounter + " words "+ corpusSize);
		//String[]  ww = {"computer", "stock", "tiger"};
		//String[] cc = {"systems", "personal", "software", "exchange", "market", "shares", "tiger", "computer", "stock" };
		//freqtester(wordFreqs, ww, contextFreqsAll,  cc );
		
		return new RawCounts(contextFreqsAll, wordFreqs);
	}
	
	private void freqtester(HashMap<String, Integer> indepfreq, String[] printWords, HashMap<String, HashMap<String, Integer>> cCounts, String[] cWords){
		for(String word : printWords){
			System.out.println( word +" : "+ indepfreq.get(word) );
			for(String cWord : cWords){
				if(cCounts.get(word).containsKey(cWord)){
					System.out.println("\t"+cWord+" : "+cCounts.get(word).get(cWord) );
				}
				else{
					System.out.println("\t"+cWord+" : 0");
				}
			}
		}
	}
	
	/**reads in a plain text file with nGram frequencies
	 * expected: tab, comma, whitespace spearated. first Element has to be the frequency.
	 * computes quasi collocational counts for the terms
	 *@param terms contains the terms for which the counts should be computed
	 *@param nGramFilename a plain text file with nGram counts, if it is empty String it 
	 *tries to use the DefNGramfile which is expected in working directory 
	 * */
	public RawCounts readFromNgramFile(TreeSet<String> terms, String nGramFilename, boolean saveResult){
		
		/**term map with counts of context words*/
		HashMap<String, HashMap<String, Integer>> termMap = new HashMap<String, HashMap<String, Integer>>();
		/**the independent frequency for each word in the corpus*/
		HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
		
		
		if(nGramFilename.equals("")){
			nGramFilename = new String(DefNGramFile);
		}
		System.out.println("reading nGram File: "+nGramFilename);
			
		File nGramFile = new File(nGramFilename);
		FileReader fr;
		BufferedReader br;
		
		//since there are only nGram counts available, independent frequencies of context words for the terms
		//have to be reverse computed. instead of going through the nGram file a second time and then collect
		//this stats. frequencies for all words in position 1 of an nGram are collected which should add up to 
		
		try{
			//open nGram file 
			fr = new FileReader(nGramFile);
			br = new BufferedReader(fr);
			//a row of the nGram table
			String aLine = br.readLine();
			
			System.out.println("processing nGram File for terms");
			
			while(!(aLine==null)){
			//for testing
			//for(int a = 0; a < 158000; a++){	
				
				String[] lineElmts = aLine.split("\t|,| ");
				//last array index for better readability
				final Integer li = (lineElmts.length-1);
				//System.out.print(lineElmts.length+" "+lineElmts[li]);
				
				///////////////////////////////
				//independent word frequencies
				//if word is not contained, add it
				if(!wordFreqs.containsKey(lineElmts[1])){
					wordFreqs.put(lineElmts[1], 0);
				}
				
				//assuming proper input here!! add freq from file to existing one
				Integer newWordFreq = wordFreqs.get(lineElmts[1]) + Integer.parseInt(lineElmts[0]);
				wordFreqs.put(lineElmts[1], newWordFreq);
				
				///////////////////////////////
				//proceeding to given terms
				//if the first or last element of the nGram is one of the terms
				//add frequency count of nGram Elements to the map for the term
				//get counts for context after word
				if(terms.contains(lineElmts[1])){
					//if term is not contained yet, add it
					if(!termMap.containsKey(lineElmts[1])){
						termMap.put(lineElmts[1], new HashMap<String, Integer>());
					}
					
					HashMap<String, Integer> contextMap = termMap.get(lineElmts[1]);
					//frequency of the nGram. assuming correct input here
					Integer freq = Integer.parseInt(lineElmts[0]);
					
					//add frequencies for the words of the nGram
					//b = 2 because count for term is not added
					//System.out.print("-> ");
					for(int b = 2; b<lineElmts.length; b++){
						
						//if context word not contained yet add it
						if(!contextMap.containsKey(lineElmts[b])){
							contextMap.put(lineElmts[b], 0);	
						}
						//get freq of context and the one from file
						Integer newfreq = contextMap.get(lineElmts[b])+freq;
						//put new freq for context word into the map
						contextMap. put(lineElmts[b], newfreq);
						
						//System.out.print(" "+lineElmts[b]+" : "+newfreq);
					}
					//System.out.println(" ");
					
				}//get counts for context before term
				if(terms.contains(lineElmts[li])){
					//if term is not contained yet, add it
					if(!termMap.containsKey(lineElmts[li])){
						termMap.put(lineElmts[li], new HashMap<String, Integer>());
					}
					
					HashMap<String, Integer> contextMap = termMap.get(lineElmts[li]);
					//frequency of the nGram. assuming correct input here
					Integer freq = Integer.parseInt(lineElmts[0]);
					
					//System.out.print("<- ");
					//add frequencies for the words of the nGrams
					//li is correct, because count for term is not added
					for(int b = 1; b<li; b++){
						
						//if context word not contained yet add it
						if(!contextMap.containsKey(lineElmts[b])){
							contextMap.put(lineElmts[b], 0);	
						}
						//get freq of context and the one from file
						Integer newfreq = contextMap.get(lineElmts[b])+freq;
						//put new freq for context word into the map
						contextMap. put(lineElmts[b], newfreq);
						
						//System.out.print(" "+lineElmts[b]+" : "+newfreq);
					}
					//System.out.println("");
				}//end if
				//read next line
				aLine = br.readLine();
			}//end while
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		catch(ArrayIndexOutOfBoundsException e){
			System.err.println("Error: list needs to contain at least 3 Elements  "+e.getMessage());
		}
		System.out.print("\n");
		
		if(saveResult){
			writeRawCounts(new RawCounts(termMap, wordFreqs), "ccounts.obj");
		}
		
		System.out.println("Construction from nGram file done");
		
		return new RawCounts(termMap, wordFreqs);
	}
	
	
	public RawCounts readFromNgramFile(TreeSet<String> terms, String nGramFilename, String saveFile){
		RawCounts theCounts = readFromNgramFile(terms, nGramFilename, false);
		
		writeRawCounts(theCounts, saveFile);
		
		System.out.println("Construction from nGram file done");
		
	return theCounts;
	}
	
	/**save a {@link RawCounts} Object that a read method contructed in specified file
	 * @param someCounts the counts that should be written to disk
	 * @param fileName filename where to save the object, if empty String default is used
	 * */
	private boolean writeRawCounts(RawCounts someCounts, String fileName){
		
		if(fileName.equals("")){
			fileName = new String(DefResultFile);
		}
		
		System.out.println("writing counts from Corpus to: "+fileName);
		
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		try {
		  fos = new FileOutputStream(fileName);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(someCounts);
		  
		  oos.close();
		  fos.close();
		}
		catch (IOException e) {
			System.err.println("writing object failed");
			e.printStackTrace();
		}
		finally {
		  if (oos != null) try { oos.close(); } catch (IOException e) {}
		  if (fos != null) try { fos.close(); } catch (IOException e) {}
		}
		
		return false;
	}
	
	/**reads the counts from a previously computed RawCount object.
	 * this is much faster, than computing from the nGram files or ANC files.
	 * but of course the object only contains data for the terms that were given for run on which it was contructed. 
	 * */
	public RawCounts readFromObj(String fileName){
		//TODO: potentially add the terms parameter to test if all terms are present in the object
		
		if(fileName==null || fileName.equals("")){
			fileName = DefResultFile;
			resultFile = DefResultFile;
		}
		else{
			resultFile = new String(fileName);
		}
		
		System.out.print("reading corpus counts object from "+fileName);
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		try {
		  fis = new FileInputStream(resultFile);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof RawCounts) {
		    RawCounts corpusCounts = (RawCounts)obj;
		    System.out.println(" ...done");
		    System.out.println("corpussize="+corpusCounts.getCorpusSize()+" typesize="+corpusCounts.getTypeSize());
		    ois.close();
		    fis.close();
		    return corpusCounts;
		  }
		  else{
			  throw new ClassNotFoundException("Class in file was not of type tagrelator.RawCounts");
			  
		  }
		   
		}
		catch (FileNotFoundException e) {
		  System.err.println("reading corpus object file returned null value because: "+e.getMessage());
		  //e.printStackTrace();
		  return null;
		}
		catch(IOException e){
			System.err.println("reading corpus object file returned null value because: "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		catch (ClassNotFoundException e) {
			System.err.println("reading corpus object file returned null value because:");
			e.printStackTrace();
		  return null;
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close();} catch (IOException e) {}
		}
	
	}
	
	private ANCdocument[] readANCFile(File file){
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		try {
		  fis = new FileInputStream(file);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof ANCdocument[]) {
		    ANCdocument[] corpusdocs = (ANCdocument[])obj;
		    //System.out.println(" ...done");
		    ois.close();
		    fis.close();
		    return corpusdocs;
		  }
		  else{
			  throw new ClassNotFoundException("Class in file was not of type ANCdocument[]");
		  }
		  
		}
		catch (FileNotFoundException e) {
		  System.err.println("reading ANCdocument Array object file returned null value because: "+e.getMessage());
		  //e.printStackTrace();
		  return null;
		}
		catch(IOException e){
			System.err.println("reading ANCdocument Array object file returned null value because: "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		catch (ClassNotFoundException e) {
			System.err.println("reading ANCdocument Array object file returned null value because:");
			e.printStackTrace();
		  return null;
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close();} catch (IOException e) {}
		}
	}
	
	/**copied from http://www.java-forum.org/allgemeines/33129-verzeichnisse-durchsuchen-bearbeiten-auslesen.html
	 * and modiefied
	 * */
	public ArrayList<File> searchFile(File dir, String find) {

		File[] files = dir.listFiles();
		ArrayList<File> matches = new ArrayList<File> ();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().endsWith(find) ) {
					
					matches.add(files[i]);
				}
				if (files[i].isDirectory()) {
					matches.addAll(searchFile(files[i], find));
				}
			}
		}
		return matches;
	}
	
}
	
	

