package com.example.chatapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ChatApp extends JFrame {

    private JSplitPane splitPane;
    private JPanel contactListPanel;
    private DefaultListModel<Contact> contacts = new DefaultListModel<>();
    private JList<Contact> contactList;
    private Map<String, ChatPanel> chatPanels = new HashMap<>();
    private ChatPanel currentChatPanel;
    private JTextField searchField;
    private static final Color WHATSAPP_GREEN = new Color(37, 211, 102);
    private static final Color TELEGRAM_BLUE = new Color(0, 136, 204);
    private static final Color BG_COLOR = new Color(230, 221, 212);
    private static final Color SIDEBAR_BG = Color.WHITE;
    private static final String[] EMOJIS = {"😊", "😂", "❤️", "👍", "🎉", "🔥", "😍", "😢", "🤔", "👋", "✅", "📷", "🎵", "⭐", "💯"};
    private Map<String, List<Message>> messageHistory = new HashMap<>();
    private BackendClient backendClient;
    private Map<String, String> contactMobileMap = new HashMap<>(); // Maps contact name to mobile number
    private javax.swing.Timer messageRefreshTimer;
    private Set<String> loadedMessageIds = new HashSet<>();

    public ChatApp() {
        // Show login dialog first
        showLoginDialog();
        
        setTitle("Chat App - " + (backendClient.getCurrentUsername().isEmpty() ? 
            backendClient.getCurrentUserMobile() : backendClient.getCurrentUsername()));
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Load existing messages immediately after login
        loadMessagesFromBackend();
        
        // Start message refresh timer (check for new messages every 3 seconds)
        startMessageRefreshTimer();

        // Header bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(WHATSAPP_GREEN);
        headerBar.setPreferredSize(new Dimension(0, 60));
        JLabel appTitle = new JLabel("  Chat App");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        appTitle.setForeground(Color.WHITE);
        headerBar.add(appTitle, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(WHATSAPP_GREEN);
        
        JButton settingsBtn = new JButton("⚙ Settings");
        settingsBtn.setForeground(Color.WHITE);
        settingsBtn.setBackground(WHATSAPP_GREEN);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setFocusPainted(false);
        settingsBtn.addActionListener(e -> showSettingsDialog());
        
        JButton addContactBtn = new JButton("+ Add Contact");
        addContactBtn.setForeground(Color.WHITE);
        addContactBtn.setBackground(WHATSAPP_GREEN);
        addContactBtn.setBorderPainted(false);
        addContactBtn.setFocusPainted(false);
        addContactBtn.addActionListener(e -> showAddContactDialog());
        
        buttonPanel.add(settingsBtn);
        buttonPanel.add(addContactBtn);
        headerBar.add(buttonPanel, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);


        // Contact list with search
        contactListPanel = new JPanel(new BorderLayout());
        contactListPanel.setBackground(SIDEBAR_BG);
        contactListPanel.setPreferredSize(new Dimension(280, 0));

        searchField = new JTextField("Search...");
        searchField.setBorder(new EmptyBorder(10, 10, 10, 10));
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterContacts(searchField.getText());
            }
        });
        contactListPanel.add(searchField, BorderLayout.NORTH);

        // Contact list (start empty)
        contactList = new JList<>(contacts);
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactList.setCellRenderer(new ContactRenderer());
        contactList.setBackground(SIDEBAR_BG);
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Contact selected = contactList.getSelectedValue();
                if (selected != null) {
                    showChat(selected.name);
                }
            }
        });

        JScrollPane contactScroll = new JScrollPane(contactList);
        contactScroll.setBorder(null);
        contactListPanel.add(contactScroll, BorderLayout.CENTER);

        // Initial empty chat panel
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        welcomePanel.setBackground(BG_COLOR);
        JLabel welcomeLabel = new JLabel("Select a contact or add a new one to start chatting");
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        welcomeLabel.setForeground(Color.GRAY);
        welcomePanel.add(welcomeLabel);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contactListPanel, welcomePanel);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(1);
        add(splitPane, BorderLayout.CENTER);
    }

    private void showChat(String contact) {
        ChatPanel panel = chatPanels.computeIfAbsent(contact, k -> new ChatPanel(k, this));
        
        // Load message history
        List<Message> history = messageHistory.get(contact);
        if (history != null && panel.getMessageCount() == 0) {
            for (Message msg : history) {
                panel.addMessage(msg.type, msg.time, msg.text, msg.delivered, msg.isFile, msg.fileName, msg.fileData);
            }
        }
        
        splitPane.setRightComponent(panel);
        currentChatPanel = panel;
        revalidate();
        repaint();
    }

    private void filterContacts(String searchText) {
        if (searchText.equals("Search...") || searchText.isEmpty()) {
            contactList.clearSelection();
            return;
        }
        // Simple filter - could be enhanced
        String lower = searchText.toLowerCase();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).name.toLowerCase().contains(lower)) {
                contactList.setSelectedIndex(i);
                contactList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    private void saveMessage(String contact, Message msg) {
        messageHistory.computeIfAbsent(contact, k -> new ArrayList<>()).add(msg);
    }

    private static class Message {
        String type;
        String time;
        String text;
        boolean delivered;
        boolean isFile;
        String fileName;
        String fileData;

        Message(String type, String time, String text, boolean delivered, boolean isFile, String fileName, String fileData) {
            this.type = type;
            this.time = time;
            this.text = text;
            this.delivered = delivered;
            this.isFile = isFile;
            this.fileName = fileName;
            this.fileData = fileData;
        }
    }

    private static class Contact {
        String name;
        String status;
        boolean online;
        String mobile;

        Contact(String name, String status, boolean online, String mobile) {
            this.name = name;
            this.status = status;
            this.online = online;
            this.mobile = mobile;
        }
    }

    private static class ContactRenderer extends JPanel implements ListCellRenderer<Contact> {
        private JLabel nameLabel;
        private JLabel statusLabel;
        private JPanel statusDot;

        public ContactRenderer() {
            setLayout(new BorderLayout(10, 5));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            setOpaque(true);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            nameLabel = new JLabel();
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            statusLabel = new JLabel();
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            statusLabel.setForeground(Color.GRAY);

            textPanel.add(nameLabel);
            textPanel.add(statusLabel);

            statusDot = new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.fillOval(0, 0, 10, 10);
                }
            };
            statusDot.setPreferredSize(new Dimension(10, 10));
            statusDot.setOpaque(false);

            add(statusDot, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Contact> list, Contact contact, int index, boolean isSelected, boolean cellHasFocus) {
            // Create HTML to show name in normal font and mobile in smaller font
            String displayText;
            if (contact.mobile != null && !contact.name.equals(contact.mobile)) {
                // Show both name and mobile if they're different
                displayText = "<html><b>" + contact.name + "</b><br/>" +
                            "<font size='2' color='gray'>" + contact.mobile + "</font></html>";
            } else {
                // Show only name if mobile is same as name (auto-added contacts)
                displayText = "<html><b>" + contact.name + "</b></html>";
            }
            nameLabel.setText(displayText);
            statusLabel.setText(contact.status);
            statusDot.setBackground(contact.online ? WHATSAPP_GREEN : Color.GRAY);

            if (isSelected) {
                setBackground(new Color(240, 240, 240));
            } else {
                setBackground(Color.WHITE);
            }
            return this;
        }
    }

    private static class ChatPanel extends JPanel {
        private JPanel messagePanel;
        private JTextField inputField;
        private JButton sendButton;
        private JScrollPane scrollPane;
        private String contactName;
        private ChatApp parent;
        private int messageCount = 0;
        private JLabel contactLabel;

        public ChatPanel(String contactName, ChatApp parent) {
            this.contactName = contactName;
            this.parent = parent;
            setLayout(new BorderLayout());

            // Chat header
            JPanel chatHeader = new JPanel(new BorderLayout());
            chatHeader.setBackground(new Color(245, 245, 245));
            chatHeader.setPreferredSize(new Dimension(0, 50));
            chatHeader.setBorder(new EmptyBorder(10, 15, 10, 15));
            
            contactLabel = new JLabel(contactName);
            contactLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            chatHeader.add(contactLabel, BorderLayout.WEST);
            
            // Fetch and update username from backend
            new Thread(() -> {
                String mobile = parent.contactMobileMap.get(contactName);
                if (mobile != null) {
                    Map<String, String> userInfo = parent.backendClient.getUserInfo(mobile);
                    String username = userInfo.get("username");
                    if (username != null && !username.isEmpty() && !username.equals(mobile)) {
                        SwingUtilities.invokeLater(() -> contactLabel.setText(username));
                    }
                }
            }).start();
            
            JLabel statusLabel = new JLabel("online");
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            statusLabel.setForeground(Color.GRAY);
            chatHeader.add(statusLabel, BorderLayout.CENTER);
            
            add(chatHeader, BorderLayout.NORTH);

            // Message panel with background
            messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(BG_COLOR);
            messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            scrollPane = new JScrollPane(messagePanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(null);
            scrollPane.getViewport().setBackground(BG_COLOR);
            add(scrollPane, BorderLayout.CENTER);

            // Input panel
            JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
            inputPanel.setBackground(Color.WHITE);
            inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            inputField = new JTextField();
            inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(8, 10, 8, 10)
            ));
            
            sendButton = new JButton("Send");
            sendButton.setBackground(TELEGRAM_BLUE);
            sendButton.setForeground(Color.WHITE);
            sendButton.setFocusPainted(false);
            sendButton.setBorderPainted(false);
            sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            sendButton.setPreferredSize(new Dimension(80, 40));

            ActionListener sendAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String text = inputField.getText().trim();
                    if (!text.isEmpty()) {
                        // Clear input field immediately for better UX
                        inputField.setText("");
                        
                        // Send message to backend - don't add to UI yet to prevent duplicates
                        String receiverMobile = parent.contactMobileMap.get(contactName);
                        if (receiverMobile != null && !receiverMobile.isEmpty()) {
                            new Thread(() -> {
                                boolean success = parent.backendClient.sendMessage(receiverMobile, text);
                                if (!success) {
                                    SwingUtilities.invokeLater(() -> {
                                        JOptionPane.showMessageDialog(ChatPanel.this, 
                                            "Failed to send message to backend!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                                    });
                                } else {
                                    System.out.println("Message sent successfully to " + receiverMobile);
                                    // Trigger immediate refresh to show the message from backend
                                    parent.loadMessagesFromBackend();
                                }
                            }).start();
                        } else {
                            JOptionPane.showMessageDialog(ChatPanel.this, 
                                "No mobile number found for " + contactName + ".\nPlease add their mobile number.", 
                                "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            };
            
            sendButton.addActionListener(sendAction);
            inputField.addActionListener(sendAction);

            // Emoji picker button
            JButton emojiBtn = new JButton("😊");
            emojiBtn.setFont(new Font("SansSerif", Font.PLAIN, 18));
            emojiBtn.setBorderPainted(false);
            emojiBtn.setFocusPainted(false);
            emojiBtn.setBackground(Color.WHITE);
            emojiBtn.addActionListener(e -> showEmojiPicker());
            
            // File attachment button
            JButton attachBtn = new JButton("📎");
            attachBtn.setFont(new Font("SansSerif", Font.PLAIN, 18));
            attachBtn.setBorderPainted(false);
            attachBtn.setFocusPainted(false);
            attachBtn.setBackground(Color.WHITE);
            attachBtn.addActionListener(e -> attachFile());

            JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            leftButtons.setBackground(Color.WHITE);
            leftButtons.add(emojiBtn);
            leftButtons.add(attachBtn);

            inputPanel.add(leftButtons, BorderLayout.WEST);
            inputPanel.add(inputField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);

            add(inputPanel, BorderLayout.SOUTH);
        }

        public void addMessage(String type, String time, String text, boolean delivered, boolean isFile, String fileName, String fileData) {
            MessageBubble bubble = new MessageBubble(type, time, text, delivered, isFile, fileName, fileData);
            
            JPanel wrapper = new JPanel();
            wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
            wrapper.setOpaque(false);
            
            if ("sent".equals(type)) {
                wrapper.add(Box.createHorizontalGlue());
                wrapper.add(bubble);
            } else {
                wrapper.add(bubble);
                wrapper.add(Box.createHorizontalGlue());
            }
            
            messagePanel.add(wrapper);
            messagePanel.add(Box.createVerticalStrut(8));
            messagePanel.revalidate();
            messagePanel.repaint();
            messageCount++;
            
            // Auto-scroll to bottom when new message is added
            scrollToBottom();
        }

        public int getMessageCount() {
            return messageCount;
        }

        private void showEmojiPicker() {
            JPopupMenu emojiMenu = new JPopupMenu();
            emojiMenu.setLayout(new GridLayout(3, 5, 5, 5));
            
            for (String emoji : EMOJIS) {
                JButton btn = new JButton(emoji);
                btn.setFont(new Font("SansSerif", Font.PLAIN, 20));
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                btn.addActionListener(e -> {
                    inputField.setText(inputField.getText() + emoji);
                    emojiMenu.setVisible(false);
                    inputField.requestFocus();
                });
                emojiMenu.add(btn);
            }
            
            emojiMenu.show(inputField, 0, -emojiMenu.getPreferredSize().height);
        }

        private void attachFile() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a file to send");
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Images, Documents, Videos", "jpg", "png", "gif", "pdf", "doc", "docx", "txt", "mp4", "avi");
            fileChooser.setFileFilter(filter);
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String fileName = selectedFile.getName();
                long fileSize = selectedFile.length();
                
                // Check file size limit (5MB)
                if (fileSize > 5 * 1024 * 1024) {
                    JOptionPane.showMessageDialog(this, 
                        "File too large! Maximum size is 5MB.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Send file to backend
                String receiverMobile = parent.contactMobileMap.get(contactName);
                if (receiverMobile != null && !receiverMobile.isEmpty()) {
                    new Thread(() -> {
                        try {
                            // Read file and convert to Base64
                            byte[] fileBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                            
                            boolean success = parent.backendClient.sendFile(receiverMobile, fileName, base64Data);
                            
                            if (!success) {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(ChatPanel.this, 
                                        "Failed to send file!", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                });
                            } else {
                                System.out.println("File sent successfully: " + fileName);
                                // Trigger immediate refresh to show the file message
                                parent.loadMessagesFromBackend();
                            }
                        } catch (Exception e) {
                            System.err.println("Error reading file: " + e.getMessage());
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(ChatPanel.this, 
                                    "Error reading file: " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No mobile number found for " + contactName + ".\nPlease add their mobile number.", 
                        "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            else return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }

        private String getAutoReply(String message) {
            String lower = message.toLowerCase();
            if (lower.contains("hello") || lower.contains("hi")) return "Hey! How can I help you?";
            if (lower.contains("how are you")) return "I'm doing great, thanks for asking!";
            if (lower.contains("bye")) return "Goodbye! Talk to you later!";
            if (lower.contains("?")) return "That's a good question! Let me think...";
            return "Got your message: \"" + message + "\"";
        }

        private void scrollToBottom() {
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
    }

    private static class MessageBubble extends JPanel {
        private String type;
        private String time;
        private String text;
        private boolean delivered;
        private boolean isFile;
        private String fileName;
        private String fileData; // Base64 encoded file data

        public MessageBubble(String type, String time, String text, boolean delivered, boolean isFile, String fileName, String fileData) {
            this.type = type;
            this.time = time;
            this.text = text;
            this.delivered = delivered;
            this.isFile = isFile;
            this.fileName = fileName;
            this.fileData = fileData;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            
            if (isFile) {
                setToolTipText("Click to download: " + fileName);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // Add click listener for file download
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        downloadFile();
                    }
                });
            }
        }
        
        private void downloadFile() {
            if (fileData == null || fileData.isEmpty()) {
                JOptionPane.showMessageDialog(this, "File data not available", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new java.io.File(fileName));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                
                try {
                    // Decode Base64 data
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(fileData);
                    
                    // Write to file
                    java.nio.file.Files.write(selectedFile.toPath(), decodedBytes);
                    
                    JOptionPane.showMessageDialog(this, 
                        "File downloaded successfully!\n" + selectedFile.getAbsolutePath(), 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to download file: " + ex.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = 15;
            int padding = 12;

            Color bubbleColor = "sent".equals(type) ? new Color(220, 248, 198) : Color.WHITE;
            Color textColor = "sent".equals(type) ? new Color(0, 0, 0) : Color.BLACK;
            
            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 20));
            RoundRectangle2D shadow = new RoundRectangle2D.Float(padding + 2, 7, width - padding * 2, height - 12, arc, arc);
            g2.fill(shadow);
            
            // Draw bubble
            g2.setColor(bubbleColor);
            RoundRectangle2D bubble = new RoundRectangle2D.Float(padding, 5, width - padding * 2, height - 12, arc, arc);
            g2.fill(bubble);
            
            // Draw border
            g2.setColor(new Color(200, 200, 200));
            g2.draw(bubble);

            // Draw file icon or text
            g2.setColor(textColor);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            int textY = 22;
            
            if (isFile) {
                // Draw file icon
                g2.setFont(new Font("SansSerif", Font.PLAIN, 30));
                g2.drawString("📄", padding + 10, textY + 15);
                
                // Draw file info
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(fileName != null ? fileName : "File", padding + 50, textY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.setColor(new Color(100, 100, 100));
                g2.drawString(text, padding + 50, textY + 15);
            } else {
                // Draw regular text
                String[] lines = text.split("\n");
                for (String line : lines) {
                    g2.drawString(line, padding + 10, textY);
                    textY += fm.getHeight();
                }
            }

            // Draw timestamp and checkmarks
            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String timeStr = time;
            if ("sent".equals(type) && delivered) {
                timeStr = time + " ✓✓";
            }
            FontMetrics timeFm = g2.getFontMetrics();
            g2.drawString(timeStr, width - padding - timeFm.stringWidth(timeStr) - 5, height - 8);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = new Font("SansSerif", Font.PLAIN, 13);
            FontMetrics fm = getFontMetrics(font);
            
            int bubbleWidth;
            int bubbleHeight;
            
            if (isFile) {
                bubbleWidth = 300;
                bubbleHeight = 70;
            } else {
                String[] lines = text.split("\n");
                int maxWidth = 0;
                for (String line : lines) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(line));
                }
                
                bubbleWidth = Math.max(maxWidth + 80, 150);
                bubbleWidth = Math.min(bubbleWidth, 400);
                bubbleHeight = fm.getHeight() * lines.length + 35;
            }
            
            return new Dimension(bubbleWidth, bubbleHeight);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(500, pref.height);
        }
    }

    private void showLoginDialog() {
        backendClient = new BackendClient();
        
        JDialog loginDialog = new JDialog(this, "Login / Register", true);
        loginDialog.setSize(400, 250);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel mobileLabel = new JLabel("Mobile Number:");
        JTextField mobileField = new JTextField(15);
        JLabel usernameLabel = new JLabel("Username (optional):");
        JTextField usernameField = new JTextField(15);
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(mobileLabel, gbc);
        gbc.gridx = 1;
        mainPanel.add(mobileField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        
        loginBtn.setBackground(TELEGRAM_BLUE);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        
        registerBtn.setBackground(WHATSAPP_GREEN);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        
        loginBtn.addActionListener(e -> {
            String mobile = mobileField.getText().trim();
            if (mobile.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter mobile number!");
                return;
            }
            
            if (backendClient.login(mobile)) {
                JOptionPane.showMessageDialog(loginDialog, "Login successful!");
                loginDialog.dispose();
                loadMessagesFromBackend();
            } else {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Login failed! User not found. Please register first.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        registerBtn.addActionListener(e -> {
            String mobile = mobileField.getText().trim();
            String username = usernameField.getText().trim();
            
            if (mobile.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter mobile number!");
                return;
            }
            
            if (backendClient.register(mobile, username.isEmpty() ? mobile : username)) {
                JOptionPane.showMessageDialog(loginDialog, "Registration successful!");
                loginDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Registration failed! Mobile number might be already registered.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        
        loginDialog.add(mainPanel, BorderLayout.CENTER);
        loginDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        loginDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loginDialog.setVisible(true);
    }

    private void loadMessagesFromBackend() {
        new Thread(() -> {
            List<Map<String, String>> messages = backendClient.getMessages();
            System.out.println("Loaded " + messages.size() + " messages from backend");
            System.out.println("Currently have " + loadedMessageIds.size() + " message IDs in cache");
            
            SwingUtilities.invokeLater(() -> {
                int newMessagesCount = 0;
                int skippedCount = 0;
                for (int i = 0; i < messages.size(); i++) {
                    Map<String, String> msg = messages.get(i);
                    String messageId = msg.get("id");
                    String senderMobile = msg.get("sender_mobile");
                    String receiverMobile = msg.get("receiver_mobile");
                    String content = msg.get("content");
                    String timestamp = msg.get("timestamp");
                    
                    System.out.println("Message " + (i+1) + ": ID=" + messageId + ", from=" + senderMobile + " to=" + receiverMobile);
                    
                    // Skip if already loaded
                    if (loadedMessageIds.contains(messageId)) {
                        skippedCount++;
                        System.out.println("  -> SKIPPED (duplicate)");
                        continue;
                    }
                    
                    boolean isSent = senderMobile.equals(backendClient.getCurrentUserMobile());
                    String contactMobile = isSent ? receiverMobile : senderMobile;
                    
                    // Check if contact exists, if not add it
                    String contactName = findContactByMobile(contactMobile);
                    if (contactName == null) {
                        // Fetch username from backend
                        Map<String, String> userInfo = backendClient.getUserInfo(contactMobile);
                        String username = userInfo.get("username");
                        contactName = (username != null && !username.isEmpty() && !username.equals(contactMobile)) 
                                      ? username : contactMobile;
                        
                        contacts.addElement(new Contact(contactName, "Chat available", true, contactMobile));
                        contactMobileMap.put(contactName, contactMobile);
                        System.out.println("  -> Auto-added contact: " + contactName + " (" + contactMobile + ")");
                    }
                    
                    // Get or create chat panel
                    ChatPanel panel = chatPanels.computeIfAbsent(contactName, k -> new ChatPanel(k, this));
                    
                    loadedMessageIds.add(messageId);
                    newMessagesCount++;
                    
                    // Format timestamp to show only time (HH:mm)
                    String displayTime = "";
                    if (timestamp != null && !timestamp.isEmpty()) {
                        try {
                            // Parse timestamp format: "2025-11-06 14:30:45"
                            String[] parts = timestamp.split(" ");
                            if (parts.length > 1) {
                                String timePart = parts[1]; // "14:30:45"
                                String[] timeParts = timePart.split(":");
                                if (timeParts.length >= 2) {
                                    displayTime = timeParts[0] + ":" + timeParts[1]; // "14:30"
                                }
                            }
                        } catch (Exception e) {
                            displayTime = new SimpleDateFormat("HH:mm").format(new Date());
                        }
                    } else {
                        displayTime = new SimpleDateFormat("HH:mm").format(new Date());
                    }
                    
                    // Check if this is a file message
                    String isFileStr = msg.get("is_file");
                    boolean isFile = "true".equalsIgnoreCase(isFileStr) || "True".equalsIgnoreCase(isFileStr);
                    String fileName = msg.get("file_name");
                    String fileData = msg.get("file_data");
                    
                    // Add message to chat panel
                    panel.addMessage(isSent ? "sent" : "received", displayTime, content, true, isFile, fileName, fileData);
                    System.out.println("  -> ADDED to chat" + (isFile ? " (FILE: " + fileName + ")" : ""));
                }
                System.out.println("Summary: Added " + newMessagesCount + " new messages, skipped " + skippedCount + " duplicates");
                System.out.println("Total message IDs in cache now: " + loadedMessageIds.size());
            });
        }).start();
    }
    
    private void startMessageRefreshTimer() {
        messageRefreshTimer = new javax.swing.Timer(3000, e -> {
            loadMessagesFromBackend();
        });
        messageRefreshTimer.start();
    }

    private String findContactByMobile(String mobile) {
        for (Map.Entry<String, String> entry : contactMobileMap.entrySet()) {
            if (entry.getValue().equals(mobile)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showAddContactDialog() {
        JDialog addContactDialog = new JDialog(this, "Add Contact", true);
        addContactDialog.setSize(400, 200);
        addContactDialog.setLocationRelativeTo(this);
        addContactDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel nameLabel = new JLabel("Contact Name:");
        JTextField nameField = new JTextField(20);
        JLabel mobileLabel = new JLabel("Mobile Number:");
        JTextField mobileField = new JTextField(20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(mobileLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(mobileField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");
        
        addBtn.setBackground(WHATSAPP_GREEN);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String mobile = mobileField.getText().trim();
            
            if (name.isEmpty() || mobile.isEmpty()) {
                JOptionPane.showMessageDialog(addContactDialog, 
                    "Please fill in all fields!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean exists = false;
            for (int i = 0; i < contacts.size(); i++) {
                if (contacts.get(i).name.equals(name)) {
                    exists = true;
                    break;
                }
            }
            
            if (exists) {
                JOptionPane.showMessageDialog(addContactDialog, 
                    "Contact already exists!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            contacts.addElement(new Contact(name, "Available", true, mobile));
            chatPanels.put(name, new ChatPanel(name, this));
            contactMobileMap.put(name, mobile);
            
            JOptionPane.showMessageDialog(addContactDialog, 
                "Contact added successfully!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            addContactDialog.dispose();
        });
        
        cancelBtn.addActionListener(e -> addContactDialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        addContactDialog.add(inputPanel, BorderLayout.CENTER);
        addContactDialog.add(buttonPanel, BorderLayout.SOUTH);
        addContactDialog.setVisible(true);
    }

    private void showSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Settings", true);
        settingsDialog.setSize(450, 300);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel mobileLabel = new JLabel("Mobile:");
        JLabel mobileValue = new JLabel(backendClient.getCurrentUserMobile());
        mobileValue.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(
            backendClient.getCurrentUsername().isEmpty() ? 
            backendClient.getCurrentUserMobile() : 
            backendClient.getCurrentUsername(), 20);
        
        JLabel aboutLabel = new JLabel("About:");
        JTextArea aboutField = new JTextArea(3, 20);
        aboutField.setLineWrap(true);
        aboutField.setWrapStyleWord(true);
        aboutField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        aboutField.setText(backendClient.getCurrentUserAbout());
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(mobileLabel, gbc);
        gbc.gridx = 1;
        mainPanel.add(mobileValue, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(usernameLabel, gbc);
        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(aboutLabel, gbc);
        gbc.gridx = 1;
        JScrollPane aboutScroll = new JScrollPane(aboutField);
        mainPanel.add(aboutScroll, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveBtn = new JButton("Save Changes");
        JButton closeBtn = new JButton("Close");
        
        saveBtn.setBackground(TELEGRAM_BLUE);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        
        saveBtn.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();
            String about = aboutField.getText().trim();
            
            if (about.isEmpty()) {
                about = "Hey there! I am using Chat App";
            }
            
            if (!newUsername.isEmpty()) {
                final String finalAbout = about;
                new Thread(() -> {
                    boolean success = backendClient.register(
                        backendClient.getCurrentUserMobile(), 
                        newUsername,
                        finalAbout);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            setTitle("Chat App - " + newUsername);
                            JOptionPane.showMessageDialog(settingsDialog, 
                                "Settings saved successfully!", 
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(settingsDialog, 
                                "Note: Username and about saved locally.", 
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                }).start();
            }
        });
        
        closeBtn.addActionListener(e -> settingsDialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);
        
        settingsDialog.add(mainPanel, BorderLayout.CENTER);
        settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatApp().setVisible(true);
        });
    }
}