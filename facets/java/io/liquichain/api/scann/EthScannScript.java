package io.liquichain.api.scann;

import io.liquichain.core.BlockForgerScript;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.math.BigInteger;
import java.io.IOException;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.api.exception.EntityDoesNotExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.model.customEntities.CustomEntityInstance;

import org.web3j.crypto.*;

public class EthScannScript extends Script {

    private static final Logger log = LoggerFactory.getLogger(EthScannScript.class);
  
    public static boolean isJSONValid(String jsonInString ) {
      try {
         final ObjectMapper mapper = new ObjectMapper();
         mapper.readTree(jsonInString);
         return true;
      } catch (Exception e) {
         return false;
      }
    }

  	private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();
  
    private String result;
  
    private String module;
	  private String action;
	  private String address;
    private String tag;
    private String apikey;
    private int offset;
    private int limit=10;

  
    public String getResult() {
        return result;
    }

  
    public void setModule(String module){
      this.module = module;
    }
    
    public void setAction(String action){
      this.action = action;
    }
    
    public void setAddress(String address){
      this.address = address;
    }
    
    public void setTag(String tag){
      this.tag = tag;
    }
    
    public void setApikey(String apikey){
      this.apikey = apikey;
    }

    public void setOffset(int offset){
      this.offset = offset;
    }

    public void setLimit(int limit){
      this.limit = limit;
    }
  
public void execute(Map<String, Object> parameters) throws BusinessException {
      	//log.info("projectId : {}", projectId);
  		address=address.toLowerCase();
        if(address.startsWith("0x")){
            address=address.substring(2);
        }
        switch(action) {
          case "balance":
                result = getBalance(address);
                break;
          case "balancehistory":
                result = getTransactionList(address);
                break;
        }
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }
  
    private String toBigHex(String i){
       return "0x"+new BigInteger(i).toString(16).toLowerCase();
    }
  
  	private String createResponse(String status,String message,String result) {
        String res = "{\n";
        res += "  \"status\": " + 1 + ",\n";
        res += " \"message\" : \"" + message + "\",\n";
        res += " \"result\" : " + result + "\n";
        res += "}";
        log.info("response:{}", res);
        return res;
    }
  
    private String getBalance(String hash) {
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class).by("hexHash", hash).getResult();
            return createResponse("1","OK-Missing/Invalid API Key, rate limit of 1/5sec applied", "\"0x"+new BigInteger(wallet.getBalance()).toString(16))+"\"";
        } catch (Exception e) {
            //e.printStackTrace();
            return createResponse("0",  "Resource not found","");
        }
    }
  
    public String getTransactionList(String hash){
         List<Transaction> transactions = crossStorageApi.find(defaultRepo, Transaction.class).by("fromHexHash", hash).limit(offset+limit).getResults();
         List<Transaction> transactionsTo = crossStorageApi.find(defaultRepo, Transaction.class).by("toHexHash", hash).limit(offset+limit).getResults();
         for(Transaction transac:transactionsTo){
           //we reverse the amount for transfer received
            BigInteger amount = new BigInteger(transac.getValue()).negate();
            transac.setValue(amount.toString());
          }
      	 transactions.addAll(transactionsTo);
         transactions = transactions.stream()
			      .sorted(Comparator.comparing(Transaction::getCreationDate).reversed())
			      .collect(Collectors.toList());
         //check offset and limit
         if(transactions.size()<=offset){
          transactions = new ArrayList<>();
         } else {
          transactions = transactions.subList(offset,Math.min(offset+limit,transactions.size()));
         }
         String result="[";
      	 String sep="";
         for(Transaction transac:transactions){
           result+=sep+"{";
           result+="\"blockNumber\":\""+transac.getBlockNumber()+"\",";
           result+="\"timeStamp\":\""+transac.getCreationDate()+"\",";
           result+="\"hash\":\"0x"+transac.getHexHash()+"\",";
           result+="\"nonce\":\""+this.toBigHex(transac.getNonce())+"\",";
           result+="\"blockHash\":\""+transac.getBlockHash()+"\",";
           result+="\"transactionIndex\":\""+transac.getTransactionIndex()+"\",";
           result+="\"from\":\"0x"+transac.getFromHexHash()+"\",";
           result+="\"to\":\"0x"+transac.getToHexHash()+"\",";
           result+="\"value\":\"0x"+(new BigInteger(transac.getValue())).toString(16)+"\",";
           if(transac.getData()!=null){
             if(isJSONValid(transac.getData())){
               result += "\"data\": " + transac.getData() + ",\n";
             } else {
               result += "\"data\": \"" + transac.getData() + "\",\n";
             }
           result+="\"gas\":\"0\",";
           result+="\"gasPrice\":\"0x"+transac.getGasPrice()+"\",";
           result+="\"isError\":\"0\",";
           result+="\"txreceipt_status\":\"1\",";
           result+="\"input\":\"0x\",\"contractAddress\":\"\",\"cumulativeGasUsed\":\"\",\"gasUsed\":\"\",\"confirmations\":\"281736\"";
           result+="}";
           sep=",";
         }
         result+="]";
         return createResponse("1","OK",result);
    }
}
