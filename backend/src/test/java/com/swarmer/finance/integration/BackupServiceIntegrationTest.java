package com.swarmer.finance.integration;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swarmer.finance.dto.dump.Dump;
import com.swarmer.finance.dto.dump.DumpAcl;
import com.swarmer.finance.dto.dump.DumpGroup;
import com.swarmer.finance.dto.dump.DumpTransaction;
import com.swarmer.finance.models.User;
import com.swarmer.finance.repositories.UserRepository;
import com.swarmer.finance.services.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class BackupServiceIntegrationTest {

        // Configure the PostgreSQL container for testing
        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        // Inject the backup service and user repository (for creating test users)
        @Autowired
        private BackupService backupService;

        @Autowired
        private UserRepository userRepository;

        private final ObjectMapper objectMapper = new ObjectMapper();
        private User user1;
        private User user2;
        private Dump dump;
        private Dump dumpOuter;

        @BeforeEach
        void setup() throws StreamReadException, DatabindException, IOException {
                objectMapper.registerModule(new JavaTimeModule());
                // Create test user 1 and test user 2.
                user1 = new User();
                user1.setEmail("user1@gmail.com");
                user1.setPassword("password1");
                user1.setName("Test User 1");
                userRepository.save(user1);

                user2 = new User();
                user2.setEmail("user2@gmail.com");
                user2.setPassword("password2");
                user2.setName("Test User 2");
                userRepository.save(user2);
                // load test dump
                dump = objectMapper.readValue(new ClassPathResource("test1.json").getInputStream(), Dump.class);
                // adjust dump to test users
                var groups1 = dump.groups().stream()
                                .map(g -> new DumpGroup(g.id(), g.acls().stream()
                                                .map(a -> new DumpAcl(user2.getId(), a.admin(), a.readonly(), a.name(),
                                                                a.created(), a.updated()))
                                                .toList(), g.accounts(), g.name(), g.deleted(), g.created(),
                                                g.updated()))
                                .toList();
                dump = new Dump(user1.getId(), dump.created(), groups1, dump.categories(), dump.transactions(),
                                dump.rules());
                // make invalid dump with outer transactions
                var outer = dump.transactions().stream()
                                .map(t -> new DumpTransaction(t.id(), t.opdate(),
                                                t.accountId() == null ? null : t.accountId() + 1000, t.debit(),
                                                t.recipientId(), t.credit(), t.categoryId(), t.currency(), t.party(),
                                                t.details(), t.created(), t.updated()))
                                .toList();
                dumpOuter = new Dump(user1.getId(), dump.created(), groups1, dump.categories(), outer,
                                dump.rules());
        }

        @Test
        void testLoadBackup() throws Exception {
                // Call loadDump for user 1. This should successfully load all backup data.
                backupService.loadDump(user1.getId(), dump, false);

                // Retrieve the loaded backup and perform validations.
                Dump loadedDump = backupService.getDump(1L);
                assertNotNull(loadedDump, "Loaded dump should not be null");
                assertEquals(dump.groups().size(), loadedDump.groups().size(),
                                "There should be same count of groups in the dump.");
                assertEquals(dump.transactions().size(), loadedDump.transactions().size(),
                                "There should be same count of transactions in the dump.");
        }

        @Test
        void testLoadInvalidBackup() throws Exception {
                // Load backup for user 1.
                Exception exception = assertThrows(
                                RuntimeException.class,
                                () -> backupService.loadDump(1L, dumpOuter, false),
                                "Expected a RuntimeException when loading a dump with outer transactions.");
                // check exception message
                String actualMessage = exception.getMessage();
                assertTrue(actualMessage.contains("not found") || actualMessage.contains("is not owner"));
        }

        @Test
        void testLoadInvalidBackupSkipData() throws Exception {
                // Load backup for user 1, skip invalid transactions
                backupService.loadDump(1L, dumpOuter, true);

                // Retrieve the backup and verify it was loaded correctly.
                Dump loadedDump = backupService.getDump(1L);
                assertNotNull(loadedDump, "Loaded dump should not be null");
                assertEquals(dumpOuter.groups().size(), loadedDump.groups().size(),
                                "There should be 2 groups in the dump.");
                assertTrue(dump.transactions().size() > loadedDump.transactions().size(),
                                "Some transactions must be skipped.");
        }
}