package com.nokia.nsw.uiv.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nokia.nsw.uiv.adapterworkflow.metadata.WorkflowData;
import com.nokia.nsw.uiv.message.metadata.DiscoveryType;
import com.nokia.nsw.uiv.message.metadata.IPayload;
import com.nokia.nsw.uiv.message.processor.InventoryOperation;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration.Filters.Filter;
import com.nokia.nsw.uiv.rda.payload.DiscoveryPayload;
import com.nokia.oss.sure.adapter.common.FloeAdapterConstants;

import com.nokia.nsw.uiv.adapter.si.http.NFMPL2VPNHttpSeedParam;
import com.nokia.nsw.uiv.adapter.si.sftp.NFMPL2VPNSftpSeedParam;

public final class NFMPL2VPNSeedParam {
	
	public final static String MDL_OUTPUT = "MDL";
	public final static String KAFKA_OUTPUT = "KAFKA";
	public final static String CONFIG_DIR = "config";
	public static final String NSPIP_ADAPTER_PARTAIL_DISCOVERY_CONFIG_FILE = "NFMPL2VPNPartialDiscoveryConfigFile";
	public static final String NSPIP_ADAPTER_RESULT_FILTERS_CONFIG_FILE = "NFMPL2VPNResultFiltersConfigFile";

	public static final String SKIP_COLLECTION = "skipCollection";
	private final static String ENTITIES_WITH_EMSNAME = "entitiesWithEmsName";
	

	private final static String USER_NAME = "userName";
	private final static String PASSWORD = "password";
	private final static String ADAPTER_NAME = "adapterName";
	private final static String INCLUDE_ADAPTER_NAME_IN_CONTEXT = "includeAdapterNameInContext";
	private WorkflowData workflowData;
	private Map<String, String> seedFeatureMap;

	private NFMPL2VPNHttpSeedParam httpSeedParam;
	private NFMPL2VPNSftpSeedParam sftpSeedParam;
	
	private DiscoveryType discoveryType = null;
	private String configDir = null;
	private String configFile = null;
	private String batchId = null;
	private String outputMode = null;
	private String localDir = null;
	
	public NFMPL2VPNSeedParam(WorkflowData workflowData) {
		this.workflowData = workflowData;
		this.seedFeatureMap = workflowData.getDiscoveryOrder().getSeed().seedFeaturesAsMAP();
		this.httpSeedParam = new NFMPL2VPNHttpSeedParam(this);
		this.sftpSeedParam = new NFMPL2VPNSftpSeedParam(this);
	}

	
	public WorkflowData getWorkflowData() {
		return workflowData;
	}
	
	public NFMPL2VPNHttpSeedParam getHttpSeedParam() {
		return httpSeedParam;
	}

	public NFMPL2VPNSftpSeedParam getSftpSeedParam() {
		return sftpSeedParam;
	}

	public Map<String, String> getSeedFeatureMap() {
		return seedFeatureMap;
	}

	public String getHostName() {
		return seedFeatureMap.get(FloeAdapterConstants.HOST_NAME);
	}

	public String getUserName() {
		return seedFeatureMap.get(USER_NAME);
	}

	public String getPassword() {
		return seedFeatureMap.get(PASSWORD);
	}

	
	
	public String getOperationId() {
		return workflowData.getInventoryOperation().getMessageHeader().getRoutingKey().getOperationId();
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}

	public String getLocalDir() {
		if (localDir == null) {
			if (skipCollection()) {
				return seedFeatureMap.get(FloeAdapterConstants.LOCAL_HOST_DIRECTORY);
			} else {
				return seedFeatureMap.get(FloeAdapterConstants.LOCAL_HOST_DIRECTORY) + File.separator
						+ getOperationId();
			}			
		} else {
			return localDir;
		}
	}

	public String getConfigFile() {
		if (configFile == null) {
			return seedFeatureMap.get(FloeAdapterConstants.ADAPTER_CONFIG_FILE);
		} else {
			return configFile;
		}
	}

	public String getConfigDir() {
		if (configDir == null) {
			return seedFeatureMap.get(FloeAdapterConstants.ADAPTER_CONFIG_FILE_DIR) + File.separator;
		} else {
			return configDir;
		}
	}

	public String getBatchId() {
		if (batchId == null) {
			return workflowData.getInventoryOperation().getBatchRecord().getBatchId().toString();
		} else {
			return batchId;
		}
	}

	public String getOutputMode() {
		if (outputMode == null) {
			return seedFeatureMap.get(FloeAdapterConstants.OUTPUT_MODE);
		} else {
			return outputMode;
		}
	}

	public DiscoveryType getDiscoveryType() {
		if (discoveryType == null) {
			return workflowData.getInventoryOperation().getMessageHeader().getDiscoveryType();
		} else {
			return discoveryType;
		}
	}

	public InventoryOperation getInventoryOperation() {
		return workflowData.getInventoryOperation();
	}
	
	public String[] getPipeline() {
		if (seedFeatureMap.get(FloeAdapterConstants.ADAPTER_PIPELINE) != null) {
			return seedFeatureMap.get(FloeAdapterConstants.ADAPTER_PIPELINE).split(",");
		} else {
			return new String[0];
		}
	}

	public Boolean skipCollection() {
		return Boolean.valueOf(seedFeatureMap.get(SKIP_COLLECTION));
	}	
	
	public List<Filter> getPartialDiscoveryFilterRules() {
		IPayload discoveryBatchPayload = workflowData.getDiscoveryOrder().getPayload();

		if (discoveryBatchPayload != null && discoveryBatchPayload instanceof DiscoveryPayload) {
			FilterConfiguration filterConfig = ((DiscoveryPayload) discoveryBatchPayload).getFilterConfig();
			return filterConfig.getFilters().getFilter();
		}
		return new ArrayList<Filter>();
	}
	
	public FilterConfiguration getDiscoveryFilterConfiguration() {
		IPayload discoveryBatchPayload = workflowData.getDiscoveryOrder().getPayload();
		if (discoveryBatchPayload != null && discoveryBatchPayload instanceof DiscoveryPayload) {
			FilterConfiguration filterConfig = ((DiscoveryPayload) discoveryBatchPayload).getFilterConfig();
			return filterConfig;
		}
		return null;
	}
	
	public String getNFMPL2VPNPartialDiscoveryConfig() {
		return seedFeatureMap.get(NSPIP_ADAPTER_PARTAIL_DISCOVERY_CONFIG_FILE);
	}
	public String getNFMPL2VPNResultFiltersConfig() {
		return seedFeatureMap.get(NSPIP_ADAPTER_RESULT_FILTERS_CONFIG_FILE);
	}
	public String getAdapterName() {
		return seedFeatureMap.get(ADAPTER_NAME);
	}
	public Boolean getIncludeAdapterNameInContext() {
		return Boolean.valueOf(seedFeatureMap.get(INCLUDE_ADAPTER_NAME_IN_CONTEXT));
	}
}