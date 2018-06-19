package com.euroasia.ko;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.euroasiamp3.eula.GUtils;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Top10Activity extends ListActivity {
	
    //UI Elements
	public EditText searchEdit;
		
	private RetrieveSearch retrievesearch;
	
	public MyAdapter adapter;
	
	public int fail = 0, ia;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.top10);

        GUtils.getGTRACKER(this).trackPageViewEvent("Top10Activity");
		//Action Bar Setup:		
		final Context thisact = this;
		
		TextView headerview = (TextView)this.findViewById(R.id.title_bar_text);
		headerview.setText("인기 곡");
		
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
    	
        //List View Scroll Speed
        ListView lv = getListView();
        lv.setCacheColorHint(Color.TRANSPARENT); // not sure if this is required for you. 
        lv.setFastScrollEnabled(true);
        lv.setScrollingCacheEnabled(false);
        
        startsearch();		

        //Ads Test
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		//
    }
    
    public void startsearch(){
		TextView failresults = (TextView)findViewById(R.id.failresults);
		failresults.setText("");
			
		retrievesearch = new RetrieveSearch();
		retrievesearch.execute();
    }
    
	@Override
	public void onBackPressed() {
	    this.finish();
	    return;
	}
	
	public void toastmake(String title){
		Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
	}	
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        AdapterItem data = adapter.items.get(position);

        String title = data.first;
        String title1 = data.second;

		Intent intent = new Intent(Top10Activity.this, SearchActivity.class);
			
		intent.putExtra("search", title+" "+title1);
		
		startActivity(intent);
	}
	        
	
	private class RetrieveSearch extends AsyncTask<Void, Void, Void>{
		private ProgressDialog cancelDialog = null;
	
		@Override
		protected Void doInBackground(Void... params) {
			
			adapter = new MyAdapter();
			ia = 0;

			try{
				getTop10();
			} catch(IOException e) {
				e.printStackTrace();
				fail++;
			} catch(StringIndexOutOfBoundsException e){
				e.printStackTrace();
				fail++;
			}
			
			return null;
		}
	
		@Override
		protected void onCancelled() {		
			super.onCancelled();
			
	        try{
				cancelDialog.dismiss();
		        cancelDialog = null;
	        }catch(Exception e){
	        	
	        }
		}
	        
		@Override
		protected void onPreExecute() {
			cancelDialog = new ProgressDialog(Top10Activity.this);
			cancelDialog.setMessage("상위 10곡 노래를 가져오는 중…");
			cancelDialog.setButton(getString(R.string.btn_cancel_name), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) {
					retrievesearch.cancel(true);
			    }
			   });
			cancelDialog.show();

			super.onPreExecute();
		} 
	        
		@Override
		protected void onPostExecute(Void result) {
			if(ia == 0){
				fail = 0;
				
				adapter = new MyAdapter();
				setListAdapter(adapter);

				TextView failresults = (TextView)findViewById(R.id.failresults);
				
		        if(!isInternetConnectionActive(getApplicationContext())) {
					failresults.setText(R.string.no_active);
		        }else{
					failresults.setText(R.string.error_results);
		        }
			}
			
			setListAdapter(adapter);
			
			try{
				cancelDialog.dismiss();
				cancelDialog = null;
		    } catch (Exception e) {
		        // nothing
		    }

			super.onPostExecute(result);
		}
	        
		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
	}	 
	
	public void getTop10() throws IOException, MalformedURLException, NullPointerException{		
	    // Send data
	    URL url = new URL("http://www.apple.com/euro/itunes/charts/top10songs.html");
	    URLConnection conn = url.openConnection();
	    conn.setConnectTimeout(6000);
	    conn.setReadTimeout(6000);
	    
	    // Get the response
	    StringBuffer sb = new StringBuffer();
	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    
	    String line;
	    while ((line = rd.readLine()) != null) {
	    	sb.append(line);
	    }
	    
	    rd.close();
	    
	    String top10 = sb.toString();
	    top10 = top10.substring(top10.indexOf("<div class=\"grid5col\">"), top10.indexOf("<div class=\"column\">"));
	   
	    String[] links = top10.split("<li>");

		for (int i = 1; i < links.length; i++) {
			String content = links[i];
			String title, artist;
			
			title = content.substring(content.indexOf("<strong>"), content.indexOf("</strong>"));
			title = StringEscapeUtils.unescapeHtml(title);
			title = title.replaceAll("\\(.+?\\)", "");
			title = title.replaceAll("  ", " ");
			title = title.replaceAll("	", " ");
			title = title.replace("<strong>", "");
			
			artist = content.substring(content.indexOf("<span>"), content.indexOf("</span>"));
			artist = StringEscapeUtils.unescapeHtml(artist);
			artist = artist.replace("<span>", "");

			adapter.addAdapterItem(new AdapterItem(title, artist));
			ia++;
		}
	  	
	}
	
	public class MyAdapter extends BaseAdapter {
		private List<AdapterItem> items = new ArrayList<AdapterItem>();
		
	    public void addAdapterItem(AdapterItem item) {
	        items.add(item);
	    }

	    public int getCount() {
	      return items.size();
	    }

	    public Object getItem(int position) {
	      return items.get(position);
	    }

	    public long getItemId(int position) {
	            return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent){
	      View rowView = getLayoutInflater().inflate(R.layout.toptenitemview, null);
	      TextView firstTextView = (TextView) rowView.findViewById(R.id.txtTitle);
	      firstTextView.setText((position+1)+") " + items.get(position).first + " - " + items.get(position).second);	   
	      return rowView;
	    }

	}
	
	private boolean isInternetConnectionActive(Context context) {
	   	NetworkInfo networkInfo = ((ConnectivityManager) context
	   	    .getSystemService(Context.CONNECTIVITY_SERVICE))
	   	    .getActiveNetworkInfo();
	
	   	if(networkInfo == null || !networkInfo.isConnected()) {
	   		return false;
	   	}
		return true;
	}	
	
	class AdapterItem {
		public String first;
		public String second;

		public AdapterItem(String first, String second) {
			this.first = first;
			this.second = second;
		}
	}
}