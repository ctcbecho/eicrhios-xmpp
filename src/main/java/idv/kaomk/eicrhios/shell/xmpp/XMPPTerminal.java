package idv.kaomk.eicrhios.shell.xmpp;

import jline.TerminalSupport;

public class XMPPTerminal extends TerminalSupport{

	public XMPPTerminal() {
		super(true);
		setAnsiSupported(false); 
	}	
}
