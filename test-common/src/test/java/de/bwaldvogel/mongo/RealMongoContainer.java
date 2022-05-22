package de.bwaldvogel.mongo;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import de.bwaldvogel.mongo.backend.Assert;

public class RealMongoContainer implements BeforeAllCallback, AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(RealMongoContainer.class);

    private GenericContainer<?> mongoContainer;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        mongoContainer = startIfNecessary();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (mongoContainer != null) {
            mongoContainer.stop();
        }
        mongoContainer = null;
    }

    public void restart() {
        if (shouldUseExistingContainer()) {
            // not supported
        } else {
            mongoContainer.stop();
            mongoContainer.start();
        }
    }

    private static GenericContainer<?> startIfNecessary() {
        if (shouldUseExistingContainer()) {
            log.info("Not starting a testcontainer in favor of an existing container.");
            return null;
        }
        return start();
    }

    private static boolean shouldUseExistingContainer() {
        return Boolean.getBoolean("mongo-java-server-use-existing-container");
    }

    static GenericContainer<?> start() {
        GenericContainer<?> mongoContainer = new GenericContainer<>("mongo:4.2.20").withExposedPorts(27017);
        mongoContainer.start();
        return mongoContainer;
    }

    public InetSocketAddress getServerAddress() {
        return getServerAddress(mongoContainer);
    }

    static InetSocketAddress getServerAddress(GenericContainer<?> mongoContainer) {
        if (mongoContainer != null) {
            return new InetSocketAddress("127.0.0.1", mongoContainer.getFirstMappedPort());
        } else {
            Assert.isTrue(shouldUseExistingContainer(),
                () -> "Unexpected: Got no container although we should not use an existing container");
            return new InetSocketAddress("127.0.0.1", 27018);
        }
    }
}
