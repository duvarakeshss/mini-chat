# Chat App

## Prerequisites

- JDK 21 or higher

### Installing JDK 21

1. Download JDK 21 from [Eclipse Adoptium](https://adoptium.net/temurin/releases/?version=21)
2. Install the JDK
3. Set environment variables:
   - `JAVA_HOME` to the JDK installation directory
   - Add `%JAVA_HOME%\bin` to your PATH

Verify installation:

```bash
java -version
# Should show Java 21.x.x
```

## Setup

### Backend Setup

1. Navigate to the backend directory:

```bash
cd backend
```

2. Create a virtual environment:

```bash
python -m venv venv
```

3. Activate the virtual environment:

   - On Windows:
     ```cmd
     venv\Scripts\activate
     ```
   - On macOS/Linux:
     ```bash
     source venv/bin/activate
     ```

4. Create a `.env` file in the backend directory with your Firebase configuration:

```bash
# Example .env content
FIREBASE_TYPE=service_account
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY_ID=your-private-key-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com
FIREBASE_CLIENT_ID=your-client-id
FIREBASE_AUTH_URI=https://accounts.google.com/o/oauth2/auth
FIREBASE_TOKEN_URI=https://oauth2.googleapis.com/token
FIREBASE_AUTH_PROVIDER_X509_CERT_URL=https://www.googleapis.com/oauth2/v1/certs
FIREBASE_CLIENT_X509_CERT_URL=https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx%40your-project.iam.gserviceaccount.com
FIREBASE_UNIVERSE_DOMAIN=googleapis.com
```

Replace the values with your actual Firebase service account credentials.

5. Install the required packages:

```bash
pip install -r requirements.txt
```

6. Start the backend server:

```bash
uvicorn main:app --reload
```

### Build

```bash
javac -source 21 -target 21 App/src/main/java/com/example/chatapp/*.java -d App/target/classes
```

Or using Maven:

```bash
cd App
mvn clean compile
```

### Run

```bash
java -cp App/target/classes com.example.chatapp.ChatApp
```

Or use VS Code task: Ctrl+Shift+P > Tasks: Run Task > run

## Features

### UI Enhancements

- Modern sidebar with contact list, status, and online indicators
- Search bar at the top of contacts
- Custom header bar with app branding and quick actions
- Chat headers showing contact name and online status
- Rich message bubbles with shadows, rounded corners, and proper alignment
- WhatsApp-style green theme and Telegram-style blue accents
- Chat background with subtle color (like WhatsApp's beige pattern)

### Messaging Features

- Sent messages (light green, right-aligned) with delivery checkmarks ✓✓
- Received messages (white, left-aligned)
- Timestamps on all messages
- Emoji support with quick emoji button 😊
- Attachment button 📎 for future file sharing
- Press Enter to send messages
- Auto-scroll to latest message

### Contact Management

- Multiple contacts with online/offline status
- Add new contacts via header button
- Contact status messages
- Visual online indicators (green dots)

## Usage

1. Select a contact from the left sidebar to view their chat
2. Type a message in the input field (or click 😊 for emoji)
3. Press Enter or click Send to send the message
4. Messages appear with timestamps and delivery status
5. Click "+ Add Contact" to add new contacts
6. Use the search bar to filter contacts (coming soon)

## UI Design

The app combines the best of WhatsApp and Telegram:

- Clean, modern Material Design-like interface
- Smooth rounded message bubbles with shadows
- Professional typography and spacing
