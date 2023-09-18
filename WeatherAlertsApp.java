/*
 * Emmanuel Michael
 * Final Exam
 * INT-2200-RD01
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherAlertsApp extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private JTextField stateCodeField;
    private JTextArea alertTextArea;

    public WeatherAlertsApp() {
        setTitle("Weather Alerts");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create input panel
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("StateCode:"));
        stateCodeField = new JTextField(2);
        inputPanel.add(stateCodeField);
        JButton runButton = new JButton("Run Button");
        runButton.addActionListener(this);
        inputPanel.add(runButton);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);

        // Create output panel
        alertTextArea = new JTextArea(10, 40);
        alertTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(alertTextArea);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new WeatherAlertsApp();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    // When Run Button is pressed, use State Code to fetch data from URL to display to alert text area
        String stateCode = stateCodeField.getText().toUpperCase();
        String urlString = "https://api.weather.gov/alerts/active?area=" + stateCode;
        String responseData = null;
        try {
            responseData = fetchDataFromURL(urlString);
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        if (responseData == null) {
            alertTextArea.setText("Failed to retrieve data from the server.");
            return;
        }

        JSONObject responseJson = new JSONObject(responseData);
        JSONArray alertsJson = responseJson.getJSONArray("features");

        if (alertsJson.length() == 0) {
            alertTextArea.setText("No alerts found.");
            return;
        }

        StringBuilder alertsBuilder = new StringBuilder();
        for (int i = 0; i < alertsJson.length(); i++) {
            JSONObject alertJson = alertsJson.getJSONObject(i);
            JSONObject propertiesJson = alertJson.getJSONObject("properties");
            alertsBuilder.append("Effective: " + propertiesJson.getString("effective") + "\n");
            alertsBuilder.append("Expires: " + propertiesJson.getString("expires") + "\n");
            alertsBuilder.append("Headline: " + propertiesJson.getString("headline") + "\n");
            alertsBuilder.append("Description: " + propertiesJson.getString("description") + "\n\n");
        }
        alertTextArea.setText(alertsBuilder.toString());
    }

    private String fetchDataFromURL(String urlString) throws IOException {
    // Make connection to, and fetch data from, URL
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        InputStream inputStream = connection.getInputStream();
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String responseData = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return responseData;
    }
}
