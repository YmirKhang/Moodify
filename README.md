# Moodify [![Build Status](https://travis-ci.com/dnzprmksz/Moodify.svg?branch=master)](https://travis-ci.com/dnzprmksz/Moodify)

Get insights from your Spotify activities and create personalized playlists with your own preferences.


# Features
Show user's favourite tracks for long term, mid term and short term time intervals.

Visualize user's recent listening style by comparing the average audio features of last three and fifteen tracks.

Let the user to create a playlist by selecting seed artists/tracks and desired audio feature levels for acousticness, instrumentalness, speechiness, danceability, liveness, energy and valence. A playlist with recommended songs will appear in user's Spotify account under a playlist called Discover Moodify.


# Installation
Install JDK and sbt.

You should run following command to compile the application with sbt. It will also fetch the missing dependencies.

````
sbt assembly
````

Start a local Redis server or edit `test.conf` file to connect your own Redis server.

Start the application with following command and it will start to listen requests on port 9000.

````
sbt
> reStart
````

Now the API is running. If you want to run the web application, you can simply use `python` for a local web server with following command. This will run the web application and make it listen to port 8000 on localhost.

````
python -m SimpleHTTPServer
````

Note that you need to set environment variables `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET` and `SPOTIFY_REDIRECT_URI` for the application to run and communicate with Spotify API. If you do not have these credentials, you must create a Spotify developer account and register your app to get these credentials. For detailed information please check https://developer.spotify.com/documentation/general/guides/app-settings/
