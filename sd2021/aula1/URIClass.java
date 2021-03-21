package sd2021.aula1;

import java.net.URI;

public class URIClass {
	private long timestamp;
	private URI u;
	public URIClass(URI u,long timestamp) {
		this.u=u;
		this.timestamp=timestamp;
		
	}
	public void setTimestamp() {
		timestamp=System.currentTimeMillis();
	}
	public URI getURI()
	{
		return u;
	}
	public long getTimestamp() {
		return timestamp;
	}
}
