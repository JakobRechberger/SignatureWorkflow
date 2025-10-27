# SignatureWorkflow
This project handles the backend for the research of optimizing testing and auditing of software projects.
The corresponding Frontend can be found [here](https://github.com/JakobRechberger/signaturewebsite)
### Functionality
#### Supervisor Input:
- selecting a git repository and timeframe
- fetching all contributors and sending mails with unique invite links
- by entering the project name in the project name field a status can be requested of all contributors whether they have signed the project already

#### Contributor View
- click the link sent via mail or add the token as a ?token= paramater to the url
- enter the hash value sent via mail in the respective input field
- a download of the project is started which can be saved as a .zip file (currently restricted -> large projects not supported)
- when the review is finished a signature which is timestamped by [FreeTSA](https://freetsa.org/index_de.php) is appended to the user
### Prerequisites
- Java 18
- git installed
- go to application.properties and enter mysql db credentials and db connection and gmail setup for sending mail functionality
- to use email services uncomment line 56 at /database/service/ProjectService.java and enter your mail address again at line 23 at /database/service/EmailService.java
- start a development server on localhost:3000 for the [frontend](https://github.com/JakobRechberger/signaturewebsite) and start the Spring Application


