/* Wire up some specific functions. */
window.FB_Init = function (apiKey) {
    FB_RequireFeatures(["XFBML"], function () {
        // FB.XFBML.Host.autoParseDomTree = false;
        FB.init(apiKey, "xd_receiver.html");
    });
};

window.FB_RequireSession = function () {
    FB.Bootstrap.ensureInit(function () {
        FB.Connect.requireSession(function () {
            if (FB_RequireSessionCallback) {
                FB_RequireSessionCallback(FB.Facebook.apiClient.get_session().uid);
                FB_RequireSessionCallback = null;
            }
        });
    });
};

window.FB_ParseXFBML = function (elem) {
    FB.Bootstrap.ensureInit(function () {
        if (elem != null) {
            FB.XFBML.Host.parseDomElement(elem);
        }
    });
};

window.FB_ShowBragDialog = function (thing_name, thing_descrip, thing_image, series_url) {
    var attachment = {
        'name': thing_name,
        'description': thing_descrip,
        'href': series_url,
        'media': [{'type': 'image',
                   'src': thing_image,
                   'href': series_url }],
        'properties': {'Play Everything': {'text': 'What will you get?',
                                           'href': 'http://apps.facebook.com/everythinggame/'}},
    };
    FB.Connect.streamPublish("Woo!", attachment);
};

/* Start up our iframe resizer once FB_Init is called by GWT */
if (window != window.top) {
    FB.Bootstrap.ensureInit(function () {
        FB.CanvasClient.startTimerToSizeToContent();
    });
}
