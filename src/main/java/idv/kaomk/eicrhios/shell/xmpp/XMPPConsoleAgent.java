package idv.kaomk.eicrhios.shell.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
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

	public void connect() throws XMPPException {
		if (mXMPPConnection != null &&  mXMPPConnection.isConnected()){
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

		mXMPPConnection = new XMPPConnection(config);

		mXMPPConnection.connect();

		logger.debug("XMPPConnection connect successfully");
//		SmackConfiguration.setLocalSocks5ProxyPort(17777);
//		SASLAuthentication.supportSASLMechanism("PLAIN", 0);

//		System.out.println(org.jivesoftware.smackx.ServiceDiscoveryManager.class);
//		mXMPPConnection.login(mUsername, mPassword);
//		
//		mXMPPConnection.sendPacket(new Presence(Presence.Type.available));
//
//		// Assume we've created a Connection name "connection".
//		ChatManager chatmanager = mXMPPConnection.getChatManager();
//
//		chatmanager.addChatListener(new ChatManagerListener() {
//			@Override
//			public void chatCreated(Chat chat, boolean createdLocally) {
//				if (!createdLocally)
//					chat.addMessageListener(new MessageListener() {
//						@Override
//						public void processMessage(Chat arg0, Message arg1) {
//							try {
//								arg0.sendMessage(arg1.getBody());
//							} catch (XMPPException e) {
//								throw new RuntimeException(e);
//							}
//
//						}
//					});
//			}
//		});
//
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("mXMPPConnection: %s", mXMPPConnection));
//		}
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

}
