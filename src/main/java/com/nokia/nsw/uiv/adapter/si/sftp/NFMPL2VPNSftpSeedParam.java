package com.nokia.nsw.uiv.adapter.si.sftp;

import java.util.Map;
import com.nokia.nsw.uiv.utils.EncryptionUtil;
import com.nokia.oss.sure.adapter.common.FloeAdapterConstants;

import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;

public class NFMPL2VPNSftpSeedParam {

	public static final String SFTP_USER_NAME = "sftpUsername";
	public static final String SFTP_PASSWORD = "sftpPassword";

	public static final String SFTP_HOST = "sftpHostname";
	public static final String SFTP_PORT = "sftpPort";

	private String remoteDir;
	private final Map<String, String> seedFeatureMap;
	
	public NFMPL2VPNSftpSeedParam(NFMPL2VPNSeedParam adapterSeedParam) {
		this.seedFeatureMap = adapterSeedParam.getSeedFeatureMap();
	}
	
	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}
	
	public String getRemoteDir() {
		if (remoteDir == null) {
			return seedFeatureMap.get(FloeAdapterConstants.REMOTE_HOST_DIRECTORY) + "/";
		} else {
			return remoteDir + "/";
		}
	}

	public String getFileNamePattern() {
		return seedFeatureMap.get(FloeAdapterConstants.FILE_PATTERN);
	}

	public String getHostName() {
		return EncryptionUtil.decryptValue(SFTP_HOST, seedFeatureMap.get(SFTP_HOST));
	}

	public int getPort() {
		if (seedFeatureMap.get(SFTP_PORT) != null) {
			return Integer.parseInt(seedFeatureMap.get(SFTP_PORT));
		} else {
			return 22;
		}
	}

	public String getUserName() {
		return seedFeatureMap.get(SFTP_USER_NAME);
	}

	public String getPassword() {
		return EncryptionUtil.decryptValue(SFTP_PASSWORD, seedFeatureMap.get(SFTP_PASSWORD));
	}

}