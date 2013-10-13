/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.service.mgt.weather;

/**
 * Created by IntelliJ IDEA.
 * User: isuru
 * Date: Nov 4, 2009
 * Time: 10:56:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class weather {
	public static double c2f (double cTemp){
		return cTemp * 180/100 + 32.0;
	}
}
