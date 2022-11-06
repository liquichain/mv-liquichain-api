import EndpointInterface from "#{API_BASE_URL}/api/rest/endpoint/EndpointInterface.js";

// the request schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this is used to validate and parse the request parameters
const requestSchema = {
  "title" : "verifyOtpForPasswordResetRequest",
  "id" : "verifyOtpForPasswordResetRequest",
  "default" : "Schema definition for verifyOtpForPasswordReset",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "password" : {
      "title" : "password",
      "type" : "string",
      "minLength" : 1
    },
    "otp" : {
      "title" : "otp",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// the response schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this could be used to parse the result
const responseSchema = {
  "title" : "verifyOtpForPasswordResetResponse",
  "id" : "verifyOtpForPasswordResetResponse",
  "default" : "Schema definition for verifyOtpForPasswordReset",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "result" : {
      "title" : "result",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// should contain offline mock data, make sure it adheres to the response schema
const mockResult = {};

class verifyOtpForPasswordReset extends EndpointInterface {
	constructor() {
		// name and http method, these are inserted when code is generated
		super("verifyOtpForPasswordReset", "POST");
		this.requestSchema = requestSchema;
		this.responseSchema = responseSchema;
		this.mockResult = mockResult;
	}

	getRequestSchema() {
		return this.requestSchema;
	}

	getResponseSchema() {
		return this.responseSchema;
	}
}

export default new verifyOtpForPasswordReset();