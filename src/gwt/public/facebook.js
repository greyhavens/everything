var api_key = 'eed6c8a8d9be1718c1a2922caa344ead';
var channel_path = 'xd_receiver.html';
FB_RequireFeatures(["Api", "CanvasUtil"], function() {
    // Create an ApiClient object, passing app's API key and
    // a site relative URL to xd_receiver.htm
    FB.Facebook.init(api_key, channel_path);
    var api = FB.Facebook.apiClient;
    alert("Hello client " + FB.Facebook.get_isInCanvas());
    FB.CanvasClient.startTimerToSizeToContent();
//     // require user to login
//     api.requireLogin(function(exception) {
//         alert("Hello login! " + api.get_session().uid);
//         Debug.dump("Current user id is " + api.get_session().uid);
//         // Get friends list //5-14-09: this code below is broken, correct code follows
//         // //api.friends_get(null, function(result){ // Debug.dump(result, 'friendsResult
//         // from non-batch execution '); // });
//         api.friends_get(new Array(), function(result, exception){
//             Debug.dump(result, 'friendsResult from non-batch execution ');
//         });
//     });
});
