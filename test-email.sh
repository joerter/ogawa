curl "https://api.postmarkapp.com/email" \
  -X POST \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "X-Postmark-Server-Token: 5a1dcd09-5e5c-4b5e-88f0-d0df9f371d47" \
  -d '{
  "From": "ogawa@simplevelocity.com",
  "To": "test@simplevelocity.com",
  "Subject": "Postmark test",
  "TextBody": "Hello dear Postmark user.",
  "HtmlBody": "<html><body><strong>Hello</strong> dear Postmark user.</body></html>",
  "MessageStream": "outbound"
}'

