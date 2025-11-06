from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Body, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import credentials, firestore, initialize_app, storage
from pydantic import BaseModel
from typing import Optional
import json
import os
import base64
from dotenv import load_dotenv

load_dotenv()

# Initialize Firebase from environment variables
firebase_config = {
    "type": os.getenv("FIREBASE_TYPE"),
    "project_id": os.getenv("FIREBASE_PROJECT_ID"),
    "private_key_id": os.getenv("FIREBASE_PRIVATE_KEY_ID"),
    "private_key": os.getenv("FIREBASE_PRIVATE_KEY").replace("\\n", "\n") if os.getenv("FIREBASE_PRIVATE_KEY") else None,
    "client_email": os.getenv("FIREBASE_CLIENT_EMAIL"),
    "client_id": os.getenv("FIREBASE_CLIENT_ID"),
    "auth_uri": os.getenv("FIREBASE_AUTH_URI"),
    "token_uri": os.getenv("FIREBASE_TOKEN_URI"),
    "auth_provider_x509_cert_url": os.getenv("FIREBASE_AUTH_PROVIDER_X509_CERT_URL"),
    "client_x509_cert_url": os.getenv("FIREBASE_CLIENT_X509_CERT_URL"),
    "universe_domain": os.getenv("FIREBASE_UNIVERSE_DOMAIN")
}

if all(firebase_config.values()):
    cred = credentials.Certificate(firebase_config)
    initialize_app(cred)
    db = firestore.client()
else:
    # Fallback for demo
    initialize_app()
    db = firestore.client()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class UserCreate(BaseModel):
    mobile: str
    username: Optional[str] = None
    about: Optional[str] = None

class UserLogin(BaseModel):
    mobile: str

class MessageCreate(BaseModel):
    sender_mobile: str
    receiver_mobile: str
    content: str

@app.get("/")
def read_root():
    return {"message": "Server is running"}


@app.post("/register")
def register(user: UserCreate):
    # Check if user exists
    user_ref = db.collection('users').document(user.mobile)
    user_data = {
        'mobile': user.mobile,
        'username': user.username
    }
    if user.about:
        user_data['about'] = user.about
    
    if user_ref.get().exists:
        # Update existing user
        user_ref.update(user_data)
    else:
        # Create new user
        user_ref.set(user_data)
    
    return {"mobile": user.mobile, "username": user.username, "about": user.about}

@app.post("/login")
def login(data: UserLogin):
    user_ref = db.collection('users').document(data.mobile)
    user_doc = user_ref.get()
    if not user_doc.exists:
        raise HTTPException(status_code=404, detail="User not found")
    user_data = user_doc.to_dict()
    return {
        "mobile": user_data['mobile'], 
        "username": user_data.get('username'),
        "about": user_data.get('about', 'Hey there! I am using Chat App')
    }

@app.get("/user/{mobile}")
def get_user(mobile: str):
    user_ref = db.collection('users').document(mobile)
    user_doc = user_ref.get()
    if not user_doc.exists:
        return {"mobile": mobile, "username": mobile, "about": ""}
    user_data = user_doc.to_dict()
    return {
        "mobile": user_data['mobile'], 
        "username": user_data.get('username', mobile),
        "about": user_data.get('about', 'Hey there! I am using Chat App')
    }

@app.post("/send_message")
def send_message(data: MessageCreate):
    sender_ref = db.collection('users').document(data.sender_mobile)
    receiver_ref = db.collection('users').document(data.receiver_mobile)
    if not sender_ref.get().exists or not receiver_ref.get().exists:
        raise HTTPException(status_code=404, detail="User not found")
    
    message_data = {
        'sender_mobile': data.sender_mobile,
        'receiver_mobile': data.receiver_mobile,
        'content': data.content,
        'timestamp': firestore.SERVER_TIMESTAMP,
        'is_file': False
    }
    db.collection('messages').add(message_data)
    return {"status": "Message sent"}

@app.post("/send_file")
async def send_file(
    sender_mobile: str = Body(...),
    receiver_mobile: str = Body(...),
    file_name: str = Body(...),
    file_data: str = Body(...)  # Base64 encoded file
):
    sender_ref = db.collection('users').document(sender_mobile)
    receiver_ref = db.collection('users').document(receiver_mobile)
    if not sender_ref.get().exists or not receiver_ref.get().exists:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Store file message with base64 data (for small files) or file info
    message_data = {
        'sender_mobile': sender_mobile,
        'receiver_mobile': receiver_mobile,
        'content': f"📎 {file_name}",
        'file_name': file_name,
        'file_data': file_data,  # Base64 encoded
        'is_file': True,
        'timestamp': firestore.SERVER_TIMESTAMP
    }
    db.collection('messages').add(message_data)
    return {"status": "File sent", "file_name": file_name}

@app.get("/messages/{mobile}")
def get_messages(mobile: str):
    # Get sent messages
    sent_query = db.collection('messages').where('sender_mobile', '==', mobile)
    sent_messages = sent_query.stream()
    
    # Get received messages
    received_query = db.collection('messages').where('receiver_mobile', '==', mobile)
    received_messages = received_query.stream()
    
    messages = []
    for msg in sent_messages:
        msg_data = msg.to_dict()
        msg_data['id'] = msg.id
        # Convert Firestore timestamp to string
        if 'timestamp' in msg_data and msg_data['timestamp']:
            try:
                ts = msg_data['timestamp']
                msg_data['timestamp'] = ts.strftime('%Y-%m-%d %H:%M:%S') if hasattr(ts, 'strftime') else str(ts)
            except:
                msg_data['timestamp'] = ''
        else:
            msg_data['timestamp'] = ''
        messages.append(msg_data)
    for msg in received_messages:
        msg_data = msg.to_dict()
        msg_data['id'] = msg.id
        # Convert Firestore timestamp to string
        if 'timestamp' in msg_data and msg_data['timestamp']:
            try:
                ts = msg_data['timestamp']
                msg_data['timestamp'] = ts.strftime('%Y-%m-%d %H:%M:%S') if hasattr(ts, 'strftime') else str(ts)
            except:
                msg_data['timestamp'] = ''
        else:
            msg_data['timestamp'] = ''
        messages.append(msg_data)
    
    # Sort by timestamp (oldest first for chat history)
    messages.sort(key=lambda x: x.get('timestamp', ''))
    
    print(f"Returning {len(messages)} messages for mobile: {mobile}")
    for msg in messages[:5]:  # Print first 5 for debugging
        print(f"  {msg.get('sender_mobile')} -> {msg.get('receiver_mobile')}: {msg.get('content')[:30]}...")
    
    return messages

# WebSocket for real-time messaging
active_connections = {}

@app.websocket("/ws/{mobile}")
async def websocket_endpoint(websocket: WebSocket, mobile: str):
    await websocket.accept()
    active_connections[mobile] = websocket
    try:
        while True:
            data = await websocket.receive_text()
            message_data = json.loads(data)
            receiver_mobile = message_data.get("receiver_mobile")
            content = message_data.get("content")
            
            # Save to Firestore
            db.collection('messages').add({
                'sender_mobile': mobile,
                'receiver_mobile': receiver_mobile,
                'content': content,
                'timestamp': firestore.SERVER_TIMESTAMP
            })
            
            # Send to receiver if connected
            if receiver_mobile in active_connections:
                await active_connections[receiver_mobile].send_text(json.dumps({
                    "sender_mobile": mobile,
                    "content": content
                }))
    except WebSocketDisconnect:
        del active_connections[mobile]