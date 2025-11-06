# Chat App Backend

FastAPI server for real-time chat application using Firebase Firestore.

## Setup

1. Create a Firebase project at https://console.firebase.google.com/

2. Enable Firestore Database in your project

3. Go to Project Settings > Service Accounts > Generate new private key

4. Download the JSON key file and place it as `backend/serviceAccountKey.json`

5. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

6. Run the server:
   ```
   uvicorn main:app --reload
   ```

## API Endpoints

- POST /register: Register a new user with mobile number
- POST /login: Login with mobile number
- POST /send_message: Send a message
- GET /messages/{mobile}: Get messages for a user
- WebSocket /ws/{mobile}: Real-time messaging

## Database Schema (Firestore Collections)

- users: {mobile, username}
- messages: {sender_mobile, receiver_mobile, content, timestamp}

## Notes

- Uses Firebase Firestore as NoSQL database
- Real-time messaging via WebSockets
- No server maintenance required