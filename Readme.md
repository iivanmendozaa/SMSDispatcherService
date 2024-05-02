**SMS Dispatcher Service**

---

**Overview:**

SMS Dispatcher Service is an Android application designed to handle outgoing SMS messages from a specified device. It retrieves messages from a Microsoft SQL Server database and sends them as SMS messages to predefined numbers. This README provides an overview of the application's functionality, setup instructions, and important considerations.

---

**Functionality:**

- Retrieves outgoing SMS messages from a Microsoft SQL Server database.
- Sends retrieved messages as SMS messages to predefined numbers.
- Marks sent messages as "sent" in the database to avoid duplicate sending.

---

**Setup Instructions:**

1. **Permissions:**
    - Ensure that the following permissions are granted in the AndroidManifest.xml file:
        - `INTERNET`: Allows the app to connect to the internet.
        - `ACCESS_NETWORK_STATE`: Allows the app to access information about networks.
        - `FOREGROUND_SERVICE`: Allows the app to use foreground services.
        - `WAKE_LOCK`: Allows the app to prevent the device from sleeping.
        - `SEND_SMS`: Allows the app to send SMS messages.

2. **Database Configuration:**
    - Ensure that the Microsoft SQL Server database is accessible and contains the necessary tables (`OutgoingMessages`).

   ```SQL
   CREATE TABLE OutgoingMessages (
    Id INT PRIMARY KEY IDENTITY,
    AndroidDeviceId VARCHAR(255),
    Number VARCHAR(255),
    Message VARCHAR(255),
    Sent BIT DEFAULT 0
   );
   ```

3. **Android ID:**
    - The app retrieves the Android ID of the device to identify it uniquely. Ensure that the Android ID is used correctly for message retrieval.

4. **SMS Sending Limit:**
    - Modify the `SMS_OUTGOING_CHECK_MAX_COUNT` value to a large number in the `settings.db` and `gservices.db` files if necessary. Note that modifying system settings programmatically may require root access and is not recommended.

5. **Time Interval of Execution:**
    - The app executes the database query task at regular intervals. By default, it retrieves messages every 5 minutes. You can modify this interval in the `QUERY_INTERVAL_MILLISECONDS` at appSettings.json.

6. **Build and Run:**
    - Build the application using Android Studio or your preferred IDE.
    - Run the application on a compatible Android device or emulator.

---

**Important Considerations:**

- **SMS Sending Limitations:** Android imposes limitations on the number of SMS messages that can be sent within a specified time period. Ensure compliance with these limitations to avoid potential issues.
- **Database Connectivity:** Verify the connectivity and accessibility of the Microsoft SQL Server database to ensure smooth operation of the application.
- **Device Permissions:** Ensure that the required permissions are granted by the user to avoid runtime errors and unexpected behavior.
- **Error Handling:** Implement robust error handling mechanisms to handle exceptions gracefully and provide a seamless user experience.

---

**Disclaimer:**

This application is provided as-is without any warranties or guarantees. Use it responsibly and ensure compliance with relevant laws and regulations governing SMS messaging and data privacy.

---
