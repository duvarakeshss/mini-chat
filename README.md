# Chat App

## Features

### UI Enhancements

- **Modern sidebar** with contact list, status, and online indicators
- **Search bar** at the top of contacts
- **Custom header bar** with app branding and quick actions
- **Chat headers** showing contact name and online status
- **Rich message bubbles** with shadows, rounded corners, and proper alignment
- **WhatsApp-style green** theme and **Telegram-style blue** accents
- **Chat background** with subtle color (like WhatsApp's beige pattern)

### Messaging Features

- Sent messages (light green, right-aligned) with delivery checkmarks ✓✓
- Received messages (white, left-aligned)
- Timestamps on all messages
- **Emoji support** with quick emoji button 😊
- **Attachment button** 📎 for future file sharing
- Press Enter to send messages
- Auto-scroll to latest message

### Contact Management

- Multiple contacts with online/offline status
- Add new contacts via header button
- Contact status messages
- Visual online indicators (green dots)

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

## Build

```bash
javac -source 21 -target 21 App/src/main/java/com/example/chatapp/*.java -d App/target/classes
```

Or using Maven:

```bash
cd App
mvn clean compile
```

## Run

```bash
java -cp App/target/classes com.example.chatapp.ChatApp
```

Or use VS Code task: Ctrl+Shift+P > Tasks: Run Task > run

## Usage

1. Select a contact from the left sidebar to view their chat
2. Type a message in the input field (or click 😊 for emoji)
3. Press Enter or click Send to send the message
4. Messages appear with timestamps and delivery status
5. Click "+ Add Contact" to add new contacts
6. Use the search bar to filter contacts (coming soon)

## UI Design

The app combines the best of WhatsApp and Telegram:

- WhatsApp-inspired green header and light green sent bubbles
- Telegram-inspired blue send button
- Clean, modern Material Design-like interface
- Smooth rounded message bubbles with shadows
- Professional typography and spacing
