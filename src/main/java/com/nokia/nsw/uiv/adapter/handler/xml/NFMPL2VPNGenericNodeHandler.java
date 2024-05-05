package com.nokia.nsw.uiv.adapter.handler.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import com.nokia.nsw.uiv.model.resource.AdministrativeState;
import com.nokia.nsw.uiv.model.resource.OperationalState;
import com.nokia.nsw.uiv.model.service.ServiceOperationalState;
import com.nokia.nsw.uiv.adapter.transformation.intf.AdapterCache;
import com.nokia.nsw.uiv.exception.AccessForbiddenException;
import com.nokia.nsw.uiv.exception.BadRequestException;
import com.nokia.nsw.uiv.exception.ModificationNotAllowedException;
import com.nokia.nsw.uiv.model.common.party.PartyRole;
import com.nokia.nsw.uiv.model.location.*;
import com.nokia.nsw.uiv.model.resource.*;
import com.nokia.nsw.uiv.model.resource.infra.InfraComponent;
import com.nokia.nsw.uiv.model.resource.infra.InfraDevice;
import com.nokia.nsw.uiv.model.resource.infra.InfraResource;
import com.nokia.nsw.uiv.model.resource.infra.PowerState;
import com.nokia.nsw.uiv.model.resource.infra.physical.*;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualComponent;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualLink;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualPort;
import com.nokia.nsw.uiv.model.resource.logical.*;
import com.nokia.nsw.uiv.model.service.*;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.collector.NFMPL2VPNPartialDiscoveryCollector;
import com.nokia.nsw.uiv.numbermanagement.*;
import java.util.ArrayList;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;
import com.nokia.nsw.uiv.adapter.transformation.handler.util.XPathUtils;
import com.nokia.nsw.uiv.adapter.transformation.intf.NodeHandler;
import com.nokia.nsw.uiv.adapter.transformation.nodehandler.BaseNodeHandler;
import com.nokia.nsw.uiv.adapter.transformation.nodehandler.NodeHandlerResult;
import com.nokia.nsw.uiv.datatype.Neo4jDomainNodeObject;
import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.common.Entity;
import com.nokia.nsw.uiv.solution.adapter.config.xsd.entityconfig.AttributeDefinition;
import com.nokia.nsw.uiv.solution.adapter.config.xsd.entityconfig.EntityConfiguration.DiscoveryFilters.DiscoveryFilter.EntityList;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNModelClassMapper;
import com.nokia.nsw.uiv.adapter.NFMPL2VPN;

import lombok.extern.slf4j.Slf4j;

@Slf4j

public class NFMPL2VPNGenericNodeHandler extends BaseNodeHandler
		implements NodeHandler<Document, Neo4jDomainNodeObject, Class<? extends Neo4jDomainNodeObject>, String>{

	private static final String KIND_ATTR = "Kind";
	private static final String PROPERTY_ATTR = "Property";
	private static final String CATEGORY = "Category";
        private static final String SERIALNUMBER = "SerialNumber";
	private static final String DISPLAY_NAME = "DisplayName";
	private static final String DISCOVERED_NAME = "DiscoveredName";
	private static final String ADMINISTRATIVE_STATE = "AdministrativeState";
	private static final String OPERATIONAL_STATE = "OperationalState";
	private static final String STATE = "State";
	private static final String DEFAULT_INPUT_DATE_FORMAT = "dd/MM/yyyy";
	private final NFMPL2VPNModelClassMapper modelClassMapper;
	private SimpleDateFormat dateFormat;
	private List<String> includeAdapterNameInContextByKind = new ArrayList<>();
		protected Map<String, String> hints = NFMPL2VPN.adapterSeedParam.getSeedFeatureMap();
		 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNGenericNodeHandler.class);

	public NFMPL2VPNGenericNodeHandler(EntityList entityList) throws ClassNotFoundException {
		super(entityList);
		modelClassMapper = new NFMPL2VPNModelClassMapper();
		dateFormat = new SimpleDateFormat(DEFAULT_INPUT_DATE_FORMAT);
		
		if (null != hints.get("includeAdapterNameInContextByKind")
				&& !hints.get("includeAdapterNameInContextByKind").isEmpty()
				&& hints.get("includeAdapterNameInContextByKind") != "") {
			includeAdapterNameInContextByKind = Arrays
					.asList(hints.get("includeAdapterNameInContextByKind").split(","));
		

		} else {
			log.info("includeAdapterNameInContextByKind--Error");
		}
		
		
	}

	public boolean processNode(Document node) throws AdapterTransformationException {

		try {
			XdmNode xdmNode = XPathUtils.getXdmNode(node);

			if (super.processNode(xdmNode)) {
				List<NodeHandlerResult> nodeHandlerResults = super.getNodeHandlerResult();
				for (NodeHandlerResult nodeHandlerResult : nodeHandlerResults) {
					if (nodeHandlerResult != null) {
						Entity entity = (Entity) nodeHandlerResult.getEntity();

						String contextVal = null;
						contextVal = entity.getContext();
						if (hints.get("includeAdapterNameInContext").equalsIgnoreCase("true")) {

							if ((includeAdapterNameInContextByKind.size() > 0
									&& includeAdapterNameInContextByKind.contains(entity.getKind()))
									|| (includeAdapterNameInContextByKind.isEmpty())) {

								entity.setContext(hints.get("adapterName") + ":" + contextVal);

							}

						}
					}
					
					processHandlerResult(nodeHandlerResult);
				}
				return true;
			}
		} catch (SaxonApiException e) {
			throw new AdapterTransformationException(e.getMessage(), e);
		}catch (BadRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public Class<? extends Neo4jDomainNodeObject> getClassByEntityType(String entityType) {
		return modelClassMapper.getClassByEntityType(entityType);
	}

	@Override
	public void processAttribute(Neo4jDomainObject neo4jDomainObject, AttributeDefinition attributeDefinition,
								 String val) throws Exception {
	
		/*for (Map.Entry<String, String> entry : hints.entrySet()) {
			log.info("Key: " + entry.getKey() + " Value: " + entry.getValue());
		}*/
		handleNeo4jDomainObject(attributeDefinition, val, neo4jDomainObject);
		if (neo4jDomainObject instanceof Neo4jDomainNodeObject) {
			Neo4jDomainNodeObject neo4jDomainNodeObject = (Neo4jDomainNodeObject) neo4jDomainObject;
			handleNeo4jDomainNodeObject(attributeDefinition, val, neo4jDomainNodeObject);
			if (neo4jDomainObject instanceof Entity) {
				Entity entity = (Entity) neo4jDomainObject;
				handleEntity(attributeDefinition, val, entity);
				if (entity instanceof Place) {
					Place place = (Place) entity;
					handlePlace(attributeDefinition, val, place);
					if (place instanceof GeographicSite) {
						GeographicSite site = (GeographicSite) place;
						handleGeographicSite(attributeDefinition, val, site);
					} else if (place instanceof GeographicLocation) {
					}
				} else if (entity instanceof Resource) {
					Resource resource = (Resource) entity;
					handleLogicalResource(attributeDefinition, val, resource);
					if (resource instanceof LogicalResource) {
						LogicalResource logicalResource = (LogicalResource) resource;
						if (logicalResource instanceof LogicalDevice) {
							LogicalDevice logicalDevice = (LogicalDevice) logicalResource;
							handleLogicalDevice(attributeDefinition, val, logicalDevice);
						} else if (logicalResource instanceof LogicalComponent) {
							LogicalComponent logicalComponent = (LogicalComponent) logicalResource;
							switch (attributeDefinition.getName()) {
								case "software.name": {
									Software software = software(logicalComponent);
									software.setName(val);
									break;
								}
								case "software.version": {
									Software software = software(logicalComponent);
									software.setVersion(val);
									break;
								}
							}
							if (logicalComponent instanceof LogicalInterface) {
								LogicalInterface logicalInterface = (LogicalInterface) logicalComponent;
							}
						} else if (logicalResource instanceof Configuration) {

						} else if (logicalResource instanceof Protocol) {

						} else if (logicalResource instanceof Pipe) {
							Pipe pipe = (Pipe) logicalResource;
							if (pipe instanceof Trail) {
								Trail trail = (Trail) pipe;
							} else if (pipe instanceof Connection) {

							}
						}
					} else if (resource instanceof InfraResource) {
						InfraResource infraResource = (InfraResource) resource;
						handleInfraResource(attributeDefinition, val, infraResource);
						if (infraResource instanceof InfraDevice) {
							InfraDevice infraDevice = (InfraDevice) infraResource;
							handleInfraDevice(attributeDefinition, val, infraDevice);
						} else if (infraResource instanceof VirtualLink) {
						} else if (infraResource instanceof InfraComponent) {
							InfraComponent infraComponent = (InfraComponent) infraResource;
							if (infraComponent instanceof PhysicalComponent) {
								PhysicalComponent physicalComponent = (PhysicalComponent) infraComponent;
								if (physicalComponent instanceof PhysicalPort) {
									handlePhysicalPort(attributeDefinition, val, (PhysicalPort) physicalComponent);
								}
							} else if (infraComponent instanceof VirtualComponent) {
								VirtualComponent virtualComponent = (VirtualComponent) infraComponent;
								if (virtualComponent instanceof VirtualPort) {
									VirtualPort virtualPort = (VirtualPort) virtualComponent;
								}
							}
						} else if (infraResource instanceof PhysicalLink) {
							PhysicalLink physicalLink = (PhysicalLink) infraResource;
							handlePhysicalLink(attributeDefinition, val, physicalLink);
						}
					}
				} else if (entity instanceof Service) {
					Service service = (Service) entity;
					handleService(attributeDefinition, val, service);
				} else if (entity instanceof PartyRole) {
					PartyRole partyRole = (PartyRole) entity;
					if (partyRole instanceof ServiceProvider) {
					} else if (partyRole instanceof ServiceConsumer) {
					}
				} else if (entity instanceof Subscription) {
					Subscription service = (Subscription) entity;
					handleSubscription(attributeDefinition, val, service);
				}
			} else if (neo4jDomainNodeObject instanceof Identifier) {
				Identifier identifier = (Identifier) neo4jDomainNodeObject;
				handleIdentifier(attributeDefinition, val, identifier);
			} else if (neo4jDomainNodeObject instanceof Pool) {
				Pool pool = (Pool) neo4jDomainNodeObject;
				handlePool(attributeDefinition, val, pool);
			}else if (neo4jDomainNodeObject instanceof Assignment) {
				Assignment assignment = (Assignment) neo4jDomainNodeObject;
				handleAssignment(attributeDefinition, val, assignment);
			}else if (neo4jDomainNodeObject instanceof Reservation) {
				Reservation reservation = (Reservation) neo4jDomainNodeObject;
				handleReservation(attributeDefinition, val, reservation);
			}
		}
	}

	private void handleLogicalDevice(AttributeDefinition attributeDefinition, String val, LogicalDevice logicalDevice) {
		switch (attributeDefinition.getName()) {
			case "software.name": {
				Software software = software(logicalDevice);
				software.setName(val);
				break;
			}
			case "software.version": {
				Software software = software(logicalDevice);
				software.setVersion(val);
				break;
			}
		}
	}

	private void handleLogicalResource(AttributeDefinition attributeDefinition, String val, Resource resource) {
		switch (attributeDefinition.getName()) {
			case "usageState": {
				resource.setUsageState(ResourceUsageState.valueOf(val));
				break;
			}
			case "lifecycleState": {
				resource.setLifecycleState(ResourceLifecycleState.valueOf(val));
				break;
			}
			case "version": {
				resource.setVersion(val);
				break;
			}
			case "category": {
				resource.setCategory(val);
				break;
			}
			case "administrativeState": {
				resource.setAdministrativeState(AdministrativeState.valueOf(val));
				break;
			}
			case "operationalState": {
				resource.setOperationalState(OperationalState.valueOf(val));
				break;
			}
			case "protectionAssociationType": {
				resource.setProtectionAssociationType(ProtectionAssociationType.valueOf(val));
				break;
			}

		}
	}

	private void handleReservation(AttributeDefinition attributeDefinition, String val, Reservation reservation) throws ParseException {
		switch (attributeDefinition.getName()){
			case "reservationDate":{
				reservation.setReservationDate(dateFormat.parse(val));
				break;
			}
			default:{
				break;
			}
		}
	}

	private void handleAssignment(AttributeDefinition attributeDefinition, String val, Assignment assignment) throws ParseException {
		switch (attributeDefinition.getName()){
			case "assignmentDate":{
				assignment.setAssignmentDate(dateFormat.parse(val));
				break;
			}
			case "assignmentDuration":{
				assignment.setAssignmentDuration(Long.valueOf(val));
				break;
			}
			case "assignmentPrice":{
				assignment.setAssignmentPrice(Float.valueOf(val));
				break;
			}

			default:{
				break;
			}
		}
	}

	private void handlePool(AttributeDefinition attributeDefinition, String val, Pool pool) {
		switch (attributeDefinition.getName()){
			case "state":{
				//pool.setState(PoolState.valueOf(val));
				break;
			}
			case "kind":{
				//pool.setKind(val);
				break;
			}
			case "numberAvailable":{
				pool.setNumberAvailable(Long.valueOf(val));
				break;
			}
			case "numberTotal":{
				pool.setNumberTotal(Long.valueOf(val));
				break;
			}
			case "price":{
				pool.setPrice(Float.valueOf(val));
				break;
			}
			case "defaultQuarantineTimeOutInHrs":{
				pool.setDefaultQuarantineTimeOutInHrs(Long.valueOf(val));
				break;
			}
			case "defaultReservationTimeOutInHrs":{
				pool.setDefaultReservationTimeOutInHrs(Long.valueOf(val));
				break;
			}
			case "priority":{
				pool.setPriority(Integer.valueOf(val));
				break;
			}
			case "property":{
				if (attributeDefinition.getKey() != null || !attributeDefinition.getKey().isEmpty()) {
					pool.getProperties().put(attributeDefinition.getKey(), val);
				}
				break;
			}
			default:{
				break;
			}
		}
	}

	private void handleIdentifier(AttributeDefinition attributeDefinition, String val, Identifier identifier) throws ParseException {
		switch (attributeDefinition.getName()){
			case "state":{
				identifier.setState(IdentifierState.valueOf(val));
				break;
			}
			case "activationDate":{
				identifier.setActivationDate(dateFormat.parse(val));
				break;
			}
			case "quarantinedDate":{
				identifier.setQuarantinedDate(dateFormat.parse(val));
				break;
			}
			case "stateUpdateDate":{
				identifier.setStateUpdateDate(dateFormat.parse(val));
				break;
			}
			case "basePrice":{
				identifier.setBasePrice(Float.valueOf(val));
				break;
			}
			case "externalID":{
				identifier.setExternalID(val);
				break;
			}
			case "createdInPool":{
				identifier.setCreatedInPool(val);
				break;
			}
			case "priority":{
				identifier.setPriority(Integer.valueOf(val));
				break;
			}
			case "property":{
				if (attributeDefinition.getKey() != null || !attributeDefinition.getKey().isEmpty()) {
					//identifier.getProperties().put(attributeDefinition.getKey(), val);
				}
				break;
			}
			default:{
				break;
			}
		}
	}

	private void handleSubscription(AttributeDefinition attributeDefinition, String val, Subscription service) throws ParseException {
		switch (attributeDefinition.getName()) {
			case "startDate": {
				service.setStartDate(dateFormat.parse(val));
				break;
			}
			case "endDate": {
				service.setEndDate(dateFormat.parse(val));
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handleService(AttributeDefinition attributeDefinition, String val, Service service) throws ParseException {
		switch (attributeDefinition.getName()) {
			case "state": {
				service.setState(ServiceOperationalState.valueOf(val));
				break;
			}
			case "billable": {
				service.setBillable(Boolean.valueOf(val));
				break;
			}
			case "startDate": {
				service.setStartDate(dateFormat.parse(val));
				break;
			}
			case "endDate": {
				service.setEndDate(dateFormat.parse(val));
				break;
			}
			case "serviceDate": {
				service.setServiceDate(dateFormat.parse(val));
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handlePhysicalLink(AttributeDefinition attributeDefinition, String val, PhysicalLink physicalLink) {
		switch (attributeDefinition.getName()) {
			case "physicalLinkType": {
				physicalLink.setPhysicalLinkType(PhysicalLinkType.valueOf(val));
				break;
			}
			case "physicalLinkTypeOther": {
				physicalLink.setPhysicalLinkTypeOther(val);
				break;
			}
		}
	}

	private void handlePhysicalPort(AttributeDefinition attributeDefinition, String val, PhysicalPort physicalComponent) {
		PhysicalPort physicalPort = physicalComponent;
		switch (attributeDefinition.getName()) {
			case "ifType": {
				physicalPort.setIfType(PhysicalPortInterfaceType.valueOf(val));
				break;
			}
			case "portNumber": {
				physicalPort.setPortNumber(val);
				break;
			}
			case "ifTypeOther": {
				physicalPort.setIfTypeOther(val);
				break;
			}
		}
	}

	private void handleInfraDevice(AttributeDefinition attributeDefinition, String val, InfraDevice infraDevice) {
		switch (attributeDefinition.getName()) {
			case "operatingSystem.name": {
				OperatingSystem os = infraDevice.getOperatingSystem();
				os = instance(os, OperatingSystem.class);
				infraDevice.setOperatingSystem(os);
				os.setName(val);
				break;
			}
			case "operatingSystem.version": {
				OperatingSystem os = infraDevice.getOperatingSystem();
				os = instance(os, OperatingSystem.class);
				infraDevice.setOperatingSystem(os);
				os.setVersion(val);
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handleInfraResource(AttributeDefinition attributeDefinition, String val, InfraResource infraResource) {
		switch (attributeDefinition.getName()) {
			case "powerState": {
				infraResource.setPowerState(PowerState.valueOf(val));
				break;
			}
			case "serialNumber": {
				infraResource.setSerialNumber(val);
				break;
			}
			case "model": {
				infraResource.setModel(val);
				break;
			}
			case "vendor": {
				infraResource.setVendor(val);
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handleGeographicSite(AttributeDefinition attributeDefinition, String val, GeographicSite site) {
		switch (attributeDefinition.getName()) {
			case "code": {
				site.setCode(val);
				break;
			}
			case "status": {
				site.setStatus(GeographicSiteStatus.valueOf(val));
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handlePlace(AttributeDefinition attributeDefinition, String val, Place place) {
		switch (attributeDefinition.getName()) {
			case "x": {
				place.setX(Double.valueOf(val));
				break;
			}
			case "y": {
				place.setY(Double.valueOf(val));
				break;
			}
			case "z": {
				place.setZ(Double.valueOf(val));
				break;
			}
			case "radius": {
				place.setRadius(Double.valueOf(val));
				break;
			}
			case "address.country": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setCountry(val);
				break;
			}
			case "address.stateOrProvince": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStateOrProvince(val);
				break;
			}
			case "address.city": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setCity(val);
				break;
			}
			case "address.locality": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setLocality(val);
				break;
			}
			case "address.postcode": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setPostcode(val);
				break;
			}
			case "address.streetName": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetName(val);
				break;
			}
			case "address.streetSuffix": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetSuffix(val);
				break;
			}
			case "address.streetType": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetType(val);
				break;
			}
			case "address.streetNr": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetNr(val);
				break;
			}
			case "address.streetNrSuffix": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetNrSuffix(val);
				break;
			}
			case "address.streetNrLast": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetNrLast(val);
				break;
			}
			case "address.streetNrLastSuffix": {
				GeographicAddress address = instance(place.getAddress(), GeographicAddress.class);
				place.setAddress(address);
				address.setStreetNrLastSuffix(val);
				break;
			}
			default: {
				break;
			}
		}
	}

	private void handleEntity(AttributeDefinition attributeDefinition, String val, Entity entity) throws ModificationNotAllowedException {
		switch (attributeDefinition.getName()) {
			case PROPERTY_ATTR:
				if (attributeDefinition.getKey() != null || !attributeDefinition.getKey().isEmpty()) {
					entity.getProperties().put(attributeDefinition.getKey(), val);
				}
				break;

			case KIND_ATTR:
				entity.setKind(val);
				break;
			case DISCOVERED_NAME:
				entity.setDiscoveredName(val);
				break;
			case CATEGORY:
				 ((Resource) entity).setCategory(val);
				break;

                        case SERIALNUMBER:
				 ((InfraResource) entity).setSerialNumber(val);
				break;

				case ADMINISTRATIVE_STATE:
				
				try {
					if (entity instanceof Resource) {
						Resource resource = (Resource) entity;
						if (val.toUpperCase().equals("serviceUp") || val.equals("inService") || val.equals("portInService") || val.equals("circuitUp")) {
							resource.setAdministrativeState(AdministrativeState.Activated);
						} else {
							resource.setAdministrativeState(AdministrativeState.Unknown);
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
					log.error("Exception",e);
				}
				break;
				
				
				case STATE:
					try {
					if (entity instanceof Service) {
					Service service = (Service) entity;
					if (val.equals("up"))
					{
					service.setState(ServiceOperationalState.active);
					}
					else
					{
					service.setState(ServiceOperationalState.unknown);
					}
					}
					} catch (Exception e) {
					//e.printStackTrace();
					log.error("Exception",e);
					}
					break;
					
					case OPERATIONAL_STATE:
				try {
					if (entity instanceof Resource) {
						Resource resource = (Resource) entity;
						if (val.toUpperCase().equals("serviceUp") || val.equals("inService") || val.equals("portInService") || val.equals("circuitUp") ) {
							resource.setOperationalState(OperationalState.Working);
						} else {
							resource.setOperationalState(OperationalState.NotWorking);
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
					log.error("Exception",e);
				}
				break;
			case "description":
				entity.setDescription(val);
				break;
			default:
				break;
		}
	}

	private <T> T instance(T instance, Class<T> type) {
		try {
			if (instance == null) {
				instance = type.getDeclaredConstructor().newInstance();
			}
			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Software software(LogicalComponent obj) {
		Software software = obj.getSoftware();
		if (software == null) {
			software = new Software();
			obj.setSoftware(software);
		}
		return software;
	}

	private Software software(LogicalDevice obj) {
		Software software = obj.getSoftware();
		if (software == null) {
			software = new Software();
			obj.setSoftware(software);
		}
		return software;
	}

	private void handleNeo4jDomainObject(AttributeDefinition attributeDefinition, String val, Neo4jDomainObject entity) {
		switch (attributeDefinition.getName()) {
			case "ReconSource":
				entity.setReconSource(val);
				break;

			case "ref":
				entity.setRef(val);
				break;
			case "mode":
				entity.setMode(val);
				break;
			case "type":
				entity.setType(val);
				break;
			case "liveVersion":
				entity.setLiveVersion(val);
				break;
			case "schemaVersion":
				entity.setSchemaVersion(Integer.valueOf(val));
				break;
			case "sandboxId":
				entity.setSandboxId(val);
				break;
			default:
				break;
		}
	}

	private void handleNeo4jDomainNodeObject(AttributeDefinition attributeDefinition, String val, Neo4jDomainNodeObject entity) throws BadRequestException, AccessForbiddenException {
		switch (attributeDefinition.getName()) {
			case DISPLAY_NAME:
			
				try {
					entity.setDisplayName(val);
				} catch (Exception e) {
					log.error("Exception",e);
				}
				break;
			case "Context":
				entity.setContext(val);
				break;
			case "href":
				entity.setHref(val);
				break;
			case "name":
				entity.setName(val);
				break;
			case "isRootNetworkElement":
				entity.setIsRootNetworkElement(Boolean.valueOf(val));
				break;
			case "localName":
				entity.setLocalName(val);
				break;
			case "ReconSource":
				entity.setReconSource(val);
				break;
			default:
				break;
		}
	}
}
