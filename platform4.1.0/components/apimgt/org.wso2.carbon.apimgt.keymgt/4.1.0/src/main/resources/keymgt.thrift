namespace java org.wso2.carbon.apimgt.keymgt.service.thrift

exception APIKeyMgtException {
    1: required string message
}

exception APIManagementException {
    1: required string message
}

struct APIKeyValidationInfoDTO {
    1: optional bool authorized;
    2: optional string subscriber;
    3: optional string tier;
    4: optional string type;
    5: optional string endUserToken;
    6: optional string endUserName;
    7: optional string applicationName;
    8: optional i32 validationStatus;
    9: optional string applicationId;
    10: optional string applicationTier;
}

service APIKeyValidationService {
APIKeyValidationInfoDTO validateKey(1:required string context, 2:required string version, 3:required string accessToken, 4:required string sessionId, 5:required string requiredAuthenticationLevel) throws (1:APIKeyMgtException apiKeyMgtException, 2:APIManagementException apiMgtException)
}