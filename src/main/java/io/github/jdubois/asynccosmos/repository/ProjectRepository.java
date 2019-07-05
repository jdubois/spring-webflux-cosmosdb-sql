package io.github.jdubois.asynccosmos.repository;

import com.azure.data.cosmos.*;
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
import java.util.List;
import java.util.UUID;

@Repository
public class ProjectRepository {

    private final String DATABASE_NAME = "AsyncCosmos";
    private final String CONTAINER_NAME = "Project";

    private final Logger log = LoggerFactory.getLogger(ProjectRepository.class);

    @Value("${application.cosmosdb.accountHost}")
    private String accountHost;

    @Value("${application.cosmosdb.accountKey}")
    private String accountKey;

    private CosmosClient client;
    private CosmosDatabase database;
    private CosmosContainer container;

    @PostConstruct
    public void init() {
        log.info("Configuring CosmosDB connection");

        ConnectionPolicy connectionPolicy = new ConnectionPolicy();
        connectionPolicy.connectionMode(ConnectionMode.DIRECT);

        client = CosmosClient.builder()
            .endpoint(accountHost)
            .key(accountKey)
            .connectionPolicy(connectionPolicy)
            .build();

        // Create the database if it does not exist yet
        client.createDatabaseIfNotExists(DATABASE_NAME)
            .doOnSuccess(cosmosDatabaseResponse -> log.info("Database: " + cosmosDatabaseResponse.database().id()))
            .doOnError(throwable -> log.error(throwable.getMessage()))
            .publishOn(Schedulers.elastic())
            .block();

        database = client.getDatabase(DATABASE_NAME);

        // Create the container if it does not exist yet
        CosmosContainerProperties containerSettings = new CosmosContainerProperties(CONTAINER_NAME, "/id");
        IndexingPolicy indexingPolicy = new IndexingPolicy();
        indexingPolicy.automatic(false);
        containerSettings.indexingPolicy(indexingPolicy);
        database.createContainerIfNotExists(containerSettings, 400)
            .doOnSuccess(cosmosContainerResponse -> log.info("Container: " + cosmosContainerResponse.container().id()))
            .doOnError(throwable -> log.error(throwable.getMessage()))
            .publishOn(Schedulers.elastic())
            .block();

        container = database.getContainer(CONTAINER_NAME);

    }

    public Mono<Project> save(Project project) {
        project.setId(UUID.randomUUID().toString());
        return container.createItem(project)
            .map(i -> {
                Project savedProject = new Project();
                savedProject.setId(i.item().id());
                savedProject.setName(i.properties().getString("name"));
                return savedProject;
            });
    }

    public Mono<Project> update(Project project) {
        return container.upsertItem(project)
            .map(i -> {
                Project savedProject = new Project();
                savedProject.setId(i.item().id());
                savedProject.setName(i.properties().getString("name"));
                return savedProject;
            });
    }

    public Flux<List<Project>> findAll() {
        FeedOptions options = new FeedOptions();
        options.enableCrossPartitionQuery(true);

        return container.queryItems("SELECT TOP 20 * FROM Project p", options)
            .map(i -> {
                List<Project> results = new ArrayList<>();
                i.results().forEach(props -> {
                    Project project = new Project();
                    project.setId(props.id());
                    project.setName(props.getString("name"));
                    results.add(project);
                });
                return results;
            });
    }

    public Mono<Project> findById(String id) {
        return container.getItem(id, id).read().map(i -> {
            Project project = new Project();
            project.setId(i.item().id());
            project.setName(i.properties().getString("name"));
            return project;
        });
    }

    public void deleteById(String id) {
        container.getItem(id, id).delete().subscribe();
    }
}
