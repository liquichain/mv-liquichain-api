package io.liquichain.api.verification;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

public class VerifyOtpForPrivateInfo extends Script {
  private String otp;
  private String phoneNumber;
  private String result;
  
  public String getResult(){
    return this.result;  
  }
  
  public void setOtp(String otp){
    this.otp = otp;
  }
  
  public void setPhoneNumber(String phoneNumber){
    this.phoneNumber = phoneNumber;
  }
	
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
	}
	
}