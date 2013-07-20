package tagrelator.collect.flickr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import tagrelator.read.FlickrReader;


//extends HashMap<String, Searchterm> probably one day
/**a simple holder class used by the {@link FlickrCollector} and {@link FlickrReader} class
 * it is basically a {@link HashMap}, but for storage reasons it got a class wrapped around
 * for easy access to Searchterm objects by the String version of the Searchterm
 * */
public class SearchtermList implements Serializable{
	
	private HashMap<String, Searchterm> searchterms;
	
	/////////////////
	//Constructor
	public SearchtermList(){
		this.searchterms = new HashMap<String, Searchterm>();
	}
	
	///////////////
	//getter
	public ArrayList<Searchterm> getSearchtermList(){
		
		return new ArrayList<Searchterm>(searchterms.values());
	}
	
	public Searchterm getSearchterm(String stStr){
		return new Searchterm(searchterms.get(stStr));
	}
	/**get the Set of search terms in the list as Strings */
	public TreeSet<String> getSearchStrings(){
		return new TreeSet<String>(searchterms.keySet());
	}
	
	
	
	//////////////
	//add
	public boolean addSearchterm(Searchterm newST){
		searchterms.put(newST.getSearchterm(), newST);
		return true;
	}
	
	///////////
	//other
	/**test if a searchterm is present*/
	public boolean contains(String st){
		if(searchterms.containsKey(st)){
			return true;
		}
		else{
			return false;
		}
	}
	/**test if a searchterm is present*/
	public boolean contains(Searchterm st){
		return contains(st.getSearchterm());
	}
}
