package com.nokia.nsw.uiv.adapter.common;

/*
 * Configuration file to inject values from Vault.
 * 
 * 
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.Data;

@Configuration
@Data
@Profile("FloeAdapters")
public class NFMPL2VPNConfiguration {
	
	// Sample values :: Please update accordingly		
	//@Value("${adapter.user.password}") 
	//private String userPassword;
	
}
