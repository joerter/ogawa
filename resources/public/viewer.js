const servers = {
  iceServers: [
    {
      urls: ['stun:stun1.l.google.com:19302', 'stun:stun2.l.google.com:19302'],
    },
  ],
  iceCandidatePoolSize: 10,
};

const pc = new RTCPeerConnection(servers);
pc.onicecandidate = (event) => {
  console.log('got candidate', event);
};

const offerDescription = await pc.createOffer();
await pc.setLocalDescription(offerDescription);

const offer = {
  sdp: offerDescription.sdp,
  type: offerDescription.type,
};

console.log('created offer: ', offerDescription);
