FROM node:11-alpine AS base
COPY . .
RUN npm install
ARG API_ADDRESS
ARG SPOTIFY_CLIENT_ID
RUN echo $API_ADDRESS
RUN sed -i -e "s/<spotify_client_id>/$SPOTIFY_CLIENT_ID/" web/resources/js/login.js
RUN sed -i -e "s^<insert_api_host>^$API_ADDRESS^" web/resources/js/main.js
RUN sed -i -e "s^<insert_api_host>^$API_ADDRESS^" web/resources/js/callback.js	
CMD ["node", "server.js"]
