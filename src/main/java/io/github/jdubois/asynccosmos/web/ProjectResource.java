package io.github.jdubois.asynccosmos.web;

import io.github.jdubois.asynccosmos.domain.Project;
import io.github.jdubois.asynccosmos.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller for managing {@link io.github.jdubois.asynccosmos.domain.Project}.
 */
@RestController
@RequestMapping("/api")
public class ProjectResource {

    private final Logger log = LoggerFactory.getLogger(ProjectResource.class);

    private final ProjectRepository projectRepository;

    public ProjectResource(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @PostMapping("/projects")
    public Mono<Project> createProject(@RequestBody Project project) throws Exception {
        log.debug("REST request to save Project : {}", project);
        if (project.getId() != null) {
            throw new Exception("A new project cannot already have an ID");
        }
        return projectRepository.save(project);
    }

    @PutMapping("/projects")
    public Mono<Project> updateProject(@RequestBody Project project) throws Exception {
        log.debug("REST request to update Project : {}", project);
        if (project.getId() == null) {
            throw new Exception("Invalid id");
        }
        return projectRepository.update(project);
    }

    @GetMapping("/projects")
    public Flux<List<Project>> getAllProjects() {
        log.debug("REST request to get all Projects");
        return projectRepository.findAll();
    }

    @GetMapping("/projects/{id}")
    public Mono<Project> getProject(@PathVariable String id) {
        log.debug("REST request to get Project : {}", id);
        return projectRepository.findById(id);
    }

    @DeleteMapping("/projects/{id}")
    public void deleteProject(@PathVariable String id) {
        log.debug("REST request to delete Project : {}", id);
        projectRepository.deleteById(id);
    }
}
