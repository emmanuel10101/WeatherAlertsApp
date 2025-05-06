import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class WeatherAlertsApp extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JTextField stateCodeField;
    private JPanel alertsPanel;
    private JScrollPane scrollPane;
    
    // Icons for different alert types (paths to resources)
    private static final Map<String, String> ALERT_ICONS = new HashMap<>();
    static {
        ALERT_ICONS.put("Tornado", "/icons/tornado.png");
        ALERT_ICONS.put("Severe Thunderstorm", "/icons/thunderstorm.png");
        ALERT_ICONS.put("Flash Flood", "/icons/flood.png");
        ALERT_ICONS.put("Flood", "/icons/flood.png");
        ALERT_ICONS.put("Winter Storm", "/icons/winter.png");
        ALERT_ICONS.put("Blizzard", "/icons/snow.png");
        ALERT_ICONS.put("Wind", "/icons/wind.png");
        ALERT_ICONS.put("Heat", "/icons/heat.png");
        ALERT_ICONS.put("Fire", "/icons/fire.png");
        ALERT_ICONS.put("Hurricane", "/icons/hurricane.png");
        // Add more mappings as needed
    }
    
    // Severity colors
    private static final Map<String, Color> SEVERITY_COLORS = new HashMap<>();
    static {
        SEVERITY_COLORS.put("Extreme", new Color(139, 0, 0)); // Dark Red
        SEVERITY_COLORS.put("Severe", new Color(255, 0, 0)); // Red
        SEVERITY_COLORS.put("Moderate", new Color(255, 165, 0)); // Orange
        SEVERITY_COLORS.put("Minor", new Color(255, 255, 0)); // Yellow
        SEVERITY_COLORS.put("Unknown", new Color(128, 128, 128)); // Gray
    }

    public WeatherAlertsApp() {
        setTitle("Weather Alerts");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);

        // Create input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("State Code:"));
        stateCodeField = new JTextField(2);
        stateCodeField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(stateCodeField);
        JButton runButton = new JButton("Get Alerts");
        runButton.addActionListener(this);
        inputPanel.add(runButton);
        getContentPane().add(inputPanel, BorderLayout.NORTH);

        // Create panel for displaying alerts
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        scrollPane = new JScrollPane(alertsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        
        // Initial message
        JLabel initialMessage = new JLabel("Enter a 2-letter state code and click \"Get Alerts\"");
        initialMessage.setFont(new Font("Arial", Font.PLAIN, 16));
        initialMessage.setHorizontalAlignment(SwingConstants.CENTER);
        initialMessage.setBorder(new EmptyBorder(20, 20, 20, 20));
        alertsPanel.add(initialMessage);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherAlertsApp());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Validate state code
        String stateCode = stateCodeField.getText().trim().toUpperCase();
        if (stateCode.isEmpty() || stateCode.length() != 2) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid 2-letter state code", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear current alerts and show loading message
        alertsPanel.removeAll();
        JLabel loadingLabel = new JLabel("Fetching alerts for " + stateCode + "...");
        loadingLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        alertsPanel.add(loadingLabel);
        alertsPanel.revalidate();
        alertsPanel.repaint();
        
        // Use a separate thread to prevent UI freezing
        new Thread(() -> {
            try {
                String urlString = "https://api.weather.gov/alerts/active?area=" + stateCode;
                String responseData = fetchDataFromURL(urlString);

                if (responseData == null) {
                    SwingUtilities.invokeLater(() -> {
                        alertsPanel.removeAll();
                        JLabel errorLabel = new JLabel("Failed to retrieve data from the server.");
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        alertsPanel.add(errorLabel);
                        alertsPanel.revalidate();
                        alertsPanel.repaint();
                    });
                    return;
                }

                try {
                    // Parse JSON using custom parser
                    List<Map<String, String>> alerts = parseAlerts(responseData);

                    SwingUtilities.invokeLater(() -> {
                        alertsPanel.removeAll();
                        
                        if (alerts.isEmpty()) {
                            JLabel noAlertsLabel = new JLabel("No active alerts found for " + stateCode + ".");
                            noAlertsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                            noAlertsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            alertsPanel.add(noAlertsLabel);
                        } else {
                            // Add a header with alert count
                            JLabel headerLabel = new JLabel("Found " + alerts.size() + " active alerts for " + stateCode);
                            headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
                            headerLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
                            headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            alertsPanel.add(headerLabel);
                            
                            // Add each alert as a clickable panel
                            int alertNumber = 1;
                            for (Map<String, String> alert : alerts) {
                                alertsPanel.add(createAlertPanel(alert, alertNumber++));
                            }
                        }
                        
                        alertsPanel.revalidate();
                        alertsPanel.repaint();
                    });
                    
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        alertsPanel.removeAll();
                        JLabel errorLabel = new JLabel("Error parsing response: " + ex.getMessage());
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        alertsPanel.add(errorLabel);
                        alertsPanel.revalidate();
                        alertsPanel.repaint();
                    });
                }
                
            } catch (IOException ioe) {
                SwingUtilities.invokeLater(() -> {
                    alertsPanel.removeAll();
                    JLabel errorLabel = new JLabel("Connection error: " + ioe.getMessage());
                    errorLabel.setForeground(Color.RED);
                    errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    alertsPanel.add(errorLabel);
                    alertsPanel.revalidate();
                    alertsPanel.repaint();
                });
            }
        }).start();
    }
    
    private JPanel createAlertPanel(Map<String, String> alert, int alertNumber) {
        JPanel alertPanel = new JPanel(new BorderLayout());
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getSeverityColor(alert.get("severity")), 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        alertPanel.setBackground(new Color(250, 250, 250));
        alertPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        alertPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Left side: Icon based on event type
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setBackground(new Color(240, 240, 240));
        iconPanel.setPreferredSize(new Dimension(60, 60));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Find appropriate icon based on event type
        String eventType = alert.getOrDefault("event", "");
        for (Map.Entry<String, String> entry : ALERT_ICONS.entrySet()) {
            if (eventType.contains(entry.getKey())) {
                try {
                    URL iconUrl = getClass().getResource(entry.getValue());
                    if (iconUrl != null) {
                        iconLabel.setIcon(new ImageIcon(iconUrl));
                    } else {
                        // If resource not found, use text instead
                        iconLabel.setText(entry.getKey().substring(0, 1));
                        iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
                    }
                } catch (Exception e) {
                    // If any error loading icon, use text instead
                    iconLabel.setText(entry.getKey().substring(0, 1));
                    iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
                }
                break;
            }
        }
        
        // If no icon found, use default
        if (iconLabel.getIcon() == null && iconLabel.getText().isEmpty()) {
            iconLabel.setText("!");
            iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        }
        
        iconPanel.add(iconLabel, BorderLayout.CENTER);
        alertPanel.add(iconPanel, BorderLayout.WEST);
        
        // Right side: Alert details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new GridBagLayout());
        detailsPanel.setBackground(new Color(250, 250, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        // Event type and number
        JLabel eventLabel = new JLabel("#" + alertNumber + ": " + alert.getOrDefault("event", "Unknown Event"));
        eventLabel.setFont(new Font("Arial", Font.BOLD, 14));
        detailsPanel.add(eventLabel, gbc);
        
        // Headline (if available)
        gbc.gridy++;
        String headline = alert.getOrDefault("headline", "").trim();
        if (!headline.isEmpty() && !headline.equals(alert.getOrDefault("event", ""))) {
            JLabel headlineLabel = new JLabel(truncateText(headline, 100));
            headlineLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            detailsPanel.add(headlineLabel, gbc);
        }
        
        // Times
        gbc.gridy++;
        gbc.gridwidth = 1;
        
        // Format the effective date
        String effectiveStr = formatDate(alert.getOrDefault("effective", ""));
        JLabel effectiveLabel = new JLabel("Effective: " + effectiveStr);
        effectiveLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailsPanel.add(effectiveLabel, gbc);
        
        gbc.gridx = 1;
        // Format the expiration date
        String expiresStr = formatDate(alert.getOrDefault("expires", ""));
        JLabel expiresLabel = new JLabel("Expires: " + expiresStr);
        expiresLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailsPanel.add(expiresLabel, gbc);
        
        alertPanel.add(detailsPanel, BorderLayout.CENTER);
        
        // Add severity indicator
        JLabel severityLabel = new JLabel(alert.getOrDefault("severity", "Unknown"));
        severityLabel.setForeground(Color.WHITE);
        severityLabel.setFont(new Font("Arial", Font.BOLD, 12));
        severityLabel.setOpaque(true);
        severityLabel.setBackground(getSeverityColor(alert.getOrDefault("severity", "Unknown")));
        severityLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        severityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        alertPanel.add(severityLabel, BorderLayout.EAST);
        
        // Make the panel clickable
        alertPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDetailedView(alert);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                alertPanel.setBackground(new Color(240, 240, 240));
                detailsPanel.setBackground(new Color(240, 240, 240));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                alertPanel.setBackground(new Color(250, 250, 250));
                detailsPanel.setBackground(new Color(250, 250, 250));
            }
        });
        
        return alertPanel;
    }
    
    private void showDetailedView(Map<String, String> alert) {
        JDialog detailDialog = new JDialog(this, "Alert Details", true);
        detailDialog.setLayout(new BorderLayout());
        detailDialog.setSize(600, 500);
        detailDialog.setLocationRelativeTo(this);
        
        // Header panel with event name and severity
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(getSeverityColor(alert.getOrDefault("severity", "Unknown")));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel(alert.getOrDefault("event", "Alert Details"));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JLabel severityLabel = new JLabel("Severity: " + alert.getOrDefault("severity", "Unknown"));
        severityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        severityLabel.setForeground(Color.WHITE);
        headerPanel.add(severityLabel, BorderLayout.EAST);
        
        detailDialog.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Times section
        JPanel timesPanel = new JPanel(new GridBagLayout());
        timesPanel.setBorder(BorderFactory.createTitledBorder("Time Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 15);
        
        addDetailRow(timesPanel, gbc, "Effective:", formatDate(alert.getOrDefault("effective", "N/A")));
        gbc.gridy++;
        addDetailRow(timesPanel, gbc, "Expires:", formatDate(alert.getOrDefault("expires", "N/A")));
        
        if (alert.containsKey("onset")) {
            gbc.gridy++;
            addDetailRow(timesPanel, gbc, "Onset:", formatDate(alert.getOrDefault("onset", "N/A")));
        }
        
        timesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(timesPanel);
        contentPanel.add(createVerticalStrut(10));
        
        // Headline section if available
        String headline = alert.getOrDefault("headline", "").trim();
        if (!headline.isEmpty()) {
            JPanel headlinePanel = new JPanel(new BorderLayout());
            headlinePanel.setBorder(BorderFactory.createTitledBorder("Headline"));
            
            JTextArea headlineText = new JTextArea(headline);
            headlineText.setEditable(false);
            headlineText.setLineWrap(true);
            headlineText.setWrapStyleWord(true);
            headlineText.setFont(new Font("Arial", Font.PLAIN, 14));
            headlineText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            headlineText.setBackground(new Color(240, 240, 240));
            
            headlinePanel.add(headlineText, BorderLayout.CENTER);
            headlinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(headlinePanel);
            contentPanel.add(createVerticalStrut(10));
        }
        
        // Description section
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setBorder(BorderFactory.createTitledBorder("Description"));
        
        String description = alert.getOrDefault("description", "No description available.");
        JTextArea descText = new JTextArea(description);
        descText.setEditable(false);
        descText.setLineWrap(true);
        descText.setWrapStyleWord(true);
        descText.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JScrollPane descScroll = new JScrollPane(descText);
        descScroll.setPreferredSize(new Dimension(550, 200));
        descPanel.add(descScroll, BorderLayout.CENTER);
        
        descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(descPanel);
        
        // Add instruction section if available
        String instruction = alert.getOrDefault("instruction", "").trim();
        if (!instruction.isEmpty()) {
            contentPanel.add(createVerticalStrut(10));
            
            JPanel instructPanel = new JPanel(new BorderLayout());
            instructPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));
            
            JTextArea instructText = new JTextArea(instruction);
            instructText.setEditable(false);
            instructText.setLineWrap(true);
            instructText.setWrapStyleWord(true);
            instructText.setFont(new Font("Arial", Font.PLAIN, 14));
            instructText.setBackground(new Color(255, 255, 220)); // Light yellow background
            
            JScrollPane instructScroll = new JScrollPane(instructText);
            instructScroll.setPreferredSize(new Dimension(550, 100));
            instructPanel.add(instructScroll, BorderLayout.CENTER);
            
            instructPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(instructPanel);
        }
        
        JScrollPane contentScroll = new JScrollPane(contentPanel);
        detailDialog.add(contentScroll, BorderLayout.CENTER);
        
        // Close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> detailDialog.dispose());
        buttonPanel.add(closeButton);
        detailDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        detailDialog.setVisible(true);
    }
    
    private void addDetailRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(labelComponent, gbc);
        
        gbc.gridx = 1;
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(valueComponent, gbc);
        
        gbc.gridx = 0;
    }
    
    private Component createVerticalStrut(int height) {
        JPanel strut = new JPanel();
        strut.setPreferredSize(new Dimension(0, height));
        strut.setMaximumSize(new Dimension(0, height));
        strut.setMinimumSize(new Dimension(0, height));
        strut.setOpaque(false);
        return strut;
    }
    
    private Color getSeverityColor(String severity) {
        if (severity == null) {
            return SEVERITY_COLORS.get("Unknown");
        }
        
        for (Map.Entry<String, Color> entry : SEVERITY_COLORS.entrySet()) {
            if (severity.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return SEVERITY_COLORS.get("Unknown");
    }
    
    private String formatDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty() || dateString.equals("N/A")) {
            return "N/A";
        }
        
        try {
            // Parse the ISO 8601 date format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // Parse the input date
            Date date = null;
            try {
                date = inputFormat.parse(dateString.replaceAll("Z$", ""));
            } catch (ParseException e) {
                // Try alternate format with milliseconds
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = inputFormat.parse(dateString.replaceAll("Z$", ""));
            }
            
            // Format it in a more readable way
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a");
            outputFormat.setTimeZone(TimeZone.getDefault());
            return outputFormat.format(date);
            
        } catch (ParseException e) {
            // If any error in parsing, return the original
            return dateString;
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String fetchDataFromURL(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            try (InputStream inputStream = connection.getInputStream();
                 Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // Custom JSON parsing function for this specific API response - kept same as before
    private List<Map<String, String>> parseAlerts(String json) {
        List<Map<String, String>> alerts = new ArrayList<>();
        
        try {
            // Find the features array
            int featuresStart = json.indexOf("\"features\":");
            if (featuresStart == -1) {
                return alerts;
            }
            
            // Extract the features array
            int arrayStart = json.indexOf('[', featuresStart);
            if (arrayStart == -1) {
                return alerts;
            }
            
            // Find the end of the array with proper bracket matching
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayEnd == -1) {
                return alerts;
            }
            
            String featuresArray = json.substring(arrayStart + 1, arrayEnd);
            
            // Split the array into individual objects
            List<String> alertObjects = splitJsonArray(featuresArray);
            
            for (String alertObj : alertObjects) {
                // Find the properties object within each alert
                int propertiesStart = alertObj.indexOf("\"properties\":");
                if (propertiesStart == -1) {
                    continue;
                }
                
                int propObjStart = alertObj.indexOf('{', propertiesStart);
                if (propObjStart == -1) {
                    continue;
                }
                
                int propObjEnd = findMatchingBracket(alertObj, propObjStart);
                if (propObjEnd == -1) {
                    continue;
                }
                
                String propertiesObj = alertObj.substring(propObjStart + 1, propObjEnd);
                
                // Extract the fields we need
                Map<String, String> alertData = new HashMap<>();
                extractField(propertiesObj, "effective", alertData);
                extractField(propertiesObj, "expires", alertData);
                extractField(propertiesObj, "headline", alertData);
                extractField(propertiesObj, "description", alertData);
                extractField(propertiesObj, "severity", alertData);
                extractField(propertiesObj, "event", alertData);
                extractField(propertiesObj, "instruction", alertData);
                extractField(propertiesObj, "onset", alertData);
                
                alerts.add(alertData);
            }
        } catch (Exception e) {
            // If any parsing error occurs, return empty list
            return new ArrayList<>();
        }
        
        return alerts;
    }
    
    private void extractField(String jsonObj, String fieldName, Map<String, String> result) {
        String fieldPattern = "\"" + fieldName + "\":";
        int fieldStart = jsonObj.indexOf(fieldPattern);
        if (fieldStart != -1) {
            int valueStart = jsonObj.indexOf('"', fieldStart + fieldPattern.length());
            if (valueStart != -1) {
                int valueEnd = findEndOfJsonString(jsonObj, valueStart + 1);
                if (valueEnd != -1) {
                    String value = jsonObj.substring(valueStart + 1, valueEnd);
                    // Unescape JSON string
                    value = value.replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\n", "\n")
                                .replace("\\r", "\r")
                                .replace("\\t", "\t");
                    result.put(fieldName, value);
                }
            }
        }
    }
    
    private int findEndOfJsonString(String json, int startIndex) {
        boolean escaped = false;
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }
    
    private int findMatchingBracket(String json, int openBracketIndex) {
        char openBracket = json.charAt(openBracketIndex);
        char closeBracket;
        if (openBracket == '{') {
            closeBracket = '}';
        } else if (openBracket == '[') {
            closeBracket = ']';
        } else {
            return -1;
        }
        
        int nesting = 1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = openBracketIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == openBracket) {
                    nesting++;
                } else if (c == closeBracket) {
                    nesting--;
                    if (nesting == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }
    
    private List<String> splitJsonArray(String jsonArray) {
        List<String> result = new ArrayList<>();
        
        boolean inString = false;
        boolean escaped = false;
        int bracketNesting = 0;
        int objectStart = 0;
        
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    if (bracketNesting == 0) {
                        objectStart = i;
                    }
                    bracketNesting++;
                } else if (c == '}') {
                    bracketNesting--;
                    if (bracketNesting == 0) {
                        // We found a complete object
                        result.add(jsonArray.substring(objectStart, i + 1));
                    }
                }
            }
        }
        
        return result;
    }
}