package com.euroasia.ko;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.euroasiamp3.dbadapter.DownloadsDBAdapter;
import com.euroasiamp3.eula.GUtils;

public class ViewSongActivity extends Activity implements OnTouchListener, OnCompletionListener, OnBufferingUpdateListener {
    /** Called when the activity is first created. */
	DownloadsDBAdapter mDbHelper;
	
	public Button  download, share;
	public ImageButton play;
	public SeekBar sb;
	
	public String title, url;
	public int lid;
	int check_start=0;
	
	private MediaPlayer mediaPlayer;
	private int mediaFileLengthInMilliseconds; 

	private Handler handler = new Handler();
	public Runnable notification;
	public ProgressTask task;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.viewsong);
        
        //Prepare DBHelper
        mDbHelper = new DownloadsDBAdapter(this);
        mDbHelper.open();
        
        GUtils.getGTRACKER(this).trackPageViewEvent("ViewSongActivity");
        
        //Get Extras
        title = getIntent().getStringExtra("title");
        url = getIntent().getStringExtra("url");
        lid = getIntent().getIntExtra("lid", 0);
        
		//Action Bar Setup:		
		final Context thisact = this;
		
		TextView headerview = (TextView)this.findViewById(R.id.title_bar_text);
		headerview.setText(title);
		
		ImageView searchbutton = (ImageView)this.findViewById(R.id.action_search);
		searchbutton.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				if(!thisact.toString().contains("SearchActivity")){
					Intent searchintent = new Intent(thisact, SearchActivity.class);
					searchintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(searchintent);
				}
			}
		});
		
		ImageView homeicon = (ImageView)this.findViewById(R.id.logo_icon);
		homeicon.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				if(!thisact.toString().contains("DashboardActivity")){
					Intent searchintent = new Intent(thisact, DashboardActivity.class);
					searchintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(searchintent);
				}
			}
		});
		
        //GET UI ELEMENTS
        share = (Button)this.findViewById(R.id.share);
        play = (ImageButton)this.findViewById(R.id.button_play);
        download = (Button)this.findViewById(R.id.download);
		sb = (SeekBar)findViewById(R.id.progress_bar);
        
        //Check If File Already Exists
	    String path=Environment.getExternalStorageDirectory()+"/music/"+getString(R.string.app_name);  
	    File mediafile = new File(path, title+"-["+getString(R.string.app_name)+"].mp3");
	    if (mediafile.exists()) {
	    	download.setEnabled(false);
	    	download.setText("노래가 이미 다운로드 되었습니다");
		}
        
        //Buttons
		share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {		
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, R.string.checkout);
				i.putExtra(Intent.EXTRA_TEXT, getString(R.string.dlmessage)+ title + " : " +url);
				startActivity(Intent.createChooser(i, getString(R.string.share_song)));
			}
		});
		
        download.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		download.setEnabled(false);
        		
        		if(mDbHelper.fetchAllDownloads(url) == 0){
					long id = mDbHelper.createDownload(title, url, 1, 0);
					toastmake(getString(R.string.addedqueue) + " : " + id);
        		}
        	}
        });
        
		play.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if(task == null){
					task=new ProgressTask();
					task.execute(); 
				}else{
					if(check_start==1){
					if(!mediaPlayer.isPlaying()){
						mediaPlayer.start();
						handler.removeCallbacks(notification);
						primarySeekBarProgressUpdater();
						play.setImageResource(R.drawable.ic_media_pause);
					}else {
						mediaPlayer.pause();
						play.setImageResource(R.drawable.ic_media_play);
					}
					}
				}
        }});
        
        sb.setMax(99);
        sb.setOnTouchListener(this);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);

		FindLyrics fl = new FindLyrics();
		fl.execute();

        //Ads Test
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		//
	}

	class FindLyrics extends AsyncTask<Void, Void, Void>{
		String lyrics; 
		int fail = 0;
		
		@Override
		protected Void doInBackground(Void... arg0) {
			String searchQuery = title.replace("-", "");
			searchQuery = searchQuery.replace(" ", "+");
			Log.i("zacharia","search query:"+searchQuery);
			String searchResult = httpRun("http://search.azlyrics.com/search.php?q="+searchQuery).toString();
			 if(searchResult.equals("")){
				 fail = 1;
				lyrics = "텍스트 로딩 실패"; 
				return null;
			 }
			if(!searchResult.contains("Sorry, no results")){
				Log.i("zacharia", "searchResult: "+searchResult);
				String link = searchResult.split("<table width=100%>")[1];
				Log.i("zacharia", "link :"+link);
				link = link.substring(link.indexOf("<a href=\""), link.indexOf("\" rel"));
				link = link.replace("<a href=\"", "");
				Log.i("zacharia", "After modification  link :"+link);
				Log.d("Debug", link);
				
				String newline = System.getProperty("line.separator");
				
				String lR = httpRun(link).toString();
				if(lR.contains("<!-- start of lyrics -->")){
				lR = lR.substring(lR.indexOf("<!-- start of lyrics -->"), lR.indexOf("<!-- end of lyrics -->"));
				lR = lR.replace("<!-- start of lyrics -->", "");
				lR = lR.replace("<br />", newline);
				lR = lR.replace("</i>", "");
				lR = lR.replace("<i>", "");
				lyrics = lR;
				Log.d("Debug", lyrics);
				}
				else{
					fail = 1;
					lyrics = "가사를 찾을 수 없습니다";
					return null;
				}
			}else{
				fail = 1;
				lyrics = "가사를 찾을 수 없습니다";
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			TextView tv = (TextView)findViewById(R.id.lyrics_text);
			tv.setText(lyrics);
		}
		
	}
	
	public void toastmake(String title){
		Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
	}
	
	class ProgressTask extends AsyncTask<Integer, Integer, Void>{

		  @Override
		  protected void onPreExecute() {
		  }

		  @Override
		  protected Void doInBackground(Integer... params) {
				try {
					mediaPlayer.setDataSource(url); // setup song from http://www.hrupin.com/wp-content/uploads/mp3/testsong_20_sec.mp3 URL to mediaplayer data source
					mediaPlayer.prepare(); // you must call this method after setup the datasource in setDataSource method. After calling prepare() the instance of MediaPlayer starts load data from URL to internal buffer.
					check_start=1;
					publishProgress(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return null;
		  }
		  
		  protected void onProgressUpdate(Integer integers) {
			  if(integers == 1) {
			    Toast.makeText(ViewSongActivity.this, "노래를 버퍼링 하고 있습니다", Toast.LENGTH_SHORT).show(); 
			  }
		  }
			  
		  @Override
		  protected void onPostExecute(Void result) {
			  mediaFileLengthInMilliseconds = mediaPlayer.getDuration(); // gets the song length in milliseconds from URL
			
				if(!mediaPlayer.isPlaying()){
					mediaPlayer.start();
					play.setImageResource(R.drawable.ic_media_pause);
				}else {
					mediaPlayer.pause();
					play.setImageResource(R.drawable.ic_media_play);
				}

				handler.removeCallbacks(notification);
				primarySeekBarProgressUpdater();
		  }
	}
    
	@Override
	public void onBackPressed() {
		handler.removeCallbacks(notification);
		mediaPlayer.reset();
		if(mediaPlayer.isPlaying()){
			mediaPlayer.stop();
		}
		
		mediaPlayer.release();
		
		mDbHelper.close();
		
    	Intent i = new Intent(getBaseContext(), SearchActivity.class);
    	i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); 
    	ViewSongActivity.this.startActivity(i);
    	finish();
	    return;
	}

    private void primarySeekBarProgressUpdater() {
    	sb.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/mediaFileLengthInMilliseconds)*100)); // This math construction give a percentage of "was playing"/"song length"
		if (mediaPlayer.isPlaying()) {
			notification = new Runnable() {
		        public void run() {
		        	primarySeekBarProgressUpdater();
				}
		    };
		    handler.postDelayed(notification, 500);
    	}
    }
    
	public boolean onTouch(View v, MotionEvent event) {
		if(v.getId() == R.id.progress_bar){
			/** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
			if(mediaPlayer.isPlaying()){
		    	SeekBar sb = (SeekBar)v;
				int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
				mediaPlayer.seekTo(playPositionInMillisecconds);
			}
		}
		return false;
	}

	public void onCompletion(MediaPlayer mp) {
		play.setImageResource(R.drawable.ic_media_play);
		
		mediaPlayer.stop();
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		sb.setSecondaryProgress(percent);
	}

	@Override	
	protected void onResume() {
		super.onResume();
		if(mediaPlayer.isPlaying()){
			handler.removeCallbacks(notification);
			primarySeekBarProgressUpdater();
		}	
	}
	
	public String MD5(String md5) {
		   try {
		        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
		        byte[] array = md.digest(md5.getBytes());
		        StringBuffer sb = new StringBuffer();
		        for (int i = 0; i < array.length; ++i) {
		          sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
		       }
		        return sb.toString();
		    } catch (java.security.NoSuchAlgorithmException e) {
		    }
		    return null;
	}
	
	public ByteArrayOutputStream httpRun(String url){
		ByteArrayOutputStream output = null;
		try{
            DefaultHttpClient httpClient = new DefaultHttpClient();
            BasicHttpContext httpContext = new BasicHttpContext();
            HttpGet httpPost = new HttpGet(url);
            httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1");
            HttpResponse response = httpClient.execute(httpPost, httpContext);
    	    InputStream result = null;
    	    
            result = response.getEntity().getContent();
            
    		byte[] buffer = new byte[ 1024 ];
    		int size = 0;
    		output = new ByteArrayOutputStream();
    			
    		while( (size = result.read( buffer ) ) != -1 ) {
    			output.write( buffer, 0, size );
    		}

	    } catch (ClientProtocolException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } catch (java.lang.IllegalStateException e){
	    	e.printStackTrace();
	    }
		return output;
	}
	
	
	/*
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
		if(mediaPlayer.isPlaying()){
			primarySeekBarProgressUpdater();
		}	
    }
    */
}