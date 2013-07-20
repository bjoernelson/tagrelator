package tagrelator.collect.flickr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import tagrelator.read.FlickrReader;

import com.aetrion.flickr.*;
import com.aetrion.flickr.photos.*;
import com.aetrion.flickr.tags.Tag;
import com.aetrion.flickr.tags.TagsInterface;

/**with this class you can connect to the Flickr API, search for Photos, and collect some data from the photos in the search results.<br>
 * the collected data is stored in a incremental fashion, which means you can process the results in several runs,<br>
 * because it is a time costly process.<br>
 * see the documentation of the collect method, for further information
 * */
public class FlickrCollector {
	
	//private TreeSet<String> collectedWords;
	private boolean quiet;
	public final static String DefStoreFile = "tagstore.obj";
	public final static String DefCorpusFolderRead = "FlickrCorpus";
	private String theCorpusFolderRead;
	
	public final static String DefCorpusFolderWrite = "FlickrCorpus";
	private String theCorpusFolderWrite;
	//conatains independent wordfreqs for terms and context tags
	private HashMap<String, Integer> wordCounts;
	
	
	//////////
	//Flickr
	//needed to access the flickr api
	
	private final String apikey;
	private final String sharedSecret;
	
	///////////////
	//Constructor
	//////////////
	/**to access the Flickr API you need an API key, and for some things the shared secret
	 * you can get both for free on <a href="http://www.Flickr.com/api">http://www.Flickr.com/api</a>"
	 * */
	public FlickrCollector( String anApikey, String aSharedSecret){
		this.apikey = new String(anApikey);
		this.sharedSecret = new String(aSharedSecret);
		this.quiet = false;
		this.wordCounts = new HashMap<String, Integer>();
		
	}
	
	/**collects photo data for the words in the terms parameter, and saves them to file
	 * @param terms the words for which a search on Flickr is executed, one search per term
	 * @param samplesizeIn determines how many photos in the searchresult are processed
	 * @param storeToOpen a file that has been written on a previous run of the FlickrCollector, if "" a default is used<br> 
	 * the processing of the Results is resumed at point where it stopped last time.<br>
	 * for now the actual search for the word is performed on every run, the cycling through the results is handled by indexes,
	 * which means you always work on up to date results, but you may miss or double process, 
	 * some photos because they changed position in the results, due to deletion or addition of matching photos on flickr
	 * no actual bitmap data is downloaded here, so there should be no bandwith issue.<br>
	 * what is saved  is the photo_id, userId of the owner, url to the photo, tagset of the photo
	 * @param storeToWrite give a filename to store the results in, if "" a default is used
	 * */
	private boolean collect(TreeSet<String> terms, Integer samplesizeIn, String storeToOpen, String storeToWrite){
		
		System.out.println("\nwelcome to Flickr tag collector v2\n");
		if((storeToOpen==null)||storeToOpen.equals("")){
			this.theCorpusFolderRead = DefStoreFile;
		}
		else{
			this.theCorpusFolderRead = storeToOpen;
		}
		
		//open store or create new
		//SearchtermList stStore = openStore(storeToOpen);
		
		SearchtermListV2 stStore = openStoreV2(theCorpusFolderRead);
		
		//check if folder to store collected data exists
		File indir = new File(DefCorpusFolderRead);
		if(!(indir.exists())){
			indir.mkdir();
			System.out.println("created Flickr Store folder");
		}
		
		//obtains the searchterms that in the store so far
		//TreeSet<String> stSet = stStore.getSearchStrings();
		TreeSet<String> stSet = stStore.getSearchStrings();
		
		if(stSet.isEmpty()){
			System.out.println("found no terms in Store. adding terms from list");
		}
		else{
			System.out.println("\n"+stSet.size()+" terms in the store\n");
			
			/**
			Iterator<String> it = stSet.iterator();
			while(it.hasNext()){
				String t = it.next();
				if(!terms.contains(t)){
					System.out.println(t+" is in store but not in given list. will be ignored");
				}
				else{
					System.out.println(t);
				}
			}*/
		}
		//if there are terms in the store that are not in the list
		//if(!terms.isEmpty()){
		//	System.out.println("found new terms in given list");
		//	Iterator<String> it = terms.iterator();
		//	while(it.hasNext()){
		//		System.out.println(it.next());
		//	}
		//}
		
		
		////////work can begin
		//connect to Flickr
		System.out.println("\nbegin collect\nconnecting to Flickr"); 
		//Flickr api key is needed for connection  
		Flickr myFlick = new Flickr(apikey);
		myFlick.setSharedSecret(sharedSecret);
		
		
		
		//needed to execute search
		PhotosInterface pIface = myFlick.getPhotosInterface();
		//get the list of searchterm objects
		
		Iterator<String> tIt = terms.iterator();
		
		//a Runtime instance
		Runtime rt = Runtime.getRuntime();
		int mb = 1024*1024;
		
		while(tIt.hasNext()){
			
			String aTerm = tIt.next().toLowerCase();
			
			//the tags/term to be queried on Flickr is stored in here
			SearchParameters aSearch = new SearchParameters();
			//a tag/term
			String[] tag = new String[1];
			tag[0] = aTerm;
			//set tags in Searchparameters
			aSearch.setTags(tag);
			
			//Searchresult stored in here
			PhotoList result = new PhotoList();
			
			//a searchterm object froom the store
			SearchtermV2 aSearchterm;
			//SearchtermV2 newSearchterm;
			
			//test if Searchterm is in store
			if(stStore.contains(aTerm)){
				aSearchterm = stStore.getSearchterm(aTerm);
				//newSearchterm = new SearchtermV2(aSearchterm);
			}
			else{
				System.out.println("new term "+aTerm+", adding to store");
				aSearchterm = new SearchtermV2(aTerm);
				//newSearchterm = new SearchtermV2(aTerm);
			}
			
			//execute the search 
			try{
				int offset = aSearchterm.getOffset(); 
				//is the page where last search stopped
				int page = aSearchterm.getPage();
				//results per page
				final Integer perpage = 10;
				
				//interface executes search with searchparameters
				result = pIface.search(aSearch, perpage, page);
				
				int oldoff = new Integer(offset);
				
				//length of results
				int len = result.getTotal();
				int samplesize = new Integer(samplesizeIn);
				
				int diff1 = len - ( offset +((page-1)*perpage) );// - samplesize;
				int diff = diff1 -samplesize;
				//compute all remaining results
				if(samplesize == 0){
					samplesize = diff;
				}//if samplesize is bigger than remaing results, cut it
				else if( diff < 0 ){
					samplesize = samplesize+diff;
				}
				
				System.out.println("\n"+tag[0]+ "\ntotal results="+len+" uncomputed results="+diff1+  " offset="+offset+" page="+page+ " samplesize="+ samplesize);
				
				///////////////////
				//retrieve the tags for the photos in the result
				//////////////////
				//get a taginterface
				TagsInterface tagIface = myFlick.getTagsInterface();
				
				//compute samplesize photos
				for(int i = 0; i < samplesize ; i++){
					
					//since tags are not contained in this version of photo
					//and it is not possible to search with taginterface
					//the photo has to be searched, then aquired again with
					//tagsinterface by Id, and then tags can be read out from it
					try{
						
						Photo picV1 = (Photo) result.get(offset);
						
						Photo picV2 = tagIface.getListPhoto( picV1.getId());
						//System.out.print(picV1.getId()+":  ");
						System.out.print(":");	
						
						//unsafe here, but getTags only returns a Collection
						//Documentation says its Collection of Tags
						//Collection<Tag> tags =   picV2.getTags();
						
						Collection<Tag> tags =   picV2.getTags();
						//TODO: check this Tagarray stuff
						ArrayList<Tag> tagslist = new ArrayList<Tag>(tags);
						tagslist.trimToSize();
						
						String[] tagarray = new String[tagslist.size()];
						Integer index = 0;
						
						//get iterator over tag collection
						Iterator<Tag> tagiter =  tags.iterator();
						
						//fill array with string version of tags
						//TODO: maybe keep tagtype for array
						while(tagiter.hasNext()){
							
							//put tag as string in array
							tagarray[index] = tagiter.next().getValue().toLowerCase();
							index++;
						}
						
						MyPhotoV2 newPhoto = new MyPhotoV2(picV1.getUrl() ,picV1.getId(), picV1.getOwner().getId(), tagarray);
						//MyPhotoV2 newPhotoV2 = new MyPhotoV2(newPhoto);
						//adds the photo to the Searchterm object
						aSearchterm.addPhoto(newPhoto);
						//newSearchterm.addPhoto(newPhotoV2);
					}
					catch(FlickrException e){
						//if photo is not present just go on
						if(e.getMessage().endsWith("Photo not found")){
							
						}
						else{throw e;}
					}
					catch(ArrayIndexOutOfBoundsException e){
						System.err.println("uups offset too big for results of term "+aTerm+" , going to next term ");
						e.printStackTrace();
					}
					
					
					//if a page of results is consumed, get next page
					if(offset<(result.size()-1)){
						offset++;
					}
					else if(page<result.getPages()){
						//start again on next page
						offset = 0;
						page++;
						//get the next page
						result = pIface.search(aSearch, perpage, page);
					}
					else{
						offset++;
						System.out.print("\nall results computed for "+ tag[0]);
					}
					
			    }//end for
				System.out.println("\nold off="+oldoff+" new off="+offset+" page=" +page);
				
				//set values after computation a Searchterm
				aSearchterm.setOffset(offset);
				aSearchterm.setPage(page);
				aSearchterm.setTotalRes(len);
				aSearchterm.trim();
				stStore.addSearchterm(aSearchterm);
				writeSt(aSearchterm, storeToWrite);
			
				long maxMem = rt.maxMemory();
				long allocMem = rt.totalMemory();
				long freeMem = rt.freeMemory();
				
				long realFreeMem = (maxMem - (allocMem - freeMem))/mb;
				
				System.out.println("free Memory: "+realFreeMem);
				if(realFreeMem < 70){
					rt.gc();
					allocMem = rt.totalMemory();
					freeMem = rt.freeMemory();
					realFreeMem = (maxMem - (allocMem - freeMem))/mb;
					
					if(realFreeMem < 70){
						System.out.print("programm is running out of memory. only "+realFreeMem+"mb\n writing intermediate results");
						writeStListV2(stStore, "temptagstore.obj");
						rt.gc();
					}
					
				}
				
			}
			catch(FlickrException e){
				System.err.println("Flickr in collect caused : ");
				e.printStackTrace();
			}
			catch(SAXException e){
				System.err.println("SAX in collect caused: ");
				e.printStackTrace();
			}
			catch (IOException e) {
				System.err.println("collect caused: ");
				e.printStackTrace();
			}
		}//end while
		
		//after computation write the searchterm store back to file
		//writeStList(stStore, storeToWrite);
		rt.gc();
		writeStListV2(stStore, storeToWrite);
		//TODO: change to true when everyting is done
		return true;
	}
	
	
	/**collects photo data for the words in the terms parameter, and saves them to file
	 * @param terms the words for which a search on Flickr is executed, one search per term
	 * @param samplesizeIn determines how many photos in the searchresult are processed
	 * @param aCorpusFolderRead a file that has been written on a previous run of the FlickrCollector, if "" a default is used<br> 
	 * the processing of the Results is resumed at point where it stopped last time.<br>
	 * for now the actual search for the word is performed on every run, the cycling through the results is handled by indexes,
	 * which means you always work on up to date results, but you may miss or double process, 
	 * some photos because they changed position in the results, due to deletion or addition of matching photos on flickr
	 * no actual bitmap data is downloaded here, so there should be no bandwith issue.<br>
	 * what is saved  is the photo_id, userId of the owner, url to the photo, tagset of the photo
	 * @param aCorpusFolderWrite give a filename to store the results in, if "" a default is used
	 * */
	public boolean collectV2(TreeSet<String> terms, Integer samplesizeIn, String aCorpusFolderRead, String aCorpusFolderWrite)throws IOException{
		
		System.out.println("\nwelcome to Flickr tag collector v3\n");
		
		//Flickr api key is needed for connection  
		Flickr myFlick = new Flickr(apikey);
		myFlick.setSharedSecret(sharedSecret);
		

		try{
			PhotosInterface mytest =  myFlick.getPhotosInterface();
			SearchParameters params = new SearchParameters();
			String[] tester = new String[1];
			tester[0] = "test";
			params.setTags(tester);
			mytest.search(params, 20, 1);
		}
		catch(IOException e){
			System.out.println("conntecting to Flickr failed!");
			System.out.println(e.getMessage());
			System.out.println("aborting the collect procedure");
		}
		catch(SAXException e){
			System.out.println("conntecting to Flickr failed!");
			System.out.println(e.getMessage());
			System.out.println("aborting the collect procedure");
		}
		catch(FlickrException e){
			System.out.println("conntecting to Flickr failed!");
			System.out.println(e.getMessage());
			System.out.println("aborting the collect procedure\nprovide proper Flickr API key with -apik, and Flickr shared secret with -secr options, for access to Flickr");
			return false;
		}
		
		///////////////////////////////////////////////////////
		//check if Corpusfolder is given otherwise use default
		if((aCorpusFolderRead==null)||aCorpusFolderRead.equals("")){
			this.theCorpusFolderRead = DefCorpusFolderRead;
		}
		else{
			this.theCorpusFolderRead = aCorpusFolderRead;
		}
		
		//check if if the folder for the corpus is present
		if(!(new File(theCorpusFolderRead).exists())){
			System.out.println("Flickr Corpus folder doesnt exist creating "+theCorpusFolderRead);
			new File(theCorpusFolderRead).mkdir();
		}
		
		
		//////////////////////////////////
		//check if output folder is given
		if(aCorpusFolderWrite==null||aCorpusFolderWrite.equals("")){
			//will be same as outputfolder
			theCorpusFolderWrite = DefCorpusFolderRead;
		}
		//check if output folder exists and create if necessary
		if(!(new File(theCorpusFolderWrite).exists())){
			System.out.println("Flickr Corpus output folder doesnt exist creating "+theCorpusFolderWrite);
			new File(theCorpusFolderWrite).mkdir();
		}
		
		
		//write a log
		PrintStream log = null;
		try{
			log = new PrintStream(theCorpusFolderWrite+File.separatorChar+"log.txt");
			Date now = new Date();
			log.println("collectV2 run at "+now+"\n");
		}
		catch(IOException e){
			System.out.println(e.getMessage());
			//e.printStackTrace(log);
			e.printStackTrace();
			return false;
		}
		
		//check if there are terms to collect
		if(terms.isEmpty()){
			System.err.println("Error. no terms to collect. check if wordpair file is empty");
			log.println("Error. no terms to collect. check if wordpair file is empty");
			return false;
		}
		
		
		//create the terms in the Corpus from the filenames in the Corpusfolder
		//File[] corpFiles = new File(theCorpusFolderRead).listFiles();
		File[] corpFiles = searchFile(new File(theCorpusFolderRead), ".obj").toArray(new File[0]);
		//this is where they go
		TreeSet<String> corpTermSet = new TreeSet<String>();
		
		//check if Corpus is empty 
		if( corpFiles == null){
			System.out.println("found no termfiles in corpus folder. adding terms from list");
			log.println("empty Corpus Folder "+theCorpusFolderRead+" all termfiles get created");
		}
		else{
			
			System.out.println("\n"+corpFiles.length+" terms in the corpus folder");
			log.println(corpFiles.length+" termfiles in the corpus folder "+ theCorpusFolderRead);
			//check if the terms in the corpus are the given termset
			for(int a = 0; a < corpFiles.length; a++ ){
				String cfilename = corpFiles[a].getName();
				cfilename.trim();
				cfilename = cfilename.replace(".obj", "");
				corpTermSet.add(cfilename);
				
				if(!terms.contains(cfilename) && !(quiet)){
					System.out.println(cfilename+" is in corpus folder but not in given list. will be ignored");
					log.println(cfilename+" is in corpus folder but not in given list. will be ignored");
				}
				/**else{ System.out.println(t);}*/
			}
		}
		
		
		//////////////////////////////////////////////
		//read the word frequency file or create new
		/////////////////////////////////////////////
		System.out.println("reading word frequencies file in "+theCorpusFolderRead);
		readWCounts();
		
		//if there are terms in the store that are not in the list
		//if(!terms.isEmpty()){
		//	System.out.println("found new terms in given list");
		//	Iterator<String> it = terms.iterator();
		//	while(it.hasNext()){
		//		System.out.println(it.next());
		//	}
		//}
		
		
		////////////////////////////////////////
		//	work can begin
		///////////////////////////////////////
		
		//connect to Flickr
		System.out.println("\nbegin collect\nconnecting to Flickr");
		log.println("\n\nbegin collect\nconnecting to Flickr");
		
				
		//needed to execute search
		PhotosInterface pIface = myFlick.getPhotosInterface();
		
		//Iterator over the Set of Terms that should be collected 
		Iterator<String> termIter = terms.iterator();
		
		//a Runtime instance
		Runtime rt = Runtime.getRuntime();
		long maxMem = rt.maxMemory();
		int mb = 1024*1024;
		
		//for every term in the list
		//read corresponding Searchterm object from file if it exists, otherwise create
		//collect "samplesize" Samples, add them to corpus
		//store it back on disk
		while(termIter.hasNext()){
			
			//the term to collect
			String aTerm = termIter.next().toLowerCase();	
			if(!(wordCounts.containsKey(aTerm))){
				addWCount(aTerm, myFlick);
			}
			
			//a searchterm object from the corpus
			SearchtermV2 aSearchterm;			
			//read in from corpus
			if(corpTermSet.contains(aTerm)){
				aSearchterm = openSearchtermFile(theCorpusFolderRead ,aTerm);
			}
			else{
				System.out.println("new term "+aTerm+", adding to store");
				aSearchterm = new SearchtermV2(aTerm);
			}
			
			
			//the tags/term to be queried on Flickr is stored in here
			SearchParameters aSearch = new SearchParameters();
			//a tag/term
			String[] tag = new String[1];
			tag[0] = aTerm;
			//set tags in Searchparameters
			aSearch.setTags(tag);
			
			//Searchresult stored in here
			PhotoList result = new PhotoList();
			
			int offset = aSearchterm.getOffset(); 
			//is the page where last search stopped
			int page = aSearchterm.getPage();
			//results per page
			final Integer perpage = 10;
			
			//execute the search if this fails termfile is written out unchanged
			try{
				log.print("\nsearch for term "+aTerm+" -- termstats: " );
				
				//interface executes search with searchparameters
				result = pIface.search(aSearch, perpage, page);
				
				
				int oldoff = new Integer(offset);
				
				//length of results
				int totalres = result.getTotal();
				//update in searchterm
				aSearchterm.setTotalRes(totalres);
				int samplesize = new Integer(samplesizeIn);
				
				//compute
				int remainRes = totalres - ( offset +((page-1)*perpage) );// - samplesize;
				if(remainRes<0){
					remainRes = 0;
				}
				//compute all remaining results
				if(samplesize == 0){
					samplesize = remainRes;
				}//if samplesize is bigger than remaing results, cut it
				else if( samplesize > remainRes ){
					samplesize = remainRes;
				}
				
				System.out.println("\n"+tag[0]+ "\ntotal results="+totalres+" uncomputed results="+remainRes+  " offset="+offset+" page="+page+ " samplesize="+ samplesize);
				log.println("\ntotal results="+totalres+" uncomputed results="+remainRes+  " offset="+offset+" page="+page+ " samplesize="+ samplesize);
				
				////////////////////////////////////////////////////
				//	retrieve the tags for the photos in the result
				////////////////////////////////////////////////////
				
				//get a taginterface
				TagsInterface tagIface = myFlick.getTagsInterface();
				
				//try on Photo layer
				try{
					log.println("getting photos from retrieved results");
					
					//compute samplesize photos
					for(int i = 0; i < samplesize ; i++){
						
						//since tags are not contained in this version of photo
						//and it is not possible to search with taginterface
						//the photo has to be searched, then aquired again with
						//tagsinterface by Id, and then tags can be read out from it						
						Photo picV1 = (Photo) result.get(offset);
						
						
						//try on tag layer
						try{
							Photo picV2 = tagIface.getListPhoto( picV1.getId());
							//System.out.print(picV1.getId()+":  ");
							System.out.print(":");	
							
							//unsafe here, but getTags only returns a Collection
							//Documentation says its Collection of Tags
							Collection<Tag> tags =   picV2.getTags();
							
							String[] tagarray = new String[tags.size()];
							Integer index = 0;
							
							//get iterator over tag collection
							Iterator<Tag> tagiter = tags.iterator();
							
							//fill array with string version of tags
							//TODO: maybe keep tagtype for array
							while(tagiter.hasNext()){
								String newtag = tagiter.next().getValue().toLowerCase();
								
								//if tag is not in the wordcounts get the frequency
								if(!(wordCounts.containsKey(newtag))){
									addWCount(newtag, myFlick);
								}
								
								//put tag as string in array
								tagarray[index] = newtag;
								index++;
							}
							
							MyPhotoV2 newPhoto = new MyPhotoV2(picV1.getUrl() ,picV1.getId(), picV1.getOwner().getId(), tagarray);
							//MyPhotoV2 newPhotoV2 = new MyPhotoV2(newPhoto);
							//adds the photo to the Searchterm object
							aSearchterm.addPhoto(newPhoto);
						}
						catch(Exception e){
							log.println("\nException while retrieving tags for photo "+offset+" on page "+page);
							log.println(e.getMessage());
							e.printStackTrace(log);
							
							System.err.print("\nException at retrieving tags for term "+aTerm+" at photo "+offset+" on page "+page);
							e.printStackTrace();
						}
						
						/////////////////////////////////////////////////
						//if a page of results is consumed, get next page
						if(offset<(result.size()-1)){
							offset++;
						}
						else if(page<result.getPages()){
							//start again on next page
							offset = 0;
							page++;
							//get the next page
							result = pIface.search(aSearch, perpage, page);
						}
						else{
							System.out.print("\nall results computed for "+ tag[0]);
						}
						
				    }//end for
				}
				catch(FlickrException e){
					log.println("\nFlickr Ecxeption at offset "+offset+" on page "+page);
					e.printStackTrace(log);
					//if photo is not present just go on
					if(e.getMessage().endsWith("Photo not found")){
						
					}
					else{throw e;}
				}
				catch(ArrayIndexOutOfBoundsException e){
					log.println("\nException at photo "+offset+" on page "+page);
					log.println(e.getMessage());
					e.printStackTrace(log);
					System.err.println("uups offset too big for results of term "+aTerm+" , going to next term ");
					e.printStackTrace();
				}
				catch(IndexOutOfBoundsException e){//TODO: has to be handled properly
					log.println("\nException at photo "+offset+" on page "+page);
					log.println(e.getMessage());
					e.printStackTrace(log);
					System.err.println("uups offset too big for results of term "+aTerm+" , going to next term ");
					e.printStackTrace();
				}
				
				
				System.out.print("\nold off="+oldoff+" new off="+offset+" page=" +page+" ");
				
				//set values after computing a Searchterm
				aSearchterm.setOffset(offset);
				aSearchterm.setPage(page);
				aSearchterm.setTotalRes(totalres);
				//trims Araylist that holds photos to actual size
				aSearchterm.trim();
				writeSt(aSearchterm, theCorpusFolderWrite);
				
				//try to free some memory if it gets short
				memManage(rt, maxMem, mb);
				
			}//try search
			catch(FlickrException e){
				log.println("\nretrieving Searchresults failed because of a Flickr Exception\ntermfile is written unchanged");
				log.println(e.getMessage());
				e.printStackTrace(log);
				writeSt(aSearchterm, theCorpusFolderWrite);
				System.err.println("Flickr in collect getting Searchresults caused : ");
				e.printStackTrace();
			}
			catch(SAXException e){
				log.println("\nretrieving Searchresults failed because of a SAX Exception\ntermfile is written unchanged");
				log.println(e.getMessage());
				e.printStackTrace(log);
				writeSt(aSearchterm, theCorpusFolderWrite);
				System.err.println("\nSAX in collect getting Searchresults caused: ");
				e.printStackTrace();
			}
			catch (IOException e) {
				log.println("\nretrieving Searchresults failed because of a IOException\ntermfile is written unchanged");
				log.println(e.getMessage());
				e.printStackTrace(log);
				writeSt(aSearchterm, theCorpusFolderWrite);
				System.err.println("collect getting Searchresults caused: ");
				e.printStackTrace();
			}
		}//end while
		
		//write the new file with word frequencies
		writeWCounts();
		wordCounts.clear();
		
		log.close();
		rt.gc();
		
		return true;
	}
	
	/**a costly function to ensure that all terms in corpus have same samplesize, if possible
	 * in firstpass all files in corpus are openend to get the amount of contained samples
	 * the max samplesize is determined
	 * in the second pass the all files are openend again and samples are collected until max samplessize, 
	 * unless there simply arent max samplesize samples 
	 * @param pathtoFolder the CorpusFolder 
	 * */ 
	public boolean repairCollect(String pathtoFolder){
		
		System.out.println("repair collect started");
		//corpus directory
		File dir = new File(pathtoFolder);
		//a corpus File
		File aFile;
		//for file read in
		ObjectInputStream ois;
		FileInputStream fis;
		//the maximum amount of Samples
		int maxSamples = 0;
		//the minimum amount of Samples
		int minSamples = Integer.MAX_VALUE;
		
		
		ArrayList<File> corpusFiles = searchFile(dir, ".obj");
		int[] samplesizesAll = new int[corpusFiles.size()];
		String[] terms = new String[corpusFiles.size()];
		
		//write a log
		PrintStream log = null;
		try{
			//write a log
			log = new PrintStream("log.txt");
			//log = new FileOutputStream(new File(pathtoFolder), true);
		
			Date now = new Date();
			log.print("\ncollect Repair run at "+now+"\n\nfirstcycle\n");
		}
		catch(IOException e){
			System.err.println("Error couldnt create logfile");
			e.printStackTrace();
			return false;
		}
		//go through corpusfiles
		for(int a = 0; a < corpusFiles.size(); a++){
			
			aFile = corpusFiles.get(a);
			try{
				fis = new FileInputStream(aFile);
				ois = new ObjectInputStream(fis);
				
				Object obj = ois.readObject();
				  
				if (obj instanceof SearchtermV2) {
					SearchtermV2 searchTerm = (SearchtermV2)obj;
					//System.out.println(" ...done");
					terms[a] = searchTerm.getSearchterm();
					//get amount of collected Samples
					samplesizesAll[a] = searchTerm.getPhotocount();
					if( maxSamples < searchTerm.getPhotocount()){
						maxSamples = searchTerm.getPhotocount();
					}
					if( minSamples > searchTerm.getPhotocount()){
						minSamples = searchTerm.getPhotocount();
					}
					
					ois.close();
					fis.close();
				}
				
			}
			catch(FileNotFoundException e){
				log.print(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
			catch(IOException e){
				log.print(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
			catch(ClassNotFoundException e){
				log.print(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
			System.out.print(".");
		};//end for
		
		System.out.println("\nfirst pass complete\ncorpus contains "+corpusFiles.size()+" terms");
		System.out.println("max Samples "+maxSamples+" minSamples "+minSamples+"\nsecond pass started");
		
		log.println("\nfirst pass complete\ncorpus contains "+corpusFiles.size()+" terms");
		log.println("max Samples "+maxSamples+" minSamples "+minSamples+"\nsecond pass started");
		
		int toCollect = 0;
		
		//second pass
		for(int i = 0; i < corpusFiles.size(); i++){
			//how many samples to collect
			toCollect = maxSamples - samplesizesAll[i];
			
			//do collect
			if(toCollect > 0){
				TreeSet<String> termSet = new TreeSet<String>();
				termSet.add(terms[i]);
				
				try{
					collectV2( termSet, toCollect, "", "");
				}catch(IOException e){
					System.err.println("Error in repair collect 2nd pass");
					e.printStackTrace();
				}
			}
		}//end for
		
		System.out.println("second pass finished\nfinal Stats");
		log.println("\nsecond pass finished\nfinal Stats");
		for(int a = 0; a < corpusFiles.size(); a++){
			
			aFile = corpusFiles.get(a);
			try{
				fis = new FileInputStream(aFile);
				ois = new ObjectInputStream(fis);
				
				Object obj = ois.readObject();
				  
				if (obj instanceof SearchtermV2) {
					SearchtermV2 searchTerm = (SearchtermV2)obj;
					System.out.println("Term "+searchTerm.getSearchterm()+" Samples "+searchTerm.getPhotocount());
					log.println("Term "+searchTerm.getSearchterm()+" Samples "+searchTerm.getPhotocount());
				}
				ois.close();
				fis.close();
			}
			catch(FileNotFoundException e){
				log.println(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
			catch(IOException e){
				log.println(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
			catch(ClassNotFoundException e){
				log.println(e.getMessage());
				e.printStackTrace(log);
				e.printStackTrace();
			}
		}//end for
		log.close();
		return true;
	}
	
	/**opens a saved processed results object.
	 * if there is none it creates a new one
	 * @param fileName the results file that should be opened, or created
	 * */
	public SearchtermList openStore(String fileName){
		if(fileName.equals("")){
			fileName = new String(DefStoreFile);
		}
		System.out.print("trying to read "+fileName);
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		try {
		  fis = new FileInputStream(fileName);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof SearchtermList) {
		    SearchtermList stlist = (SearchtermList)obj;
		    System.out.println(" ...done");
		    ois.close();
		    fis.close();
		    return stlist;
		  }
		  else{
			  return new SearchtermList();
		  }
		  
		  
		}
		catch (FileNotFoundException e) {
		  System.out.println(" could not find store "+fileName+"\ncreating new one");
		  //e.printStackTrace();
		  return new SearchtermList();
		}
		catch(IOException e){
			e.printStackTrace();
			return new SearchtermList();
		}
		catch (ClassNotFoundException e) {
		  e.printStackTrace();
		  return new SearchtermList();
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close(); 
		  	//alte version l�schen
		    //File oldtagstore = new File(fileName);
		    //if(oldtagstore.exists() ){
		    //	oldtagstore.delete();
		    //	System.out.println("old tagstore deleted");
		    //}
		  } catch (IOException e) {}
		}
	
	}
	
	public SearchtermListV2 openStoreV2(String fileName){
		if(fileName.equals("")){
			fileName = new String(DefStoreFile);
		}
		System.out.print("trying to read "+fileName);
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		try {
		  fis = new FileInputStream(fileName);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof SearchtermListV2) {
		    SearchtermListV2 stlist = (SearchtermListV2)obj;
		    System.out.println(" ...done");
		    ois.close();
		    fis.close();
		    
		    return stlist;
		  }
		  else{
			  return new SearchtermListV2();
		  }
		  
		  
		}
		catch (FileNotFoundException e) {
		  System.out.println(" could not find store "+fileName+"\ncreating new one");
		  //e.printStackTrace();
		  return new SearchtermListV2();
		}
		catch(IOException e){
			e.printStackTrace();
			return new SearchtermListV2();
		}
		catch (ClassNotFoundException e) {
		  e.printStackTrace();
		  return new SearchtermListV2();
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close(); 
		  	//alte version l�schen
		    //File oldtagstore = new File(fileName);
		    //if(oldtagstore.exists() ){
		    //	oldtagstore.delete();
		    //	System.out.println("old tagstore deleted");
		    //}
		  } catch (IOException e) {}
		}
	
	}
	
	/**loads a single SearchtermV2 from disk and returns it
	 * the serialized objects are named after the Searchterm they contain, 
	 * file ending .obj added.
	 * */
	public SearchtermV2 openSearchtermFile(String storePath, String aTerm){
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		String filtoOpen = new String(storePath+File.separatorChar+aTerm+".obj");
		
		//System.out.println("openening -"+filtoOpen+"-");
		
		try {
		  fis = new FileInputStream(filtoOpen);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof SearchtermV2) {
		    SearchtermV2 searchTerm = (SearchtermV2)obj;
		    //System.out.println(" ...done");
		    ois.close();
		    fis.close();
		    
		    return searchTerm;
		  }
		  else{
			  return new SearchtermV2(aTerm);
		  }
		  
		}
		catch (FileNotFoundException e) {
		  System.out.println(" could not find corpusfile "+aTerm+".obj\ncreating new one");
		  //e.printStackTrace();
		  return new SearchtermV2(aTerm);
		}
		catch(IOException e){
			e.printStackTrace();
			return new SearchtermV2(aTerm);
		}
		catch (ClassNotFoundException e) {
		  e.printStackTrace();
		  return new SearchtermV2(aTerm);
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close(); 
		  	//alte version loeschen
		    //File oldtagstore = new File(fileName);
		    //if(oldtagstore.exists() ){
		    //	oldtagstore.delete();
		    //	System.out.println("old tagstore deleted");
		    //}
		  } catch (IOException e) {}
		}
	}
	
	
	/**writes the result object to disk
	 * @param fileName name for the file
	 * */
	public void writeStList(SearchtermList aList, String fileName){
		if(fileName.equals("")){
			fileName = new String(DefStoreFile);
		}
		
		System.out.println("\nwriting tagstore to "+fileName);
		
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		try {
		  fos = new FileOutputStream(fileName);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(aList);
		  
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
		
	}
	
	/**writes the result object to disk
	 * @param fileName name for the file
	 * */
	public void writeStListV2(SearchtermListV2 aList, String fileName){
		if(fileName.equals("")){
			fileName = new String(DefStoreFile);
		}
		
		System.out.println("\nwriting tagstore to "+fileName);
		
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		try {
		  fos = new FileOutputStream(fileName);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(aList);
		  
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
		
	};
	
	public void writeSt(SearchtermV2 aSearchT, String aStorepath){
		
		if(aStorepath.equals("")){
			aStorepath = DefCorpusFolderRead+File.separatorChar+aSearchT.getSearchterm()+".obj";
		}
		else{
			aStorepath = aStorepath+File.separatorChar+aSearchT.getSearchterm()+".obj";
		}
		
		//System.out.println("\nwriting -"+aStorepath+"-");
				
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		try {
		  fos = new FileOutputStream(aStorepath);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(aSearchT);
		  
		  oos.close();
		  fos.close();
		}
		catch (IOException e) {
			System.err.println("writing object "+aStorepath+" failed");
			e.printStackTrace();
		}
		finally {
		  if (oos != null) try { oos.close(); } catch (IOException e) {}
		  if (fos != null) try { fos.close(); } catch (IOException e) {}
		}
	}
	


	private void  writeStats(int typesize, int tokensize, int basewords){
		File outfile = new File(DefCorpusFolderRead+"stats.txt");
		//FileOutputStream fos = new FileOutputStream(outfile)
		try{
			FileWriter fwr = new FileWriter(outfile);
			fwr.write("corpus contains "+basewords+" basewords\n");
			fwr.write("typesize "+typesize);
			fwr.write("tokensize"+tokensize);
		}
		catch(Exception e){	//stay quiet
		}
	}
	
	/** reads the file with the independent word counts. for occuring new words counts have to be added
	 **/
	private int readWCounts(){
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		String countsFile = theCorpusFolderRead+File.separatorChar+"wordfreqs.wcounts";
		
		try {
		  fis = new FileInputStream(countsFile);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  
		  if (obj instanceof HashMap<?, ?>) {
		    wordCounts = (HashMap<String, Integer>)obj;
		    System.out.println("read wordcounts file, done");
		    ois.close();
		    fis.close();
		    return 0;
		  }
		  else{
			  System.out.println("wrong class in wordcounts file, creating new one");
			  return 0;
		  }
		  
		  
		}
		catch (FileNotFoundException e) {
		  System.out.println(" could not find wordcountsfile "+"wordfreqs.wcounts"+"\ncreating new one");
		  //e.printStackTrace();
		  return 0;
		}
		catch(IOException e){
			
			e.printStackTrace();
			System.out.println("Error while reading wordcounts file, creating new");
			return 0;
		}
		catch (ClassNotFoundException e) {
		  e.printStackTrace();
		  System.out.println("wrong class in wordcounts file, creating new one");
		  return 0;
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close(); 
		  	//alte version l�schen
		    //File oldtagstore = new File(fileName);
		    //if(oldtagstore.exists() ){
		    //	oldtagstore.delete();
		    //	System.out.println("old tagstore deleted");
		    //}
		  } catch (IOException e) {}
		}
	}
	
	private void writeWCounts(){
		
		String wfreqfile = theCorpusFolderWrite+File.separatorChar+"wordfreqs.wcounts";
		
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		
		try {
		  fos = new FileOutputStream(wfreqfile);
		  oos = new ObjectOutputStream(fos);
		  
		  oos.writeObject(wordCounts);
		  
		  oos.close();
		  fos.close();
		}
		catch (IOException e) {
			System.err.println("writing wordcount object wordfreqs.wcounts failed");
			e.printStackTrace();
		}
		finally {
		  if (oos != null) try { oos.close(); } catch (IOException e) {}
		  if (fos != null) try { fos.close(); } catch (IOException e) {}
		}
	} 
	
	/**performs a Flickr Search on a single tag of a photo and stores the result size as independent tag/word frequency
	 * */
	private void addWCount(String aTag, Flickr flickrIface){
		
		//needed to execute search
		PhotosInterface photoIface = flickrIface.getPhotosInterface();
		
		//the tag to be queried on Flickr
		SearchParameters tagSearch = new SearchParameters();
		//the tag
		String[] tag = new String[1];
		tag[0] = aTag;
		//set tags in Searchparameters
		tagSearch.setTags(tag);
		
		//Searchresult
		PhotoList result = new PhotoList();
		
		try{
			//interface executes search with searchparameters
			result = photoIface.search(tagSearch, 100, 0);
			//sets the result size as tag frequency
			this.wordCounts.put(aTag, result.getTotal());
			if(!quiet){
				System.out.println("\nnew "+aTag+" = "+result.getTotal());
			}
			
		}
		catch(Exception e){
			System.err.println("Error while retrieving wordcount for "+aTag+", retry on next collect run");
			e.printStackTrace();
		}
		
	}
	
	public void allWfreqs(String aCorpusFolderRead){
		
		System.out.println("\nwelcome to Flickr tag collector v3\ncollect all wordcounts for given Corpus");
		
		///////////////////////////////////////////////////////
		//check if Corpusfolder is given otherwise use default
		if((aCorpusFolderRead==null)||aCorpusFolderRead.equals("")){
			this.theCorpusFolderRead = DefCorpusFolderRead;
			this.theCorpusFolderWrite = DefCorpusFolderWrite;
		}
		else{
			this.theCorpusFolderRead = aCorpusFolderRead;
			this.theCorpusFolderWrite = aCorpusFolderRead;
		}
		
		//check if if the folder for the corpus is present
		if(!(new File(theCorpusFolderRead).exists())){
			System.out.println("Flickr Corpus folder "+theCorpusFolderRead+" doesnt exist, aborting");
			return;
		}
		
		//write a log
		PrintStream log = null;
		try{
			log = new PrintStream(theCorpusFolderRead+File.separatorChar+"logWCounts.txt");
			Date now = new Date();
			log.println("collect all wordcounts run at "+now+"\n");
		}
		catch(IOException e){
			System.out.println("No log will be written sorry\n"+e.getMessage());
			e.printStackTrace();
		}
		
		//read the present corpus
		FlickrReader fReader = new FlickrReader();
		//read the wordcounts
		readWCounts();
		
		try{
			fReader.readFlickrFolder(theCorpusFolderRead);
			ArrayList<String> tagsUnknown = fReader.getTagsUnknown();
			
			Flickr flickrIface = new Flickr(apikey);
			flickrIface.setSharedSecret(sharedSecret);
			
			//TreeSet<String> termSet = contextCounts.keySet();
			
			for(String tagUnknown : tagsUnknown){
					addWCount(tagUnknown, flickrIface);
			}
			
			writeWCounts();
		}
		catch(IOException e){
			log.println("Error while reading the corpus");
			e.printStackTrace(log);
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
		matches.trimToSize();
		return matches;
	}
	
	public void setQuiet(boolean state){
		this.quiet = state;
	}
	
	public void memManage(Runtime rt, long maxMem, int mb){
		
		long allocMem = rt.totalMemory();
		long freeMem = rt.freeMemory();
		
		long realFreeMem = (maxMem - (allocMem - freeMem))/mb;
		if(!(quiet)){
			System.out.println("free Memory: "+realFreeMem+"\n");
		}
		if(realFreeMem < 70){
			rt.gc();
			allocMem = rt.totalMemory();
			freeMem = rt.freeMemory();
			realFreeMem = (maxMem - (allocMem - freeMem))/mb;
			
			if(realFreeMem < 70){
				System.out.print("programm is running out of memory. only "+realFreeMem+"mb");
				rt.gc();
			}
		}
	}
}