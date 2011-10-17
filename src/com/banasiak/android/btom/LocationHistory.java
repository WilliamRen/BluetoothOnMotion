package com.banasiak.android.btom;

import java.util.LinkedList;

import android.location.Location;
import android.util.Log;

/**
 * Stores previous locations discovered in 
 * a datastructure of fixed size.
 * 
 * Locations should be added sorted according to time
 * 
 * Also has responsibility of calculating the estimated speed
 * based on previous events
 * 
 * @author dagfinn.parnas
 *
 */
public class LocationHistory {
	int maxSize;
	LinkedList<Location> listLocation;
	
	
	public LocationHistory (int maxSize){
		this.maxSize=maxSize;
		listLocation= new LinkedList<Location>();
	}
	
	public void addLocation(Location newLocation){
		listLocation.addFirst(newLocation);
		
		//delete the last element if we larger than max size
		if(listLocation.size()> maxSize){
			listLocation.removeLast();
		}
	}
	
	/**
	 * Gets estimated speed in meters pr second
	 * 
	 * TODO: should this be refactored to service class?
	 * 
	 * @return
	 */
	public float getEstimatedSpeed(){
		try{ 
			Location currentLocation= listLocation.getFirst();
			Location lastLocation=listLocation.get(1);
			
			if(currentLocation!= null && lastLocation!=null){
				long timeMillis= currentLocation.getTime() - lastLocation.getTime();
				if(timeMillis/1000==0){
					return 0f;
				}
				
				float distance = currentLocation.distanceTo(lastLocation);
				return distance/(float)(timeMillis/1000); 
			}else {
				return 0f;
			}
		}catch (IndexOutOfBoundsException e) {
			return 0f;
			// TODO: handle exception
		}catch (Throwable t){
	    	Log.w(this.getClass().getName(), "Got exception during getEstimated speed",t);
	    	return 0f;
		}
	}
	
}
