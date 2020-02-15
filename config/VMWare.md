## WorkstationONE Mutual TLS configuration:

Overview:

1. Create accounts in Azure, Office 365, and Intune
2. Configure a Certificate Authority
3. Configure Keystore on your application server
4. Quick test that mutual TLS is configured
5. Setup ForgeRock Open Access Manager oauth

### Microsoft Intune

Follow the steps in the Microsoft EMM/MDM configuration at <https://docs.microsoft.com/en-us/intune/fundamentals/free-trial-sign-up> which will walk you through the required Office 365 and Azure registration; after you complete those steps in those systems, all of the remaining work will be done in Intune.

Perform a test by installing the Apple MDM Push components <https://www.youtube.com/watch?v=mqNyYZEDY5Q>; deploy to an iOS device to ensure the above is configured properly. 

### Certificate Authority

You can choose from almost a dozen different SCEP providers (<https://docs.microsoft.com/en-us/intune/protect/certificate-authority-add-scep-overview>) are supported in Intune but for simplicity this document uses SCEPman.

Add from the Azure marketplace the free community edition of SCEPman:
<https://docs.microsoft.com/en-us/intune/protect/certificate-authority-add-scep-overview>


Video of this running is at <https://devicemanagement.microsoft.com/#blade/Microsoft_Intune_DeviceSettings/DevicesMenu/configurationProfiles>


### ForgeRock Open Access Manager Tomcat Configuration:

Update the server.xml configuration to the following:

<Connector port="8443" protocol="HTTP/1.1" SSLEnabled=“true" maxThreads="150" scheme="https" secure=“true" ciphers="ALL”     
clientAuth="true" 
sslProtocol="TLS" 

truststoreFile="certs/trust.keystore” truststorePass=“password” 
KeystoreFile="certs/ca.keystore" KeystorePass=“password"
 />


### Keytool Config used by your app server (ie, tomcat)

Earlier you configured SCEPman, so from the webserver at scepman-(your random string).azurewebsites.net download the root CA:

keytool -importcert -alias ca_mng_cert -file ManagementCA.pem -keystore trust.keystore -storepass password -noprompt . Todo: rename to pkiclient.exe.cer since pem above is from ejbca not scepman (or any other SCEP providers listed in the MS docs above)

keytool -importcert -alias ca_mng_cert_chain -file ManagementCA-chain.pem -keystore trust.keystore -storepass password -noprompt 

Restart the app server.


### Verification

Navigate to your https://forgerock_am_instance
If your browser requests your client certificate, and after presenting it you can see the tomcat landing page, congratulate yourself as the above is correctly configured.


### AM Configuration:

Create a new OpenID Connect service in the AM dashboard with all default settings. Then create a new OAuth2 client (ie, 'test').

Set the client secret to empty, set the redirect URI to https://example-app.com/redirect and set the scope and default scopes to “openid profile”.

When the client is created, go to the advanced tab of the client and set the Grant Types to Authorization Code and Client Credentials. 

Also set the Token Endpoint Authentication Method to self_signed_tls_client_auth.

Under 'Signing and Encryption', mTLS Self-Signed Certificate is set to the client certificate value; check Use Certificate-Bound Access Tokens, and also set Public key selector to X509. 

After that, you can use a URL like this to try and authenticate to AM with your client certificate and login to the Default login chain:

https://(your_instance):8443/openam/oauth2/realms/root/authorize?audience=test&scope=openid%20profile&response_type=code&client_id=test&redirect_uri=https://example-app.com/redirect&state=123456

That should ask you to present your client cert, then ask you to enter your username and password, then ask you for consent to share your data and redirect you to https://example-app.com/redirect (an example application) with a code in the query parameters that you could exchange for an access_token (certificate bound) and id_token that contains the claims on the end user.

If you’d like to take a look at what those two tokens would look like if you exchanged that code in the query parameters for access tokens, you can run the following curl command:

curl --request POST \
--data "client_id=test" \
--data "grant_type=client_credentials" \
--data "scope=profile" \
--data "response_type=token" \
--cert client.pem \
--key client.key.pem \
https://(your_instance):8443/openam/oauth2/realms/root/access_token

Where client.pem is your public client certificate and client.key.pem is the private key for that client.	
