package io.github.jdubois.asynccosmos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to the application.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Cosmosdb cosmosdb = new Cosmosdb();

    public Cosmosdb getCosmosdb() {
        return cosmosdb;
    }

    public static class Cosmosdb {

        private String accountHost = "";

        private String accountKey = "";

        public String getAccountHost() {
            return accountHost;
        }

        public void setAccountHost(String accountHost) {
            this.accountHost = accountHost;
        }

        public String getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(String accountKey) {
            this.accountKey = accountKey;
        }
    }
}
