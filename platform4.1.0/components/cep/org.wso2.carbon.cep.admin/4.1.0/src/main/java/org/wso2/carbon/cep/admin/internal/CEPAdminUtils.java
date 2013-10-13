/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cep.admin.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.admin.internal.exception.CEPAdminException;
import org.wso2.carbon.cep.core.Expression;
import org.wso2.carbon.cep.core.Query;
import org.wso2.carbon.cep.core.XpathDefinition;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.*;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;
import org.wso2.carbon.cep.core.mapping.output.Output;
import org.wso2.carbon.cep.core.mapping.output.mapping.MapOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.TextOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.TupleOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.XMLOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.property.MapOutputProperty;
import org.wso2.carbon.cep.core.mapping.output.property.TupleOutputProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CEPAdminUtils {
    private static final Log log = LogFactory.getLog(CEPAdminUtils.class);

    /**
     * This method will map CEP Admin module InputDTO Topic to CEP Core module InputDTO
     *
     * @param inputDTO - CEP Admin module inputs
     * @return InputTopic  - CEP Core module InputDTO
     * @throws org.wso2.carbon.cep.admin.internal.exception.CEPAdminException
     *
     */
    public static Input adaptInput(InputDTO inputDTO) throws CEPAdminException {

        Input backendInput = new Input();

        backendInput.setTopic(inputDTO.getTopic());
        backendInput.setBrokerName(inputDTO.getBrokerName());

        if (inputDTO.getInputXMLMappingDTO() != null) {
            InputXMLMappingDTO inputXMLMappingDTO = inputDTO.getInputXMLMappingDTO();
            backendInput.setInputMapping(adaptInputMapping(inputXMLMappingDTO));
        } else if (inputDTO.getInputTupleMappingDTO() != null) {
            InputTupleMappingDTO inputTupleMappingDTO = inputDTO.getInputTupleMappingDTO();
            backendInput.setInputMapping(adaptInputMapping(inputTupleMappingDTO));
        } else if (inputDTO.getInputMapMappingDTO() != null) {
            InputMapMappingDTO inputMapMappingDTO = inputDTO.getInputMapMappingDTO();
            backendInput.setInputMapping(adaptInputMapping(inputMapMappingDTO));
        }


        return backendInput;
    }


    /**
     * This method will adapt CEP Admin module InputXMLMappingDTO to CEP Core module InputXMLMappingDTO
     *
     * @param inputXMLMappingDTO - CEP Admin module inputXMLMappingDTO
     * @return Mapping - CEP Core module InputXMLMapping
     * @throws org.wso2.carbon.cep.admin.internal.exception.CEPAdminException
     *
     */

    private static InputMapping adaptInputMapping(InputXMLMappingDTO inputXMLMappingDTO)
            throws CEPAdminException {
        XMLInputMapping backendInputMapping = new XMLInputMapping();
        InputXMLPropertyDTO[] InputXMLPropertyDTOs = inputXMLMappingDTO.getProperties();
        List<XMLInputProperty> backendInputPropertyList = new ArrayList<XMLInputProperty>();

        if (InputXMLPropertyDTOs != null) {
            for (InputXMLPropertyDTO inputInputXMLPropertyDTO : InputXMLPropertyDTOs) {
                XMLInputProperty backendInputProperty = new XMLInputProperty();
                backendInputProperty.setName(inputInputXMLPropertyDTO.getName());
                backendInputProperty.setXpath(inputInputXMLPropertyDTO.getXpath());
                backendInputProperty.setType(inputInputXMLPropertyDTO.getType());
                backendInputPropertyList.add(backendInputProperty);
            }
        }

        XpathDefinitionDTO[] xpathDefinitionDTOs = inputXMLMappingDTO.getXpathDefinition();
        if (xpathDefinitionDTOs != null) {
            for (XpathDefinitionDTO xpathDefinitionDTO : xpathDefinitionDTOs) {
                XpathDefinition backEndXpathDefinition = new XpathDefinition();
                backEndXpathDefinition.setNamespace(xpathDefinitionDTO.getNamespace());
                backEndXpathDefinition.setPrefix(xpathDefinitionDTO.getPrefix());
                backendInputMapping.addXpathDefinition(backEndXpathDefinition);
            }
        }

        backendInputMapping.setStream(inputXMLMappingDTO.getStream());
        Class mappingClass;
        try {
            mappingClass = QueryEventTypeConvertor.mappingClassFromType(inputXMLMappingDTO.getQueryEventType());
        } catch (CEPConfigurationException e) {
            String errorMessage = "No class found matching " + inputXMLMappingDTO.getQueryEventType();
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
        backendInputMapping.setMappingClass(mappingClass);
        backendInputMapping.setProperties(backendInputPropertyList);


        return backendInputMapping;

    }

    /**
     * This method will adapt CEP Admin module InputTupleMappingDTO to CEP Core module InputTupleMappingDTO
     *
     * @param inputTupleMappingDTO - CEP Admin module inputTupleMappingDTO
     * @return Mapping - CEP Core module InputTupleMapping
     * @throws org.wso2.carbon.cep.admin.internal.exception.CEPAdminException
     *
     */
    private static InputMapping adaptInputMapping(InputTupleMappingDTO inputTupleMappingDTO)
            throws CEPAdminException {
        TupleInputMapping backendInputMapping = new TupleInputMapping();
        InputTuplePropertyDTO[] inputTuplePropertyDTOs = inputTupleMappingDTO.getProperties();
        List<TupleInputProperty> backendInputPropertyList = new ArrayList<TupleInputProperty>();

        if (inputTuplePropertyDTOs != null) {
            for (InputTuplePropertyDTO inputTuplePropertyDTO : inputTuplePropertyDTOs) {
                TupleInputProperty backendInputProperty = new TupleInputProperty();
                backendInputProperty.setName(inputTuplePropertyDTO.getName());
                backendInputProperty.setInputName(inputTuplePropertyDTO.getInputName());
                backendInputProperty.setType(inputTuplePropertyDTO.getType());
                backendInputProperty.setInputDataType(inputTuplePropertyDTO.getInputDataType());
                backendInputPropertyList.add(backendInputProperty);
            }
        }

        backendInputMapping.setStream(inputTupleMappingDTO.getStream());
        Class mappingClass;
        try {
            mappingClass = QueryEventTypeConvertor.mappingClassFromType(inputTupleMappingDTO.getQueryEventType());
        } catch (CEPConfigurationException e) {
            String errorMessage = "No class found matching " + inputTupleMappingDTO.getQueryEventType();
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
        backendInputMapping.setMappingClass(mappingClass);
        backendInputMapping.setProperties(backendInputPropertyList);

        return backendInputMapping;

    }

    /**
     * This method will adapt CEP Admin module InputMapMappingDTO to CEP Core module InputTupleMappingDTO
     *
     * @param inputMapMappingDTO - CEP Admin module inputMapMappingDTO
     * @return Mapping - CEP Core module InputTupleMapping
     * @throws org.wso2.carbon.cep.admin.internal.exception.CEPAdminException
     *
     */
    private static InputMapping adaptInputMapping(InputMapMappingDTO inputMapMappingDTO)
            throws CEPAdminException {
        MapInputMapping backendInputMapping = new MapInputMapping();
        InputMapPropertyDTO[] inputMapPropertyDTOs = inputMapMappingDTO.getProperties();
        List<MapInputProperty> backendInputPropertyList = new ArrayList<MapInputProperty>();

        if (inputMapPropertyDTOs != null) {
            for (InputMapPropertyDTO inputMapPropertyDTO : inputMapPropertyDTOs) {
                MapInputProperty backendInputProperty = new MapInputProperty();
                backendInputProperty.setName(inputMapPropertyDTO.getName());
                backendInputProperty.setInputName(inputMapPropertyDTO.getInputName());
                backendInputProperty.setType(inputMapPropertyDTO.getType());
                backendInputPropertyList.add(backendInputProperty);
            }
        }

        backendInputMapping.setStream(inputMapMappingDTO.getStream());
        Class mappingClass;
        try {
            mappingClass = QueryEventTypeConvertor.mappingClassFromType(inputMapMappingDTO.getQueryEventType());
        } catch (CEPConfigurationException e) {
            String errorMessage = "No class found matching " + inputMapMappingDTO.getQueryEventType();
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
        backendInputMapping.setMappingClass(mappingClass);
        backendInputMapping.setProperties(backendInputPropertyList);

        return backendInputMapping;

    }

    /**
     * This method will map CEP Admin module QueryDTO to CEP Core module QueryDTO
     *
     * @param queryDTO - CEP Admin module queryDTO
     * @return Query - CEP Core module query
     */
    static Query adaptQuery(QueryDTO queryDTO) {

        Query backEndQuery = new Query();
        Expression backEndExpression = new Expression();

        backEndExpression.setText(queryDTO.getExpression().getText());

        backEndQuery.setName(queryDTO.getName());
        backEndQuery.setQueryIndex(queryDTO.getQueryIndex());
        backEndQuery.setExpression(backEndExpression);

        if (queryDTO.getOutput() != null) {
            Output backEndOutput = new Output();

            OutputDTO outputDTO = queryDTO.getOutput();
            backEndOutput.setTopic(outputDTO.getTopic());
            backEndOutput.setBrokerName(outputDTO.getBrokerName());


            OutputXMLMappingDTO outputXmlMappingDTO = outputDTO.getOutputXmlMapping();
            if (outputXmlMappingDTO != null) {

                XMLOutputMapping backEndXMLOutputMapping = null;
                if (outputXmlMappingDTO.getMappingXMLText() != null && !outputXmlMappingDTO.getMappingXMLText().equals("")) {
                    backEndXMLOutputMapping = new XMLOutputMapping();
                    backEndXMLOutputMapping.setMappingXMLText(outputXmlMappingDTO.getMappingXMLText());
                }
                backEndOutput.setOutputMapping(backEndXMLOutputMapping);
            }

            OutputTextMappingDTO outputTextMappingDTO = outputDTO.getOutputTextMapping();
            if (outputTextMappingDTO != null) {

                TextOutputMapping backEndTextOutputMapping = null;
                if (outputTextMappingDTO.getMappingText() != null && !outputTextMappingDTO.getMappingText().equals("")) {
                    backEndTextOutputMapping = new TextOutputMapping();
                    backEndTextOutputMapping.setMappingText(outputTextMappingDTO.getMappingText());
//                    backEndTextOutputMapping.setMappingText(outputDTO.getTopic());
                }
                backEndOutput.setOutputMapping(backEndTextOutputMapping);
            }

            OutputTupleMappingDTO outputTupleMappingDTO = outputDTO.getOutputTupleMapping();
            if (outputTupleMappingDTO != null) {
                TupleOutputMapping backEndTupleOutputMapping = null;
                backEndTupleOutputMapping = new TupleOutputMapping();
//                try {
//                    backEndTupleOutputMapping.setStreamTopic(outputDTO.getTopic());
//                } catch (InvalidTopicException e) {
//                    new CEPAdminException(e.getMessage());
//                }
                if (outputTupleMappingDTO.getMetaDataProperties() != null && outputTupleMappingDTO.getMetaDataProperties().length != 0) {
                    backEndTupleOutputMapping.setMetaDataProperties(adaptTupleOutputPropertyList(outputTupleMappingDTO.getMetaDataProperties()));
                }
                if (outputTupleMappingDTO.getCorrelationDataProperties() != null && outputTupleMappingDTO.getCorrelationDataProperties().length != 0) {
                    backEndTupleOutputMapping.setCorrelationDataProperties(adaptTupleOutputPropertyList(outputTupleMappingDTO.getCorrelationDataProperties()));
                }
                if (outputTupleMappingDTO.getPayloadDataProperties() != null && outputTupleMappingDTO.getPayloadDataProperties().length != 0) {
                    backEndTupleOutputMapping.setPayloadDataProperties(adaptTupleOutputPropertyList(outputTupleMappingDTO.getPayloadDataProperties()));
                }
                backEndOutput.setOutputMapping(backEndTupleOutputMapping);
            }

            OutputMapMappingDTO outputMapMappingDTO = outputDTO.getOutputMapMapping();
            if (outputMapMappingDTO != null) {
                MapOutputMapping backEndMapOutputMapping = null;
                if (outputMapMappingDTO.getMapProperties() != null && outputMapMappingDTO.getMapProperties().length != 0) {
                    backEndMapOutputMapping = new MapOutputMapping();
                    backEndMapOutputMapping.setPropertyList(adaptMapOutputPropertyList(outputMapMappingDTO.getMapProperties()));
                }
                backEndOutput.setOutputMapping(backEndMapOutputMapping);
            }
            backEndQuery.setOutput(backEndOutput);
        }
        return backEndQuery;
    }

    /**
     * This method will tuple CEP Admin module outputTuplePropertyDTO to CEP Core module TupleOutputProperty
     *
     * @param outputTuplePropertyDTOs - CEP Admin module Array of outputTuplePropertyDTO
     * @return TupleOutputProperty List - CEP Core module TupleOutputProperty List
     */
    private static List<TupleOutputProperty> adaptTupleOutputPropertyList(
            OutputTuplePropertyDTO[] outputTuplePropertyDTOs) {
        List<TupleOutputProperty> tupleOutputProperties = new ArrayList<TupleOutputProperty>();
        for (OutputTuplePropertyDTO outputTuplePropertyDTO : outputTuplePropertyDTOs) {
            tupleOutputProperties.add(new TupleOutputProperty(outputTuplePropertyDTO.getName(), outputTuplePropertyDTO.getValueOf(),outputTuplePropertyDTO.getType()));
        }
        return tupleOutputProperties;

    }
    /**
     * This method will map CEP Admin module outputMapPropertyDTO to CEP Core module MapOutputProperty
     *
     * @param outputMapPropertyDTOs - CEP Admin module Array of outputMapPropertyDTO
     * @return Query - CEP Core module outputMapMapping
     */
    private static List<MapOutputProperty> adaptMapOutputPropertyList(
            OutputMapPropertyDTO[] outputMapPropertyDTOs) {
        List<MapOutputProperty> mapOutputProperties = new ArrayList<MapOutputProperty>();
        for (OutputMapPropertyDTO outputMapPropertyDTO : outputMapPropertyDTOs) {
            mapOutputProperties.add(new MapOutputProperty(outputMapPropertyDTO.getName(), outputMapPropertyDTO.getValueOf()));
        }
        return mapOutputProperties;

    }


    /**
     * This method maps CEP Core module input topic list to CEP Admin module InputDTO array
     *
     * @param backEndInputList - CEP Core module input  list
     * @return CEP Admin module InputDTO  array
     */
    public static InputDTO[] adaptInput(List<Input> backEndInputList) {
        InputDTO[] inputDTOs = new InputDTO[backEndInputList.size()];
        int i = 0;
        for (Input backEndInput : backEndInputList) {
            InputDTO inputDTO = new InputDTO();
            inputDTO.setTopic(backEndInput.getTopic());
            inputDTO.setBrokerName(backEndInput.getBrokerName());
            if (backEndInput.getInputMapping() instanceof XMLInputMapping) {
                inputDTO.setInputXMLMappingDTO(adaptMapping((XMLInputMapping) backEndInput.getInputMapping()));
            } else if (backEndInput.getInputMapping() instanceof MapInputMapping) {
                inputDTO.setInputMapMappingDTO(adaptMapping((MapInputMapping) backEndInput.getInputMapping()));
            } else {
                inputDTO.setInputTupleMappingDTO(adaptMapping((TupleInputMapping) backEndInput.getInputMapping()));
            }
            inputDTOs[i] = inputDTO;
            i++;
        }
        return inputDTOs;
    }

    /**
     * This method maps CEP Core module InputXMLMappingDTO to CEP Admin module  Mappings
     *
     * @param backEndXMLInputMapping - CEP Core module input Stream Mappings
     */
    private static InputXMLMappingDTO adaptMapping(XMLInputMapping backEndXMLInputMapping) {
        InputXMLMappingDTO inputXMLMappingDTO = new InputXMLMappingDTO();
        inputXMLMappingDTO.setStream(backEndXMLInputMapping.getStream());
        inputXMLMappingDTO.setQueryEventType(QueryEventTypeConvertor.mappingClassToType(backEndXMLInputMapping.getMappingClass()));
        inputXMLMappingDTO.setProperties(adaptXMLInputProperties(backEndXMLInputMapping.getProperties()));
        inputXMLMappingDTO.setXpathDefinition(adaptXpathDefinitions(backEndXMLInputMapping.getXpathNamespacePrefixes()));
        return inputXMLMappingDTO;
    }

    /**
     * This method maps CEP Core module InputTupleMappingDTO to CEP Admin module  Mappings
     *
     * @param backEndTupleInputMapping - CEP Core module input Stream Mappings
     */
    private static InputTupleMappingDTO adaptMapping(TupleInputMapping backEndTupleInputMapping) {
        InputTupleMappingDTO inputTupleMappingDTO = new InputTupleMappingDTO();
        inputTupleMappingDTO.setQueryEventType(QueryEventTypeConvertor.mappingClassToType(backEndTupleInputMapping.getMappingClass()));
        inputTupleMappingDTO.setStream(backEndTupleInputMapping.getStream());
        inputTupleMappingDTO.setProperties(adaptTupleInputProperties(backEndTupleInputMapping.getProperties()));
        return inputTupleMappingDTO;
    }

    private static InputMapMappingDTO adaptMapping(MapInputMapping backEndMapInputMapping) {
        InputMapMappingDTO inputMapMappingDTO = new InputMapMappingDTO();
        inputMapMappingDTO.setStream(backEndMapInputMapping.getStream());
        inputMapMappingDTO.setQueryEventType(QueryEventTypeConvertor.mappingClassToType(backEndMapInputMapping.getMappingClass()));
        inputMapMappingDTO.setProperties(adaptMapInputProperties(backEndMapInputMapping.getProperties()));
        return inputMapMappingDTO;
    }

    /**
     * This method maps CEP core module InputDTO property list to CEP Admin module input property array
     *
     * @param backEndXMLPropertyList - CEP Core module input property list
     */
    private static InputXMLPropertyDTO[] adaptXMLInputProperties(List<XMLInputProperty> backEndXMLPropertyList) {
        InputXMLPropertyDTO[] inputInputXMLPropertyDTOs = new InputXMLPropertyDTO[backEndXMLPropertyList.size()];
        int i = 0;
        for (XMLInputProperty backEndInputProperty : backEndXMLPropertyList) {
            InputXMLPropertyDTO InputXMLPropertyDTO = new InputXMLPropertyDTO();
            InputXMLPropertyDTO.setName(backEndInputProperty.getName());
            InputXMLPropertyDTO.setXpath(backEndInputProperty.getXpath());
            InputXMLPropertyDTO.setType(backEndInputProperty.getType());
            inputInputXMLPropertyDTOs[i] = InputXMLPropertyDTO;
            i++;
        }
        return inputInputXMLPropertyDTOs;
    }


    /**
     * This method maps CEP core module InputDTO property list to CEP Admin module input property array
     *
     * @param backEndTuplePropertyList - CEP Core module input property list
     */
    private static InputTuplePropertyDTO[] adaptTupleInputProperties(
            List<TupleInputProperty> backEndTuplePropertyList) {
        InputTuplePropertyDTO[] inputInputTuplePropertyDTOs = new InputTuplePropertyDTO[backEndTuplePropertyList.size()];
        int i = 0;
        for (TupleInputProperty backEndInputProperty : backEndTuplePropertyList) {
            InputTuplePropertyDTO inputTuplePropertyDTO = new InputTuplePropertyDTO();
            inputTuplePropertyDTO.setName(backEndInputProperty.getName());
            inputTuplePropertyDTO.setInputName(backEndInputProperty.getInputName());
            inputTuplePropertyDTO.setInputDataType(backEndInputProperty.getInputDataType());
            inputTuplePropertyDTO.setType(backEndInputProperty.getType());
            inputInputTuplePropertyDTOs[i] = inputTuplePropertyDTO;
            i++;
        }
        return inputInputTuplePropertyDTOs;
    }

    /**
     * This method maps CEP core module InputDTO property list to CEP Admin module input property array
     *
     * @param backEndMapPropertyList - CEP Core module input property list
     */
    private static InputMapPropertyDTO[] adaptMapInputProperties(List<MapInputProperty> backEndMapPropertyList) {
        InputMapPropertyDTO[] inputInputMapPropertyDTOs = new InputMapPropertyDTO[backEndMapPropertyList.size()];
        int i = 0;
        for (MapInputProperty backEndInputProperty : backEndMapPropertyList) {
            InputMapPropertyDTO InputMapPropertyDTO = new InputMapPropertyDTO();
            InputMapPropertyDTO.setName(backEndInputProperty.getName());
            InputMapPropertyDTO.setInputName(backEndInputProperty.getInputName());
            InputMapPropertyDTO.setType(backEndInputProperty.getType());
            inputInputMapPropertyDTOs[i] = InputMapPropertyDTO;
            i++;
        }
        return inputInputMapPropertyDTOs;
    }

    /**
     * This method maps CEP Core module nameSpacePrefixes Map in to CEP Admin module NamespacePrefixDTO array
     *
     * @param backEndXpathDefinitions
     */
    private static XpathDefinitionDTO[] adaptXpathDefinitions(
            List<XpathDefinition> backEndXpathDefinitions) {
        XpathDefinitionDTO[] xpathDefinitionDTOs = new XpathDefinitionDTO[backEndXpathDefinitions.size()];
        int i = 0;
        for (XpathDefinition xpathDefinition : backEndXpathDefinitions) {
            XpathDefinitionDTO xpathDefinitionDTO = new XpathDefinitionDTO();
            xpathDefinitionDTO.setPrefix(xpathDefinition.getPrefix());
            xpathDefinitionDTO.setNamespace(xpathDefinition.getNamespace());
            xpathDefinitionDTOs[i] = xpathDefinitionDTO;
            i++;
        }
        return xpathDefinitionDTOs;
    }

    /**
     * This method maps CEP Core module QueryDTO list in to CEP Admin module QueryDTO array
     *
     * @param backEndQueryList
     */
    public static QueryDTO[] adaptQueries(List<Query> backEndQueryList) {
        QueryDTO[] queryDTOs = new QueryDTO[backEndQueryList.size()];
        int index = 0;
        for (Query backEndQuery : backEndQueryList) {
            QueryDTO queryDTO = new QueryDTO();
            queryDTO.setName(backEndQuery.getName());
            queryDTO.setQueryIndex(backEndQuery.getQueryIndex());
            if (backEndQuery.getOutput() != null) {
                queryDTO.setOutput(adaptOutput(backEndQuery.getOutput()));
            }
            queryDTO.setExpression(adaptExpression(backEndQuery.getExpression()));
            queryDTOs[index] = queryDTO;
            index++;
        }
        return queryDTOs;
    }

    /**
     * This method maps CEP Core module OutputDTO in to CEP Admin module OutputDTO Topic
     *
     * @param backEndOutput
     */
    private static OutputDTO adaptOutput(Output backEndOutput) {

        OutputDTO outputDTO = new OutputDTO();
        outputDTO.setTopic(backEndOutput.getTopic());
        outputDTO.setBrokerName(backEndOutput.getBrokerName());
        if (backEndOutput.getOutputMapping() instanceof TupleOutputMapping) {
            outputDTO.setOutputTupleMapping(adaptOutputTupleMapping((TupleOutputMapping) backEndOutput.getOutputMapping()));
        } else if (backEndOutput.getOutputMapping() instanceof MapOutputMapping) {
            outputDTO.setOutputMapMapping(adaptOutputMapMapping((MapOutputMapping) backEndOutput.getOutputMapping()));
        } else if (backEndOutput.getOutputMapping() instanceof TextOutputMapping) {
            outputDTO.setOutputTextMapping(adaptOutputTextMapping((TextOutputMapping) backEndOutput.getOutputMapping()));
        } else {
            outputDTO.setOutputXmlMapping(adaptOutputXMLMapping((XMLOutputMapping) backEndOutput.getOutputMapping()));
        }
        return outputDTO;
    }


    /**
     * This method will map CEP Core module OutputTupleMappingDTO  in to CEP Admin module ElementMappings
     *
     * @param backEndTupleOutputMapping
     */
    private static OutputTupleMappingDTO adaptOutputTupleMapping(
            TupleOutputMapping backEndTupleOutputMapping) {
        OutputTupleMappingDTO outputTupleMappingDTO = null;
        if (backEndTupleOutputMapping != null) {
            outputTupleMappingDTO = new OutputTupleMappingDTO();
            if (backEndTupleOutputMapping.getMetaDataProperties() != null) {
                outputTupleMappingDTO.setMetaDataProperties(adaptOutputTuplePropertyDTOs(backEndTupleOutputMapping.getMetaDataProperties()));
            } else {
                outputTupleMappingDTO.setMetaDataProperties(new OutputTuplePropertyDTO[0]);
            }
            if (backEndTupleOutputMapping.getCorrelationDataProperties() != null) {
                outputTupleMappingDTO.setCorrelationDataProperties(adaptOutputTuplePropertyDTOs(backEndTupleOutputMapping.getCorrelationDataProperties()));
            } else {
                outputTupleMappingDTO.setCorrelationDataProperties(new OutputTuplePropertyDTO[0]);
            }
            if (backEndTupleOutputMapping.getPayloadDataProperties() != null) {
                outputTupleMappingDTO.setPayloadDataProperties(adaptOutputTuplePropertyDTOs(backEndTupleOutputMapping.getPayloadDataProperties()));
            } else {
                outputTupleMappingDTO.setPayloadDataProperties(new OutputTuplePropertyDTO[0]);
            }
        }
        return outputTupleMappingDTO;
    }

    /**
     * This method will tuple CEP Admin module tupleOutputProperty list to CEP Core module OutputTuplePropertyDTO Array
     *
     * @param outputTuplePropertyList - CEP Admin module List of tupleOutputProperty
     * @return OutputTuplePropertyDTO[] - CEP Core module outputTuplePropertyDTOs
     */
    private static OutputTuplePropertyDTO[]  adaptOutputTuplePropertyDTOs(
            List<TupleOutputProperty> outputTuplePropertyList) {
        OutputTuplePropertyDTO[] outputTuplePropertyDTOs = new OutputTuplePropertyDTO[outputTuplePropertyList.size()];
        for (int i = 0, outputTuplePropertyListSize = outputTuplePropertyList.size(); i < outputTuplePropertyListSize; i++) {
            TupleOutputProperty tupleOutputProperty = outputTuplePropertyList.get(i);
            OutputTuplePropertyDTO outputTuplePropertyDTO =new OutputTuplePropertyDTO();
            outputTuplePropertyDTO.setName(tupleOutputProperty.getName());
            outputTuplePropertyDTO.setValueOf(tupleOutputProperty.getValueOf());
            outputTuplePropertyDTO.setType(tupleOutputProperty.getType());
            outputTuplePropertyDTOs[i]=outputTuplePropertyDTO;
        }
        return outputTuplePropertyDTOs;

    }

    /**
     * This method will map CEP Core module OutputMapMappingDTO  in to CEP Admin module ElementMappings
     *
     * @param backEndMapOutputMapping
     */
    private static OutputMapMappingDTO adaptOutputMapMapping(
            MapOutputMapping backEndMapOutputMapping) {
        OutputMapMappingDTO outputMapMappingDTO = null;
        if (backEndMapOutputMapping != null) {
            outputMapMappingDTO = new OutputMapMappingDTO();
            if (backEndMapOutputMapping.getPropertyList() != null) {
                outputMapMappingDTO.setMapProperties(adaptOutputMapPropertyDTOs(backEndMapOutputMapping.getPropertyList()));
            } else {
                outputMapMappingDTO.setMapProperties(new OutputMapPropertyDTO[0]);
            }
        }
        return outputMapMappingDTO;
    }

    /**
     * This method will map CEP Admin module mapOutputProperty list to CEP Core module OutputMapPropertyDTO Array
     *
     * @param outputMapPropertyList - CEP Admin module List of mapOutputProperty
     * @return OutputMapPropertyDTO[] - CEP Core module outputMapPropertyDTOs
     */
    private static OutputMapPropertyDTO[]  adaptOutputMapPropertyDTOs(
            List<MapOutputProperty> outputMapPropertyList) {
        OutputMapPropertyDTO[] outputMapPropertyDTOs = new OutputMapPropertyDTO[outputMapPropertyList.size()];
        for (int i = 0, outputMapPropertyListSize = outputMapPropertyList.size(); i < outputMapPropertyListSize; i++) {
            MapOutputProperty mapOutputProperty = outputMapPropertyList.get(i);
            OutputMapPropertyDTO outputMapPropertyDTO =new OutputMapPropertyDTO();
            outputMapPropertyDTO.setName(mapOutputProperty.getName());
            outputMapPropertyDTO.setValueOf(mapOutputProperty.getValueOf());
            outputMapPropertyDTOs[i]=outputMapPropertyDTO;
        }
        return outputMapPropertyDTOs;

    }

    /**
     * This method will map CEP Core module OutputXMLMappingDTO in to CEP Admin module OutputXMLMappingDTO
     *
     * @param backEndXmlOutputMapping
     */
    private static OutputXMLMappingDTO adaptOutputXMLMapping(XMLOutputMapping backEndXmlOutputMapping) {
        OutputXMLMappingDTO outputXmlMappingDTO = null;
        if (backEndXmlOutputMapping != null && backEndXmlOutputMapping.getMappingXMLText().length() > 0) {
            outputXmlMappingDTO = new OutputXMLMappingDTO();
            String xmlMappingText = backEndXmlOutputMapping.getMappingXMLText();
            outputXmlMappingDTO.setMappingXMLText(xmlMappingText);
        }
        return outputXmlMappingDTO;
    }

    /**
     * This method will map CEP Core module OutputTextMappingDTO in to CEP Admin module OutputTextMappingDTO
     *
     * @param backEndTextOutputMapping
     */
    private static OutputTextMappingDTO adaptOutputTextMapping(TextOutputMapping backEndTextOutputMapping) {
        OutputTextMappingDTO outputTextMappingDTO = null;
        if (backEndTextOutputMapping != null && backEndTextOutputMapping.getMappingText().length() > 0) {
            outputTextMappingDTO = new OutputTextMappingDTO();
            String textMappingText = backEndTextOutputMapping.getMappingText();
            outputTextMappingDTO.setMappingText(textMappingText);
        }
        return outputTextMappingDTO;
    }

    /**
     * This method maps CEP core module ExpressionDTO to CEP Admin module ExpressionDTO
     *
     * @param backendExpression
     */
    private static ExpressionDTO adaptExpression(Expression backendExpression) {
        ExpressionDTO expressionDTO = new ExpressionDTO();
        expressionDTO.setText(backendExpression.getText());
        return expressionDTO;
    }


    /**
     * This method maps CEP Core module input topic list to CEP Admin module InputDTO array
     *
     * @param providerConfiguration
     */
    public static CEPEngineProviderConfigPropertyDTO[] adaptEngineProviderConfigs(
            Properties providerConfiguration) {
        CEPEngineProviderConfigPropertyDTO[] cepEngineProviderPropertyDTO = null;
        if (providerConfiguration != null) {
            cepEngineProviderPropertyDTO = new CEPEngineProviderConfigPropertyDTO[providerConfiguration.size()];
            int i = 0;
            for (Map.Entry<Object, Object> entry : providerConfiguration.entrySet()) {
                cepEngineProviderPropertyDTO[i] = new CEPEngineProviderConfigPropertyDTO();
                cepEngineProviderPropertyDTO[i].setNames(entry.getKey().toString());
                if (entry.getValue() instanceof String) {
                    cepEngineProviderPropertyDTO[i].setValues((String) entry.getValue());
                } else {
                    cepEngineProviderPropertyDTO[i].setValues((String) ((List) entry.getValue()).get(0));
                }
                i++;
            }
            return cepEngineProviderPropertyDTO;
        } else {
            return new CEPEngineProviderConfigPropertyDTO[]{};
        }
    }



}
