import EndpointInterface from "#{API_BASE_URL}/api/rest/endpoint/EndpointInterface.js";

// the request schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this is used to validate and parse the request parameters
const requestSchema = {
  "title" : "etherscanRequest",
  "id" : "etherscanRequest",
  "default" : "Schema definition for etherscan",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "address" : {
      "title" : "address",
      "id" : "etherscan_address",
      "type" : "string",
      "minLength" : 1
    },
    "apikey" : {
      "title" : "apikey",
      "id" : "etherscan_apikey",
      "type" : "string",
      "minLength" : 1
    },
    "module" : {
      "title" : "module",
      "id" : "etherscan_module",
      "type" : "string",
      "minLength" : 1
    },
    "action" : {
      "title" : "action",
      "id" : "etherscan_action",
      "type" : "string",
      "minLength" : 1
    },
    "tag" : {
      "title" : "tag",
      "id" : "etherscan_tag",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// the response schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this could be used to parse the result
const responseSchema = {
  "title" : "etherscanResponse",
  "id" : "etherscanResponse",
  "default" : "Schema definition for etherscan",
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

class etherscan extends EndpointInterface {
	constructor() {
		// name and http method, these are inserted when code is generated
		super("etherscan", "GET");
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

export default new etherscan();