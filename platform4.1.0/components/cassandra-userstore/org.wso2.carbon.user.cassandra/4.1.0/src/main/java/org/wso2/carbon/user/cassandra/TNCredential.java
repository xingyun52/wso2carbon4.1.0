package org.wso2.carbon.user.cassandra;

public class TNCredential {

	private String credentialType;
	private String deviceID;
	private String extUsername;
	private String extProvider;
	private String extAccessToken;
	private String extEmailAddress;
	private String phoneNumber;
	private String email;
	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCredentialType() {
		return credentialType;
	}

	public void setCredentialType(String credentialType) {
		this.credentialType = credentialType;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public String getExtEmailAddress() {
		return extEmailAddress;
	}

	public void setExtEmailAddress(String extEmailAddress) {
		this.extEmailAddress = extEmailAddress;
	}

	public String getExtAccessToken() {
		return extAccessToken;
	}

	public void setExtAccessToken(String accessToken) {
		this.extAccessToken = accessToken;
	}

	public String getExtProvider() {
		return extProvider;
	}

	public void setExtProvider(String extProvider) {
		this.extProvider = extProvider;
	}

	public String getExtUsername() {
		return extUsername;
	}

	public void setExtUsername(String extUsername) {
		this.extUsername = extUsername;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
	    return email;
    }

	public void setEmail(String email) {
	    this.email = email;
    }

}
