package io.liquichain.api.rpc;

import java.util.Map;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class WalletByContactScript extends Script {

    private String result;

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

    public String getResult() {
        return this.result;
    }
}
