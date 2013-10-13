/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.cli;

import java.io.*;

import org.apache.commons.cli.*;
import org.wso2.carbon.adc.mgt.cli.parsers.ArrayParser;
import org.wso2.carbon.adc.mgt.cli.parsers.LineParser;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;
import org.wso2.carbon.adc.mgt.cli.utils.CliMessages;

import javax.net.ssl.*;

/**
 * This class is used for input the commands through CLITool, command prompt.
 */

public class CliTool
{
   /**
    * Main executable method used to call from CLI.
    *
    */
   public static void main(final String[] args) {
       CliTool cliTool = new CliTool();
       cliTool.handleConsoleInputs(args);
   }

    /**
     * Here is the place all the command line inputs get processed first time.Selecting parsers according to the mode of
     * input and managing keystore presence are main tasks.m
     * @param arguments passed from CLI tool.
     */
   public void handleConsoleInputs(String[] arguments){
       //Create options to handle key store
       Options options = new Options();
       options.addOption("k", CliConstants.KEY_STORE, true, "Key Store");
       final CommandLineParser cmdLinePosixParser = new PosixParser();

       BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
       try {
           CommandLine commandLine = cmdLinePosixParser.parse(options, arguments);
           if(arguments.length > 1){
               //log-in command must have more than 1 arguments
               String userName = arguments[0];
               String passWord = arguments[1];
               String keyStore = null;
               if(commandLine.hasOption(CliConstants.KEY_STORE)){
                   keyStore = commandLine.getOptionValue(CliConstants.KEY_STORE);
               }
               String command = "";
               if(arguments.length == 2 || arguments.length > 2 && (arguments[2].equalsIgnoreCase("-k")
                       || arguments[2].equalsIgnoreCase(CliConstants.KEY_STORE))){
                   //here we have to provide stratos> shell in following occasions
                   //if the user has not given a third input or
                   //if the user has given the key-store as third input
                   LineParser lineParser = new LineParser();
                   if(!lineParser.authenticate(userName, passWord, keyStore, true)){
                       return;
                   }
                   System.out.println(CliMessages.SUCCESSFULLY_AUTHENTICATED);
                   while (! command.equalsIgnoreCase("exit")){
                       System.out.print(CliConstants.STRATOS_SHELL);
                       try {
                           command = br.readLine();
                           if(!command.trim().equalsIgnoreCase("")){
                               if(!command.equalsIgnoreCase("exit")){
                                   lineParser.parse(command.trim());
                               }
                           }
                       } catch (IOException ioe) {
                           lineParser.printMessage("IO error trying to read command!");
                            System.exit(1);
                       }
                   }
                   System.out.println("Exiting from Stratos CLI tool");
               } else {
                   //here the user is in single call mode, so use the array parser
                   ArrayParser arrayParser = new ArrayParser();
                   if(!arrayParser.authenticate(userName, passWord, keyStore, false)){
                       return;
                   }
                   arrayParser.parse(arguments);
               }
           } else {
               HelpPrinter helpPrinter = new HelpPrinter();
               helpPrinter.printUsage(CliConstants.LOG_IN);
           }
       } catch (ParseException e) {
           System.out.println("Please provide valid arguments for log in");
       }

   }

}
