package com.nokia.nsw.uiv.adapter.si.http;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;

import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.handler.xml.NFMPL2VPNGenericNodeHandler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class NFMPL2VPNHttpClient {

	private final NFMPL2VPNHttpSeedParam httpSeedParam;
	private WebClient webClient;
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNHttpClient.class);

	public NFMPL2VPNHttpClient(NFMPL2VPNSeedParam adapterSeedParam) {
		this.httpSeedParam = adapterSeedParam.getHttpSeedParam();
		this.webClient = WebClient.create();
	}
	
	public NFMPL2VPNHttpClient(NFMPL2VPNSeedParam adapterSeedParam, boolean disableSsl) {
		this.httpSeedParam = adapterSeedParam.getHttpSeedParam();
		if(disableSsl) {
			this.webClient = WebClient.create();
		} else {
			try {
				webClient = createWebClient();
			} catch (SSLException e) {
				log.error("Exception",e);
			}
		}
	}

	public ResponseEntity<String> doGet(String url, String uri) {
		log.info("Process Request : {}", httpSeedParam);
		ResponseEntity<String> response = webClient
				.get()
				.uri(url+uri)
				.retrieve()
				.toEntity(String.class)
				.block();
		log.info("Response : {} ", response.getBody().toString());
		return response;
	}

	public ResponseEntity<JSONObject> doPost(String url, String uri, JSONObject payload) {
		log.info("Process Request : {}", httpSeedParam);

		ResponseEntity<JSONObject> response = webClient
				.post()
				.uri(url+uri)
				.header("Authorization", getAuthString())
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(payload), JSONObject.class)
				.retrieve()
				.toEntity(JSONObject.class)
				.block();
		return response;
	}

	public ResponseEntity<JSONObject> doPost(String url, String uri, MultiValueMap<String, String> formData) {
		log.info("Process Request : {}", httpSeedParam);

		ResponseEntity<JSONObject> response = webClient
				.post()
				.uri(url+uri)
				.header("Authorization", getAuthString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(formData))
				.retrieve()
				.toEntity(JSONObject.class)
				.block();
		return response;
	}

	private String getAuthString() {
		String authString;
		if (StringUtils.isBlank(httpSeedParam.getAccessToken())) {
			authString = "Basic " + Base64Utils
					.encodeToString((httpSeedParam.getUserName() + ":" + httpSeedParam.getPassword()).getBytes());
		} else {
			authString = "Bearer " + httpSeedParam.getAccessToken();
		}
		log.info("Authantication header string generated : {} ", authString);
		return authString;
	}	
		
	private WebClient createWebClient() throws SSLException {
		SslContext sslContext = SslContextBuilder
				.forClient()
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();
		reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create().secure(t -> t.sslContext(sslContext));
		return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
				.codecs(configurer -> configurer
						.defaultCodecs()
						.maxInMemorySize(16 * 1024 * 1024))
				.build()).clientConnector(new ReactorClientHttpConnector(httpClient)).build();

	}
	

}
