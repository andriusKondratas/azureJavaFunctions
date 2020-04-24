package com.officei.azure;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;


/**
 * Azure Functions with HTTP Trigger.
 */
public class DumpToBlob {

    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl"
     * command in bash: 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function
     * key} 2. curl "{your host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, it is used to invoke function deployed to
     * Azure. More details: https://aka.ms/functions_authorization_keys
     */
    @FunctionName("dumpToBlob")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET,
            HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                                   final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);
        String fileName = "quickstart" + java.util.UUID.randomUUID() + ".txt";
        long lineCount;
        String downloadLink;

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        } else {
            try {
                CloudBlockBlob blob = createBlob(fileName, context);
                lineCount = flushQueryResultToBlob(blob, name, context);
                downloadLink = getBlobSasUri(blob);
            } catch (URISyntaxException | InvalidKeyException | StorageException e) {
                context.getLogger().info("\nGot error:\n\t" + e.getMessage());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Got Error. " + e.getMessage()).build();
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Success.\nLine count: " + lineCount + "\nDownload URL: " + downloadLink).build();
        }
    }

    private long flushQueryResultToBlob(CloudBlockBlob blob, String query,
                                        final ExecutionContext context) {
        String url = System.getenv("AZURE_SQL_CONNECTION_STRING");
        long lineCount = 0L;

        try (BlobOutputStream outputStream = blob.openOutputStream()) {
            try (Connection connection = DriverManager.getConnection(url)) {
                try (PreparedStatement st = connection.prepareStatement(query)) {
                    try (ResultSet resultSet = st.executeQuery()) {
                        ResultSetMetaData metadata = resultSet.getMetaData();
                        int columnCount = metadata.getColumnCount();
                        while (resultSet.next()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 1; i <= columnCount; i++) {
                                sb.append(resultSet.getString(i));
                                sb.append(",");
                            }
                            sb.append("\n");
                            outputStream.write(sb.toString().getBytes());
                            lineCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().info("\nGot error:\n\t" + e.getMessage());
        }
        return lineCount;
    }

    private CloudBlockBlob createBlob(String name, final ExecutionContext context)
            throws URISyntaxException, InvalidKeyException, StorageException {
        CloudStorageAccount storageAccount;
        CloudBlobClient client;
        CloudBlobContainer container;
        String connectionString = System.getenv("WEBSITE_CONTENTAZUREFILECONNECTIONSTRING");

        context.getLogger().info("\nCreating new blob for writing query results\n\t");

        storageAccount = CloudStorageAccount.parse(connectionString);
        context.getLogger().info("\nSuccessfully connected to storage account: storageAccount\n\t");

        client = storageAccount.createCloudBlobClient();
        container = client.getContainerReference("container");
        container.createIfNotExists();
        return container.getBlockBlobReference(name);
    }

    private static String getBlobSasUri(CloudBlockBlob blob)
            throws InvalidKeyException, StorageException {
        SharedAccessBlobPolicy sasConstraints = new SharedAccessBlobPolicy();
        Date expirationDate = (Date) Date.from(Instant.now().plus(Duration.ofDays(1)));
        sasConstraints.setSharedAccessExpiryTime(expirationDate);
        EnumSet<SharedAccessBlobPermissions> permissions = EnumSet.of(SharedAccessBlobPermissions.READ);
        sasConstraints.setPermissions(permissions);

        String sasContainerToken = blob.generateSharedAccessSignature(sasConstraints, null);

        return blob.getUri() + "?" + sasContainerToken;
    }
}


