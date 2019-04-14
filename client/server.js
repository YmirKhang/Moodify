const express = require('express');
const path = require('path');
const app = express();
const cookieParser = require('cookie-parser');
const morgan= require('morgan');
const rfs = require('rotating-file-stream');
const frontend_port = process.env.FRONTEND_PORT || 8000;

console.log("Server started at:",new Date().toLocaleString());
console.log("PORT:", frontend_port);

app.use(cookieParser());

const accessLogStream = rfs('frontend-server.log', {
  interval: '1d', // rotate daily
  path: path.join(__dirname, 'log')
})

app.get('/login', function(req, res) {
  res.sendFile(path.join(__dirname, 'web', 'login.html'));
});

app.get('/callback', function(req, res) {
  res.sendFile(path.join(__dirname, 'web', 'callback.html'));
});

app.use(express.static(path.join(__dirname, 'web')));

app.listen(frontend_port);
