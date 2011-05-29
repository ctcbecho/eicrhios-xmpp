package idv.kaomk.eicrhios.shell.xmpp;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.*;

@Command(scope = "xmpp", name = "start", description = "Connects to a remote XMPP server as a console agent.")
public class XMPPStartAction extends OsgiCommandSupport {
	private XMPPConsoleAgent mXmppConsoleAgent;
	
	@Override
	protected Object doExecute() throws Exception {
		mXmppConsoleAgent.connect();
		return "";
	}
	
	public void setXmppConsoleAgent(XMPPConsoleAgent xmppConsoleAgent) {
		mXmppConsoleAgent = xmppConsoleAgent;
	}	
}
