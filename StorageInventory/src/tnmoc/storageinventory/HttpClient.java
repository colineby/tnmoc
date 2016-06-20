package tnmoc.storageinventory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;
//import javax.net.ssl.SSLContext;

public class HttpClient extends DefaultHttpClient {
	  
	 
	 @Override
	 protected ClientConnectionManager createClientConnectionManager() {
		 try{
			final SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(null, new SITrustManager[] { new SITrustManager(null) }, null);
			SocketFactory s = new SocketFactory() {

				@Override
				public boolean isSecure(Socket sock) throws IllegalArgumentException {
					return true;
				}
			
				@Override
				public Socket createSocket() throws IOException {
					return sslcontext.getSocketFactory().createSocket();
				
				}
			
				@Override
				public Socket connectSocket(Socket sock, String host, int port,
						InetAddress localAddress, int localPort, HttpParams params)
						throws IOException, UnknownHostException, ConnectTimeoutException {
					int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
					int soTimeout = HttpConnectionParams.getSoTimeout(params);
					InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
					SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());
					if ((localAddress != null) || (localPort > 0)) {
		                if (localPort < 0) {
		                	localPort = 0; 
		                }
		                InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
		                sslsock.bind(isa);
		            }
		            sslsock.connect(remoteAddress, connTimeout);
		            sslsock.setSoTimeout(soTimeout);
		            return sslsock;
				}
			};
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			
			registry.unregister("https");
			registry.register(new Scheme("https", s, 443));
			HttpParams params = new BasicHttpParams();
			params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
			params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
			params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			return new ThreadSafeClientConnManager(params, registry);
		}catch(Exception ex){
			Log.e("HttpClient", ex.getLocalizedMessage());
		}
		return null;
	 }
}