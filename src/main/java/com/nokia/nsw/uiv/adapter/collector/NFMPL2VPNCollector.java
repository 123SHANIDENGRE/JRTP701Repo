package com.nokia.nsw.uiv.adapter.collector;

import java.io.*;
import java.security.KeyManagementException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.lang.reflect.Method;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.FileUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.ctc.wstx.util.StringUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.utils.Log;
import com.google.common.io.Files;
import com.nokia.nsw.uiv.adapter.common.NFMPFloeAdapterConstants;
import com.nokia.nsw.uiv.adapterworkflow.metadata.WorkflowData;
import com.nokia.nsw.uiv.discovery.metadata.SeedEntity;
import com.nokia.nsw.uiv.discovery.metadata.SeedFeatures;
import com.nokia.nsw.uiv.tef.task.TaskException;
import com.nokia.nsw.uiv.utils.EncryptionUtil;
import com.nokia.oss.sure.adapter.exception.AdapterException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nokia.nsw.uiv.adapter.NFMPL2VPN;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;

import com.nokia.nsw.uiv.adapter.si.http.NFMPL2VPNHttpClient;
import com.nokia.nsw.uiv.adapter.si.http.NFMPL2VPNHttpSeedParam;
import com.nokia.nsw.uiv.adapter.si.sftp.NFMPL2VPNSftpClient;
import com.nokia.nsw.uiv.adapter.si.sftp.NFMPL2VPNSftpSeedParam;
import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("FloeAdapters")
public class NFMPL2VPNCollector implements NFMPFloeAdapterConstants {

    @Autowired
    protected NFMPL2VPNSftpClient sftpClient;

    protected NFMPL2VPNHttpClient httpClient;
    protected NFMPL2VPNSeedParam adapterSeedParam;
    protected NFMPL2VPNHttpSeedParam httpSeedParam;
    protected NFMPL2VPNSftpSeedParam sftpSeedParam;
   
    protected Map<String, String> hints;
    protected RestTemplate restTemplate;
    protected NFMPL2VPNSeedParam seed;
    ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNCollector.class);
    int maxRetry;
    int readTimeout;
    int connectionTimeout;
    int sleepTimeForRetry;
    
    
    
    
    private JSONObject emsResultFiltersConfig;
    public static final String NSPIP_ADAPTER_RESULT_FILTERS_CONFIG_FILE = "src/config/NFMPL2VPNResultFiltersConfig.json";
    //added for manage assocaition with aggphysicaldevice
    protected String globalName = null;
    private final JSONParser parser = new JSONParser();


    public void init(NFMPL2VPNSeedParam adapterSeedParam) {
        this.adapterSeedParam = adapterSeedParam;
        this.httpSeedParam = adapterSeedParam.getHttpSeedParam();
        this.sftpSeedParam = adapterSeedParam.getSftpSeedParam();
        this.httpClient = new NFMPL2VPNHttpClient(adapterSeedParam);

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
            log.info("Inside " + hints.get("adapterName")+" Collector");
            decryption(hints);
            maxRetry = Integer.valueOf(hints.get(MAX_RETRY));
            sleepTimeForRetry = Integer.valueOf(hints.get(SLEEP_TIME));
            connectionTimeout = 1000 * Integer.valueOf(hints.get("ConnectionTimeout"));
            readTimeout = 1000 * Integer.valueOf(hints.get("ReadTimeout"));
         
            restTemplate = (hints.get(SSL_BYPASS).equalsIgnoreCase(Boolean.FALSE.toString())) ? new RestTemplate(getClientHttpRequestFactory()): byPassSSL();
            // CMM Collection
            if (hints.get(TOKEN_URL) != null && hints.get(NFMP_IP) != null) {
                createLocalDirectory(adapterSeedParam.getWorkflowData(), NFMP);
                String tokenUrl = hints.get(TOKEN_URL);
                String NfmpIp = hints.get(NFMP_IP);
                String token = generateToken(NfmpIp, restTemplate, NFMP.toLowerCase(), tokenUrl, ACCESS_TOKEN, NFMP.toLowerCase());
                log.info("Generated Token:" +  token);
                String endPoint = hints.get(ENDPOINTS_NFMP);
                String[] SREquipmentandIPVPNClasses = hints.get(SRE_EQP_IP_VPN_CLASS).split(DELIMITER_PATTERN);
                String endpointIp = hints.get(NFMP_IP_PORT);
                fetchAndDumpRespone(endpointIp, token, endPoint, NFMP, SREquipmentandIPVPNClasses, NfmpIp, restTemplate, tokenUrl);

            }

        }
        catch (Exception e) {
            //e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }

    }
    protected RestTemplate byPassSSL() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        TrustStrategy acceptingTrustStrategy = (java.security.cert.X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, null, null,
                new NoopHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        requestFactory.setBufferRequestBody(false);
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setConnectTimeout(connectionTimeout);

        restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    protected String generateToken(String endPointIp, RestTemplate restTemplate, String serviceType, String tokenUrl,
                                   String tokenName, String entityType) throws KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, InterruptedException, TaskException {

        String accessToken = "";
        String serverUrl = "https://" + endPointIp + tokenUrl;
        log.info("The serverUrl for generating token is :"+ serverUrl);
        Class cls = this.getClass();
        ResponseEntity<String> response = null;
        try {
            Method method = cls.getDeclaredMethod(serviceType, Map.class, String.class);
            HttpEntity<Object> request = nfmp(hints, entityType);
            log.info("Generating token request");
            response = getRestResponse(serverUrl, HttpMethod.POST, request, String.class, restTemplate);
            if (response != null
                    && (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED)) {
                log.info("Token generation is successful for "+ serviceType);
                String jsonStr = response.getBody();
                accessToken = getToken(jsonStr, tokenName);
            } else {
                log.info("No response returned : Status Code " + response);
            }
            accessToken = accessToken.replaceAll("\"", "");
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException
                e1) {
            throw new TaskException("Unable to generate token due to : " + e1.toString(), e1);
        }
        return accessToken;

    }

    public void decryption(Map<String, String> hints) throws TaskException {

        for (Map.Entry<String, String> entry : hints.entrySet()) {
            try {
                hints.put(entry.getKey(), EncryptionUtil.decryptValue(entry.getKey(), entry.getValue()));
            } catch (Exception e) {
                throw new TaskException(e.getMessage(), e);
            }

        }
    }

    protected void fetchAndDumpRespone(String endpointIp, String token, String endPoint, String serviceType, String[] SREquipmentandIPVPNClasses, String NfmpIp, RestTemplate restTemplate, String tokenUrl)
            throws  TaskException, InterruptedException {

        //ResponseEntity<String> response = null;
        //HttpEntity<Object> request = null;
        //HttpHeaders headers1 = new HttpHeaders();
        //headers1.setBearerAuth(token);
        //HttpEntity<Object> request = new HttpEntity<>(headers1);
        log.info("total no of classes are "+ SREquipmentandIPVPNClasses.length);
        for (int i = 0; i < SREquipmentandIPVPNClasses.length; i++) {
            log.info("iteration "+ i);
            log.info("SREquipmentandIPVPNClass="+SREquipmentandIPVPNClasses[i]);
            String resultFiltersUnfiltered = (String) ((JSONObject) emsResultFiltersConfig.get("resultFilter")).get(SREquipmentandIPVPNClasses[i]);
            String resultFilters = "";
            if (resultFiltersUnfiltered!= null){
                String[] resultFiltersArray = resultFiltersUnfiltered.split(",");
                for (int j=0; j <resultFiltersArray.length; j++){
                    resultFiltersArray[j]="\""+ resultFiltersArray[j] + "\"";
                    if (resultFilters.isEmpty()){
                        resultFilters = resultFilters +resultFiltersArray[j];
                    }
                    else {
                        resultFilters = resultFilters + "," + resultFiltersArray[j];
                    }
                }
            }
            log.info("Result Filters for "+ SREquipmentandIPVPNClasses[i]+ " are: "+ resultFilters);
            //log.info("Token: " + token);
            
            String serverUrl = "https://" + endpointIp + endPoint;
            log.info("The server url for exchanging data is " + serverUrl);
            String fileName = serviceType.concat(ADAPTER_DELIMITER)
                    .concat(endPoint.substring(endPoint.lastIndexOf('/') + 1, endPoint.length()).trim()
                            .concat(ADAPTER_DELIMITER).concat(SREquipmentandIPVPNClasses[i]).concat(".json"));
            String dumpPath = hints.get(LOCAL_TARGET_DIRECTORY);
            File file = new File(dumpPath, fileName);
            //log.info("Entering getRestResponseAsStream function");
            token = getRestResponseAsStream(token, resultFilters, SREquipmentandIPVPNClasses[i], NfmpIp, restTemplate, tokenUrl, serverUrl, HttpMethod.POST,file.getAbsolutePath(),0);

            }
    }

    
    
    
    private String addAttributeEmsName(String jsonResponse,String globalName) {
    	log.info(" addAttributeEmsName Called ");
		JSONArray jobj = new JSONArray(jsonResponse);		
		for(int index=0;index<jobj.length();index++) {					
			jobj.getJSONObject(index).put("emsName",globalName);
		}
		return jobj.toString();		
	}

    
    protected void dumpFiles(ResponseEntity response, String serviceType, String endpoint, String nameSpace)
            throws TaskException {

        String dumpFileName;
        if (response != null && response.getStatusCode() == HttpStatus.OK) {

            String dumpPath = hints.get(LOCAL_TARGET_DIRECTORY);
            if (nameSpace != null && !nameSpace.trim().isEmpty())
                dumpFileName = serviceType.concat(ADAPTER_DELIMITER).concat("partial").concat(ADAPTER_DELIMITER)
                        .concat(endpoint.substring(endpoint.lastIndexOf('/') + 1, endpoint.length()).trim()
                                .concat(ADAPTER_DELIMITER).concat(nameSpace).concat(".json"));
            else
                dumpFileName = serviceType.concat(ADAPTER_DELIMITER).concat("partial").concat(ADAPTER_DELIMITER).concat(
                        endpoint.substring(endpoint.lastIndexOf('/') + 1, endpoint.length()).trim().concat(".json"));
            File dumpFile = (new File(dumpPath.concat(File.separator).concat(dumpFileName)));
           
            
            log.info("value of the globalName "+globalName);
            
            try {  
            	
            	String jsonResponse =response.getBody().toString();
            	if(nameSpace.equalsIgnoreCase("netw.NetworkElement") && StringUtils.isNotBlank(globalName)) {
            		log.info("Started to add emsName and globalname ="+globalName);
            		jsonResponse = addAttributeEmsName(jsonResponse,globalName);
            		globalName = null;
            	} 
            	
                Files.write(jsonResponse.getBytes(), dumpFile);
                log.info("file "+ dumpFile+" has been collected");
            } catch (IOException e) {
                throw new TaskException(e.getMessage(), e);
            }
            
    

        } else {
            log.info("No response returned : Status Code +" + response);
        }

    }

    private SeedFeatures generateSeedFeature(SeedEntity seedEntity, String name, String value) {

        SeedFeatures sf = new SeedFeatures();
        sf.setName(name);
        sf.setValue(value);
        sf.setSeedEntity(seedEntity);
        return sf;

    }

    private HttpEntity<Object> getHttpEntity(String token, String resultFilters, String className) {
    	log.info("getHttpEntity Method called");
        String body = "";
        String classBody = JSON_START + "\"fullClassName\": \"".concat(className) + "\"";
        //log.info(classBody);
        //log.info("resultFiltersinsideHTTPfunction"+ resultFilters);
        if (!resultFilters.isEmpty()){
            classBody = classBody + ",\"resultFilter\":[" + resultFilters + "]";
        }
        //log.info(classBody);
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setContentType(MediaType.APPLICATION_JSON);
        if(!hints.get(PROXY_USERNAME).isEmpty() && !hints.get(PROXY_PASSWORD).isEmpty()) {
        	headers1.setBasicAuth(hints.get(PROXY_USERNAME), hints.get(PROXY_PASSWORD));
	        headers1.set("x-authorization","Bearer "+ token);
        }else {
        	headers1.setBearerAuth(token);
        }
        body = classBody.concat(JSON_END);
        log.info("searchWithFilterHeader:"+headers1);
        log.info("RequestBody="+body); 
        HttpEntity<Object> request = new HttpEntity<>(body, headers1);
        return request;

    }

public String getRestResponseAsStream(String token, String resultFilters, String SREquipmentandIPVPNClasses, String NfmpIp, RestTemplate restTemplate, String tokenUrl,String serverUrl, HttpMethod method, String filename, int i) throws TaskException {
    restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

    for (; i <= maxRetry; i++) {
        try {
            if (i > 0) {
                log.info("retrying connection, retry count " + i);
            }
            //ResponseEntity<Resource> response = restTemplate.exchange(serverUrl, method, request, Resource.class);
			//ResponseEntity<String> response = null;
            log.info("Generating REST request with resultFilters: "+resultFilters);
            
			HttpEntity<Object> request = getHttpEntity(token, resultFilters, SREquipmentandIPVPNClasses);
			log.info("Request="+request);
			//response = getRestResponse(serverUrl, HttpMethod.POST, request, String.class, restTemplate);
			log.info("Sending REST request with readTimeout : "+ readTimeout/1000 + " secs and connectTimeout :"+connectionTimeout/1000 + " secs");
			ResponseEntity<Resource> response = restTemplate.exchange(serverUrl, method, request, Resource.class);
			log.info("Response="+response);
			log.info("Received REST Response Successfully");
            
            if (response != null && (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED)){
                InputStream inputStream = response.getBody().getInputStream();
                try (FileOutputStream outputStream = new FileOutputStream(new File(filename))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    log.info("file "+ filename+" has been collected");
                }

                inputStream.close();
				return token;
                //break;
            } else {
                if (i == maxRetry) {
                	
                	
                    return token;
                }
            }
        } catch (Exception e) {
            //log.info("Entered Excption, i= " + i);
            log.info("Error msg: " + e.getMessage());
                if (e.getMessage().toLowerCase().contains("bearer token is not valid")){
                    //log.info("Check passed..error has bearer token invalid string");
                    try	{
                        log.info("Generating new token");
                        token = generateToken(NfmpIp, restTemplate, NFMP.toLowerCase(), tokenUrl, ACCESS_TOKEN, NFMP.toLowerCase());
                    }
                    catch (Exception ex) {
                        log.error("Exception in generating new token");
                        throw new TaskException(ex.getMessage(), ex);
                    }
                    token = getRestResponseAsStream(token, resultFilters, SREquipmentandIPVPNClasses, NfmpIp, restTemplate, tokenUrl, serverUrl, HttpMethod.POST,filename,i+1);
                    return token;
                }
                else if (i==maxRetry){
                    throw new TaskException(e.getMessage(), e);
                }
        }
    }
	return token;
}

    public ResponseEntity<String> getRestResponse(String serverUrl, HttpMethod method, HttpEntity<Object> request,
                                                  Class<String> responseType, RestTemplate restTemplate) throws TaskException {
       
    	
    	
    	
    	
    	
        ResponseEntity<String> response = null;
        String PingResponse=null;
        for (int i = 0; i <= maxRetry; i++) {
            try {
                if (i > 0) {
                    log.info("retrying connection, retry count " + i);
                }
                log.info("Sending REST request with readTimeout : "+ readTimeout/1000 + " secs and connectTimeout :"+connectionTimeout/1000 + " secs");
                response = restTemplate.exchange(serverUrl, method, request, String.class);
                if (response != null && (response.getStatusCode() == HttpStatus.OK
                        || response.getStatusCode() == HttpStatus.CREATED)) {
                    log.info("Received REST Response Successfully");
                    return response;
                }else
                    TimeUnit.SECONDS.sleep(sleepTimeForRetry);
            } catch (Exception e) {
                if (i == maxRetry) {
              
                    if (e.getMessage().contains("401 Unauthorized: [Access denied Failure.Bearer token is not valid]")){
                    
                    	log.info(e.getMessage());
                       
                        
                        
                    }
                    else{
                    	
                        throw new TaskException(e.getMessage(), e);
                        
                        
                    }
                }
            }

        }
        return response;
    }

    public String getToken(String jsonStr, String tokenName) throws TaskException {
        String accessToken = "";
        JsonNode root;
        try {
            root = mapper.readTree(jsonStr);
            JsonNode element = root.path(tokenName);
            accessToken = element.toString();
            if (accessToken.isEmpty()) {
                log.info("Token is empty");
            }
        } catch (IOException e) {
            throw new TaskException(e.getMessage(), e);
        }
        return accessToken;
    }

    public HttpEntity<Object> nfmp(Map<String, String> hints, String entityType) {
    	log.info("NFMP METHOD CALLED for full Discovery");

        String body = "{ \"grant_type\": \"client_credentials\"}";
      
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if(!hints.get(PROXY_USERNAME).isEmpty() && !hints.get(PROXY_PASSWORD).isEmpty())
        { 
        	String NFMP_Username = hints.get(NFMP_USERNAME);
			String NFMP_Password = hints.get(NFMP_AUTH);
			String auth = NFMP_Username + ":" + NFMP_Password;
			String encodedAuth =Base64.getEncoder().encodeToString(auth.getBytes());
			
			log.info("USERNAME=" + NFMP_Username + "  PASSWORD=" + NFMP_Password);
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

    public void createLocalDirectory(WorkflowData workflowData, String directoryName) {

        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        long batchId = workflowData.getInventoryOperation().getBatchRecord().getBatchId();
        long iterationId = workflowData.getInventoryOperation().getIterationId();
        String adapterName = workflowData.getDiscoveryOrder().getSeed().getSeedEntity().getNmsName();
        String dumpPath = hints.get(LOCAL_HOST_DIRECTORY).concat(File.separator).concat(File.separator)
                .concat(RAWDATA_FOLDER).concat(File.separator).concat(adapterName).concat(File.separator)
                .concat(f.format(new Date()).concat(File.separator).concat(String.valueOf(batchId))
                        .concat(DELIMITER_SERVER).concat(String.valueOf(iterationId)));
        hints.put(LOCAL_TARGET_DIRECTORY, dumpPath);
        workflowData.getDiscoveryOrder().getSeed().getSeedFeature().add(generateSeedFeature(workflowData.getDiscoveryOrder().getSeed().getSeedEntity(), LOCAL_TARGET_DIRECTORY, dumpPath));
        // File createLocalTargetDirectory = new File(dumpPath.concat(File.separator).concat(directoryName));
        File createLocalTargetDirectory = new File(dumpPath);
        if (!createLocalTargetDirectory.exists()) {
            createLocalTargetDirectory.mkdirs();
        }
    }

    public void fetchData() {
    }

   

    public void cleanUp() {
    }


    public void fetchSftpData(NFMPL2VPNSeedParam NFMPL2VPNSeedParam) throws AdapterException, AdapterTransformationException {
        sftpClient.doMGet(NFMPL2VPNSeedParam);
    }
    protected SimpleClientHttpRequestFactory getClientHttpRequestFactory() {

        SimpleClientHttpRequestFactory clientHttpRequestFactory  = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(connectionTimeout);
        clientHttpRequestFactory.setReadTimeout(readTimeout);
        return clientHttpRequestFactory;
    }
    

    
}