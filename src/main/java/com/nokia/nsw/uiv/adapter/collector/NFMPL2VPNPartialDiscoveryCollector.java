package com.nokia.nsw.uiv.adapter.collector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.si.http.NFMPL2VPNHttpClient;
import com.nokia.nsw.uiv.common.CommonConstants;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration.Filters.Filter;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration.Filters.Filter.Arguments;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration.Filters.Filter.Arguments.Argument;
import com.nokia.nsw.uiv.tef.task.TaskException;
import com.nokia.oss.sure.adapter.exception.AdapterException;

import edu.emory.mathcs.backport.java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class NFMPL2VPNPartialDiscoveryCollector extends NFMPL2VPNCollector  {


	public static final String FILTER_KEY = "FilterKey";
	public static final String FILTER_KEY_NES = "NetworkElements";
	public static final String NETWORK_ELEMENT_PAYLOAD = "NetworkElement_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_SLOT = "NetworkElementFilterSlot_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_SHELF = "NetworkElementFilterShelf_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_CARD = "NetworkElementFilterCard_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_DCARD_SLOT = "NetworkElementFilterDCardSlot_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_DCARD = "NetworkElementFilterDCard_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_PORT = "NetworkElementFilterPort_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_XIOM_CARD = "NetworkElementFilterXiomCard_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_XIOM_SLOT = "NetworkElementFilterXiomCardSlot_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_CONNECTOR = "NetworkElementFilterConnector_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_LPORT = "NetworkElementFilterLPort_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_LAG = "NetworkElementFilterLag_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_MAC_NAME = "NetworkElementFilterMacName_payload";
	public static final String NETWORK_ELEMENT_PAYLOAD_LOGICAL_DEVICE = "NetworkElement_payload";

	public static final String FILTER_KEY_NES_SLOT = "NetworkElements_Slot";
	public static final String FILTER_KEY_NES_SHELF = "NetworkElements_Shelf";
	public static final String FILTER_KEY_NES_CARD = "NetworkElements_Card";
	public static final String FILTER_KEY_NES_DCARD_SLOT = "NetworkElements_DCardSlot";
	public static final String FILTER_KEY_NES_DCARD = "NetworkElements_DCard";
	public static final String FILTER_KEY_NES_PORT = "NetworkElements_Port";
	public static final String FILTER_KEY_NES_XIOM_CARD = "NetworkElements_XCard";
	public static final String FILTER_KEY_NES_XIOM_SLOT = "NetworkElements_XCardSlot";
	public static final String FILTER_KEY_NES_CONNECTOR = "NetworkElements_Connector";
	public static final String FILTER_KEY_NES_LPORT = "NetworkElements_LPort";
	public static final String FILTER_KEY_NES_LAG = "NetworkElements_Lag";
	public static final String FILTER_KEY_NES_MAC_NAME = "NetworkElements_MacName";
	public static final String FILTER_KEY_NES_LOGICAL_DEVICE = "NetworkElements";

	public static final String NSPIP_ADAPTER_PARTIAL_DISCOVERY_CONFIG_FILE = "NFMPL2VPNPartialDiscoveryConfig.json";
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNPartialDiscoveryCollector.class);
	private JSONObject nspIpEmsPDConfig;
	private JSONObject emsResultFiltersConfig;


	private final JSONParser parser = new JSONParser();

	public void init(NFMPL2VPNSeedParam adapterSeedParam) {
		this.adapterSeedParam = adapterSeedParam;
		this.httpSeedParam = adapterSeedParam.getHttpSeedParam();
		this.sftpSeedParam = adapterSeedParam.getSftpSeedParam();
		this.httpClient = new NFMPL2VPNHttpClient(adapterSeedParam);

		InputStream emsPDConfigStream = null;
		try {
			String nspIpEmsPDConfigFile = adapterSeedParam.getNFMPL2VPNPartialDiscoveryConfig() == null ? NSPIP_ADAPTER_PARTIAL_DISCOVERY_CONFIG_FILE
					: adapterSeedParam.getNFMPL2VPNPartialDiscoveryConfig();
			try {
				emsPDConfigStream = new FileInputStream(nspIpEmsPDConfigFile);
			} catch (FileNotFoundException e) {
				emsPDConfigStream = this.getClass().getClassLoader().getResourceAsStream(nspIpEmsPDConfigFile);
			}

			try {
				this.nspIpEmsPDConfig = (JSONObject) parser.parse(new InputStreamReader(emsPDConfigStream));
			} catch (Exception e) {
				//e.printStackTrace();
				log.error("Exception",e);
			}
		} finally {
			if (emsPDConfigStream != null)
				try {
					emsPDConfigStream.close();
				} catch (Exception e) {
					//e.printStackTrace();
					log.error("Exception",e);
				}
		}
		InputStream emsResultFilterConfigStream = null;
		try {
			String emsResultFilterConfigFile = adapterSeedParam.getNFMPL2VPNResultFiltersConfig() == null ? NSPIP_ADAPTER_RESULT_FILTERS_CONFIG_FILE
					: adapterSeedParam.getNFMPL2VPNResultFiltersConfig();
			try {
				emsResultFilterConfigStream = new FileInputStream(emsResultFilterConfigFile);
			} catch (FileNotFoundException e) {
				log.error("Exception",e);
				emsResultFilterConfigStream = this.getClass().getClassLoader().getResourceAsStream(emsResultFilterConfigFile);
			}

			try {
				this.emsResultFiltersConfig = (JSONObject) parser.parse(new InputStreamReader(emsResultFilterConfigStream));
			} catch (Exception e) {
				//e.printStackTrace();
				log.error("Exception",e);
			}


		} finally {
			if (emsResultFilterConfigStream != null)
				try {
					emsResultFilterConfigStream.close();
				} catch (Exception e) {
					//e.printStackTrace();
					log.error("Exception",e);
				}
		}
	}

	public void assembleData() throws Exception {

		try {
		seed = adapterSeedParam;
        hints = seed.getSeedFeatureMap();
		log.info("Inside " + hints.get("adapterName")+" PartialDiscovery Collector");
        decryption(hints);
        maxRetry = Integer.valueOf(hints.get(MAX_RETRY));
        sleepTimeForRetry = Integer.valueOf(hints.get(SLEEP_TIME));
		connectionTimeout = 1000 * Integer.valueOf(hints.get("ConnectionTimeout"));
		readTimeout = 1000 * Integer.valueOf(hints.get("ReadTimeout"));
        restTemplate = (hints.get(SSL_BYPASS).equalsIgnoreCase(Boolean.FALSE.toString())) ? new RestTemplate(getClientHttpRequestFactory()): byPassSSL();
		int networkElementCheck = 0;

        if (hints.get(TOKEN_URL) != null && hints.get(NFMP_IP) != null) {
            createLocalDirectory(adapterSeedParam.getWorkflowData(), NFMP);
            String tokenUrl = hints.get(TOKEN_URL);
            String NfmpIp = hints.get(NFMP_IP);
            String token = generateToken(NfmpIp, restTemplate, NFMP.toLowerCase(), tokenUrl, ACCESS_TOKEN, NFMP.toLowerCase());
            String endPoint = hints.get(ENDPOINTS_NFMP);

            String endpointIp = hints.get(NFMP_IP_PORT);


    		List<Filter> filters = adapterSeedParam.getPartialDiscoveryFilterRules();


    		for(Filter filter : filters)
    		{
    			String partialDiscoveryType = "";
    		
    			for (Arguments arguments : filter.getArguments()) {
					for (Argument argument : arguments.getArgument()) {

							if(argument.getName().trim().equalsIgnoreCase("Kind"))
								partialDiscoveryType=argument.getValue().trim().toLowerCase();
							
							  if(argument.getName().trim().equalsIgnoreCase("GlobalName")&& StringUtils.isNotBlank(argument.getName().trim()))
							  globalName=argument.getValue();
							  
							 
							
							
					}
				}
    		/*	if(partialDiscoveryType.isEmpty()) {
    			if (filter.getKind()!=null && !filter.getKind().isEmpty())
    				partialDiscoveryType=filter.getKind().trim().toLowerCase();
    			}		*/
    		  	String keys="";
    			JSONObject payLoad;
    			List<String> supportedKeys;
    			switch (partialDiscoveryType)
    			{
    			case "aggphysicaldevice":
    				log.info("Processing for Agg Physical Device ");
					if (networkElementCheck==0){
						log.info("Discovering NetworkElement");
						keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES);
						supportedKeys = Arrays.asList(keys.split(","));
						payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD);
						appendFilterPayload(payLoad, filter, supportedKeys);
						log.info("value of the globalName= "+globalName);
						log.info("value of the kind "+partialDiscoveryType);
						fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
						networkElementCheck=1;
						break;
					}
					else{
						break;
					}	
    			case "aggshelf":
    				log.info("Discovering Shelf");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_SHELF);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_SHELF);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
    			case "aggslot":
					log.info("Discovering CardSlot");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_SLOT);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_SLOT);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
    			case "aggcard":
    				log.info("Discovering Card");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_CARD);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_CARD);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
    			case "aggdaughtercardslot":
    				log.info("Discovering Daughter Card Slot");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_DCARD_SLOT);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_DCARD_SLOT);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
    			case "aggdaughtercard":
    				log.info("Discovering Daughter Card");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_DCARD);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_DCARD);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
    			case "aggphysicalport":
    				log.info("Discovering PhysicalPort");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_PORT);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_PORT);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "aggxiomcardslot":
    				log.info("Discovering xiomcardslot");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_XIOM_SLOT);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_XIOM_SLOT);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "aggxiomcard":
    				log.info("Discovering xiomcard");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_XIOM_CARD);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_XIOM_CARD);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "aggconnector":
    				log.info("Discovering connector");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_CONNECTOR);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_CONNECTOR);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "agglag":
    				log.info("Discovering lag");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_LAG);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_LAG);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "agglagport":
    				log.info("Discovering lag port");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_LPORT);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_LPORT);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "macname":
    				log.info("Discovering macname");
    				keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES_MAC_NAME);
    				supportedKeys = Arrays.asList(keys.split(","));
    				payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD_MAC_NAME);
    				appendFilterPayload(payLoad, filter, supportedKeys);
    				fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
    				break;
				case "agglogicaldevice":
    				if (networkElementCheck==0){
						log.info("Discovering NetworkElement");
						keys = (String) ((JSONObject) nspIpEmsPDConfig.get(FILTER_KEY)).get(FILTER_KEY_NES);
						supportedKeys = Arrays.asList(keys.split(","));
						payLoad = (JSONObject) nspIpEmsPDConfig.get(NETWORK_ELEMENT_PAYLOAD);
						appendFilterPayload(payLoad, filter, supportedKeys);
						fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, payLoad);
						networkElementCheck=1;
						break;
					}
					else{
						break;
					}
    			default:
    				log.error(partialDiscoveryType + " is not supported ");
    				throw new ConnectException( partialDiscoveryType + " is not supported ");

    			}

    		}

         }

		}
		catch (Exception e) {
            throw new Exception(e.getMessage(), e);
        }

	}

	 public HttpEntity<Object> nfmp(Map<String, String> hints, String entityType) {
             
		    log.info("NFMP METHOD CALLED for partial Discovery");
	        String body = "{ \"grant_type\": \"client_credentials\"}";
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        
	        if(!hints.get(PROXY_USERNAME).isEmpty() && !hints.get(PROXY_PASSWORD).isEmpty())
	        { 
	        	String NFMP_Username = hints.get(NFMP_USERNAME);
				String NFMP_Password = hints.get(NFMP_AUTH);
				String auth = NFMP_Username + ":" + NFMP_Password;
				String encodedAuth =Base64.getEncoder().encodeToString(auth.getBytes());
				
				log.info("NFMP-USERNAME=" + NFMP_Username + "NFMP PASSWORD=" + NFMP_Password);
			    log.info("x-authorization=" + encodedAuth);
	            headers.setBasicAuth(hints.get(PROXY_USERNAME), hints.get(PROXY_PASSWORD));        
	            headers.set("x-authorization","Basic "+encodedAuth );
	        }
	        else {
	        	headers.setBasicAuth(hints.get(NFMP_USERNAME), hints.get(NFMP_AUTH));
	        }
	        
	        log.info("Header="+headers);
	        HttpEntity<Object> request = new HttpEntity<>(body, headers);
	        return request;

	    }

	private void appendFilterPayload(JSONObject payLoad, Filter pdfilter, List<String> supportedKeys) {

		if (pdfilter != null ) {
			boolean filterConditionFound = false;
			String filterExpression = "";
				for (Arguments arguments : pdfilter.getArguments()) {
					 for (Argument argument : arguments.getArgument()) {
						String _delimeter = (argument.getDelimiter() != null && argument.getDelimiter() != "")
								? argument.getDelimiter()
								: CommonConstants._DELIMITER;
						if (supportedKeys.contains(argument.getName().trim())) {
							filterConditionFound = true;
							String filterKeyPrefix = argument.getName().trim() + " IN (";
							String[] values = argument.getValue().split(_delimeter);
                            String filterdata = Stream.of(values).map(s -> "'"+s+"'").collect(Collectors.joining(","));
                            	if (filterExpression.isEmpty()) {
									filterExpression = filterKeyPrefix.concat(filterdata).concat(")");
								} else {
									filterExpression = filterExpression.concat(" AND ").concat(filterKeyPrefix).concat(filterdata)
											.concat(")");
								}
							}
					}
				}

			if (filterConditionFound) {

				payLoad.put("filterExpression", filterExpression);
				log.debug("JSON payLoad after adding filter conditions is : {}", payLoad.toString());
			}
		}


	}

	protected void fetchAndDumpRespone(String endpointIp, String token, String endPoint, String serviceType, JSONObject payload)
            throws  TaskException {
		String name = (String) payload.get("fullClassName");
		log.info("ClassName is " + name);
		String resultFiltersUnfiltered = (String) ((JSONObject) emsResultFiltersConfig.get("resultFilter")).get(name);
		String resultFilters = "";
		if (resultFiltersUnfiltered != null) {
			String[] resultFiltersArray = resultFiltersUnfiltered.split(",");
			for (int j = 0; j < resultFiltersArray.length; j++) {
				resultFiltersArray[j] = "\"" + resultFiltersArray[j] + "\"";
				if (resultFilters.isEmpty()) {
					resultFilters = resultFilters + resultFiltersArray[j];
				} else {
					resultFilters = resultFilters + "," + resultFiltersArray[j];
				}
			}
		}
			log.info("Result Filters for " + name + " are: " + resultFilters);
			ResponseEntity<String> response = null;
			HttpEntity<Object> request = null;
			log.info("Generating REST request with resultFilters: "+resultFilters);
			request = getHttpEntity(token, resultFilters, payload);
			String serverUrl = "https://" + endpointIp + endPoint;
            log.info("The server url for exchanging data is " + serverUrl);
			response = getRestResponse(serverUrl, HttpMethod.POST, request, String.class, restTemplate);
			dumpFiles(response, serviceType, endPoint, name);


	}
		private HttpEntity<Object> getHttpEntity (String token, String resultFilters,JSONObject payload ){
			log.info("getHttpEntity Method called for partial discovery");
			HttpHeaders headers1 = new HttpHeaders();
			headers1.setContentType(MediaType.APPLICATION_JSON);
			if (!hints.get(PROXY_USERNAME).isEmpty() && !hints.get(PROXY_PASSWORD).isEmpty()) {
				headers1.setBasicAuth(hints.get(PROXY_USERNAME), hints.get(PROXY_PASSWORD));
				headers1.set("x-authorization", "Bearer " + token);

			} else {
				headers1.setBearerAuth(token);
			}
		
			String payloadWithResultFilter = payload.toString();
			log.info("payload is: "+payloadWithResultFilter);
			log.info("resultFilters are"+ resultFilters);
			if (!resultFilters.isEmpty()){
				payloadWithResultFilter=payloadWithResultFilter.replace('}',',');
				payloadWithResultFilter = payloadWithResultFilter + "\"resultFilter\":[" + resultFilters + "]}";
			}
			
            log.info("header="+headers1);
			log.info("payload body after adding resultFilters is"+payloadWithResultFilter);
			HttpEntity<Object> request = new HttpEntity<>(payloadWithResultFilter, headers1);
			return request;

		}


}

