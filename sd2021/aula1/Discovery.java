package sd2021.aula1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p>A class to perform service discovery, based on periodic service contact endpoint 
 * announcements over multicast communication.</p>
 * 
 * <p>Servers announce their *name* and contact *uri* at regular intervals. The server actively
 * collects received announcements.</p>
 * 
 * <p>Service announcements have the following format:</p>
 * 
 * <p>&lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;</p>
 */
public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());
	private Map<String,List<URIClass>> uri;

	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	
	// The pre-aggreed multicast endpoint assigned to perform discovery. 
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private InetSocketAddress addr;
	private String serviceName;
	private String serviceURI;

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	Discovery( InetSocketAddress addr, String serviceName, String serviceURI) {
		this.addr = addr;
		this.serviceName = serviceName;
		this.serviceURI  = serviceURI;
		uri=new HashMap<>();
	}
	
	/**
	 * Starts sending service announcements at regular intervals... 
	 */
	public void start() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName, serviceURI));
		
		byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			MulticastSocket ms = new MulticastSocket( addr.getPort());
			ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			// start thread to send periodic announcements
			new Thread(() -> {
				for (;;) {
					try {
						ms.send(announcePkt);
						Thread.sleep(DISCOVERY_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
						// do nothing
					}
				}
			}).start();
			
			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);
						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(DELIMITER);
						if( msgElems.length == 2) {	//periodic announcement
							System.out.printf( "FROM %s (%s) : %s\n", pkt.getAddress().getCanonicalHostName(), 
									pkt.getAddress().getHostAddress(), msg);
							URIClass u=new URIClass(URI.create(serviceURI),System.currentTimeMillis());
							addToMap(serviceName,u);
							URI[] u1=knownUrisOf(serviceName);
							long[] ts=knownTSOf(serviceName);
							for(int i=0;i<u1.length;i++) {
								System.out.print(u1[i]+" "+ts[i]+", ");
								System.out.println("----------------------------");
							}
						}
					} catch (IOException e) {
						// do nothing
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the known servers for a service.
	 * 
	 * @param  serviceName the name of the service being discovered
	 * @return an array of URI with the service instances discovered. 
	 * 
	 */
	public URI[] knownUrisOf(String serviceName) {
		List<URIClass> list=uri.get(serviceName);
		URI[] res=new URI[list.size()];
		int counter=0;
		if(list!=null) {
			for(URIClass i : list) {
				res[counter]=i.getURI();
				counter++;
			}
		}
		
		return res;
	}	
	private long[] knownTSOf(String serviceName) {
		List<URIClass> list=uri.get(serviceName);
		long[] res=new long[list.size()];
		int counter=0;
		if(list!=null) {
			for(URIClass i : list) {
				res[counter]=i.getTimestamp();
				counter++;
			}
		}
		
		return res;
	}	
	
	private void addToMap(String serviceName,URIClass u){
		List<URIClass> uvec=new ArrayList<>();
		if(uri.get(serviceName)==null) {
			uvec.add(u);
		}
		else {
			uvec=uri.get(serviceName);
			try { 
				if(hasURI(uvec,u.getURI())!=-1) {
					uvec.get(hasURI(uvec,u.getURI())).setTimeStamp();
				}}
			catch(Exception e) {
				uvec.add(u);
			}
		}
		uri.put(serviceName, uvec);
	}
	private int hasURI(List<URIClass> u,URI u1) {
		String u2=u1.toString();
		for(int i=0;i<u.size();i++) {
			String u3=u.get(i).getURI().toString();
			if(u3.equals(u2)) return i;
		}
		return -1;
	}
	
	
	
	// Main just for testing purposes
	public static void main( String[] args) throws Exception {
		Discovery discovery = new Discovery( DISCOVERY_ADDR, "test", "http://" + InetAddress.getLocalHost().getHostAddress());
		discovery.start();
		
	}
}
