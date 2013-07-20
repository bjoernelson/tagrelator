package tagrelator.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;



import tagrelator.collect.flickr.FlickrCollector;
import tagrelator.collect.flickr.MyPhotoV2;
import tagrelator.collect.flickr.SearchtermV2;
import tagrelator.collect.flickr.SearchtermListV2;
import tagrelator.pmi.RawCounts;
/**provides access and processing to Object that was contructed by the FlickrCollector
 * */
//TODO: add a user id filter at some point which allows only around 5 photo from the same user per searchterm
//TODO: add check for uniqueness of all photo ids in the collected data
//TODO: add a function to switch on this two checks by boolean values
public class FlickrReader {
	//private SearchtermListV2 flickrPhotoCounts;
	private HashMap<String, Integer> wordCounts;
	private HashMap<String, HashMap<String, Integer>> contextCounts;
	private HashMap<Integer, Integer> photoTagStats;
	private boolean tagstofile;
	private ArrayList<String> tagsUnknown;
	
	public final String DefFlickrStore = "tagstore.obj";
	public final String DefFlickrFolderRead = "FlickrCorpus";
	
	///////////////
	//Constructor
	//TODO: maybe not allow parameteless Constructor
	public FlickrReader() {
		//flickrPhotoCounts = new SearchtermListV2();
		wordCounts = new HashMap<String, Integer>();
		contextCounts = new HashMap<String, HashMap<String, Integer>>();
		photoTagStats = new HashMap<Integer, Integer>();
		tagstofile = false;
		tagsUnknown = new ArrayList<String>();
		
	}
	
	
	public FlickrReader(String objFileName){
		
		//flickrPhotoCounts = new SearchtermList();
		wordCounts = new HashMap<String, Integer>();
		contextCounts = new HashMap<String, HashMap<String, Integer>>();
		photoTagStats = new HashMap<Integer, Integer>();
		tagstofile = false;
		//readFlickrObj(objFileName);
	}
	
	/**method to print several files with statistical data for the corpus
	 * 
	 **/
	public void corpusStats(String flickrPath) throws IOException{
		
		//clear the contents in case there was a read in the lifetime of this object before
		wordCounts.clear();
		contextCounts.clear();
		photoTagStats.clear();
		
		File flickrFolder;
		
		if(flickrPath == null || flickrPath.equals("")){
			flickrFolder = new File(DefFlickrFolderRead);
		}
		else{
			flickrFolder = new File(flickrPath);
		}
		
		System.out.print("\nreading Flickr count objects from "+flickrFolder.getName()+"\n");
		
		//check if flickrPath is a directory
		if(!(flickrFolder.isDirectory())){
			throw new IOException(flickrPath+" is not a directory");
		}
		
		String statspath = flickrPath+File.separatorChar+"stats";
		
		File statsfolder = new File(statspath);
		if(!(statsfolder.isDirectory())){
			statsfolder.mkdir();
		}
		
		//get all files in folder ending with .obj, indicating that its a corpus file  
		ArrayList<File> corpusfiles = searchFile(flickrFolder, ".obj");
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		
		//printstream to print tags for the photos
		PrintStream tagps = new PrintStream(new File(statspath+File.separatorChar+"phototags.csv"));
		//file to store stats for term
		PrintStream termps = new PrintStream(new File(statspath+File.separatorChar+"termstats.csv"));
		termps.println("term ; photos ; tagtypes; tagcount");
		
		HashMap<Integer, Integer> photosperterm = new HashMap<Integer, Integer>();
		
		HashMap<Integer, Integer> tagtypesperterm = new HashMap<Integer, Integer>();
		
		HashMap<Integer, Integer> tagsperterm = new HashMap<Integer, Integer>();
		//result sizes of terms equals their indepfreqs 
		HashMap<String, Integer> termfreqs = new HashMap<String, Integer>();
		
		//tags per photo stats
		PrintStream tagphoto = new PrintStream(new File(statspath+File.separatorChar+"tagsphoto.csv"));
		HashMap<Integer, Integer> tagphotoNumHash = new HashMap<Integer, Integer>();
		
		
		////////////////////////////////////////////
		//read wordcounts file for specified folder
		////////////////////////////////////////////
		{
			File fileWC = new File(flickrFolder.getAbsolutePath()+File.separatorChar+"wordfreqs.wcounts");
			
			FileInputStream fisWC = new FileInputStream(fileWC);
			ObjectInputStream oisWC = new ObjectInputStream(fisWC);
		
			try{
				Object objWC = oisWC.readObject();
			
				fisWC.close();
				oisWC.close();
			
			//check for appropriate class
			if(objWC instanceof HashMap<?, ?>){
				//TODO: circumvent the typecasting problem
				wordCounts = (HashMap<String, Integer>) objWC;
				}
			}
			catch(ClassNotFoundException e){
				System.err.println("Error: no class in wordcounts file, aborting");
				e.printStackTrace();
			throw new IOException("Error reading wordcounts: no class in file");
			}
			
			//printstream to write contents of word frequencies 
			PrintStream wordfreqs = new PrintStream("wordfreqs.csv");
			//iterate over words/tags
			for(String key : wordCounts.keySet()){
				wordfreqs.println(key+" ; "+wordCounts.get(key));
			}
			
			wordfreqs.close();
		}
		
		
		
		int index = 0;
		//count photos in in corpus
		int photocount = 0;
		//count of collected tags
		int tagcount  = 0;
		
		//read in the corpusfiles/ termfiles and count
		for(File aFile : corpusfiles ){
				  
			  fis = new FileInputStream(aFile);
			  ois = new ObjectInputStream(fis);
			  Object obj;
			  
			  try{
				  obj = ois.readObject();
			  }
			  catch(ClassNotFoundException e){
				  obj = new Object();
				  System.out.println(aFile.getName()+" did not contain a Object. ingoring it");
			  }
			  //close after reading
			  fis.close();
			  ois.close();
			  
			  Runtime rt = Runtime.getRuntime();
			  rt.gc();
			  
			  //Object where read in Object is finally held
			  SearchtermV2 aSearchterm;
			  
			  //check for appropriate class
			  if(obj instanceof SearchtermV2){
				aSearchterm = (SearchtermV2) obj;
				 
				//add to photocount
				photocount += aSearchterm.getPhotocount();
				
				//get the term and the result size
				termfreqs.put(aSearchterm.getSearchterm(), aSearchterm.getTotalRes());
				
				//get iterator over the photos for the term
				Iterator<MyPhotoV2> photoIt = aSearchterm.iterator();
				
				//counts for context words
				HashMap<String, Integer> photosContextC = new HashMap<String, Integer>();
				
				int termtagcount = 0;
				
				while(photoIt.hasNext()){
					
					MyPhotoV2 thephoto = photoIt.next();
					
					//get tags for the photo
					String[] tags = thephoto.getTags();
					//count tags
					tagcount += tags.length;
					termtagcount += tags.length;
					
					
					if(tagstofile){ tagps.print(thephoto.getId()); };
					
					tagphoto.println(thephoto.getId()+" ; "+ tags.length);
					if(!(tagphotoNumHash.containsKey(tags.length))){
						tagphotoNumHash.put(tags.length, 1);
					}else{
						tagphotoNumHash.put(tags.length	, (tagphotoNumHash.get(tags.length)+1));
					}
					
					for(int a = 0; a<tags.length; a++){
						
						String tag = tags[a].toLowerCase();
						
						if(tagstofile){tagps.print(";"+tag); }
						
						//do the same for context words of the photo
						//init if necessary
						if(!photosContextC.containsKey(tag)){
							photosContextC.put(tag, 0);
						}
						//add one to freq
						Integer newCfreq = photosContextC.get(tag) +1;
						photosContextC.put(tag, newCfreq);
						
					}//end for
					
					if(tagstofile){tagps.println(""); }
					
				}//end while
				
				termps.println(aSearchterm.getSearchterm() +" ; "+ aSearchterm.getPhotocount() +" ; "+ photosContextC.keySet().size() +" ; "+ termtagcount);
				
				//count how many terms have a certain amount of photos
				if(!(photosperterm.containsKey(aSearchterm.getPhotocount()))){
					photosperterm.put(aSearchterm.getPhotocount(), 1);
				}
				else{
					photosperterm.put(aSearchterm.getPhotocount(), (photosperterm.get(aSearchterm.getPhotocount())+1));
				}
				//count how terms have a certain amount of tagtypes
				if(!(tagtypesperterm.containsKey(photosContextC.keySet().size()))){
					tagtypesperterm.put(photosContextC.keySet().size(), 1);
				}
				else{
					tagtypesperterm.put(photosContextC.keySet().size(), (tagtypesperterm.get(photosContextC.keySet().size()+1)));
				}
				//count how many terms have a certain amount of tags
				if(!(tagsperterm.containsKey(termtagcount))){
					tagsperterm.put(termtagcount, 1);
				}
				else{
					tagsperterm.put(termtagcount, (tagsperterm.get(termtagcount)+1));
				}
				
				//after processing all photos for a term
		    	//add term and its counts to contextCount
		    	contextCounts.put(aSearchterm.getSearchterm().toLowerCase(), photosContextC);
		    	
			  }//end if
			  else{
				  System.out.println(aFile.getName()+" did not contain a Searchterm Object. ingoring it");
			  }
			  if(index == 10){
				  System.out.print(":");
				  index = 0;
			  }
			  index++;
		}
		
		termps.close();
		tagphoto.close();
		tagps.close();
		
		//print the stats
		
		{//general stats 
			PrintStream genStatsPs = new PrintStream(new File(statspath+File.separatorChar+"generalstats.csv"));
			genStatsPs.println("photos: "+ photocount +"\ntagcount: "+tagcount+"\ntag typesize: "+ wordCounts.keySet().size());
			genStatsPs.close();
		}
		
		//count and print word frequency types
		{
			HashMap<Integer, Integer> wCStats = new HashMap<Integer, Integer>(); 
			
			//count frequencies
			for(Integer val: wordCounts.values()){
				
				val = val/1;
				
				if(!wCStats.containsKey(val+1)){
					wCStats.put(val+1, 1);
				}
				else{
					wCStats.put(val+1, (wCStats.get(val+1)+1));
				}
			}
			
			PrintStream wordcountStatsPs = new PrintStream(new File(statspath+File.separatorChar+"wordcountstats.csv"));
			wordcountStatsPs.println("frequency ; words");
			for(Integer key : wCStats.keySet()){
				wordcountStatsPs.println(key +" ; "+ wCStats.get(key));
			}
			
			
			wordcountStatsPs.close();
		}
		
		//count small word frequencies
		{
			HashMap<String, Integer> smallfreqs = new HashMap<String, Integer>();
			
			int maxfreq = 100;
			
			for(String key: wordCounts.keySet()){
				
				if(wordCounts.get(key) <= maxfreq){
					smallfreqs.put(key, wordCounts.get(key));
				}
			}
			
			PrintStream smallfrequencyPs = new PrintStream(new File(statspath+File.separatorChar+"smallfrequencies.csv"));
			smallfrequencyPs.println("words with frequencies lower than " + maxfreq);
			for(String key : new TreeSet<String>(smallfreqs.keySet()) ){
				smallfrequencyPs.println(key +";"+ smallfreqs.get(key));
			}
			smallfrequencyPs.close();
		}
		
		//print the terms and their result sizes
		{
			PrintStream termfreqPs = new PrintStream(new File(statspath+File.separatorChar+"termfreqs.csv"));
			for(String key : new TreeSet<String>(termfreqs.keySet())){
				termfreqPs.println(key +";"+ termfreqs.get(key));
			}
			termfreqPs.close();
		}
		
		
		//how many tags per photo
		{
			PrintStream tagphotoNum = new PrintStream(new File(statspath+File.separatorChar+"tagsphotoNum.csv"));
			TreeSet<Integer> pkeyset = new TreeSet<Integer>( tagphotoNumHash.keySet());
			tagphotoNum.println("tagsperphoto; amountofphotos");
			for(int pkey : pkeyset){
				tagphotoNum.println(pkey+" ; "+tagphotoNumHash.get(pkey));
			}
			tagphotoNum.close();
			tagphotoNumHash.clear();
		}
		
		{
			PrintStream photopertermNum = new PrintStream(new File(statspath+File.separatorChar+"photopertermNum.csv"));
			photopertermNum.println("photocount ; amount");
			TreeSet<Integer> pkeyset = new TreeSet<Integer>(photosperterm.keySet());
			for(int pkey : pkeyset){
				photopertermNum.println(pkey+" ; "+photosperterm.get(pkey));
			}
			photopertermNum.close();
			photosperterm.clear();
		}
		
		{
			PrintStream tagtypespertermNum = new PrintStream(new File(statspath+File.separatorChar+"tagtypespertermNum.csv"));
			tagtypespertermNum.println("tagtypes ; amount");
			TreeSet<Integer> ttkeyset = new TreeSet<Integer>(tagtypesperterm.keySet());
			for(int tkey : ttkeyset){
				tagtypespertermNum.println(tkey +" ; "+tagtypesperterm.get(tkey));
			}
			tagtypespertermNum.close();
			tagtypesperterm.clear();
		}
		
		{
			PrintStream tagspertermNum = new PrintStream(new File(statspath+File.separatorChar+"tagspertermNum.csv"));
			tagspertermNum.println("tagtypes ; amount");
			TreeSet<Integer> tkeyset = new TreeSet<Integer>(tagsperterm.keySet());
			for(int tkey : tkeyset){
				tagspertermNum.println(tkey +" ; "+tagsperterm.get(tkey));
			}
			tagspertermNum.close();
			tagsperterm.clear();
		}
		
		System.out.println(" ...done");
	    
	    
	}
	
	public RawCounts readFlickrFolder(String flickrPath) throws IOException{
		
		//clear the contents in case there was a read in the lifetime of this object before
		wordCounts.clear();
		contextCounts.clear();
		photoTagStats.clear();
		
		File flickrFolder;
		
		if(flickrPath == null || flickrPath.equals("")){
			flickrFolder = new File(DefFlickrFolderRead);
		}
		else{
			flickrFolder = new File(flickrPath);
		}
		
		System.out.print("\nreading Flickr count objects from "+flickrFolder.getName()+"\n");
		
		//check if flickrPath is a directory
		if(!(flickrFolder.isDirectory())){
			throw new IOException(flickrPath+" is not a directory");
		}
		
		////////////////////////////////////////////
		//read wordcounts file for specified folder
		////////////////////////////////////////////
		{
			File fileWC = new File(flickrFolder.getPath()+File.separatorChar+"wordfreqs.wcounts");
			
			FileInputStream fisWC = new FileInputStream(fileWC);
			ObjectInputStream oisWC = new ObjectInputStream(fisWC);
			
			try{
				Object objWC = oisWC.readObject();
				
				fisWC.close();
				oisWC.close();
				
				//check for appropriate class
				if(objWC instanceof HashMap<?, ?>){
					//TODO: circumvent the typecasting problem
					wordCounts = (HashMap<String, Integer>) objWC;
				}
				
			}
			catch(ClassNotFoundException e){
				System.err.println("Error: no class in wordcounts file, aborting");
				e.printStackTrace();
				throw new IOException("Error reading wordcounts: no class in file");
			}
			
		}
		
		////////////////////////////////////////////
		//read the files and make context counts
		///////////////////////////////////////////
		
		//get all files in folder ending with .obj, indicating that its a corpus file  
		ArrayList<File> corpusfiles = searchFile(flickrFolder, ".obj");
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		
		int index = 0;
		
		for(File aFile : corpusfiles ){
				  
			  fis = new FileInputStream(aFile);
			  ois = new ObjectInputStream(fis);
			  Object obj;
			  
			  try{
				  obj = ois.readObject();
			  }
			  catch(ClassNotFoundException e){
				  obj = new Object();
				  System.out.println(aFile.getName()+" did not contain a Object. ingoring it");
			  }
			  //close after reading
			  fis.close();
			  ois.close();
			  
			  Runtime rt = Runtime.getRuntime();
			  rt.gc();
			  
			  //Object where read in Object is finally held
			  SearchtermV2 aSearchterm;
			  
			  //check for appropriate class
			  if(obj instanceof SearchtermV2){
				  aSearchterm = (SearchtermV2) obj;
				  
				//get iterator over the photos for the term
				Iterator<MyPhotoV2> photoIt = aSearchterm.iterator();
				//counts for context words
				HashMap<String, Integer> photoContextC = new HashMap<String, Integer>();
				
				while(photoIt.hasNext()){
					
					MyPhotoV2 thephoto = photoIt.next();
					//get tags for the photo
		    		String[] tags = thephoto.getTags();
		    		
		    		
		    		for(int a = 0; a<tags.length; a++){
		    			
		    			String tag = tags[a].toLowerCase();
		    			
		    			//init entry for a word is necessary
		    			if(!wordCounts.containsKey(tag)){
		    				//wordCounts.put(tag, 1);
		    				//System.err.println("Error: the tag "+tag+" is in context counts but not in wordcounts, set to 1\nrun with -collWC option to correct error");
		    				tagsUnknown.add(tag);
		    			}
		    			
		    			//do the same for context words of the photo
		    			//init if necessary
		    			if(!photoContextC.containsKey(tag)){
		    				photoContextC.put(tag, 0);
		    			}
		    			//add one to freq
		    			Integer newCfreq = photoContextC.get(tag) +1;
		    			photoContextC.put(tag, newCfreq);
		    			
		    		}//end for
				}//end while
				
				//after processing all photos for a term
		    	//add term and its counts to contextCount
		    	contextCounts.put(aSearchterm.getSearchterm().toLowerCase(), photoContextC);
		    	
			  }//end if
			  else{
				  System.out.println(aFile.getName()+" did not contain a Searchterm Object. ingoring it");
			  }
			  if(index == 10){
				  System.out.print(":");
				  index = 0;
			  }
			  index++;
		}
		
		tagsUnknown.trimToSize();
		if(tagsUnknown.size()>0){
			System.err.println("there are "+tagsUnknown.size()+" tags in the context counts that are not contained in the wordcounts\nrun with -collWC option to correct error");
		}
		System.out.println(" ...done");
		
	    
	    return new RawCounts(contextCounts, wordCounts);
	}
	
	
	
	/**reads a Flickr object constructed by the {@link FlickrCollector}
	 * @param objFileName file where the object is stored in
	 * */
	public RawCounts readFlickrObj(String objFileName){
		if(objFileName.equals("")){
			objFileName = new String(DefFlickrStore);
		}
		System.out.print("reading Flickr counts object from "+objFileName);
		
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		try {
		  fis = new FileInputStream(objFileName);
		  ois = new ObjectInputStream(fis);
		  Object obj = ois.readObject();
		  fis.close();
		  ois.close();
		  Runtime rt = Runtime.getRuntime();
		  rt.gc();
		  
		  if (obj instanceof SearchtermListV2) {
			SearchtermListV2 flickrPhotoCounts = (SearchtermListV2)obj;
		    
			System.out.print(" ...done\nprocessing contents");
		    
		    //do processing to Raw counts here
		    ArrayList<SearchtermV2> searchTermList = flickrPhotoCounts.getSearchtermList();
		    
		    Iterator<SearchtermV2> stIt = searchTermList.iterator();
		    
		    //Integer flickrCorpus
		    
		    //iterate over contained Searchterms
		    while(stIt.hasNext()){
		    	
		    	SearchtermV2 aSearchterm = stIt.next();
		    	//get iterator over the photos for the term
		    	Iterator<MyPhotoV2> photoIt = aSearchterm.iterator();
		    	
		    	//collect counts for context words
		    	HashMap<String, Integer> photoContextC = new HashMap<String, Integer>();
    			
		    	
		    	//iterate over photos for the searchterm
		    	while(photoIt.hasNext()){
		    		
		    		//get tags for the photo
		    		String[] tags = photoIt.next().getTags();
		    		
		    		for(int a = 0; a<tags.length; a++){
		    			
		    			String tag = tags[a].toLowerCase();
		    			//init entry for a word is necessary
		    			if(!wordCounts.containsKey(tag)){
		    				wordCounts.put(tag, 0);
		    			}
		    			//add one to frequency
		    			Integer newWfreq = wordCounts.get(tag)+1;
		    			wordCounts.put(tag, newWfreq);
		    			
		    			//do the same for context words of the photo
		    			//init if necessary
		    			if(!photoContextC.containsKey(tag)){
		    				photoContextC.put(tag, 0);
		    			}
		    			//add one to freq
		    			Integer newCfreq = photoContextC.get(tag) +1;
		    			photoContextC.put(tag, newCfreq);
		    			
		    		}//end for
 		    	}//end while
		    	
		    	//after processing all photos for a term
		    	//add term and its counts to contextCount
		    	contextCounts.put(aSearchterm.getSearchterm().toLowerCase(), photoContextC);
		    }
		    
		    RawCounts theCounts = new RawCounts(contextCounts, wordCounts);
		    
		    System.out.println(" ...done");
		    
		    return theCounts;
		  }
		  else{
			  throw new ClassNotFoundException("Class in file was not of type tagrelator.SearchtermList");
			  
		  }
		  
		  
		}
		catch (FileNotFoundException e) {
		  System.err.println("\nreading flickr object file returned null value because: "+e.getMessage());
		  //e.printStackTrace();
		  return null;
		}
		catch(IOException e){
			System.err.println("\nreading flickr object file returned null value because: "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		catch (ClassNotFoundException e) {
			System.err.println("\nreading flickr object file returned null value because:");
			e.printStackTrace();
		  return null;
		}
		finally {
		  if (ois != null) try { ois.close(); } catch (IOException e) {}
		  if (fis != null) try { fis.close();} catch (IOException e) {}
		}
	
	};//end method
	
	
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
	
	/**collects stats about the sizes of tagsets from the photos
	 * counts how many photos have tagset of size X where X 0..75 
	 * */
	private void statscount(Integer tagcount){
		if(photoTagStats.containsKey(tagcount)){
			photoTagStats.put(tagcount, (photoTagStats.get(tagcount)+1));
		}
		else{
			photoTagStats.put(tagcount, 1);
		}
		
	}
	
	private void statsToFile(int corpsize, int typesize, String statspath){
		
		//File statfile = new File("phototagstats.csv");
		PrintStream ps;
		try{
			ps =  new PrintStream(statspath+"phototagstats.csv");
			
			
			
			ps.println("tags; photos");
			TreeSet<Integer> sortedkeyset = new TreeSet<Integer>(photoTagStats.keySet()); 
			for(Integer key : sortedkeyset ){
				ps.println(key+"; "+photoTagStats.get(key));
			}
			
			ps.println("csize; "+corpsize);
			ps.println("types; "+typesize);
			
			ps.close();
			
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<String> getTagsUnknown(){
		return new ArrayList<String>(this.tagsUnknown);
	}
	
};//end obj

