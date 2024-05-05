package com.nokia.nsw.uiv.adapter.si.http;

import java.util.Map;
import com.nokia.nsw.uiv.utils.EncryptionUtil;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;

public class NFMPL2VPNHttpSeedParam {

	public static final String HTTP_USER_NAME = "httpUsername";
	public static final String HTTP_PASSWORD = "httpPassword";

	public static final String BASE_URL = "baseUrl";
	public static final String TOKEN_BASE_URL = "tokenBaseUrl";
	public static final String REVOKE_TOKEN_BASE_URL = "revokeTokenBaseUrl";

	public String accessToken = null;

	private final Map<String, String> seedFeatureMap;

	public NFMPL2VPNHttpSeedParam(NFMPL2VPNSeedParam adapterSeedParam) {
		this.seedFeatureMap = adapterSeedParam.getSeedFeatureMap();
	}

	public String getUserName() {
		return seedFeatureMap.get(HTTP_USER_NAME);
	}

	public String getPassword() {
		return EncryptionUtil.decryptValue(HTTP_PASSWORD, seedFeatureMap.get(HTTP_PASSWORD));
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getTokenBaseUrl() {
		return seedFeatureMap.get(TOKEN_BASE_URL);
	}

	public String getRevokeTokenBaseUrl() {
		return seedFeatureMap.get(REVOKE_TOKEN_BASE_URL);
	}

	public String getBaseUrl() {
		return seedFeatureMap.get(BASE_URL);
	}

}
