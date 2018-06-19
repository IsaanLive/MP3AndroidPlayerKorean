package com.euroasiamp3.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.euroasiamp3.dbadapter.DownloadsDBAdapter;
import com.euroasia.ko.R;
import com.euroasia.ko.DownloadingActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

public class DownloadService extends Service {
	
	public final static String MY_ACTION = "MY_ACTION";
	
    private final IBinder mBinder = new LocalBinder();
    
	private DownloadsDBAdapter mDbHelper;
	
	public DownloadFilesTask dltask1;
	public DownloadFilesTask dltask2;
	public DownloadFilesTask dltask3;
	
    Handler tick_Handler = new Handler();
    MyThread tick_thread = new MyThread();
    
	public int bound = 0, http = 0, a = 0;
	
	public int timer = 0;

    @Override 
    public void onCreate(){
    	super.onCreate();
    	
        mDbHelper = new DownloadsDBAdapter(this);
        mDbHelper.open();
        mDbHelper.updateDownloaded();
        
        tick_Handler.post(tick_thread);
    }	
    
    public class LocalBinder extends Binder {
    	public DownloadService getService() {
            return DownloadService.this;
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
	public void toastmake(String title){
		Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
	}
   
	private class MyThread implements Runnable {
    	public void run() { 
    		Cursor cursor = mDbHelper.fetchnondownloaded();
    	       
    		if (cursor.moveToFirst()){ // data?
	    		int id = cursor.getInt(cursor.getColumnIndex("_id"));
	    		String title = cursor.getString(cursor.getColumnIndex("title"));
	    		String url = cursor.getString(cursor.getColumnIndex("url"));
	    		int size = (int) cursor.getDouble(cursor.getColumnIndex("dlsize"));
	    		
	    		if(http < 3){
	    			http++;
	    			
	    			int dltask = 1;
	    			
	    			if(dltask1 == null){
		    			dltask1 = new DownloadFilesTask(url, title, id, size, 1);
		    			dltask1.execute();
		    			dltask = 1;
	    			}else if(dltask2 == null){
		    			dltask2 = new DownloadFilesTask(url, title, id, size, 2);
		    			dltask2.execute();
		    			dltask = 2;
	    			}else if(dltask3 == null){
		    			dltask3 = new DownloadFilesTask(url, title, id, size, 3);
		    			dltask3.execute();
		    			dltask = 3;
	    			}

	    			mDbHelper.updateDownloaded(id, "2", dltask); // Update Downloaded Set To 2 So It Wont Be Called Again
					
	    			
	    			timer = 0;
	    		}
    		}
    		
    		timer++;
    		
    		cursor.close();
    		
    		if(timer >= 240){
    			tick_Handler.postDelayed(tick_thread, 25000);
    		}else{
    			tick_Handler.postDelayed(tick_thread, 2500);
    		}
    	}
    }    
	
	public class DownloadFilesTask  extends AsyncTask<String, Void, Void> {
		//Constants
		int totalSize, downloadedSize, totalKbRead;
		String url, filename;
		int id, numberd, handler = 0, handler2 = 0, doneamount, ongoing;
		
		//Identifiers for update timer
	    Handler tick_Handler = new Handler();
	    MyThread tick_thread = new MyThread();

	    Handler tick_Handler2 = new Handler();
	    //MyThread2 tick_thread2 = new MyThread2();
	    
	    //Notification Managers
		public Notification notification;
		public NotificationManager notificationManager;
	    
		public int fail = 0;
		
	    public DownloadFilesTask(String url, String filename, int id, int filesize, int number) {
	        super();
			this.url = url;
			this.filename = filename;
			this.id = id;
			this.numberd = number;
			totalSize = filesize;
			
    	    Intent intent = new Intent();
    	    intent.putExtra("yes", "yes");
    	    intent.setAction(MY_ACTION);
    	    sendBroadcast(intent);
	    }

		@Override
		protected Void doInBackground(String... params) {
			try {
				downloadingfile(url, filename);
			} catch (IOException e) {
				fail = 1;
				e.printStackTrace();
			} catch (Exception e) {
				fail = 1;
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... values){
			super.onProgressUpdate(values);

			if(notification == null){
				try{
					ongoingNotif("Downloading File", "Download Started: "+filename, filename);
					ongoing = 1;
				}catch(Exception e){
					
				}
			}
			
			if(handler == 0){
				tick_Handler.post(tick_thread);
			}
			
		}
	   
	    @Override
		protected void onCancelled() {
			if(numberd == 1){
				dltask1 = null;
			}else if(numberd == 2){
				dltask2 = null;
			}else if(numberd == 3){
				dltask3 = null;
			}
			
	        tick_Handler.removeCallbacks(tick_thread);
	        
			regularNotif("다운로드 취소됨","취소됨: "+filename,filename); 
			
			http--;
	        
	        File file = new File(Environment.getExternalStorageDirectory()+"/ABDownloader/"+filename);
	        file.delete();
	    }

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if(numberd == 1){
				dltask1 = null;
			}else if(numberd == 2){
				dltask2 = null;
			}else if(numberd == 3){
				dltask3 = null;
			}
			
			if(fail == 1){
				regularNotif("다운로드 실패","실패: "+filename,filename); 
				http--;
		        
		        File file = new File(Environment.getExternalStorageDirectory()+"/ABDownloader/"+filename);
		        file.delete();
		        
				mDbHelper.updateDownloaded(id, "-1");
			}else{
				regularNotif("다운로드 완료","완료: "+filename,filename); 
			}
			
    	    Intent intent = new Intent();
    	    intent.putExtra("yes", "yes");
    	    intent.setAction(MY_ACTION);
    	    sendBroadcast(intent);
		}		

		public void regularNotif(String contentTitle, String tickerText, String contentText ){
			String ns = Context.NOTIFICATION_SERVICE;
			notificationManager = (NotificationManager) getSystemService(ns);
			notificationManager.cancel(1);
			int icon = R.drawable.icon;
			long when = System.currentTimeMillis();
			
			notification = new Notification(icon, tickerText, when);
			Context context = getApplicationContext();
			 
			Intent notificationIntent = new Intent(DownloadService.this, DownloadingActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(DownloadService.this, 0, notificationIntent, 0);
			
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			notificationManager.notify(numberd+9, notification);
		}
		
		public void ongoingNotif(String contentTitle, String tickerText, String contentText ){
			String ns = Context.NOTIFICATION_SERVICE;
			notificationManager = (NotificationManager) getSystemService(ns);
			notificationManager.cancel(1);
			int icon = R.drawable.icon;
			long when = System.currentTimeMillis();
			
			notification = new Notification(icon, tickerText, when);
			Context context = getApplicationContext();
			 
			Intent notificationIntent = new Intent(DownloadService.this, DownloadingActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(DownloadService.this, 0, notificationIntent, 0);
			
			notification.flags = Notification.FLAG_ONGOING_EVENT;
			notification.setLatestEventInfo(context, contentTitle, "진행을 보려면 여기를 클릭하십시오", contentIntent);
			notificationManager.notify(numberd+9, notification);
		}
		
		//Downloading File Handler
		public void downloadingfile(String link, String filename) throws IOException{	        
	        URL url = new URL(link);

	        //create the new connection
	        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
	        urlConnection.setRequestMethod("GET");
	        urlConnection.setConnectTimeout(10000);
	        urlConnection.connect();
	        totalSize = urlConnection.getContentLength()/1024;
	        mDbHelper.updateDownloadSize(id, totalSize); // Update Downloaded Set To 2 So It Wont Be Called Again
			
	        String filepath = Environment.getExternalStorageDirectory()+"/music/"+getString(R.string.app_name);
	        
		    boolean exists = (new File(filepath)).exists();  
		    if (!exists){
		    	new File(filepath).mkdirs();
		    } 
		    
	        File file = new File(filepath, filename+"-["+getString(R.string.app_name)+"].mp3");
	        
		    if (file.exists()) {
				file.delete();
			}
	        //this will be used to write the downloaded data into the file we created
	        FileOutputStream fileOutput = new FileOutputStream(file);

	        //this will be used in reading the data from the internet
	        InputStream inputStream = urlConnection.getInputStream();
	        
	        //variable to store total downloaded bytes
	        downloadedSize = 0;

	        //create a buffer...
	        byte[] buffer = new byte[1024];
	        int bufferLength = 0;

	        Log.d("Debug", "ABDownloader Total Size: " + totalSize + " Link: "+url);
           
	        if(handler == 0){
            	publishProgress();
            }
	        
	        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
	                fileOutput.write(buffer, 0, bufferLength);
	                
	                downloadedSize += bufferLength;
		            totalKbRead = downloadedSize/1000;
	        }

	        Log.d("Debug", "ABDownloader Closing Connections For Download: " + link);

			mDbHelper.updateDownloaded(id, "0");
			
			ongoing = 0;
			
	        tick_Handler.removeCallbacks(tick_thread);
	        
	        fileOutput.close();
	        inputStream.close();
	        urlConnection.disconnect();
	        
	        http--;
	        
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		}
		
		private class MyThread implements Runnable {
	    	public void run() { 
	    		handler = 1;
		        
	    		
				if(bound == 1){
					doneamount = (int) (((double)totalKbRead/(double)totalSize)*100);
					
					if(doneamount > 100){
			        	doneamount = 100;
			        }
			        
		    	    Intent intent = new Intent();
		    	    intent.putExtra("type", "pbar");
		    	    intent.putExtra("pos", ""+doneamount);
		    	    intent.putExtra("id", ""+id);
		    	    intent.setAction(MY_ACTION);
		    	    sendBroadcast(intent);
				}
	    		tick_Handler.postDelayed(tick_thread, 1000);
	    	}
	    }    
	}
}
