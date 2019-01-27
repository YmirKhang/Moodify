$(document).ready(function () {
  const CLIENT_HOST = 'https://moodify.app';
  const API_HOST = 'https://api.moodify.app';
  const LOGIN_PAGE = `${CLIENT_HOST}/login.html`;
  const udidKey = "moodify-udid";
  const spotifyIdKey = "moodify-spotifyId";
  
  let udid = localStorage.getItem(udidKey);
  let spotifyId = localStorage.getItem(spotifyIdKey);

  if (udid == null || spotifyId == null) {
    window.location.replace(LOGIN_PAGE);
    return;
  }

  const historicalTrackCount = 15;
  const recentTrackCount = 3;

  let historicTrendlineURL = `${API_HOST}/user/${udid}/trendline/${historicalTrackCount}?userId=${spotifyId}`;
  let recentTrendlineURL = `${API_HOST}/user/${udid}/trendline/${recentTrackCount}?userId=${spotifyId}`;
  let profileUrl = `${API_HOST}/user/${udid}/profile/?userId=${spotifyId}`;
  
  // Populate profile data.
  $.get(profileUrl, function (response) {
    let json = JSON.parse(response);
    let data = json.data;
    let imageSize = 25;
    let image = `<image src='${data.imageUrl}' class='profile-image inline' width='${imageSize}' heigth='${imageSize}'>`;
    let name = `<p class='profile-name inline'>${data.name}</p>`;
    let profileData = image + name;
    $("#profile-inner").append(profileData);
  });

  /**
   * Populates top artists and tracks.
   * @param type artist or track
   */
  $.populateTopArtistTracks = function (type, limit) {
    let terms = ["long", "medium", "short"];
    terms.forEach(function (term) {
      let url = `${API_HOST}/user/${udid}/top-${type}s/${term}_term?userId=${spotifyId}`;
      $.get(url, function (response) {
        let json = JSON.parse(response);
        let data = json.data.slice(0, limit);
        let divId = `#top-${type}-${term}-term`;
        var blockData = "<div class='col-sm-12'>";
        let imageSize = 45;
        data.forEach(function (object) {
          let image = `<div class='col-sm-2'><image src='${object.imageUrl}' class='list-item-image' width='${imageSize}' heigth='${imageSize}'></div>`;
          let artistNames = object.artists.map(artist => artist.name).join(", ");
          let name = `<div class='col-sm-10'>
          <div class='row list-item-track-name'><div class='col-sm-12'>${object.name}</div></div>
          <div class='row list-item-artist-name'><div class='col-sm-12'>${artistNames}</div></div>
          </div>`;
          let listItem = "<div class='row list-item'>" + image + name + "</div>";
          let clickableListItem = `<a target='_blank' rel='noopener noreferrer' href='https://open.spotify.com/${type}/${object.id}'>${listItem}</a>`;
          blockData += clickableListItem;
        });
        blockData += "</div>";
        $(divId).append(blockData);
      });
    });
  }

  let limit = 5;
  $.populateTopArtistTracks("track", limit);

  /**
   * Search
   */
  $.searchResultArtists = [];
  $.searchResultTracks = [];
  $.seeds = [];
  var jqxhr = { abort: function () { } };
  let searchBox = $("#recommendation-seed-search");
  let searchResultBox = $("#recommendation-search-result-box");

  searchBox.on("input", function () {
    let input = searchBox.val();
    if (input.trim() != "") {
      searchResultBox.show();
      let url = `${API_HOST}/user/${udid}/search?userId=${spotifyId}&query=${input}`;
      jqxhr.abort();
      jqxhr = $.get(url, function (response) {
        let json = JSON.parse(response);
        let data = json.data;
        $.searchResultArtists = [];
        $.searchResultTracks = [];

        data.forEach(function (item) {
          if (item.itemType == "artist") {
            $.searchResultArtists.push(item);
          } else if (item.itemType == "track") {
            $.searchResultTracks.push(item);
          }
        });

        $.updateSearchResults($.searchResultArtists, "artist");
        $.updateSearchResults($.searchResultTracks, "track");
      });
    } else {
      $.clearSearch();
    }
  });

  $.clearSearch = function () {
    searchResultBox.hide();
    $.searchResultArtists = [];
    $.searchResultTracks = [];
    $("#recommendation-search-result-artist").empty();
    $("#recommendation-search-result-track").empty();
    searchBox.val("");
  };

  $.updateSearchResults = function (items, itemType) {
    let listId = `#recommendation-search-result-${itemType}`;
    var listData = "";
    let imageSize = 35;

    items.forEach(function (item) {
      let extraData = "";
      if (itemType == "track") {
        extraData = `, ${item.extra}`;
      }
      let imageHtml = `<image src='${item.imageUrl}' class='list-item-image-${itemType}' width='${imageSize}' heigth='${imageSize}'></div>`;

      listData += `<li id='search-result-${item.id}' class='list-item-search-result' data-type='${itemType}'>${imageHtml} ${item.name + extraData}</li>`;
    });

    $(listId).html(listData);

    items.forEach(function (item) {
      $(`#search-result-${item.id}`).click(function () {
        let uri = `spotify:${itemType}:${item.id}`;
        if ($.seeds.length == 5) {
          alert("You can specify at most five artists and tracks.");
        } else if (!$.seeds.includes(item)) {
          $.addSeed(item);
        }
        $.clearSearch();
      });
    });
  };

  $.addSeed = function (item) {
    $.seeds.push(item);
    $.populateSeedList();
  };

  $.populateSeedList = function () {
    let htmlList = $.seeds.map(function (item) {
      let divId = `seed-${item.id}`;
      let html = `<div id='${divId}' class='button selected-seed inline' data-hover='Remove'>${item.name}</div>`;
      return html;
    });

    let seedHtml = htmlList.join("");
    $("#recommendation-seed-selected").html(seedHtml);

    $.seeds.forEach(function (item) {
      let divId = `#seed-${item.id}`;
      $(divId).click(function () {
        let index = $.seeds.indexOf(item)
        if (index > -1) {
          $.seeds.splice(index, 1);
          $.populateSeedList();
        }
      });
    });
  }

  // Fill default artists data.
  let defaultArtistsUrl = `${API_HOST}/user/${udid}/default-artists?userId=${spotifyId}`;
  $.get(defaultArtistsUrl, function (response) {
    let json = JSON.parse(response);
    let data = json.data;
    data.forEach(function (item) {
      $.addSeed(item);
    });
  });

  let recommendationButton = $("#recommendation-create-button");
  recommendationButton.click(
    () => {
      let callback = () => {
        recommendationButton.html("CHECK SPOTIFY");
        setTimeout(
          () => {
            recommendationButton.html("CREATE PLAYLIST");
            alert("Check your Spotify for Discover Moodify playlist!");
          },
          500);
      };

      recommendationButton.html("PLEASE WAIT");
      $.createPlaylist(callback);
    }
  );

  $.getAudioFeatureValue = function (name) {
    return parseFloat($(`#range-${name}`).val());
  };

  $.getSeedIdList = function (type) {
    return $.seeds
      .filter((item) => item.itemType == type)
      .map((item) => item.id);
  };

  $.createPlaylist = function (callback) {
    if ($.seeds.length == 0) {
      alert("You should specify at least one artist or track.");
    } else if ($.seeds.length > 5) {
      alert("You can specify at most five artists and tracks.");
    } else {
      let url = `${API_HOST}/user/${udid}/recommendation?userId=${spotifyId}`;
      let data = {
        "seedArtistIdList": $.getSeedIdList("artist"),
        "seedTrackIdList": $.getSeedIdList("track"),
        "acousticness": $.getAudioFeatureValue("acousticness"),
        "instrumentalness": $.getAudioFeatureValue("instrumentalness"),
        "speechiness": $.getAudioFeatureValue("speechiness"),
        "danceability": $.getAudioFeatureValue("danceability"),
        "liveness": $.getAudioFeatureValue("liveness"),
        "energy": $.getAudioFeatureValue("energy"),
        "valence": $.getAudioFeatureValue("valence")
      };

      $.postJSON(url, data, callback);
    }
  }

  $("#logout").click(function () {
    localStorage.removeItem(udidKey);
    localStorage.removeItem(spotifyIdKey);
    location.reload();
    return;
  });

  $.postJSON = function (url, data, callback) {
    return jQuery.ajax({
      'type': 'POST',
      'url': url,
      'data': JSON.stringify(data),
      'success': () => callback(),
      'error': () => console.log('Request failed.')
    });
  };

  /**
   * Chart
   */
  let ctx = $("#stats-chart");

  var chart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: ["Acousticness", "Instrumentalness", "Speechiness", "Danceability",
        "Liveness", "Energy", "Valence"],
      datasets: [{
        label: 'Last Fifteen Songs',
        borderColor: 'rgba(170, 170, 170, 1)',
        backgroundColor: 'rgba(170, 170, 170, 0.75)',
        borderWidth: 2.5
      },
      {
        label: 'Last Three Songs',
        borderColor: 'rgba(47, 213, 102, 1)',
        backgroundColor: 'rgba(47, 213, 102, 0.75)',
        borderWidth: 2.5
      }]
    },
    options: {
      legend: {
        labels: {
          fontColor: 'rgba(255, 255, 255, 0.9)',
          fontFamily: "'Lato', 'sans-serif'",
          fontSize: 12
        }
      },
      elements: {
        line: {
          fill: false
        },
        point: {
          pointStyle: "star"
        }
      },
      scales: {
        yAxes: [{
          ticks: {
            beginAtZero: true,
            fontFamily: "'Lato', 'sans-serif'",
            fontColor: 'rgba(255, 255, 255, 0.65)',
            max: 100,
            padding: 20
          },
          gridLines: {
            display: false
          }
        }],
        xAxes: [{
          ticks: {
            beginAtZero: true,
            fontFamily: "'Lato', 'sans-serif'",
            fontColor: 'rgba(255, 255, 255, 0.9)',
            fontSize: 14,
            padding: 20
          },
          gridLines: {
            display: false
          }
        }]
      }
    }
  });

  $.getField = function (json, field) {
    let rawValue = parseFloat(json[field]);
    let value = rawValue * 100;
    let result = value.toFixed(0);

    return result;
  }

  $.drawChart = function (response, chartIndex) {
    let json = jQuery.parseJSON(response);
    let data = json.data;
    let acousticness = $.getField(data, "acousticness");
    let instrumentalness = $.getField(data, "instrumentalness");
    let speechiness = $.getField(data, "speechiness");
    let danceability = $.getField(data, "danceability");
    let liveness = $.getField(data, "liveness");
    let energy = $.getField(data, "energy");
    let valence = $.getField(data, "valence");

    chart.data.datasets[chartIndex].data = [acousticness, instrumentalness, speechiness,
      danceability, liveness, energy, valence];
    chart.update();
  }

  $.updateRangePosition = function (json, field) {
    let value = parseFloat(json[field]).toFixed(1);
    $(`#range-${field}`).val(value);
  }

  // Update the historical trendline chart.
  $.get(historicTrendlineURL, function (response) {
    $.drawChart(response, 0);
  });

  // Update the recent trendline chart.
  $.get(recentTrendlineURL, function (response) {
    $.drawChart(response, 1);
    let json = jQuery.parseJSON(response);
    let data = json.data;
    $.updateRangePosition(data, "acousticness");
    $.updateRangePosition(data, "instrumentalness");
    $.updateRangePosition(data, "speechiness");
    $.updateRangePosition(data, "danceability");
    $.updateRangePosition(data, "liveness");
    $.updateRangePosition(data, "energy");
    $.updateRangePosition(data, "valence");
  });

});
