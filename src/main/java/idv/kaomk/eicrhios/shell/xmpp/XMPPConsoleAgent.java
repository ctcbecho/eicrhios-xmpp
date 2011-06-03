package idv.kaomk.eicrhios.shell.xmpp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jline.UnsupportedTerminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.jline.Console;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMPPConsoleAgent {

	private Logger logger = LoggerFactory.getLogger(XMPPConsoleAgent.class);

	private String mHost;
	private int mPort = -1;
	private String mServiceName;
	private String mUsername;
	private String mPassword;
	private String mHttpProxyHost;
	private int mHttpProxyPort;
	private Connection mXMPPConnection;
	private CommandProcessor mCommandProcessor;
	private String[] mAllowUsers;
	private String karafName = "root";
	private String consoleUser = "karaf";

	private Map<String, SessionTerminal> mSessionMap = new Hashtable<String, SessionTerminal>();

	private void syncRosterEntries() throws XMPPException {
		if (mXMPPConnection != null) {
			Roster roster = mXMPPConnection.getRoster();
			List<String> userList = mAllowUsers == null ? new ArrayList<String>()
					: new ArrayList<String>(Arrays.asList(mAllowUsers));
			List<RosterEntry> deleteList = new ArrayList<RosterEntry>();
			for (RosterEntry re : roster.getEntries()) {
				String user = re.getUser();
				if (userList.contains(user)) {
					logger.debug(String.format("user: %s already in roster",
							user));
					userList.remove(user);
				} else {
					deleteList.add(re);
				}
			}

			for (RosterEntry re : deleteList) {
				logger.debug(String.format("user: %s remove from roster",
						re.getUser()));
				roster.removeEntry(re);
			}

			for (String user : userList) {
				if (user.length() > 0) {
					logger.debug(String.format("user: %s add to roster", user));
					roster.createEntry(user, user, new String[0]);
				}

			}

		}

	}

	public void disconnect() throws XMPPException {
		if (mXMPPConnection != null && mXMPPConnection.isConnected()) {
			mXMPPConnection.disconnect();
			logger.debug("XMPPConnection disconnect successfully");

			for (Map.Entry<String, SessionTerminal> entry : mSessionMap
					.entrySet()) {
				entry.getValue().close();
			}
			mSessionMap.clear();
		}
	}

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
			throw new IllegalArgumentException(String.format(
					"Bad arguments for ConnectionConfiguration: %s", this));

		Connection.DEBUG_ENABLED = logger.isDebugEnabled();

		mXMPPConnection = new XMPPConnection(config);

		mXMPPConnection.connect();

		logger.debug("XMPPConnection connect successfully");

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("XMPPConnection isSecureConnection: %s",
					mXMPPConnection.isSecureConnection()));
			logger.debug(String.format("XMPPConnection isUsingCompression: %s",
					mXMPPConnection.isUsingCompression()));
		}

		mXMPPConnection.getRoster().addRosterListener(new RosterListener() {

			@Override
			public void presenceChanged(Presence presence) {
				logger.debug(String.format(
						"RosterListener.presenceChanged: %s %s",
						presence.getFrom(), presence.getType()));

				if (!presence.isAvailable()
						&& mSessionMap.containsKey(presence.getFrom())) {
					mSessionMap.remove(presence.getFrom()).close();
				}
			}

			@Override
			public void entriesUpdated(Collection<String> arg0) {

			}

			@Override
			public void entriesDeleted(Collection<String> arg0) {

			}

			@Override
			public void entriesAdded(Collection<String> arg0) {

			}
		});

		ChatManager chatmanager = mXMPPConnection.getChatManager();

		chatmanager.addChatListener(new ChatManagerListener() {
			@Override
			public void chatCreated(Chat chat, boolean createdLocally) {
				if (!createdLocally)
					chat.addMessageListener(new MessageListener() {
						@Override
						public void processMessage(Chat chat, Message message) {
							try {
								StringBuilder sb = new StringBuilder();
								SessionTerminal st = mSessionMap.get(message
										.getFrom());

								if (st == null || st.isClosed()) {
									st = new SessionTerminal(chat);
									mSessionMap.put(message.getFrom(), st);
								}

								switch (message.getType()) {
								case chat:
									sb.append(message.getBody()).append("\n");
									st.write(sb.toString());
									break;
								case error:
									logger.warn(String.format(
											"receive error xmpp essage: %s",
											message.toXML()));
									chat.sendMessage("\n xmpp-error: "
											+ message.getError().getCondition());
									break;
								default:
									throw new RuntimeException(
											"shoud not happen!!");

								}
							} catch (Exception e) {
								throw new RuntimeException(
										"error during processMessage: ", e);
							}

						}
					});
			}

		});

		mXMPPConnection.login(mUsername, mPassword);
		logger.debug("XMPPConnection login successfully.");
		syncRosterEntries();

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
		this.mCommandProcessor = commandProcessor;
	}

	public void setAllowUsers(String allowUsers) {
		mAllowUsers = allowUsers.split(",");
	}

	
	public String getKarafName() {
		return karafName;
	}

	public void setKarafName(String karafName) {
		this.karafName = karafName;
	}

	public String getConsoleUser() {
		return consoleUser;
	}

	public void setConsoleUser(String consoleUser) {
		this.consoleUser = consoleUser;
	}

	@Override
	public String toString() {
		return "XMPPConsoleAgent [mHost=" + mHost + ", mPort=" + mPort
				+ ", mServiceName=" + mServiceName + ", mUsername=" + mUsername
				+ ", mPassword=" + mPassword + ", mHttpProxyHost="
				+ mHttpProxyHost + ", mHttpProxyPort=" + mHttpProxyPort + "]";
	}

	public class SessionTerminal implements Runnable {

		private Console mConsole;
		private PipedOutputStream mConsoleInputStream;
		private PipedInputStream mConsoleOutputStream;
		private boolean mClosed;
		private Chat mChat;
		private List<String> mLineBuffer = new ArrayList<String>();

		private int mMessageLength = 0;

		public SessionTerminal(Chat chat) throws IOException {
			try {
				this.mChat = chat;
				mConsoleInputStream = new PipedOutputStream();
				mConsoleOutputStream = new PipedInputStream();
				PrintStream pipedOut = new PrintStream(new PipedOutputStream(
						mConsoleOutputStream), true);

				mConsole = new Console(mCommandProcessor, new PipedInputStream(
						mConsoleInputStream), pipedOut, pipedOut,
						new UnsupportedTerminal(), null);

				CommandSession session = mConsole.getSession();
				session.put("APPLICATION",
						System.getProperty("karaf.name", karafName));
				session.put("USER", consoleUser);

			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				throw (IOException) new IOException().initCause(e);
			}
			new Thread(mConsole).start();
			new Thread(this).start();
		}

		public boolean isClosed() {
			return mClosed;
		}

		public void close() {
			mConsole.close();
			mClosed = true;
		}

		public void write(String str) throws IOException {
			try {
				if (str != null && str.length() > 0) {
					mConsoleInputStream.write(str.getBytes());
					mConsoleInputStream.flush();
				}
			} catch (IOException e) {
				close();
				throw e;
			}
		}

		private void addLine(String line) throws Exception {
			line = line.replaceAll("\u001B\\[[\\d;]*m", "");

			if (mMessageLength + line.length() > 1024) {
				flushLines();
			}

			mLineBuffer.add(line);
			mMessageLength += line.length();

		}

		private void flushLines() throws XMPPException {
			StringBuilder sb = new StringBuilder();
			for (String line : mLineBuffer) {
				sb.append(line);
			}
			mLineBuffer.clear();
			mMessageLength = 0;

			if (sb.length() > 0) {
				try {
					mChat.sendMessage(sb.toString());

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		}

		public void run() {
			try {
				int bytesRead = 0;
				int retryTimes = 0;
				int matchedPos = 0;
				byte[] buffer = new byte[256];
				byte[] prompt = (karafName + "> ").getBytes();
				ByteArrayOutputStream output = new ByteArrayOutputStream();

				readData: for (;;) {
					while (mConsoleOutputStream.available() == 0) {
						if (retryTimes > 6 * 100) {
							addLine(output.toString());
							flushLines();
							output = new ByteArrayOutputStream();
							break;
						}
						retryTimes++;
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							logger.error("interrupted: ", e);
						}
					}

					logger.debug("start to read console...");
					bytesRead = mConsoleOutputStream.read(buffer);
					logger.debug(String.format("read console %d bytes",
							bytesRead));

					switch (bytesRead) {
					case -1:
						break readData;
					default:
						retryTimes = 0;
						for (int i = 0; i < bytesRead; i++) {
							output.write(buffer[i]);

							if (matchedPos >= prompt.length) {
								matchedPos = 0;
							}
							if (buffer[i] == prompt[matchedPos]) {
								matchedPos++;
							} else {
								matchedPos = 0;
							}
							if (buffer[i] == 0X0A) {
								addLine(output.toString());
								output = new ByteArrayOutputStream();
							}
						}
						if (matchedPos == prompt.length) {
							matchedPos = 0;
							addLine(output.toString());
							flushLines();
							output = new ByteArrayOutputStream();

						}
					}
				}
			} catch (Exception e) {
				logger.error("error during console reading: ", e);
				close();
				
			}
		}

	}

}
