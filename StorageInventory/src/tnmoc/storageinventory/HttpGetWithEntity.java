package tnmoc.storageinventory;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {

	public final static String METHOD_NAME = "GET";

	
	public HttpGetWithEntity(String url){
		super();
		try{
			setURI(new URI(url));
		}catch(Exception ex){
			System.err.println(ex);
		}
	}
	
	@Override
	public String getMethod() {
	    return METHOD_NAME;
	}
	

}
