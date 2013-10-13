(function () {
    var localeResourcesBasePath = '',
            localizations = {};
    //initialization with the request and loads the correct localization resources
    this.init = function (req, localeResourcePath) {
        var locale = req.getLocale();
        try {
            if (localeResourcePath) {
                localeResourcesBasePath = localeResourcePath;
            }
            var file = new File(localeResourcesBasePath + 'locale_' + locale + '.json');
            //Check relevant locale file exists
            if (file.isExists()) {
                localizations = require(localeResourcesBasePath + 'locale_' + locale + '.json');
            } else {  //If not reading Strings from default English locale file.
                localizations = require(localeResourcesBasePath + 'locale_en.json');
            }
        } catch (e) {
            localizations = {};
        }
    };

    var getLocalString = function (key, fallback) {
        if (localizations[key]) {
            return localizations[key]
        } else {
            return  key;
        }

    };

    this.localize = function (key, fallback) {
        var localized = getLocalString(key);
        if (localized !== key) {
            return localized;
        } else {
            return fallback;
        }
    };
})();