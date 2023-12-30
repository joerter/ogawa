// Add a <video> element to the HTML page this code runs in:
// <video id="input-video" autoplay muted></video>

import WHIPClient from "./WHIPClient.js"; // an example WHIP client, see https://github.com/cloudflare/workers-sdk/blob/main/templates/stream/webrtc/src/WHIPClient.ts

const url = ""; // add the webRTC URL from your live input here
const videoElement = document.getElementById("input-video");

const startStreamButton = document.getElementById('startStreamButton');

startStreamButton.onclick = () => {
  const client = new WHIPClient(url, videoElement);
}
