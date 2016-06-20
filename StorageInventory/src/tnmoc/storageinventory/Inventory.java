package tnmoc.storageinventory;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Inventory extends ActionBarActivity {
	private static String STORAGE_LIST_CACHE = "storage_location_cach.jobj";

	private static String worklist = "CollectiveAccessUpdate.csv";
	private AutoCompleteTextView storageLocationField = null;
	private static String PREFS_FILE = "CollectiveAccessConnection";
	private String serverURL = null;
	private EditText lotField ;
	private EditText objectField ;
	private TableLayout historyTable;
	private String authKey = null;
	private boolean enableSelfSigned = false;
	
	
	private OnClickListener deleteListener = new OnClickListener(){
		public void onClick(View v) {
			removeRow(v.getId());
		}
	};
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_form);
	     //   signIn(getCurrentFocus());
		storageLocationField = (AutoCompleteTextView) findViewById(R.id.locationSpinner);
		lotField =(EditText) findViewById(R.id.lotField);
		objectField =(EditText) findViewById(R.id.objectField);
		historyTable = (TableLayout) findViewById(R.id.history_table);
  		addItemsOnSpinner();
  		populateTable();
  		
	}

	private void addItemsOnSpinner() {
		try{
			StorageLocation[] cache = new ReadStaticDataCache().execute((Void)null).get();
		ArrayAdapter adapter = new ArrayAdapter(this,
		        android.R.layout.simple_spinner_item, cache);
			adapter.setDropDownViewResource
           (android.R.layout.simple_spinner_dropdown_item);
			storageLocationField.setAdapter(adapter);
		}catch(Exception ex){
			Log.e("addItemsOnSpinner", ex.getMessage());
		}
	
	}
	
	private void addRow(String[] row){
		TableRow r = new TableRow(historyTable.getContext());
		Button erase = new Button(r.getContext());
		erase.setText("X");
		erase.setId(historyTable.getChildCount());
		erase.setOnClickListener(deleteListener);
		r.addView(erase);
		for(String s : row){
			TextView t1 = new TextView(r.getContext());
			t1.setText(s);
			r.addView(t1);
		}
		
		historyTable.addView(r);
		
	}
	/* BUG
	 * At this point the system appears to have a race condition where we have to do this synchronously.
	 * Second problem appears to be we aren't updating the file here. 
	 * */
	private void removeRow(int i ){
		
		historyTable.removeViewAt(i);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.inventory, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}else if(id == R.id.menu_share) {
			 Intent i = new Intent();
			    i.setAction(Intent.ACTION_SEND);
			    i.setType("text/csv");
			    i.putExtra(Intent.EXTRA_STREAM, Uri.parse(Environment.getDataDirectory() + File.separator + worklist));
			    i.setData(Uri.parse(Environment.getDataDirectory() + File.separator + worklist));
			    startActivity(i);
		}else if(id== R.id.clear_history){
			historyTable.removeAllViews();
			
		}else if(id == R.id.refresh_storage_location){
			if(authKey==null ){
				signIn(getCurrentFocus());
			}
			else{
				populateStorageLocations();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void populateStorageLocations(){
		try{
			StorageLocation[] locations = new GetStorageLocationsTask().execute(enableSelfSigned).get();
			ArrayAdapter adapter = new ArrayAdapter(this,
			        android.R.layout.simple_spinner_item, locations);
				adapter.setDropDownViewResource
	           (android.R.layout.simple_spinner_dropdown_item);
				storageLocationField.setAdapter(adapter);
		}catch(Exception ex){
			Log.e("refresh_storage_locations",ex.getMessage());
		}
	}

	private void signIn(View V)
    {
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        String username = settings.getString("username", null);
        serverURL = settings.getString("serverURL", null);
    	final Dialog dialog = new Dialog(Inventory.this);
    	dialog.setContentView(R.layout.login);
    	dialog.setTitle("Login");
        final  EditText editTextUserName=(EditText)dialog.findViewById(R.id.editTextUserNameToLogin);
        final  EditText editTextPassword=(EditText)dialog.findViewById(R.id.editTextPasswordToLogin);
        final  EditText editTextServer=(EditText)dialog.findViewById(R.id.editServerNameToLogin);
        final  RadioButton enableSelfSignedB = (RadioButton)dialog.findViewById(R.id.rbutton_self_signed);
        if(serverURL!=null) editTextServer.setText(serverURL);  
        if(username!=null) editTextUserName.setText(username);
        Button btnSignIn=(Button)dialog.findViewById(R.id.buttonSignIn);
        // Set On ClickListener
        btnSignIn.setOnClickListener(new View.OnClickListener() {
                 
             public void onClick(View v) {
                 String username=editTextUserName.getText().toString();
                 String password=editTextPassword.getText().toString();
                 serverURL = editTextServer.getText().toString();
                 //alf.tnmoc.org/collections/service.php
                 SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
                 Editor e = settings.edit();
                 e.putString("username", username);
                 e.putString("serverURL", serverURL);
                 e.commit();
                 try{
                   enableSelfSigned = enableSelfSignedB.isChecked();
                   new LoginTask().execute(new String[]{serverURL,username, password, Boolean.valueOf(enableSelfSignedB.isChecked()).toString()}).get();
                   populateStorageLocations();
                 }catch(Exception ex){
                	 Log.e("clickSignIn",ex.getLocalizedMessage());
                	 ex.printStackTrace();
                	  dialog.dismiss(); 
                	  return;
                 }
                 dialog.dismiss();
                
             }
           });
         
           dialog.show();
          
    }
	
	private void populateTable(){
		try{
			Iterator<String[]> table = new ReadRecordsTask().execute((Void[])null).get().iterator();
			while(table.hasNext()){
				addRow(table.next());
			}
		}catch(Exception ex){
			Log.e("populateTable", ex.getMessage());
		}
	}
	 
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//SCAN LOCATION
		if (requestCode == 0) {		
			if (resultCode == RESULT_OK) {
				String s = intent.getStringExtra("SCAN_RESULT");
				Log.d("scanLocation","Scan returned '" + s.trim() + "'");
				ListAdapter sa = storageLocationField.getAdapter();
				boolean addNew = true;
			/*	for(int i = 0; i < sa.getCount(); i++){
					StorageLocation sl = (StorageLocation)sa.getItem(i);
					if(sl.label.equals(s.trim())){
						storageLocationSpinner.setSelection(i, true);
						addNew = false;
						Log.d("scanLocation","Scan matches a record, setting selection");
						
						break;
					}
				}*/
			/*	if(addNew==true){
					Log.d("scanLocation","Scan didn't match a record, add selection");
					((ArrayAdapter)sa).add(new StorageLocation(s.trim(),null, -1, -1));
					try{
						writeStorageLocationCache( );
					}catch(Exception ex){
						Log.e("scanLocation- new Location", ex.getMessage());
					}
				}*/
			} else if (resultCode == RESULT_CANCELED) {
				Log.d("scanLocation","Cancelled scan");
			}
		}
		//SCAN LOT
		else if (requestCode == 1) {		
			if (resultCode == RESULT_OK) {
				String s = intent.getStringExtra("SCAN_RESULT");
				lotField.setText(s.trim());
				objectField.setText("");
				Log.d("scanLot","Scan returned '" + s.trim() + "'");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d("scanLot","Cancelled scan");
			}
		}
		//SCAN ITEM
		else if (requestCode == 2) {		
			if (resultCode == RESULT_OK) {
				String s = intent.getStringExtra("SCAN_RESULT");
				objectField.setText(s.trim());
				Log.d("scanObject","Scan returned '" + s.trim() + "'");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d("scanObject","Cancelled scan");
			}
		}
		
	}
	
	private void showMessage(String text){
		if(text.length()>20)
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
		
	}
	


	public void onScan(View view){
		int target = -1;
		if(getCurrentFocus().getId()==R.id.locationSpinner) target=0;
		else if(getCurrentFocus().getId()==R.id.lotField) target=1;
		else if (getCurrentFocus().getId()==R.id.objectField) target=2;
    	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	intent.putExtra("SCAN_MODE", "DATA_MATRIX_MODE");
    	intent.putExtra("SCAN_WIDTH",200);
    	intent.putExtra("SCAN_HEIGHT",200);
    	
    	startActivityForResult(intent, target);    	
    }	
		 
	public void onClear(View view){
		if(getCurrentFocus().getId()==R.id.locationSpinner) {
			storageLocationField.setText(null);;
			lotField.setText(null);
			objectField.setText(null);
		}
		else if(getCurrentFocus().getId()==R.id.lotField) {
			lotField.setText(null);
			objectField.setText(null);
		}
		else if (getCurrentFocus().getId()==R.id.objectField) {
			objectField.setText(null);
		}
		else{
			clear();
		}
     	
    }
	   
/*
	public void onScanItem(View view){
    	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	intent.putExtra("SCAN_MODE", "DATA_MATRIX_CODE");
    	startActivityForResult(intent, 2);    	
    }
	 */
	public void onSave(View view){
		try{	
		
			
			
	    
			saveRecord(getStorageLocationIdId(), getLotId(), getObjectId());
			if(getCurrentFocus().getId()==R.id.locationSpinner) {
				lotField.setText("");
			}
			else if(getCurrentFocus().getId()==R.id.lotField) {
				objectField.setText("");
			}
			
			showMessage(getText(R.string.record_saved_successful).toString());
		}
		catch(Exception e){
			Log.e("save:", e.getMessage());
			showMessage(getText(R.string.record_saved_failed).toString());
		}
	}
	
	private String getLotId() throws Exception{
		String lotId = lotField.getText().toString();
		if(lotId ==null || lotId.length()==0){
			return "";
		}else return lotId;
	}
	
	private String getObjectId() throws Exception{
		String objectId =objectField.getText().toString();
		if(objectId==null || objectId.length()==0){
			return "";
		}else return objectId;
	}
	
	private String getStorageLocationIdId() throws Exception{
		String o = storageLocationField.getText().toString();
		if(o==null) 
		{
			return "";
		//	throw new Exception("Storage location not set.");
		}
	//	StorageLocation location = (StorageLocation)o;
		return o;		
	}
	
	private void saveRecord(String locationId, String lotId, String objectId){	
		String[] row = new String[]{Long.valueOf(System.currentTimeMillis()).toString(),locationId,lotId,objectId};
		addRow( row);
		new WriteRecordTask().execute(row);	
	}
	
	private void clear(){
		objectField.setText(null);
		lotField.setText(null);
		storageLocationField.setText(null);
	}
	  
	
	protected void tryAgain(){
		 signIn(getCurrentFocus()); 
	}
	
	
 	 
	
	private CSVWriter getWorkListWriter() {
		 try{
		    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
		    	FileOutputStream fOut = new FileOutputStream(Environment.getExternalStorageDirectory() +"/"+ worklist, true);
		    	return new CSVWriter( new OutputStreamWriter(fOut),',','"');
		    }else {
		    	 Log.e("getWorkListWriter","External media availabe write exception");
		    	 FileOutputStream fOut = openFileOutput(worklist, Context.MODE_PRIVATE);
		    	 return new CSVWriter( new OutputStreamWriter(fOut),',','"');
		    	
		    }
		 }catch(Exception e){
			 Log.e("getWorkListWriter","File write exception");
			 return null;
			 
		 }
	}
	
	private CSVReader getWorkListReader() {
		 try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				FileInputStream fOut = new FileInputStream(Environment.getExternalStorageDirectory() +"/"+ worklist);
				return new CSVReader( new InputStreamReader(fOut),',','"');
		    }else {
		    	 Log.e("getWorkListWriter","External media availabe read exception");
		    	 FileInputStream fOut = openFileInput(worklist);
				return new CSVReader( new InputStreamReader(fOut),',','"');
		    	// return null;
		    }
		 }catch(Exception e){
			 Log.e("getWorkListReader","File input exception");
			 return null;
			 
		 }
	}
	

	
	private class ReadRecordsTask extends AsyncTask<Void, Integer, List<String[]>>{
		@Override
		public List<String[]> doInBackground(Void... params) {
			try{
				CSVReader r = getWorkListReader();
				if(Looper.myLooper()==null) Looper.prepare();
				List<String[]> table = r.readAll();
			
				r.close();
				
				return table;
			}catch(Exception ex){
				showMessage(getText(R.string.record_saved_failed).toString());
				//Log.e("ReadRecordsTask", ex.getMessage());
				return new ArrayList<String[]>();
			}
		}
	}
	
	private class ClearRecordsTask extends AsyncTask<Void, Integer, String>{
		@Override
		protected String doInBackground(Void... params) {
			try{
				File f = new File(worklist);
				if(f.exists()){
					f.delete();
					f = null;
					showMessage(getText(R.string.record_history_clear).toString());
				}else{
					showMessage(getText(R.string.record_history_empty).toString());
				}
				clear();
				return null;
			}catch(Exception ex){
				showMessage(getText(R.string.record_history_clear_failure).toString());
				Log.e("clearRecords", ex.getMessage());
				return null;
			}
		}
	}
	
	
	private class WriteRecordTask extends AsyncTask<String[], Integer, String>{
		@Override
		protected String doInBackground(String[]... params) {
			try{
				CSVWriter w = getWorkListWriter();
				if(Looper.myLooper()==null) Looper.prepare();
				w.writeNext(params[0]);
				Log.i("file write", "CSV row written");
				showMessage(getText(R.string.record_saved_successful).toString());
				w.flush();
				w.close();
				
				return null;
			}catch(Exception ex){
				showMessage(getText(R.string.record_saved_failed).toString());
				Log.e("saveRecord", ex.getMessage());
				return null;
			}
		}
		
	}
	
	private class GetStorageLocationsTask extends AsyncTask<Boolean, Integer, StorageLocation[]>{
					
		@Override
		protected StorageLocation[] doInBackground(Boolean... params) {
			try{
				if(Looper.myLooper()==null) Looper.prepare();
				StorageLocation[] storageLocations = CollectiveAccessAdaptor.readStorageLocationsFromService(serverURL, authKey, params[0]);
				new WriteStaticDataCache().execute(storageLocations);
		    	return storageLocations;
			}catch(Exception ex){
				System.err.println(ex);
			}
			return null;
		}
			
	}
	
	private class LoginTask extends AsyncTask<String[], Integer, String>{
		@Override
		protected String doInBackground(String[]... params) {
			try{
				if(Looper.myLooper()==null) Looper.prepare();
				authKey = CollectiveAccessAdaptor.getAuthCookie(params[0][0], params[0][1], params[0][2], params[0][3].equalsIgnoreCase("true"));
				Log.i("LoginTask", "Login succeeded");
				showMessage("Logged in");
			}catch(Exception ex){
				Log.e("LoginTask", ex.getMessage());
				showMessage("Login failed");
			}
			return null;
		}
	}
	
	public class WriteStaticDataCache extends AsyncTask<StorageLocation[], Integer, Void>{
		@Override
		protected Void doInBackground(StorageLocation[]...locations ){
			try{
				if(Looper.myLooper()==null) Looper.prepare();
				Arrays.sort(locations);
				FileOutputStream cacheFile = openFileOutput( STORAGE_LIST_CACHE , Context.MODE_PRIVATE );
		    	ObjectOutputStream oStream = new ObjectOutputStream(cacheFile);
		    	oStream.writeObject(locations);
		    	oStream.flush();
		    	oStream.close();
		    	Log.d("writeStorageLocationCache", "Wrote " + locations.length + " storage location records to cache file.");
		
		    	return (Void)null;
			}catch(Exception ex){
				Log.e("WriteStaticDataCache", ex.getMessage());
			}
			return (Void)null;
		}
	}
	
	private class ReadStaticDataCache extends AsyncTask<Void, Integer, StorageLocation[]>{
		@Override
		protected StorageLocation[] doInBackground(Void... v){
			try{
				ObjectInputStream oStream = new ObjectInputStream(  openFileInput(STORAGE_LIST_CACHE));
			  	StorageLocation[] storageLocations = (StorageLocation[])oStream.readObject();
		      	oStream.close();
		      	Log.d("readStorageLocationCache", "Read " + storageLocations.length + " storage location records from cache file.");
		      	return storageLocations;
			}catch(Exception ex){
				Log.e("readStorageLocationCache", ex.getMessage());
		      	
				StorageLocation[] storageLocations = new StorageLocation[]{ new StorageLocation("location 1", "location 0", 1, 0) };
				return storageLocations;
		
			}
		}
	}
	
	
}
