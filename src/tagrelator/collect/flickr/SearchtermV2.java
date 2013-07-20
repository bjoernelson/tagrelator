package tagrelator.collect.flickr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**reimplementation of Searchterm class for more compact memory
 * */
public class SearchtermV2 implements Serializable, Iterable<MyPhotoV2>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5720819382192945554L;
	private ArrayList<MyPhotoV2> photos;
	private String theSearchterm;
	private int offset;
	private int page;
	private int totalResults;
	
	
	/////////////////
	//Constructor
	public SearchtermV2(String term){
		this.theSearchterm = term;
		this.offset = 0;
		this.page = 1;
		this.photos = new ArrayList<MyPhotoV2>();
		this.totalResults = 0;
	}
	
	////////////////
	//Copy Constructor
	public SearchtermV2(SearchtermV2 st){
		this(st.getSearchterm(), st.getOffset(), st.getPage(), st.getPhotos(), st.getTotalRes());
	}
	
	
	private SearchtermV2(String termIn, Integer offsetIn, Integer pageIn, ArrayList<MyPhotoV2> photosIn, Integer totalRes ){
		this.theSearchterm = termIn;
		this.offset = offsetIn.intValue();
		this.page = pageIn.intValue();
		this.photos = photosIn;
		
		this.totalResults = totalRes.intValue();
	}
	//////////////////////
	//rewrite Constructor-legacy from previous version of Searchterm
	public SearchtermV2(Searchterm st){
		this.offset = st.getOffset().intValue();
		this.page = st.getPage().intValue();
		this.totalResults = st.getTotalRes().intValue();
		
		this.theSearchterm = st.getSearchterm();
		
		ArrayList<MyPhoto> oldPhotos = st.getPhotos();
		
		this.photos = new ArrayList<MyPhotoV2>();
		
		for(int i=0; i<oldPhotos.size(); i++){
			this.photos.add(new MyPhotoV2(oldPhotos.get(i)));
		}
	}
	
	///////////////
	//inherited func
	@Override
	public Iterator<MyPhotoV2> iterator(){
		
		Iterator<MyPhotoV2> it = new Iterator<MyPhotoV2>() {
		
			private int currentIndex = 0;
		
			@Override
			public boolean hasNext(){
				return currentIndex < photos.size() && photos.get(currentIndex) != null;
			}
			@Override
			public MyPhotoV2 next(){
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
		return new String(this.theSearchterm);
	}
	
	public ArrayList<MyPhotoV2> getPhotos(){
		return new ArrayList<MyPhotoV2>(this.photos);
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
	public void addPhoto(MyPhotoV2 photo){
		this.photos.add(photo);
	}
	
	///////////
	//other
	//access to to trimtosize method of arraylist member
	public void trim(){
		photos.trimToSize();
	}
	
}
