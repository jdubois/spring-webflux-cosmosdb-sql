package io.github.jdubois.asynccosmos.repository;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.IndexingPolicy;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.ThroughputProperties;

import io.github.jdubois.asynccosmos.domain.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
public class ProjectRepository {

    private final Logger log = LoggerFactory.getLogger(ProjectRepository.class);

    @Value("${application.cosmosdb.accountHost}")
    private String accountHost;

    @Value("${application.cosmosdb.accountKey}")
    private String accountKey;

    @Value("${application.cosmosdb.databaseName}")
    private String databaseName;

    @Value("${application.cosmosdb.containerName}")
    private String containerName;

    private CosmosAsyncClient client;
    private CosmosAsyncDatabase database;
    private CosmosAsyncContainer container;

    @PostConstruct
    public void init() {
        log.info("Configuring CosmosDB connection");

        //DirectConnectionConfig directConnectionConfig = DirectConnectionConfig.getDefaultConfig();
        GatewayConnectionConfig gatewayConnectionConfig = GatewayConnectionConfig.getDefaultConfig();

        client = new CosmosClientBuilder()
            .endpoint(accountHost)
            .key(accountKey)
            .preferredRegions(Collections.singletonList("East US 2"))
            .consistencyLevel(ConsistencyLevel.SESSION)
            .contentResponseOnWriteEnabled(true)
            .gatewayMode(gatewayConnectionConfig)
            .buildAsyncClient();

        // Create the database if it does not exist yet
        client.createDatabaseIfNotExists(databaseName)
            .doOnSuccess(cosmosDatabaseResponse -> log.info("Database: " + cosmosDatabaseResponse.getProperties().getId()))
            .doOnError(throwable -> log.error(throwable.getMessage()))
            .publishOn(Schedulers.elastic())
            .block();

        database = client.getDatabase(databaseName);

        // Create the container if it does not exist yet
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/id");
        ThroughputProperties throughputProperties = ThroughputProperties.createManualThroughput(400);
        IndexingPolicy indexingPolicy = new IndexingPolicy();
        indexingPolicy.setAutomatic(false);
        containerProperties.setIndexingPolicy(indexingPolicy);
        database.createContainerIfNotExists(containerProperties, throughputProperties)
            .doOnSuccess(cosmosContainerResponse -> log.info("Container: " + cosmosContainerResponse.getProperties().getId()))
            .doOnError(throwable -> log.error(throwable.getMessage()))
            .publishOn(Schedulers.elastic())
            .block();

        container = database.getContainer(containerName);

    }

    public Mono<Project> save(Project project) {
        project.setId(UUID.randomUUID().toString());
        return container.createItem(project)
            .map(i -> {
                Project savedProject = new Project();
                savedProject.setId(i.getItem().getId());
                savedProject.setName(i.getItem().getName());
                return savedProject;
            });
    }

    public Mono<Project> update(Project project) {
        return container.upsertItem(project)
            .map(i -> {
                Project savedProject = new Project();
                savedProject.setId(i.getItem().getId());
                savedProject.setName(i.getItem().getName());
                return savedProject;
            });
    }

    public Flux<List<Project>> findAll() {
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

        return container.queryItems("SELECT TOP 100000 * FROM c", options, Project.class)
            .byPage(100)
            .map(i -> {
                List<Project> results = new ArrayList<>();
                i.getResults().forEach(props -> {
                    Project project = new Project();
                    project.setId(props.getId());
                    project.setName(props.getName());
                    results.add(project);
                });
                return results;
            });
    }

    public Mono<Project> findById(String id) {
        return container.readItem(id, new PartitionKey(id), Project.class).map(i -> {
            Project project = new Project();
            project.setId(i.getItem().getId());
            project.setName(i.getItem().getName());
            return project;
        });
    }

    public void deleteById(String id) {
        container.deleteItem(id, new PartitionKey(id)).subscribe();
    }
}
