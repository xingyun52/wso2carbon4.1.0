package org.wso2.carbon.adc.mgt.cli;

import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

/**
 *
 */
public class HelpPrinter {
    
    

    /**
    * Print usage information to provided OutputStream.
    *
    */
    public void printUsage(String action) {
        String loginAction = "    \n    Usage for login:" +
                             "    \n       sh stratos.sh [username] [password] -keystore <path_to_keystore>" +
                             "    \n          -keystore : Default key store for Stratos" +
                             "    \n" ;
        String listAction =  "    \n    Usage for action list:" +
                             "    \n        list" ;
        String subAction =   "    \n    Usage for action subscribe:" +
                             "    \n        subscribe [cartridge type] [cartridge alias] -min <min instances> -max <max instances> -repoURL  <Git repository url> " +
                             "    \n            -min : minimum number of instances. Default is 1" +
                             "    \n            -max : maximum number of instances. Default is 1" +
        					 "	  \n            -connect : Connects one cartridge to another. This need to use with -alias" +
        					 "	  \n            -alias : data cartridge alias" +
        					 " 	  \n                eg: subscribe [cartridge type] [cartridge alias] -connect [data cartridge type] -alias [data cartridge alias] -repoURL <Git repository url>  " +
"    \n            BTW. Aliases cannot include Uppercase letters "

;
        String unsubAction = "   \n    Usage for action unsubscribe:" +
                             "    \n        unsubscribe [cartridge alias] ";
        String infoAction = "    \n    Usage for action info:" +
                            "    \n        info [cartridge alias] -v" +
                            "    \n        		-v : verbose" +
                            "    \n" ;
        String dmAction =   "    \n    Usage for action domain-mapping:" +
                            "    \n        domain-mapping [cartridge alias] [mapped domain]" +
                            "    \n" ;
        String helpAction = "    \n    Usage for action help:" +
                            "    \n        help [action]" +
                            "    \n" ;
        //TODO unsubscribe help
        if(action == null){
            System.out.println("\nUsage: stratos> [action] <mandatory argument>* <option identifier>* <optional " +
                    "argument>*\n" +
                    "    \n    Action can be one of the following," +
                    "    \n        list : List the available cartridges with details." +
                    "    \n        subscribe : Subscribe to a new cartridge." +
                    "    \n        info: Print a detailed description about a cartridge. " +
                    "    \n        domain-mapping : Give a domain mapping to a subscribed cartridge." +
                    "    \n        help : Print general help and help for different actions." +
                    "    \n" +
                    "    \n    Mandatory and optional arguments depend on the action. Following is the list of usages " +
                    "for each action, " +
                    "    \n" +
                    listAction +
                    "    \n" +
                    subAction +
                    "    \n" +
                    unsubAction +
                    "    \n" +
                    infoAction +
                    "    \n" +
                    dmAction +
                    "    \n" +
                    helpAction +
                    "\n");
        } else {
            if(action.equalsIgnoreCase(CliConstants.LOG_IN)){
                System.out.println(loginAction);
            }
            else if(action.equalsIgnoreCase(CliConstants.LIST)){
                System.out.println(listAction);
            }
            else if(action.equalsIgnoreCase(CliConstants.SUBSCRIBE) || action.equalsIgnoreCase(CliConstants.SUB)){
                System.out.println(subAction);
            }
            else if(action.equalsIgnoreCase(CliConstants.INFO)){
                System.out.println(infoAction);
            }
            else if(action.equalsIgnoreCase(CliConstants.UNSUBSCRIBE)){
                System.out.println(unsubAction);
            }
            else if(action.equalsIgnoreCase(CliConstants.DOMAIN_MAPPING) || action.equalsIgnoreCase(CliConstants.DM)){
                System.out.println(dmAction);
            } else {
                printUsage(null);
            }
        }
    }
}
