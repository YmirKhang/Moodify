$(document).ready(function () {
  const CLIENT_HOST = window.location.origin;
  const udidKey = "moodify-udid";
  
  let udid = localStorage.getItem(udidKey);

  $.uuidv4 = function () {
    return ([1e7] + -1e3 + -4e3 + -8e3 + -1e11).replace(/[018]/g, c =>
      (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    )
  };

  if (udid == null) {
    udid = $.uuidv4();
    localStorage.setItem(udidKey, udid);
  }

  $("#login-button").click(function () {
    let endpoint = "https://accounts.spotify.com/authorize";
    let clientID = "<spotify_client_id>";
    let redirectUri = `${CLIENT_HOST}/callback.html`;
    let scope = "user-read-recently-played,playlist-modify-public,user-top-read,user-read-private,ugc-image-upload";

    window.dataLayer = window.dataLayer || [];
    function gtag() { dataLayer.push(arguments); }
    gtag('event', 'login-button-click', {
      'event_category': 'engagement',
    });

    let url = endpoint + "?client_id=" + encodeURIComponent(clientID) +
      "&redirect_uri=" + encodeURIComponent(redirectUri) +
      "&scope=" + encodeURIComponent(scope) +
      "&response_type=code&show_dialog=true";

    window.location.replace(url);
  });

});
