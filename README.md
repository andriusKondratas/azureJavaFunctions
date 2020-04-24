## Azure Java functions POC
* `dumpToBlob` - Accepts HTTP GET request with sample query & executes & dumps contents to blob, responds with blob download URL
* `sendMail` - Accepts HTTP GET request with email body & sends email
### Prerequisites:
* Maven installed & configured, e.g: `sudo apt install maven`
* Any Open / Oracle / .. => 1.8 JDK installed, e.g : `sudo apt install default-jdk`
* Repository cloned `https://github.com/officeI/azureJavaFunctions.git ${repository location}`
* Completes without errors:
```
   cd '${repository location}'
   mvn install
   mvn clean package
```
* Get azure account
* Create resource group
* [Install Azure Core Tools](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=linux%2Ccsharp%2Cbash#v2)
* [Deploy using Azure maven plugin](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Web-App:-Deploy)
