package tagrelator.collect.flickr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

												//, Iterator<String>
public class Searchterm implements Serializable, Iterable<MyPhoto>{
	private ArrayList<MyPhoto> photos;
	private String theSearchterm;
	private Integer offset;
	private Integer page;
	private Integer totalResults;
	
	
	/////////////////
	//Constructor
	public Searchterm(String term){
		this.theSearchterm = term;
		this.offset = 0;
		this.page = 1;
		this.photos = new ArrayList<MyPhoto>();
		this.totalResults = 0;
	}
	
	////////////////
	//Copy Constructor
	public Searchterm(Searchterm st){
		this(st.getSearchterm(), st.getOffset(), st.getPage(), st.getPhotos(), st.getTotalRes());
	}
	
	private Searchterm(String termIn, Integer offsetIn, Integer pageIn, ArrayList<MyPhoto> photosIn, Integer totalRes ){
		this.theSearchterm = termIn;
		this.offset = offsetIn;
		this.page = pageIn;
		this.photos = photosIn;
		this.totalResults = totalRes;
	}
	
	///////////////
	//inherited func
	@Override
	public Iterator<MyPhoto> iterator(){
		
		Iterator<MyPhoto> it = new Iterator<MyPhoto>() {
		
			private int currentIndex = 0;
		
			@Override
			public boolean hasNext(){
				return currentIndex < photos.size() && photos.get(currentIndex) != null;
			}
			@Override
			public MyPhoto next(){
				//currentIndex++;
				//does it ever access first element in list
				return photos.get(currentIndex++);
			}
			@Override
			public void remove(){
				System.err.println("Sorry remove not supported by Iterator<MyPhoto>"); 
			}
			
		};
		return it;
	}
	
	///////////
	//getter
	public Integer getOffset(){
		return new Integer(offset);
	}
	
	public Integer getPage(){
		return new Integer(page);
	}
	
	public String getSearchterm(){
		return new String(theSearchterm);
	}
	
	public ArrayList<MyPhoto> getPhotos(){
		return new ArrayList<MyPhoto>(this.photos);
	}
	
	public Integer getPhotocount(){
		return photos.size();
	}
	public Integer getTotalRes(){
		return new Integer(this.totalResults);
	}
	
	///////////
	//setter
	public void setOffset(Integer newOffset){
		this.offset = newOffset;
	}
	
	public void setPage(Integer newPage){
		this.page = newPage;
	}
	public void setTotalRes(Integer total){
		this.totalResults = total;
	}
	
	////////////
	//add
	public void addPhoto(MyPhoto photo){
		this.photos.add(photo);
	}

}
