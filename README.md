# spring-webflux-cosmosdb-sql
Demo a fully reactive Spring Webflux application using CosmosDB SQL

## Prerequisites
* JDK 11 or higher
* Maven 3.6.3 or higher

## Configuration
Store your Cosmos DB configuration in following environment variables before running sample
* EndpointUrl - set to the URL of your Cosmos DB Account, for example "https://your-cosmos-db-account.documents.azure.com:443/" (without quote)
* PrimaryKey - set to your Cosmos DB primary access key
* DatabaseName - set to the name of your Cosmos DB database
* ContainerName - set to the name of your Cosmos DB Container

## Running the sample app
1. In the root folder of the project run
*mvnw spring-boot:run*
1. Use a tool like Postman or curl send an HTTP Get request to http://localhost:8080/api/projects
