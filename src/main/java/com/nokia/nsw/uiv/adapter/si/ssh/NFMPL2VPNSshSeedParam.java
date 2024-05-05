package com.nokia.nsw.uiv.adapter.si.ssh;

import java.util.Map;
import com.nokia.nsw.uiv.utils.EncryptionUtil;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;

public class NFMPL2VPNSshSeedParam {

	public static final String SSH_USER_NAME = "sshUsername";
	public static final String SSH_PASSWORD = "sshPassword";
	public static final String SSH_HOST_NAME = "sshHostname";
	public static final String SSH_PORT = "sshPort";
	
	private final Map<String, String> seedFeatureMap;

	public NFMPL2VPNSshSeedParam(NFMPL2VPNSeedParam adapterSeedParam) {
		this.seedFeatureMap = adapterSeedParam.getSeedFeatureMap();
	}
	
	public String getHostName() {
		return EncryptionUtil.decryptValue(SSH_HOST_NAME, seedFeatureMap.get(SSH_HOST_NAME));
	}

	public int getPort() {
		return Integer.parseInt(seedFeatureMap.get(SSH_PORT));
	}
	
	public String getUserName() {
		return seedFeatureMap.get(SSH_USER_NAME);
	}

	public String getPassword() {
		return EncryptionUtil.decryptValue(SSH_PASSWORD, seedFeatureMap.get(SSH_PASSWORD));
	}	
}
