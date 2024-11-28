# SignatureWorkflow
This project handles the backend for the research of optimizing testing and auditing of software projects.
The corresponding Frontend can be found [here](https://github.com/JakobRechberger/signaturewebsite)
### Functionality
#### Supervisor Input:
- A supervisor initiates the workflow by selecting an existing Git repository.
- They provide the repository URL, along with a specified start and end date, to define the timeframe for the audit.
- This data is entered via the frontend interface.
#### Backend Repository Processing:
- The backend clones the specified Git repository into a temporary directory.
- The cloned repository is compressed into a ZIP file for storage efficiency.
- A SHA-256 hash value is calculated for the ZIP file, ensuring the repository's integrity can be verified.
- Both the ZIP file and its hash value are stored in a MySQL database for future validation.
#### Contributor Identification:
- The backend identifies all contributors who have made commits to the repository within the specified date range.
- Each contributor's email address is extracted from the commit history.
- A unique token is generated for each contributor. This token establishes a secure link between the contributor and the repository being audited.
- The contributors' details and their associated tokens are stored in the database.
#### Contributor Notification (To Be Implemented):
- An email is sent to each contributor containing:
- A download link with their unique token.
- The SHA-256 hash value of the repository ZIP file.
- This email provides the contributor with the means to access and verify the repository.
#### Contributor Authentication:
- A contributor accesses the download link provided in their email.
- Using the unique token, the backend validates the contributor's identity and fetches the relevant repository information.
- The hash value of the ZIP file stored in the database is compared to the value sent in the email to ensure the file's integrity has not been tampered with.
#### Repository Download:
- Upon successful validation, the contributor is granted access to download the repository as a ZIP file.
- This allows them to audit their contributions during the specified timeframe.


