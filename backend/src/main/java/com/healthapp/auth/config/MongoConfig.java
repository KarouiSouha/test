package com.healthapp.auth.config;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Slf4j
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.database:health_auth_db}")
    private String databaseName;

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Override
    protected String getDatabaseName() {
        log.info("✅ MongoDB Database Name: {}", databaseName);
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        String connectionString = String.format("mongodb://%s:%d", host, port);
        log.info("✅ MongoDB Connection String: {}", connectionString);
        return MongoClients.create(connectionString);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        MongoTemplate template = new MongoTemplate(mongoClient(), getDatabaseName());
        
        // Verify database name
        String actualDbName = template.getDb().getName();
        log.info("✅ MongoTemplate Database: {}", actualDbName);
        
        if (!databaseName.equals(actualDbName)) {
            log.error("❌ DATABASE MISMATCH! Expected: {}, Actual: {}", databaseName, actualDbName);
            throw new IllegalStateException("MongoDB database configuration error");
        }
        
        return template;
    }
}
