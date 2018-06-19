package com.euroasia.ko;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.euroasiamp3.dbadapter.DownloadsDBAdapter;
import com.euroasiamp3.eula.GUtils;
import com.euroasiamp3.services.DownloadService;
import com.euroasiamp3.services.DownloadService.LocalBinder;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadingActivity extends ListActivity {
	
	MyReceiver myReceiver;
	DownloadService mService;
    boolean mBound = false;
    public MyAdapter adapter;
    
	private DownloadsDBAdapter mDbHelper;
	
	public TextView text;
	public Button clearall;

	private List<AdapterItem> items = new ArrayList<AdapterItem>();
	
    @Override
    public void onCreate(Bundle icicle) {
    	super.onCreate(icicle);
    	setContentView(R.layout.download);

        mDbHelper = new DownloadsDBAdapter(this);
        mDbHelper.open();
        
        GUtils.getGTRACKER(this).trackPageViewEvent("DownloadingActivity");
        
		//Action Bar Setup:		
		final Context thisact = this;
		
		TextView headerview = (TextView)this.findViewById(R.id.title_bar_text);
		headerview.setText("Downloads");
		
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
		
		
        this.text = (TextView)this.findViewById(R.id.totaldls);    
        this.clearall = (Button)this.findViewById(R.id.clearall);      
        
        //Total Downloads
        int totaldownloading = mDbHelper.fetchtotaler();
        if(totaldownloading >= 1){
        	text.setText("전체 다운로드: "+totaldownloading);
        }else{
        	text.setText("전체 다운로드: 0");
        }
        
        //Clear All
        this.clearall.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		mDbHelper.deleteDownloads();
        		
        		refreshlist();
        		
                int totaldownloading = mDbHelper.fetchtotaler();
                if(totaldownloading >= 1){
                	text.setText("전체 다운로드: "+totaldownloading);
                }else{
                	text.setText("전체 다운로드: 0");
                }
                
        		toastmake("재설정 완료 & 다운로드 실패");
        	}
        });
        
        refreshlist();
        
        //List View Scroll Speed
        ListView lv = getListView();
        lv.setCacheColorHint(Color.TRANSPARENT); // not sure if this is required for you. 
        lv.setFastScrollEnabled(true);
        lv.setScrollingCacheEnabled(false);
        /*
        //Ads Test
		final DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		if(getString(R.string.bannertype) == "1"){
			LinearLayout layout = (LinearLayout)findViewById(R.id.adView); 
			layout.post(new Runnable() { 
				public void run() {
					int screenWidth = dm.widthPixels; 
					String myAdId = getString(R.string.lowreslb); 
					if(screenWidth >= 468){ 
						myAdId = getString(R.string.highreslb); 
					} 
					AdController myController = new AdController(thisact, myAdId); 
					myController.loadAd();
				} 
			}); 
		}else if(getString(R.string.bannertype) == "2"){
		    AdView adView = new AdView(this, AdSize.BANNER, getString(R.string.admobBID));
		    LinearLayout layout = (LinearLayout)findViewById(R.id.adView);
		    layout.addView(adView);
		    adView.loadAd(new AdRequest());
		}else if(getString(R.string.bannertype) == "3"){
		    Hashtable<String, String> map = new Hashtable<String, String>();
		    MMAdView adView = new MMAdView(this, getString(R.string.MMID), MMAdView.BANNER_AD_BOTTOM, 30, map);
		    adView.setId(MMAdViewSDK.DEFAULT_VIEWID);
		    LinearLayout adFrameLayout = (LinearLayout)findViewById(R.id.adView);
		    LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT);
		    adFrameLayout.addView(adView, lp);
		} */
    }
    
    @SuppressWarnings("static-access")
	@Override
	protected void onStart() {
        super.onStart();
        
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(mService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);
        
        // Bind to LocalService
        Intent intent = new Intent(this, DownloadService.class);
        bindService(intent, mConnection, this.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mDbHelper.close(); 
        unregisterReceiver(myReceiver);
        // Unbind from the service
        if (mBound) {
        	mService.bound=0;
            unbindService(mConnection);
            mBound = false;
        }
        
        this.finish();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        	mService.bound=1;
        }
        
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };
    
	public void toastmake(String title){
		Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
	}	
	
	public class MyAdapter extends ArrayAdapter<AdapterItem> {       
		public MyAdapter(Context context, int textviewid) {
            super(context, textviewid);
            
		}
		
	    public void addAdapterItem(AdapterItem item) {
	        items.add(item);
	    }

	    @Override
		public int getCount() {
	      return items.size();
	    }

        @Override
        public AdapterItem getItem(int position) {
                return ((null != items) ? items.get(position) : null);
        }
        
	    @Override
		public long getItemId(int position) {
	            return position;
	    }

	    @Override
		public View getView(int position, View convertView, ViewGroup parent){
	    	View rowView = null;
	    	if(items.get(position).third == 100){
	    		rowView = getLayoutInflater().inflate(R.layout.download_divider, null);
	    		TextView progress = (TextView)rowView.findViewById(R.id.txtTitle);
	    		progress.setText(items.get(position).second);
	    	}else{

	    		int a = items.get(position).third;
	    		
	    		if(a == 2){
		    		rowView = getLayoutInflater().inflate(R.layout.download_list_progress, null);
		    		TextView summary = (TextView)rowView.findViewById(R.id.title);
		    		summary.setText(items.get(position).second);
	    		}else{
		    		rowView = getLayoutInflater().inflate(R.layout.download_list, null);
		    		TextView summary = (TextView)rowView.findViewById(R.id.title);
		    		summary.setText(items.get(position).second);
		    		
		    		TextView progress = (TextView)rowView.findViewById(R.id.progress);
		    		if(a == 1){
		        		progress.setText("다운로드 대기 중");
		    		}else if(a == 0){
		    			progress.setText("다운로드 완료");
		    		}else if(a == -1){
		        		progress.setText("다운로드 실패");
		    		}
	    		}
	    		mDbHelper.updatelvid(items.get(position).first, position);
	    	}
	    	
	    	return rowView;
	    }
	}
	
	@SuppressWarnings("static-access")
	@Override
	protected void onListItemClick(ListView l, View v, int position, final long id) {
		super.onListItemClick(l, v, position, id);
		
		if(items.get(position).third != 100){
			final long ida = items.get(position).first;
			
			Cursor cursor = mDbHelper.fetchId(items.get(position).first);
			
			final String title = cursor.getString(cursor.getColumnIndex(mDbHelper.KEY_TITLE));
			final double downloaded = items.get(position).third;
			final double dltotal = cursor.getDouble(cursor.getColumnIndex(mDbHelper.KEY_SIZE));
			final String filepath = Environment.getExternalStorageDirectory()+"/music/"+getString(R.string.app_name)+"/"+title+"-["+getString(R.string.app_name)+"].mp3";
			final int sid = cursor.getInt(cursor.getColumnIndex(mDbHelper.KEY_SID));
		
			cursor.close();
			
			if(downloaded == -1){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("다운로드 옵션")
				       .setCancelable(false)
				       .setNegativeButton("삭제", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                mDbHelper.deleteDownload(ida);
				                refreshlist();
				                toastmake("MP3 다운로드 삭제됨");
				           }
				       })
				       .setNeutralButton("재시도", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   mDbHelper.updateDownloaded(ida, "1"); 
				        	   refreshlist();
				        	   toastmake("재시도로 설정");
				           }
				       })
				       .setPositiveButton(getString(R.string.btn_cancel_name), new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
			}else if(downloaded == 0){
				final CharSequence[] items = {"MP3 열기", "벨소리로 설정", "다시 다운로드", "삭제", getString(R.string.btn_cancel_name)};
	
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("옵션을 선택");
				builder.setItems(items, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	if(items[item] == "MP3 열기"){
				    	    try {
				    	        //launch intent
				    	        Intent i = new Intent(Intent.ACTION_VIEW);
				    	        Uri uri = Uri.fromFile(new File(filepath)); 
				    	        String url = uri.toString();
	
				    	        //grab mime
				    	        String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				    	                MimeTypeMap.getFileExtensionFromUrl(url));
	
				    	        i.setDataAndType(uri, newMimeType);
				    	        startActivity(i);
				    	    } catch (Exception e) {
				    	    	toastmake("MP3 로딩 오류");
				    	        e.printStackTrace();
				    	    }
				    	}else if(items[item] == "벨소리로 설정"){
				    		File k = new File(filepath); // path is a file to /sdcard/media/ringtone
	
				    		ContentValues values = new ContentValues();
				    		values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
				    		values.put(MediaStore.MediaColumns.TITLE, title);
				    		values.put(MediaStore.MediaColumns.SIZE, (dltotal*1024));
				    		values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
				    		values.put(MediaStore.Audio.Media.ARTIST, getString(R.string.app_name));
				    		values.put(MediaStore.Audio.Media.DURATION, 230);
				    		values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
				    		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
				    		values.put(MediaStore.Audio.Media.IS_ALARM, false);
				    		values.put(MediaStore.Audio.Media.IS_MUSIC, false);
	
				    		//Insert it into the database
				    		Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.getAbsolutePath());
				    		Uri newUri = DownloadingActivity.this.getContentResolver().insert(uri, values);
	
				    		RingtoneManager.setActualDefaultRingtoneUri(
				    		  DownloadingActivity.this,
				    		  RingtoneManager.TYPE_RINGTONE,
				    		  newUri);
				    		
				    	}else if(items[item] == "삭제"){
							AlertDialog.Builder builder = new AlertDialog.Builder(DownloadingActivity.this);
							builder.setMessage("정말로 삭제하시겠습니까: " + title + " ?")
							       .setCancelable(false)
							       .setPositiveButton("예", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							        	    File file = new File(filepath);
							        	    file.delete();
							        	    mDbHelper.deleteDownload(ida);
							        	    refreshlist();
							                toastmake("MP3 삭제됨");
							           }
							       })
							       .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							        	   toastmake("MP3 삭제되지 않음");
							               dialog.cancel();
							           }
							       });
							AlertDialog alert = builder.create();
							alert.show();
				    	}else if(items[item] == "다시 다운로드"){
				    		mDbHelper.updateDownloaded(ida, "1"); 
				    		refreshlist();
				    		toastmake("재시도로 설정");
				    	}
				    }
				});
				AlertDialog alert = builder.create();
				alert.show();
			}else if(downloaded == 1){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("다운로드 옵션")
				       .setCancelable(false)
				       .setNegativeButton("삭제", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                mDbHelper.deleteDownload(ida);
				                refreshlist();
				                toastmake("MP3 다운로드 삭제됨");
				           }
				       })
				       
				       .setPositiveButton(getString(R.string.btn_cancel_name), new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
			}else if(downloaded == 2){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("다운로드 옵션")
				       .setCancelable(false)
				       .setNegativeButton("다운로드 중지", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   if(sid == 1){
									 mService.dltask1.cancel(true);
								}else if(sid == 2){
									 mService.dltask2.cancel(true);
								}else if(sid == 3){
									 mService.dltask3.cancel(true);
								}
					   			
								mDbHelper.updateDownloaded(ida, "-1");
					   			
								refreshlist();
					    		
				                toastmake("MP3 다운로드 중지됨");
				           }
				       })
				       .setPositiveButton(getString(R.string.btn_cancel_name), new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}
	
	class AdapterItem {
		public int first;
		public String second;
		public int third;

		public AdapterItem(int first, String second, int third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}
    
    public void refreshlist(){
    	items.clear();
    	adapter = new MyAdapter(this, 0);
    	//Current Downloading
    	Cursor a = mDbHelper.getEachDownload(2);
    	if(a.getCount() != 0){
    		adapter.addAdapterItem(new AdapterItem(102109, "현재 다운로드 중", 100));
    		a.moveToFirst();
        	for (int i = 0; i < a.getCount(); i++){
        		a.moveToPosition(i);
        		int v = (int) a.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_ROWID));
        		int x = (int) a.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_DOWNLOADED));
        		String c = a.getString(a.getColumnIndex(DownloadsDBAdapter.KEY_TITLE));

        		adapter.addAdapterItem(new AdapterItem(v, c, x));
        	}
    	}
    	a.close();
    	
    	//Pending Download
    	Cursor a1 = mDbHelper.getEachDownload(1);
    	if(a1.getCount() != 0){
    		adapter.addAdapterItem(new AdapterItem(102110, "다운로드 대기 중", 100));
			
        	for (int i = 0; i < a1.getCount(); i++){
        		a1.moveToPosition(i);
        		int v = (int) a1.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_ROWID));
        		int x = (int) a1.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_DOWNLOADED));
        		String c = a1.getString(a.getColumnIndex(DownloadsDBAdapter.KEY_TITLE));

        		adapter.addAdapterItem(new AdapterItem(v, c, x));
        	}
    	}
    	a1.close();
    	
    	//Finished Download
    	Cursor a11 = mDbHelper.getEachDownload(0);
    	if(a11.getCount() != 0){
    		adapter.addAdapterItem(new AdapterItem(102111, "다운로드 완료", 100));
			
        	for (int i = 0; i < a11.getCount(); i++){
        		a11.moveToPosition(i);
        		int v = (int) a11.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_ROWID));
        		int x = (int) a11.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_DOWNLOADED));
        		String c = a11.getString(a.getColumnIndex(DownloadsDBAdapter.KEY_TITLE));

        		adapter.addAdapterItem(new AdapterItem(v, c, x));
        	}
    	}
    	a11.close();
    	
    	//Failed Download
    	Cursor a111 = mDbHelper.getEachDownload(-1);
    	if(a111.getCount() != 0){
    		adapter.addAdapterItem(new AdapterItem(102112, "다운로드 실패", 100));
			
        	for (int i = 0; i < a111.getCount(); i++){
        		a111.moveToPosition(i);
        		int v = (int) a111.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_ROWID));
        		int x = (int) a111.getDouble(a.getColumnIndex(DownloadsDBAdapter.KEY_DOWNLOADED));
        		String c = a111.getString(a.getColumnIndex(DownloadsDBAdapter.KEY_TITLE));

        		adapter.addAdapterItem(new AdapterItem(v, c, x));
        	}
    	}
    	a111.close();

		setListAdapter(adapter);
    }
	
	public void updateprogress(int id, int pos){
		try{
			ListView listview = getListView();   
			View view = listview.getChildAt(id);
			
			ProgressBar progress = (ProgressBar)view.findViewById(R.id.progressBar1);
			progress.setProgress(pos);
			adapter.notify();
			adapter.notifyDataSetChanged();
		}catch(NullPointerException e){
			
		}
	}
	
    private class MyReceiver extends BroadcastReceiver {
    	 @Override
    	 public void onReceive(Context arg0, Intent arg1) {
    		 try{
	    		 if(mService.http != 0){
	    			 if(arg1.hasExtra("yes")){
	    				 refreshlist();
	    			 }else{
		    			//refreshlist();
					    int id = Integer.parseInt(arg1.getStringExtra("id"));
						Cursor cursor = mDbHelper.fetchId(id);
						
						final int lvid = cursor.getInt(cursor.getColumnIndex(DownloadsDBAdapter.KEY_LISTVIEW));
						
					    int pos = Integer.parseInt(arg1.getStringExtra("pos"));
					    updateprogress(lvid, pos);
	    			 }
	    		 }else{
	    			 try{
	    				mDbHelper.open();
	    				refreshlist();
	    			 }catch(RuntimeException e){
	    				 
	    			 }catch(Exception e){
	    				 
	    			 }
		    	 }
    		 }catch(NullPointerException e){
    			 
    		 }catch(Exception e){
    			 
    		 }
    	 }
    }
}