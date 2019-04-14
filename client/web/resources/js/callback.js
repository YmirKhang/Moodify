$(document).ready(function () {
  const CLIENT_HOST = 'http://localhost:8000';
  const API_HOST = 'http://localhost:9000';
  const LOGIN_PAGE = `${CLIENT_HOST}/login.html`;
  const udidKey = "moodify-udid";
  const spotifyIdKey = "moodify-spotifyId";

  let udid = localStorage.getItem(udidKey);

  let url = window.location.href;
  let split = url.split("?code=");
  let code = split[1];
  
  let requestUrl = `${API_HOST}/authenticate`;
  let data = {
    udid: udid,
    code: code
  };

  $.get(requestUrl, data, function (response) {
    let json = JSON.parse(response);
    
    if (json.success) {
      let data = json.data;
      let spotifyId = data.userId;

      window.dataLayer = window.dataLayer || [];
      function gtag() { dataLayer.push(arguments); }
      if (json.success && spotifyId) {
        gtag('event', 'login');
      }

      localStorage.setItem(spotifyIdKey, spotifyId);
      window.location.replace(CLIENT_HOST);

    } else {
      localStorage.removeItem(spotifyIdKey);
      window.location.replace(LOGIN_PAGE);
    }
    
  });

});
