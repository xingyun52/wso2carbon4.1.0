#!/bin/sh
START=$1
COUNT=$2
SERVER=http://localhost:9763
curl -X POST -c cookies $SERVER/publisher/site/blocks/user/login/ajax/login.jag -d 'action=login&username=provider1&password=provider1';

curl -X POST -b cookies $SERVER/publisher/site/blocks/item-add/ajax/add.jag -d "action=addAPI&name=YoutubeFeeds&visibility=public&version=1.0.0&description=Youtube Live Feeds&endpoint=http://gdata.youtube.com/feeds/api/standardfeeds&wsdl=&tags=youtube,gdata,multimedia&tier=Silver&thumbUrl=http://www.10bigideas.com.au/www/573/files/pf-thumbnail-youtube_logo.jpg&context=/youtube&tiersCollection=Gold&resourceCount=0&resourceMethod-0=GET&resourceMethodAuthType-0=Application&uriTemplate-0=/*";

curl -X POST -b cookies $SERVER/publisher/site/blocks/life-cycles/ajax/life-cycles.jag -d "name=YoutubeFeeds&version=1.0.0&provider=provider1&status=PUBLISHED&publishToGateway=true&action=updateStatus";



