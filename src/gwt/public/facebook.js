/* Make our FB root object visible to GWT. */
window.FB = FB;

/* Wire up some specific functions. */
window.FB_Init = function (apiKey) {
    FB_RequireFeatures(["Api", "Connect", "XFBML", "CanvasUtil"], function () {
        FB.init(apiKey, "xd_receiver.html");
    });
}
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
}

/* Start up our iframe resizer once FB_Init is called by GWT */
FB.Bootstrap.ensureInit(function () {
    FB.CanvasClient.startTimerToSizeToContent();
});

/* Note: all callbacks seem to get called twice. Thanks Facebook! */
