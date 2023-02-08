package io.liquichain.api.handler;

import java.math.BigInteger;
import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class MethodHandlerResult extends Script {
    private String transactionType;
    private String extraData;
    private BigInteger value;

    public MethodHandlerResult(String transactionType, String extraData, BigInteger value) {
        this.transactionType = transactionType;
        this.extraData = extraData;
        this.value = value;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MethodHandlerResult{" +
            "transactionType='" + transactionType + '\'' +
            ", extraData='" + extraData + '\'' +
            ", value=" + value +
            '}';
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
