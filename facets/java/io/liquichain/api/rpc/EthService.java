package io.liquichain.api.rpc;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.meveo.commons.utils.ParamBean;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.liquichain.api.rpc.EthApiConstants.INTERNAL_ERROR;
import static io.liquichain.api.rpc.EthApiConstants.PROXY_REQUEST_ERROR;
import static io.liquichain.api.rpc.EthApiUtils.createErrorResponse;
import static io.liquichain.api.rpc.EthApiUtils.toJson;

public class EthService extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthService.class);

    private final String BESU_API_URL;
    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private static final Client client = new ResteasyClientBuilder()
        .connectionPoolSize(CONNECTION_POOL_SIZE)
        .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
        .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
        .build();

    public EthService(ParamBean config) {
        super();
        BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");
    }

    public String callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        Object id = parameters.get("id");
        Object jsonRpcVersion = parameters.get("jsonrpc");
        Object method = parameters.get("method");
        Object params = parameters.get("params");
        boolean isNumeric = id == null || NumberUtils.isParsable("" + id);

        String result;
        Response response = null;

        String body = "{" +
            (isNumeric ? "\"id\": " + id + "," : "\"id\": \"" + id + "\",") +
            "\"jsonrpc\":\"" + jsonRpcVersion + "\"," +
            "\"method\":\"" + method + "\"," +
            "\"params\": " + toJson(params) +
            "}";

        LOG.info("callEthJsonRpc body: {}", body);

        try {
            response = client.target(BESU_API_URL)
                             .request(MediaType.APPLICATION_JSON)
                             .post(Entity.json(body));
            result = response.readEntity(String.class);
        } catch (Exception e) {
            LOG.error(PROXY_REQUEST_ERROR, e);
            return createErrorResponse(requestId, INTERNAL_ERROR, PROXY_REQUEST_ERROR);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        LOG.info("callEthJsonRpc result: {}", result);
        return result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
