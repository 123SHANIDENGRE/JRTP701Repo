package com.nokia.nsw.uiv.adapter.si.sftp;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.dsl.SftpOutboundGatewaySpec;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;
import com.nokia.oss.sure.adapter.exception.AdapterException;

import lombok.extern.slf4j.Slf4j;

import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.si.http.NFMPL2VPNHttpClient;

@Slf4j
@Component
@EnableIntegration
@Profile("FloeAdapters")
public class NFMPL2VPNSftpClient {

	private static final String MGET_REQUEST_CHANNEL = "NFMPL2VPNMGetRequestChannel";
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNSftpClient.class);
	private final MessageChannel mgetRequestChannel = MessageChannels.direct(MGET_REQUEST_CHANNEL).get();
	
	private final DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();

	private final SftpOutboundGatewaySpec sftpOutboundGatewaySpec = Sftp
			.outboundGateway(sftpSessionFactory, AbstractRemoteFileOutboundGateway.Command.MGET, "payload")
			.autoCreateLocalDirectory(true).localDirectory(new File("."));

	public void doMGet(NFMPL2VPNSeedParam adapterSeedParam) throws AdapterException, AdapterTransformationException {
		NFMPL2VPNSftpSeedParam sftpSeedParam = adapterSeedParam.getSftpSeedParam();
		
		log.info("Sftp Process Request : {}", sftpSeedParam);		
		
		this.sftpSessionFactory.setHost(sftpSeedParam.getHostName());
		this.sftpSessionFactory.setPort(sftpSeedParam.getPort());		
		this.sftpSessionFactory.setUser(sftpSeedParam.getUserName());
		this.sftpSessionFactory.setPassword(sftpSeedParam.getPassword());
		this.sftpSessionFactory.setAllowUnknownKeys(true);

		File localDir = new File(adapterSeedParam.getLocalDir());
		this.sftpOutboundGatewaySpec.patternFileNameFilter(sftpSeedParam.getFileNamePattern()).localDirectory(localDir);

		Message<String> message = MessageBuilder.withPayload(sftpSeedParam.getRemoteDir()).build();
		mgetRequestChannel.send(message);
	}

	public void handleSftpResponse(Message<?> message) {
		log.info("Sftp Response : {}", message);
	}

	@Bean
	public IntegrationFlow NFMPL2VPNSftpMGetFlow() {
		return IntegrationFlows.from(mgetRequestChannel).handle(sftpOutboundGatewaySpec)
				.handle(m -> handleSftpResponse(m)).get();
	}

}