// Add a <video> element to the HTML page this code runs in:
// <video id="output-video" autoplay muted></video>

import WHEPClient from "./WHEPClient.js"; // an example WHEP client, see https://github.com/cloudflare/workers-sdk/blob/main/templates/stream/webrtc/src/WHEPClient.ts

const url = ""; // add the webRTCPlayback URL from your live input here
const videoElement = document.getElementById("input-video");

joinStreamButton.onclick = () => {
  const client = new WHEPClient(url, videoElement);
}
