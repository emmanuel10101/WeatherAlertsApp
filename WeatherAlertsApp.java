import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class WeatherAlertsApp extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JTextField stateCodeField;
    private JTextArea alertTextArea;

    public WeatherAlertsApp() {
        setTitle("Weather Alerts");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create input panel
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("State Code:"));
        stateCodeField = new JTextField(2);
        inputPanel.add(stateCodeField);
        JButton runButton = new JButton("Get Alerts");
        runButton.addActionListener(this);
        inputPanel.add(runButton);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        // Create output panel
        alertTextArea = new JTextArea(20, 50);
        alertTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(alertTextArea);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        pack();
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

        alertTextArea.setText("Fetching alerts for " + stateCode + "...");
        
        // Use a separate thread to prevent UI freezing
        new Thread(() -> {
            try {
                String urlString = "https://api.weather.gov/alerts/active?area=" + stateCode;
                String responseData = fetchDataFromURL(urlString);

                if (responseData == null) {
                    SwingUtilities.invokeLater(() -> 
                        alertTextArea.setText("Failed to retrieve data from the server."));
                    return;
                }

                try {
                    // Parse JSON using simple string parsing
                    List<Map<String, String>> alerts = parseAlerts(responseData);

                    if (alerts.isEmpty()) {
                        SwingUtilities.invokeLater(() -> 
                            alertTextArea.setText("No active alerts found for " + stateCode + "."));
                        return;
                    }

                    StringBuilder alertsBuilder = new StringBuilder();
                    int alertNumber = 1;
                    for (Map<String, String> alert : alerts) {
                        alertsBuilder.append("Alert #").append(alertNumber++).append("\n");
                        alertsBuilder.append("Effective: ").append(alert.getOrDefault("effective", "N/A")).append("\n");
                        alertsBuilder.append("Expires: ").append(alert.getOrDefault("expires", "N/A")).append("\n");
                        alertsBuilder.append("Headline: ").append(alert.getOrDefault("headline", "N/A")).append("\n");
                        alertsBuilder.append("Description: ").append(alert.getOrDefault("description", "N/A")).append("\n\n");
                    }
                    
                    final String alertText = alertsBuilder.toString();
                    SwingUtilities.invokeLater(() -> alertTextArea.setText(alertText));
                    
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> 
                        alertTextArea.setText("Error parsing response: " + ex.getMessage()));
                }
                
            } catch (IOException ioe) {
                SwingUtilities.invokeLater(() -> 
                    alertTextArea.setText("Connection error: " + ioe.getMessage()));
            }
        }).start();
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
    
    // Custom JSON parsing function for this specific API response
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
        int nesting = 0;
        int objectStart = -1;
        
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
                    if (nesting == 0) {
                        objectStart = i;
                    }
                    nesting++;
                } else if (c == '}') {
                    nesting--;
                    if (nesting == 0 && objectStart != -1) {
                        result.add(jsonArray.substring(objectStart, i + 1));
                        objectStart = -1;
                    }
                }
            }
        }
        
        return result;
    }
}