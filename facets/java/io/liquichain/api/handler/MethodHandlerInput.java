package io.liquichain.api.handler;

import static org.web3j.utils.Numeric.*;

import java.math.BigInteger;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

public class MethodHandlerInput extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandlerInput.class);

    private RawTransaction rawTransaction;
    private String smartContractAddress;
    private String recipient;
    private BigInteger value;
    private boolean isContract;

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

    public String getRecipient() {
        if (noRawData()) {
            return null;
        }
        if (isSmartContract()) {
            recipient = rawTransaction.getData().substring(34, 74);
        } else {
            recipient = rawTransaction.getTo();
        }
        LOG.info("MethodHandlerInput getRecipient: {}", recipient);
        return recipient;
    }

    public BigInteger getValue() {
        if (noRawData()) {
            return null;
        }
        if (isSmartContract()) {
            value = new BigInteger(rawTransaction.getData().substring(74), 16);
        } else {
            value = rawTransaction.getValue();
        }
        LOG.info("MethodHandlerInput value: {}", value);
        return value;
    }

    public boolean isSmartContract() {
        boolean isContract = rawTransaction != null
            && smartContractAddress != null
            && prependHexPrefix(rawTransaction.getData()).equals(prependHexPrefix(smartContractAddress));
        LOG.info("MethodHandlerInput isSmartContract: {}", isContract);
        return isContract;
    }

    @Override public String toString() {
        return "MethodHandlerInput{" +
            "rawTransaction=" + rawTransaction +
            ", smartContractAddress='" + smartContractAddress + '\'' +
            ", recipient='" + recipient + '\'' +
            ", value=" + value +
            '}';
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
