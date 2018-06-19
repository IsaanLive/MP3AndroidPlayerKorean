package com.euroasia.ko;

import android.content.Context;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class GooTracker {

	private static GoogleAnalyticsTracker tracker;
	private static Context                context ;
	private String                        appVersion   = "1.3" ;
	
	private String U_ID="UA-36411459-1";
	
	public GooTracker(Context con){
		 try {
	            context = con ;
	            tracker = GoogleAnalyticsTracker.getInstance ( ) ;
	            tracker.startNewSession ( U_ID , 20 , context ) ;
	            
	            appVersion = context.getPackageManager ( ).getPackageInfo ( context.getPackageName ( ) , 0 ).versionName ;
	        } catch ( Exception e ) {
	            Log.e ( "Exception" , "GTracker Message = " + e.toString ( ) ) ;
	        }
	}
	
	 public void trackAppStartedEvent ( ) {
	        try {
	            if ( trackerStarted ( ) ) {
	                tracker.trackEvent ( "Application launched" , "Launched" , "" + appVersion ,
	                        1 ) ;
	                String appVersion = context.getPackageManager ( ).getPackageInfo ( context.getPackageName ( ) , 0 ).versionName ;
	                String versionOfOS = android.os.Build.VERSION.RELEASE ;
	                
	                String phoneName = android.os.Build.MODEL ;
	                
	                tracker.setCustomVar ( 1 , "Android_OS", "" + versionOfOS ) ;
	                tracker.setCustomVar ( 2 , "Android_Application", "" + appVersion ) ;
	                tracker.setCustomVar ( 3 , "Android_Hardware" , "" + phoneName ) ;
	            }
	        } catch ( Exception e ) {
	            Log.e ( "Exception" , "trackAppStartedEvent Message = " + e.toString ( ) ) ;
	            e.printStackTrace ( ) ;
	        }
	        
	    }
	    
	    private boolean trackerStarted ( ) {
	        try {
	            if ( tracker == null ) {
	                if ( context != null ) {
	                    tracker = GoogleAnalyticsTracker.getInstance ( ) ;
	                    tracker.startNewSession ( U_ID , 20 , context ) ;
	                    return true ;
	                }
	                Log.e ( "Wait" , "Google Analytics tracker is not initialized" ) ;
	                return false ;
	            }
	        } catch ( Exception e ) {
	            Log.e ( "Exception" , "trackerStarted Message = " + e.toString ( ) ) ;
	            e.printStackTrace ( ) ;
	        }
	        
	        return true ;
	    }
	    
	    public void endsession(){
	    	try {
	            tracker.stopSession ( ) ;
	        } catch ( Exception e ) {
	            Log.e ( "Exception" , "endTrackingSession Message = " + e.toString ( ) ) ;
	            e.printStackTrace ( ) ;
	        }
	    }
	  
	    public void trackPageViewEvent ( String pageView ) {
	        try {
	            if ( trackerStarted ( ) ) {
	                tracker.trackPageView ( pageView ) ;
	            }
	        } catch ( Exception e ) {
	            Log.e ( "Exception" , "trackPageViewEvent Message = " + e.toString ( ) ) ;
	            e.printStackTrace ( ) ;
	        }
	        
	    }
}
