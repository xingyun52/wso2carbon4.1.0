/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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

package org.wso2.appserver.sample.exchange.trader;

import org.wso2.appserver.sample.exchange.Exchange;
import org.wso2.www.types.exchange.trader.service.RegisterClientResponse;


/**
 * ExchangeTraderSkeleton java skeleton for the axisService
 */
public class ExchangeTraderSkeleton implements ExchangeTraderSkeletonInterface {
    Exchange exchange = Exchange.getInstance();

    public void sell(org.wso2.www.types.exchange.trader.service.SellRequest param0) {
        exchange.sell(param0.getUserid(), param0.getSymbol(), param0.getQty());
    }

    public void buy(org.wso2.www.types.exchange.trader.service.BuyRequest param1) {
        exchange.buy(param1.getUserid(), param1.getSymbol(), param1.getQty());
    }

    public void login(org.wso2.www.types.exchange.trader.service.LoginRequest param2) {
        //Donothing mate
    }

    public void registerFeedTarget(
            org.wso2.www.types.exchange.trader.service.RegisterFeedTargetRequest param3) {
        exchange.registerTrader(param3.getEPR());
    }

    public org.wso2.www.types.exchange.trader.service.RegisterClientResponse registerClient(
            org.wso2.www.types.exchange.trader.service.RegisterClientRequest param4) {
        RegisterClientResponse resp = new RegisterClientResponse();
        resp.setUserid(exchange.registerClient(param4.getClientInfo()));

        return resp;
    }
}
