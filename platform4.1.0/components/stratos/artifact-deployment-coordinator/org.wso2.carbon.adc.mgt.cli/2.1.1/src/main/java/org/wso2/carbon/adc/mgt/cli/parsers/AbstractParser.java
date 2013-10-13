package org.wso2.carbon.adc.mgt.cli.parsers;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.wso2.carbon.adc.mgt.cli.ParsedCliCommandManager;
import org.wso2.carbon.adc.mgt.cli.HelpPrinter;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public abstract class AbstractParser<T> {

    static final String[] ACTIONS = {
            CliConstants.SUBSCRIBE,
            CliConstants.SUB,
            CliConstants.LIST,
            CliConstants.DOMAIN_MAPPING,
            CliConstants.DM,
            CliConstants.HELP,
            CliConstants.INFO,
            CliConstants.UNSUBSCRIBE};

    final ParsedCliCommandManager cliCommandManager = new ParsedCliCommandManager();
    final CommandLineParser cmdLinePosixParser = new PosixParser();
    final Options options = constructOptions();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    HelpPrinter helpPrinter =  new HelpPrinter();
    /**
    * Construct and provide Options.
    *
    * @return Options expected from command-line.
    */
    public Options constructOptions()
    {
        Options posixOptions = new Options();

        //subscribe options
        posixOptions.addOption("n", CliConstants.MIN, true, "Minimum number of instances of the cartridge");
        posixOptions.addOption("x", CliConstants.MAX, true, "Maximum number of instances of the cartridge");
        posixOptions.addOption("r", CliConstants.REPO_URL, true, "External repo url. Default is null");
        posixOptions.addOption("u", CliConstants.REPO_USERNAME, true, "Username of the external repository.");
        posixOptions.addOption("p", CliConstants.REPO_PASSWORD, true, "Password of the external repository.");
        posixOptions.addOption("c", CliConstants.CONNECT, true, "Connects a data cartridge");
        posixOptions.addOption("a", CliConstants.ALIAS, true, "Alias");
        
        // 
        posixOptions.addOption("v", CliConstants.VERBOSE, false, "Verbose mode");

     return posixOptions;
    }

    public abstract void parse(T args);

    abstract void unSubscribe(T args, String action);

    abstract void info(T args, String action);

    abstract void addDomainMapping(T args, String action);

    abstract void subscribe(T args, String action);

    boolean isAlphaNumericWithHyphen(String input){
        String patternString = "([a-zA-Z0-9]+([-][a-zA-Z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);
        return pattern.matcher(input).matches();
    }

    public void printMessage(String message){
        System.out.println(message);
    }


    public boolean authenticate(String user, String passWord, String keyStorePath, boolean validateAuth) {
        if(keyStorePath != null ){
           //user has given a keystore path
           boolean wrongAnswer = false;
           do{
               System.out.println("Do you trust the connection to Stratos without a certificate? (y/n)");
               try {
                   String answer = br.readLine();
                   if("y".equalsIgnoreCase(answer) || "yes".equalsIgnoreCase(answer) ){
                       System.out.println("Continue to connect without a certificate!");
                   } else if("n".equalsIgnoreCase(answer) || "no".equalsIgnoreCase(answer)){
                       return false;
                   } else {
                       wrongAnswer = true;
                   }
               } catch (IOException e) {
                   System.out.println("Error while trying to read command!");
                   System.exit(1);
               }
           } while (wrongAnswer);
            System.setProperty("javax.net.ssl.trustStore", keyStorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        } else {
            try{
                //Following code will avoid validating certificate
                SSLContext sc;
                // Get SSL context
                sc = SSLContext.getInstance("SSL");
                // Create empty HostnameVerifier
                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String urlHostName, SSLSession session) {
                        return true;
                    }
                };
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                            String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                            String authType) {
                    }
                } };
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                 SSLContext.setDefault(sc);
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            } catch (Exception e) {
                System.out.println("Error while authentication process!");
            }
        }
        String stratosHost = System.getenv("STRATOS_ADC_HOST");
        if(stratosHost == null){
            System.out.println("Stratos host is not exported!");
            return false;
        }
        return cliCommandManager.loggingToRemoteServer(stratosHost.trim(),
                System.getenv("STRATOS_ADC_PORT"), user, passWord, validateAuth);
    }


    public String getStringInputFor(String printThis){
        System.out.print(printThis);
        try {
            return br.readLine();
        }
        catch(IOException e) {
            System.out.println("Error while recording your input!");
            return getStringInputFor(printThis);
        }
    }

}
