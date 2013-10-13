package org.wso2.carbon.adc.mgt.cli.parsers;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

import java.util.Arrays;

/**
 * user can call the cli tool without using the stratos> prompt
 */
public class ArrayParser extends AbstractParser<String[]>{    
    CommandLine commandLine;
    
    @Override
    public void parse(String[] args) {
        String action = args[2];
        if(Arrays.asList(ACTIONS).contains(action) ){
            if(action.equalsIgnoreCase(CliConstants.LIST)){
                cliCommandManager.listTypes();
            } else if(action.equalsIgnoreCase(CliConstants.DOMAIN_MAPPING) || action.equalsIgnoreCase(CliConstants.DM)){
                addDomainMapping(args, action);
            } else if(action.equalsIgnoreCase(CliConstants.SUBSCRIBE) || action.equalsIgnoreCase(CliConstants.SUB)){
                subscribe(args, action );
            } else if(action.equalsIgnoreCase(CliConstants.UNSUBSCRIBE)){
                unSubscribe(args, action);
            } else if (action.equalsIgnoreCase(CliConstants.INFO)) {
            	info(args, action);
            } else if (action.equalsIgnoreCase(CliConstants.HELP)) {
                if(args.length == 4){
                    helpPrinter.printUsage(args[3]);
                } else {
                    helpPrinter.printUsage(null);
                }
            }
        }else {
            //Sending null as input for printing general help
            helpPrinter.printUsage(null);
        }
    }

    @Override
    void unSubscribe(String[] inputCommandArray, String action) {

        if(inputCommandArray.length > 3){
            System.out.println(inputCommandArray.length);
            String alias = inputCommandArray[3];
            System.out.println("Unsubscribing the cartridge " + alias);
            cliCommandManager.unsubscribe(alias);
        } else {
            helpPrinter.printUsage(CliConstants.UNSUBSCRIBE);
        }
    }

    @Override
    void info(String[] inputCommandArray, String action) {
        if(inputCommandArray.length > 2){
        	String verbose = null;
        	try {
	            commandLine = cmdLinePosixParser.parse(options, inputCommandArray);
            } catch (ParseException parseException) {
                helpPrinter.printUsage(action);
            }
        	if(commandLine.hasOption(CliConstants.VERBOSE)){
        		verbose = CliConstants.VERBOSE;
        	}
            String alias = inputCommandArray[2];
            cliCommandManager.listCartridgeInfo(alias,verbose);
        } else {
            helpPrinter.printUsage(CliConstants.INFO);
        }
    }



    @Override
     void addDomainMapping(String[] inputCommandArray, String action) {
        String cartridgeName = null;
        if(inputCommandArray.length > 4){
            cartridgeName = inputCommandArray[3];
            String mappedDomain = inputCommandArray[4];
            String domainToDisplay;

            if(mappedDomain.indexOf('.') == -1) {
                printMessage("\nOwn domain should include a '.' ");
                return;
            }

            domainToDisplay = cliCommandManager.addDomainMapping(mappedDomain, cartridgeName);

            if(domainToDisplay == null){
                printMessage("Selected domain mapping is already exist or backend failure. \nTry again...");
            } else {
                printMessage("\nYour own domain is added. Please CNAME it to systems domain " + domainToDisplay + ".");
            }
        } else {
            printMessage("Command is wrong for " + action);
            helpPrinter.printUsage(action);
        }

    }

    @Override
    void subscribe(String[] inputCommandArray, String action) {
        try{
            commandLine = cmdLinePosixParser.parse(options, inputCommandArray);
            if(inputCommandArray.length > 3){
                String cartridge = inputCommandArray[2];
                String name = inputCommandArray[3];
                if(!isAlphaNumericWithHyphen(name)){
                    System.out.println("Cartridge alias can contain alphanumeric characters and hyphens only\n");
                    helpPrinter.printUsage(action);
                    return;
                }
                String  min = null, max = null, repoURL = null , mySQLParams = null, userName="", password="";
                //to get optional values
                if(commandLine.hasOption("min") && !commandLine.getOptionValue("min").equalsIgnoreCase("")){
                    if(commandLine.hasOption("max")&& !commandLine.getOptionValue("max").equalsIgnoreCase("")){
                        //assign max
                        max = commandLine.getOptionValue("max");
                        //assign min
                        min = commandLine.getOptionValue("min");
                    }
                }
                if(commandLine.hasOption(CliConstants.REPO_URL) && !commandLine.getOptionValue(CliConstants.REPO_URL)
                        .equalsIgnoreCase("")){
                    repoURL = commandLine.getOptionValue(CliConstants.REPO_URL);
                }
                if(commandLine.hasOption(CliConstants.REPO_USERNAME) && !commandLine.getOptionValue(CliConstants.REPO_USERNAME)
                        .equalsIgnoreCase("")){
                    userName = commandLine.getOptionValue(CliConstants.REPO_USERNAME);
                }
                if(commandLine.hasOption(CliConstants.REPO_PASSWORD) && !commandLine.getOptionValue(CliConstants.REPO_PASSWORD)
                        .equalsIgnoreCase("")){
                    password = commandLine.getOptionValue(CliConstants.REPO_PASSWORD);
                }
                if(commandLine.hasOption(CliConstants.CONNECT) && !commandLine.getOptionValue(CliConstants.CONNECT)
                        .equalsIgnoreCase("")){
                	mySQLParams = commandLine.getOptionValue(CliConstants.CONNECT);
                }

                // mysql*mysqlAlias

				String mysqlcartridgeType = null;
				String mysqlcartridgeAlias = null;

				if (mySQLParams != null) {
					String[] mysqlParamsArray = mySQLParams.split("-");
					mysqlcartridgeType = mysqlParamsArray[0];
					mysqlcartridgeAlias = mysqlParamsArray[1];

					System.out.println(" Subscribing to data cartridge : " + mysqlcartridgeType + " with Alias " +  mysqlcartridgeAlias);
					cliCommandManager.subscribe(mysqlcartridgeType.toLowerCase(), mysqlcartridgeAlias, min, max, repoURL , userName, password, mysqlcartridgeType, mysqlcartridgeAlias);
					System.out.println(" Subscribing to " + name + " cartridge and connecting with " + mysqlcartridgeAlias + " data cartridge");
				}

                cliCommandManager.subscribe(cartridge.toLowerCase(), name, min, max, repoURL, userName, password, mysqlcartridgeType, mysqlcartridgeAlias);
            } else {
                printMessage("Command is wrong for " + action);
                helpPrinter.printUsage(action);
            }

        }
        catch (ParseException parseException) {
            helpPrinter.printUsage(action);
       }
    }


}
