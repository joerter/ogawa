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

const joinStreamButton = document.getElementById('joinStreamButton');

async function createJoinOffer() {
  const joinOfferInput = document.getElementById('joinOffer');
  const offerDescription = await pc.createOffer();
  await pc.setLocalDescription(offerDescription);

  const offer = {
    sdp: offerDescription.sdp,
    type: offerDescription.type,
  };
  joinOfferInput.value = JSON.stringify(offer);
  joinStreamButton.disabled = false;

  console.log('created offer: ', offer);
}

createJoinOffer();
