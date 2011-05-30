package idv.kaomk.eicrhios.shell.xmpp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.karaf.webconsole.gogo.Terminal;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMPPConsoleAgent {
	public static final int TERM_WIDTH = 120;
	public static final int TERM_HEIGHT = 39; 
	
	private Logger logger = LoggerFactory.getLogger(XMPPConsoleAgent.class);

	private String mHost;
	private int mPort = -1;
	private String mServiceName;
	private String mUsername;
	private String mPassword;
	private String mHttpProxyHost;
	private int mHttpProxyPort;
	private Connection mXMPPConnection;
	private CommandProcessor commandProcessor;
	private Map<String, SessionTerminal> mSessionMap = new Hashtable<String, SessionTerminal>();

	public void connect() throws XMPPException {
		if (mXMPPConnection != null && mXMPPConnection.isConnected()) {
			return;
		}

		ConnectionConfiguration config = null;
		ProxyInfo proxyInfo = null;
		if (mHttpProxyHost != null && !mHttpProxyHost.equals("")) {
			proxyInfo = ProxyInfo.forHttpProxy(mHttpProxyHost, mHttpProxyPort,
					"", "");
		} else {
			proxyInfo = ProxyInfo.forNoProxy();
		}

		if (mPort == -1 && (mServiceName != null && !mServiceName.equals(""))) {
			config = new ConnectionConfiguration(mServiceName, proxyInfo);
		} else if (mPort != -1
				&& (mServiceName == null || mServiceName.equals(""))) {
			config = new ConnectionConfiguration(mHost, mPort, proxyInfo);
		} else if (mPort != -1 && mServiceName != null) {
			config = new ConnectionConfiguration(mHost, mPort, mServiceName,
					proxyInfo);
		} else
			throw new IllegalArgumentException(
					"Bad arguments for ConnectionConfiguration");

		Connection.DEBUG_ENABLED = logger.isDebugEnabled();

		mXMPPConnection = new XMPPConnection(config);

		mXMPPConnection.connect();

		logger.debug("XMPPConnection connect successfully");

		// SmackConfiguration.setLocalSocks5ProxyPort(17777);
		// SASLAuthentication.supportSASLMechanism("PLAIN", 0);

		// System.out.println(org.jivesoftware.smackx.ServiceDiscoveryManager.class);

		mXMPPConnection.getRoster().addRosterListener(new RosterListener() {

			@Override
			public void presenceChanged(Presence presence) {
				System.out.println("presenceChanged:" + presence.getType() + ": "
						+ presence.getFrom());
				if (presence.isAvailable()){
					String from = presence.getFrom();
					if (!mSessionMap.containsKey(from) || mSessionMap.get(from).isClosed()){
						try {
							mSessionMap.put(from, new SessionTerminal());
						} catch (IOException e) {
							logger.error("error during SessionTerminal init: ", e);
						}
					}
				}
				
				if (!presence.isAvailable() && mSessionMap.containsKey(presence.getFrom())){
					mSessionMap.remove(presence.getFrom()).closed();
				}
			}

			@Override
			public void entriesUpdated(Collection<String> arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void entriesDeleted(Collection<String> arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void entriesAdded(Collection<String> arg0) {
				// TODO Auto-generated method stub

			}
		});

		mXMPPConnection.login(mUsername, mPassword);

		mXMPPConnection.sendPacket(new Presence(Presence.Type.available));

		// omm.deleteMessages();

		// Assume we've created a Connection name "connection".
		ChatManager chatmanager = mXMPPConnection.getChatManager();

		chatmanager.addChatListener(new ChatManagerListener() {
			@Override
			public void chatCreated(Chat chat, boolean createdLocally) {
				if (!createdLocally)
					chat.addMessageListener(new MessageListener() {
						@Override
						public void processMessage(Chat arg0, Message arg1) {
							try {
//								System.out.println(arg1.toXML());
//								arg0.sendMessage(arg1.getBody());
								SessionTerminal st = mSessionMap.get(arg1.getFrom());
								if (st != null){
									StringBuilder sb = new StringBuilder();
									sb.append(arg1.getBody()).append("\n");
									String str = arg1.getBody();
									try {
										String dump = st.handle(sb.toString(),  false);
										arg0.sendMessage(dump);
									} catch (IOException e) {
										logger.error("error during handle: ", e);
									}
								}
								 
							} catch (XMPPException e) {
								throw new RuntimeException(e);
							}

						}
					});
			}

		});

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("mXMPPConnection: %s", mXMPPConnection));
		}
	}

	public String getHost() {
		return mHost;
	}

	public void setHost(String host) {
		mHost = host;
	}

	public int getPort() {
		return mPort;
	}

	public void setPort(int port) {
		mPort = port;
	}

	public String getServiceName() {
		return mServiceName;
	}

	public void setServiceName(String serviceName) {
		mServiceName = serviceName;
	}

	public String getUsername() {
		return mUsername;
	}

	public void setUsername(String username) {
		mUsername = username;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setPassword(String password) {
		mPassword = password;
	}

	public String getHttpProxyHost() {
		return mHttpProxyHost;
	}

	public void setHttpProxyHost(String httpProxyHost) {
		mHttpProxyHost = httpProxyHost;
	}

	public int getHttpProxyPort() {
		return mHttpProxyPort;
	}

	public void setHttpProxyPort(int httpProxyPort) {
		mHttpProxyPort = httpProxyPort;
	}

	
	public void setCommandProcessor(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
	}


	public class SessionTerminal implements Runnable {

		private Terminal terminal;
		private Console console;
		private PipedOutputStream in;
		private PipedInputStream out;
		private boolean closed;

		public SessionTerminal() throws IOException {
			try {
				this.terminal = new Terminal(TERM_WIDTH, TERM_HEIGHT);
				terminal.write("\u001b\u005B20\u0068"); // set newline mode on

				in = new PipedOutputStream();
				out = new PipedInputStream();
				PrintStream pipedOut = new PrintStream(new PipedOutputStream(
						out), true);

				console = new Console(commandProcessor,
						new PipedInputStream(in), pipedOut, pipedOut,
						new XMPPTerminal(), null);
				CommandSession session = console.getSession();
				session.put("APPLICATION",
						System.getProperty("karaf.name", "root"));
				session.put("USER", "karaf");
				session.put("COLUMNS", Integer.toString(TERM_WIDTH));
				session.put("LINES", Integer.toString(TERM_HEIGHT));
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				throw (IOException) new IOException().initCause(e);
			}
			new Thread(console).start();
			new Thread(this).start();
		}

		public boolean isClosed() {
			return closed;
		}

		public void closed(){
			console.close();
		}
		
		public String handle(String str, boolean forceDump) throws IOException {
			try {
				if (str != null && str.length() > 0) {
					String d = terminal.pipe(str);
					for (byte b : d.getBytes()) {
						in.write(b);
					}
					in.flush();
				}
			} catch (IOException e) {
				closed = true;
				throw e;
			}
			try {
				return terminal.dump(10, forceDump);
			} catch (InterruptedException e) {
				throw new InterruptedIOException(e.toString());
			}
		}

		public void run() {
			try {
				for (;;) {
					byte[] buf = new byte[8192];
					int l = out.read(buf);
					InputStreamReader r = new InputStreamReader(
							new ByteArrayInputStream(buf, 0, l));
					StringBuilder sb = new StringBuilder();
					for (;;) {
						int c = r.read();
						if (c == -1) {
							break;
						}
						sb.append((char) c);
					}
					if (sb.length() > 0) {
						terminal.write(sb.toString());
					}
					String s = terminal.read();
					if (s != null && s.length() > 0) {
						for (byte b : s.getBytes()) {
							in.write(b);
						}
					}
				}
			} catch (IOException e) {
				closed = true;
				e.printStackTrace();
			}
		}

	}
}
