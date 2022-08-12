# Veracode SCA Mitigations
This project exposes a REST API that allows for adding mitigations to SCA licenses.

# Requirements:
- Veracode SCA License
- Veracode API Credentials
- Veracode account using username & password to login
- Mitigation Approver permissions
- Selenium webdriver

# Usage:
- Set the path to the selenium web driver and it's name on selenium.WebDriverWrapper
- Build the application using Maven
- Deploy it to a web server
- Call the API endpoint at /licenseMitigation by passing the following parameters as json:
  - veracodeApiId - The API credential's API ID
  - veracodeApiKey - The API credential's API KEY
  - veracodeUsername - Veracode account Username (usually the e-mail)
  - veracodePassword - Password for this Veracode account
  - licensesToMitigate - list of licenses to mitigate. It uses this format:
    - componentFileName - the library you want to mitigate
    - licenseName - the name of the license you want to mitigate
    - mitigationType - the type of mitigation to apply. Allowed values are:
      - Commercially Licensed
      - Approved By Legal
      - Internal Use
      - Experimental
    - applicationId - GUID of the application to mitigate
    - mitigationText - Text to add to the mitigation
