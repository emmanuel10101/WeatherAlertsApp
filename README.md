# Weather Alerts Application

## Description
Weather Alerts App is a lightweight Java desktop application that provides real-time access to active weather alerts across the United States. Using the National Weather Service API, this application allows users to quickly retrieve and view critical weather warnings, watches, and advisories for any state in the US.
Features

Simple State-Based Queries: Enter a two-letter state code to instantly retrieve all active alerts
Visual Alert Display: Color-coded alerts based on severity level with intuitive visual indicators
At-a-Glance Information: Each alert shows event type, effective and expiration times, and severity
Detailed Alert View: Click on any alert to view comprehensive information including:

Full descriptions and instructions;
Precise timing information;
Emergency actions recommended by authorities


Alert Type Icons: 
* Visual indicators for different types of weather events (tornados, floods, winter storms, etc.)
* Severity Color Coding: Easily distinguish between Extreme, Severe, Moderate, and Minor alerts

## Technical Details

* Built with Java Swing for a responsive desktop interface
* Connects directly to the National Weather Service API (weather.gov)
* No external dependencies required - uses only standard Java libraries
* Custom JSON parser optimized for the NWS API response format
* Multi-threaded design to prevent UI freezing during API calls

## Requirements

* Java Runtime Environment (JRE) 8 or higher
* Internet connection to access the National Weather Service API

## Usage

* Launch the application
* Enter a two-letter US state code (e.g., TX for Texas, CA for California)
* Click "Get Alerts" to retrieve current weather alerts
* Click on any alert to view detailed information and instructions

## Getting Started
#### Compile the application
```java
javac WeatherAlertsApp.java
```
#### Run the application
```java
java WeatherAlertsApp
```

Stay informed about critical weather events with this simple, efficient, and user-friendly application designed for quick access to important safety information.
