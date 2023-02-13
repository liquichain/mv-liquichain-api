package io.liquichain.api.handler;

import static io.liquichain.api.rpc.EthApiUtils.*;

import java.math.BigInteger;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class MethodHandlerInput extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandlerInput.class);

    private RawTransaction rawTransaction;
    private String smartContractAddress;

    public MethodHandlerInput(RawTransaction rawTransaction, String smartContractAddress) {
        this.rawTransaction = rawTransaction;
        this.smartContractAddress = smartContractAddress;
    }

    public boolean noRawData() {
        return rawTransaction == null || rawTransaction.getData() == null;
    }

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public void setRawTransaction(RawTransaction rawTransaction) {
        this.rawTransaction = rawTransaction;
    }

    public String getSmartContractAddress() {
        return smartContractAddress;
    }

    public void setSmartContractAddress(String smartContractAddress) {
        this.smartContractAddress = smartContractAddress;
    }

    @Override public String toString() {
        return "MethodHandlerInput{" +
            "rawTransaction=" + rawTransaction +
            ", smartContractAddress='" + smartContractAddress + '\'' +
            '}';
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
