package tnmoc.storageinventory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;


@SuppressLint("TrulyRandom")
public  class CollectiveAccessAdaptor {

	private static String STORAGE_LIST_FORMAT ="{\"bundles\":{\"ca_storage_locations.parent_id\":{\"convertCodesToDisplayText\":true}}}";
	private static String STORAGE_LIST_QUERY = "find/ca_storage_locations?q=*";
	//private static String STORAGE_LIST_CACHE = "storage_location_cach.jobj";
	
	
	protected static StorageLocation[] readStorageLocationsFromService(String serverURL, String authCookie, boolean enableSelfSigned
			) throws Exception
	{
		//if(enableSelfSigned) trustSelfSigned();
		org.apache.http.client.HttpClient client;
		if(enableSelfSigned) {
			client = new HttpClient();
			trustSelfSigned();
		}
		else client = new DefaultHttpClient();
		HttpGetWithEntity e = new HttpGetWithEntity(serverURL + "/" + STORAGE_LIST_QUERY);
		
		e.setHeader("Set-Cookie", authCookie);
		e.setHeader("Accept", "application/json");
		e.setHeader("Content-Type","application/json");
		StringEntity se = new StringEntity(STORAGE_LIST_FORMAT);
		e.setEntity(se);
		
		HttpResponse response = client.execute(e);
		
		if (response.getStatusLine().getStatusCode() < 200 && response.getStatusLine().getStatusCode() > 400) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatusLine().getStatusCode());
		}
		String retSrc = EntityUtils.toString(response.getEntity()); 
        // parsing JSON
        JSONObject result = new JSONObject(retSrc);
        JSONArray arroy = result.getJSONArray("results");
		ArrayList<StorageLocation> locations = new ArrayList<StorageLocation>();
		for(int i = 0; i< arroy.length(); i++){
			JSONObject o = arroy.getJSONObject(i);
			StorageLocation l = new StorageLocation();
			l.id = o.getInt("location_id");
			if(o.getString("ca_storage_locations.parent_id").length()>0){
				l.parentid = o.getInt("ca_storage_locations.parent_id");
			}
			l.label = o.getString("display_label");
			locations.add(l);
		}
		for(StorageLocation location : locations){
			String parent = "";
			parent = getParent(parent,location,locations);
			location.parent = parent;
		}
		Collections.sort(locations);
		return locations.toArray(new StorageLocation[]{});
	}
	
	private static String getParent(String parent, StorageLocation sl, ArrayList<StorageLocation> l){
		String _parent = "";
		if(sl.parentid>1){
			for(StorageLocation _sl : l){
				if(sl.parentid==_sl.id){
					if(_sl.label.length()>0)
						_parent = _sl.label + "/" + parent ;
					else _parent = _sl.label;
					if(_sl.parentid>-1) return  getParent(parent,_sl,l) + _parent;
					else return _parent;
				}
			}
		}
		return _parent;
	}
	
	protected static String getAuthCookie(String serverUrl, String username, String password, boolean enableSelfSigned) throws Exception{
		if(enableSelfSigned) trustSelfSigned();
		String server = serverUrl.replace("http://", "").replace("https://", "");
		String autenticationURLString = "https://" +username + ":" + password + "@"+server+"/find/ca_storage_locations";
		URL url = new URL(autenticationURLString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		//	
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}
		Map<String,List<String>> headers = conn.getHeaderFields();
		List<String> cookies = (List<String>)headers.get("Set-Cookie");
		//String[] cookies = cookieString.split("\\;");
		String authCookie = null;
		for(String c: cookies){
			if(c.startsWith("TNMoC")){
				authCookie = c;
				break;
			}
		}
		return authCookie;
		
	}
	
	
	private static void  trustSelfSigned(){
		try{
			HttpsURLConnection.setDefaultHostnameVerifier(
				new HostnameVerifier() {
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				}
			);
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, 
						new X509TrustManager[]{
							new X509TrustManager(){
								public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
								public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
								public X509Certificate[] getAcceptedIssuers() {
									return new X509Certificate[0];
								}
							}
						}, 
						new SecureRandom()
			);
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		}catch(Exception ex){
			Log.e("trustSelfSigned", ex.getMessage());
		}
	}
	
	
	
	
}
