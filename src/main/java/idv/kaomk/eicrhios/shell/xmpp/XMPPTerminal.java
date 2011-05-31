package idv.kaomk.eicrhios.shell.xmpp;

import java.io.IOException;
import java.io.InputStream;

import jline.TerminalSupport;

public class XMPPTerminal extends TerminalSupport{

	public XMPPTerminal() {
		super(true);
	}

	
	
	@Override
	public int readCharacter(InputStream in) throws IOException {
		System.out.println("readCharacter");
		return super.readCharacter(in);
	}



	@Override
	public synchronized boolean isAnsiSupported() {
		System.out.println("isAnsiSupported?: false");
		return false;
	}


	@Override
	public int readVirtualKey(InputStream in) throws IOException {
		System.out.println("readVirtualKey:" + in);
		return super.readVirtualKey(in);
	}



	@Override
	public InputStream getDefaultBindings() {
		// TODO Auto-generated method stub
		return super.getDefaultBindings();
	}



	@Override
	protected synchronized void setAnsiSupported(boolean supported) {
		// TODO Auto-generated method stub
		super.setAnsiSupported(supported);
	}



	@Override
	public synchronized void setEchoEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEchoEnabled(enabled);
	}
	
	
}
