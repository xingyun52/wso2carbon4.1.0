package org.wso2.carbon.adc.mgt.cli.parsers;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

import java.util.Arrays;

/**
 * This class parses commands from shell.
 * This includes a parse
 * from apache to parse the arguments input.
 */
public class LineParser extends AbstractParser<String>{

    CommandLine commandLine;
    String[] commandLineArguments;


    /**
    * Processes command-line arguments.
    *
    * @param commandLineInput Command-line arguments to be processed with parse.
    */
    @Override
    public void parse(String commandLineInput) {
        String line = commandLineInput;
        String action = getFirstWord(line);
        line = removeFirstWord(line);
        if(Arrays.asList(ACTIONS).contains(action) ){
            if(action.equalsIgnoreCase(CliConstants.LIST)){
                cliCommandManager.listTypes();
            } else if(action.equalsIgnoreCase(CliConstants.DOMAIN_MAPPING) || action.equalsIgnoreCase(CliConstants.DM)){
                addDomainMapping(line, action);
            } else if(action.equalsIgnoreCase(CliConstants.SUBSCRIBE) || action.equalsIgnoreCase(CliConstants.SUB)){
                subscribe(line, action );
            } else if(action.equalsIgnoreCase(CliConstants.UNSUBSCRIBE)){
                unSubscribe(line, action);
            } else if (action.equalsIgnoreCase(CliConstants.INFO)) {
            	info(line, action);
            } else if (action.equalsIgnoreCase(CliConstants.HELP)) {
                String helpAction = getFirstWord(line);
                line = removeFirstWord(line);
                if("".equalsIgnoreCase(line) && Arrays.asList(ACTIONS).contains(helpAction)){
                    helpPrinter.printUsage(helpAction);
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
    protected void unSubscribe(String line, String action) {

    	String alias = getFirstWord(line);
        System.out.println("Unsubscribing the cartridge " + alias);
        if(!alias.equalsIgnoreCase("")){
            cliCommandManager.unsubscribe(alias);
        } else {
            helpPrinter.printUsage(CliConstants.UNSUBSCRIBE);
        }
    }

    @Override
	void info(String line, String action) {

		String alias = getFirstWord(line);
		line = removeFirstWord(line);
		String verbose = null;

		if (!alias.equalsIgnoreCase("")) {
			try {
				commandLineArguments = line.split("\\s+");
				commandLine = cmdLinePosixParser.parse(options, commandLineArguments);

				if (commandLine.hasOption(CliConstants.VERBOSE)) {
					verbose = CliConstants.VERBOSE;
				}
				cliCommandManager.listCartridgeInfo(alias, verbose);
			} catch (ParseException parseException) {
				printMessage("Command is wrong for " + action);
				helpPrinter.printUsage(action);
			}
		} else {
			printMessage("Command is wrong for " + action);
			helpPrinter.printUsage(action);
		}

	}


    @Override
     void addDomainMapping(String line, String action) {
        String cartridgeName = getFirstWord(line);

        line = removeFirstWord(line);
        if(!"".equalsIgnoreCase(cartridgeName)){
            String mappedDomain = getFirstWord(line);
            line = removeFirstWord(line);
            String domainToDisplay = null;

            if(mappedDomain.indexOf('.') == -1) {
                printMessage("\nOwn domain should include a '.' ");
                return;
            }
            domainToDisplay = cliCommandManager.addDomainMapping(mappedDomain, cartridgeName);
            if(domainToDisplay == null){
                printMessage("Selected domain mapping is already exist or backend failure... " +
                        "\nTry again,");
            }else {
                printMessage("\nYour own domain is added. Please CNAME it to systems domain " + domainToDisplay + ".");
            }
        } else {
            printMessage("Command is wrong for " + action);
            helpPrinter.printUsage(action);
        }

    }

    private String getFirstWord(String line) {
        return (line.indexOf(' ') != -1) ? line.substring(0, line.indexOf(' ')).trim() : line.trim();
    }

    private String removeFirstWord(String line) {
        return (line.indexOf(' ') != -1) ? line.substring(line.indexOf(' ') + 1).trim() : "";

    }

    @Override
     void subscribe(String line, String action) {
        String cartridge = getFirstWord(line);
        line = removeFirstWord(line);
        String name = getFirstWord(line);
        line = removeFirstWord(line);
        if(!cartridge.equalsIgnoreCase("") && !name.equalsIgnoreCase("")){
            if(!isAlphaNumericWithHyphen(name)){
                System.out.println("Cartridge alias can contain alphanumeric characters and hyphens only");
                return;
            }
            try{
                commandLineArguments = line.split("\\s+");//split the arguments on several whitespaces
                commandLine = cmdLinePosixParser.parse(options, commandLineArguments);
                String  min = null, max = null, repoURL = null , dataCartridge = null, alias = null, userName = "", password = "";
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

                    if(commandLine.hasOption(CliConstants.ALIAS) && !commandLine.getOptionValue(CliConstants.ALIAS)
                            .equalsIgnoreCase("")){

                        alias = commandLine.getOptionValue(CliConstants.ALIAS);
                        dataCartridge = commandLine.getOptionValue(CliConstants.CONNECT);
                        System.out.println(" Subscribing to data cartridge : " + dataCartridge + " with Alias " +  alias);
                        cliCommandManager.subscribe(dataCartridge.toLowerCase(), alias, min, max, null,"","", null, null);
                        System.out.println(" Subscribing to " + name + " cartridge and connecting with " + alias + " data cartridge");
                        cliCommandManager.subscribe(cartridge, name, min, max, repoURL ,userName,password,dataCartridge, alias);
                        return;
                    } else {
                        printMessage("Command is wrong for " + action);
                        helpPrinter.printUsage(action);
                        return;
                    }
                }

                cliCommandManager.subscribe(cartridge, name, min, max, repoURL ,userName,password,dataCartridge, alias);
            }
            catch (ParseException parseException) {
                helpPrinter.printUsage(action);
            }

        } else {
            printMessage("Command is wrong for " + action);
            helpPrinter.printUsage(action);
        }
    }






}
