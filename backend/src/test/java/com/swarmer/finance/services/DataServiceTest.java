package com.swarmer.finance.services;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swarmer.finance.TestcontainersConfiguration;
import com.swarmer.finance.dto.Dump;
import com.swarmer.finance.models.Account;
import com.swarmer.finance.models.AccountGroup;
import com.swarmer.finance.models.TransactionType;
import com.swarmer.finance.models.User;
import com.swarmer.finance.repositories.CategoryRepository;
import com.swarmer.finance.repositories.GroupRepository;
import com.swarmer.finance.repositories.RuleRepository;
import com.swarmer.finance.repositories.TransactionRepository;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@DataJpaTest(showSql = true, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        DataService.class }))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class DataServiceTest {
    static Dump dump1 = null;
    static Dump dump2 = null;

    @Autowired
    GroupRepository groupRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    RuleRepository ruleRepository;
    @Autowired
    DataService dataService;
    @Autowired
    EntityManager em;
    private final User user1 = new User(null, "test1@gmail.com", "{noop}123456", true, "test1", "EUR",
            LocalDateTime.now(), LocalDateTime.now(), "test1");
    private final User user2 = new User(null, "test2@gmail.com", "{noop}123456", true, "test2", "EUR",
            LocalDateTime.now(), LocalDateTime.now(), "test2");

    @BeforeAll
    static void initAll() throws ClassNotFoundException, StreamReadException, DatabindException, IOException {
        var cls = Class.forName("com.swarmer.finance.services.DataServiceTest");
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        dump1 = objectMapper.readValue(cls.getClassLoader().getResourceAsStream("test1.json"), Dump.class);
        dump2 = objectMapper.readValue(cls.getClassLoader().getResourceAsStream("test2.json"), Dump.class);
    }

    @BeforeEach
    void init() throws StreamReadException, DatabindException, IOException {
        // create test users
        em.persist(user1);
        em.persist(user2);
        em.flush();
        // map test users to dumps
        var userIds = Map.of(dump1.ownerId(), user1.getId(), dump2.ownerId(), user2.getId());
        dump1 = Dump.mapUsers(dump1, userIds);
        dump2 = Dump.mapUsers(dump2, userIds);
    }

    @Test
    void testGetDump() {
        dataService.loadDump(user1.getId(), dump1);
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(user1.getId()));
        var expectedNames = groups.stream().filter(g -> !g.getDeleted()).map(g -> g.getName()).sorted().toList();
        var expectedAccs = groups.stream().flatMap(g -> g.getAccounts().stream()).filter(a -> !a.getDeleted()).count();
        var expectedAcls = groups.stream().flatMap(g -> g.getAcls().stream()).mapToLong(a -> a.getUserId()).toArray();
        var transactions = transactionRepository.findAllByOwnerId(user1.getId());
        var expectedDebit = transactions.mapToDouble(t -> t.getDebit()).sum();
        var expectedCatNames = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user1.getId())).stream()
                .filter(c -> c.getParentId() != null).map(c -> c.getName()).sorted().toList();

        var copy = dataService.getDump(user1.getId());

        var actualNames = copy.groups().stream().filter(g -> !g.deleted()).map(g -> g.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
        var actualAcls = copy.groups().stream().flatMap(g -> g.acls().stream()).mapToLong(a -> a.userId()).toArray();
        assertThat(actualAcls).containsExactlyInAnyOrder(expectedAcls);
        var actualAccs = copy.groups().stream().flatMap(g -> g.accounts().stream()).filter(a -> !a.deleted()).count();
        assertThat(actualAccs).isEqualTo(expectedAccs);
        var actualDebit = copy.transactions().stream().mapToDouble(t -> t.debit()).sum();
        assertThat(actualDebit).isEqualTo(expectedDebit);
        var actualCatNames = copy.categories().stream().map(c -> c.name()).sorted().toList();
        assertThat(actualCatNames).hasSameElementsAs(expectedCatNames);

        assertThat(copy.ownerId()).isEqualTo(user1.getId());
        assertThat(copy.groups()).hasSize(dump1.groups().size());
        assertThat(copy.transactions()).hasSize(dump1.transactions().size());
        assertThat(copy.categories()).hasSize(dump1.categories().size());
        assertThat(copy.rules()).hasSize(dump1.rules().size());
    }

    @Test
    void testLoadDumpSameUser() {
        dataService.loadDump(user1.getId(), dump1);
        // check group names
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(user1.getId()));
        var actualNames = groups.stream().filter(g -> !g.getDeleted()).map(g -> g.getName()).sorted().toList();
        var expectedNames = dump1.groups().stream().filter(g -> !g.deleted()).map(g -> g.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
        // check account counts
        var actualAccs = groups.stream().flatMap(g -> g.getAccounts().stream()).filter(a -> !a.getDeleted()).count();
        var expectedAccs = dump1.groups().stream().flatMap(g -> g.accounts().stream()).filter(a -> !a.deleted())
                .count();
        assertThat(actualAccs).isEqualTo(expectedAccs);
        // check acls
        var actualAcls = groups.stream().flatMap(g -> g.getAcls().stream()).mapToLong(a -> a.getUserId()).toArray();
        var expectedAcls = dump1.groups().stream().flatMap(g -> g.acls().stream()).mapToLong(a -> a.userId())
                .toArray();
        assertThat(actualAcls).containsExactlyInAnyOrder(expectedAcls);
        // check transactions
        var transactions = transactionRepository.findAllByOwnerId(user1.getId());
        var actualDebit = transactions.mapToDouble(t -> t.getDebit()).sum();
        var expectedDebit = dump1.transactions().stream().mapToDouble(t -> t.debit()).sum();
        assertThat(actualDebit).isEqualTo(expectedDebit);
        // check categories
        actualNames = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user1.getId())).stream()
                .filter(c -> c.getParentId() != null).map(c -> c.getName()).sorted().toList();
        expectedNames = dump1.categories().stream().map(c -> c.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
    }

    @Test
    void testLoadDumpOtherUser() {
        dataService.loadDump(user2.getId(), dump1);
        // check group names
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(user2.getId()));
        var actualNames = groups.stream().filter(g -> !g.getDeleted()).map(g -> g.getName()).sorted().toList();
        var expectedNames = dump1.groups().stream().filter(g -> !g.deleted()).map(g -> g.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
        // check account counts
        var actualAccs = groups.stream().flatMap(g -> g.getAccounts().stream()).filter(a -> !a.getDeleted()).count();
        var expectedAccs = dump1.groups().stream().flatMap(g -> g.accounts().stream()).filter(a -> !a.deleted())
                .count();
        assertThat(actualAccs).isEqualTo(expectedAccs);
        // check acls
        var actualAcls = groups.stream().flatMap(g -> g.getAcls().stream()).mapToLong(a -> a.getUserId()).toArray();
        var expectedAcls = dump1.groups().stream().flatMap(g -> g.acls().stream()).mapToLong(a -> a.userId())
                .toArray();
        assertThat(actualAcls).containsExactlyInAnyOrder(expectedAcls);
        // check transactions
        var transactions = transactionRepository.findAllByOwnerId(user2.getId());
        var actualDebit = transactions.mapToDouble(t -> t.getDebit()).sum();
        var expectedDebit = dump1.transactions().stream().mapToDouble(t -> t.debit()).sum();
        assertThat(actualDebit).isEqualTo(expectedDebit);
        // check categories
        actualNames = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user2.getId())).stream()
                .filter(c -> c.getParentId() != null).map(c -> c.getName()).sorted().toList();
        expectedNames = dump1.categories().stream().map(c -> c.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
    }

    @Test
    void testRepeatedLoad() {
        // load dump and get new one with real ids
        dataService.loadDump(user1.getId(), dump1);
        var copy = dataService.getDump(user1.getId());
        assertThat(copy.ownerId()).isEqualTo(user1.getId());
        assertThat(copy.groups()).hasSize(dump1.groups().size());
        assertThat(copy.transactions()).hasSize(dump1.transactions().size());
        assertThat(copy.categories()).hasSize(dump1.categories().size());
        assertThat(copy.rules()).hasSize(dump1.rules().size());
        // modify data
        var groups = groupRepository.findByOwnerIdInOrderById(List.of(user1.getId()));
        assertThat(groups).hasSize(copy.groups().size());
        var cash = groups.get(0);
        cash.setDeleted(true);
        groupRepository.save(cash);
        var lhv = groups.get(1);
        em.remove(lhv.getAcls().get(0));
        lhv.getAcls().clear();
        lhv.getAccounts().add(new Account(null, lhv, "New Account", "EUR", 0.0, false,
                LocalDateTime.now(), LocalDateTime.now()));
        groupRepository.save(lhv);
        groupRepository.save(new AccountGroup(null, groups.get(0).getOwner(), List.of(), List.of(), "New Test Group",
                false, LocalDateTime.now(), LocalDateTime.now()));
        var fe = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user1.getId())).stream()
                .filter(c -> c.getOwnerId() != null && c.getType() == TransactionType.EXPENSE).findFirst()
                .orElseThrow();
        fe.setOwnerId(user2.getId());
        categoryRepository.save(fe);
        var fi = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user1.getId())).stream()
                .filter(c -> c.getOwnerId() != null && c.getType() == TransactionType.INCOME).findFirst()
                .orElseThrow();
        fi.setOwnerId(user2.getId());
        categoryRepository.save(fi);
        transactionRepository.findAllByOwnerId(user1.getId())
                .filter(t -> t.getCategory() != null
                        && (t.getCategory().getId().equals(fe.getId()) || t.getCategory().getId().equals(fi.getId())))
                .forEach(t -> {
                    t.setOwner(user2);
                    transactionRepository.save(t);
                });
        ruleRepository.findAllByOwnerId(user1.getId()).stream()
                .filter(r -> r.getCategory().getId().equals(fe.getId()) || r.getCategory().getId().equals(fi.getId()))
                .forEach(r -> {
                    r.setOwnerId(user2.getId());
                    ruleRepository.save(r);
                });
        em.flush();
        // restore from dump
        dataService.loadDump(user1.getId(), copy);
        // check group names
        groups = groupRepository.findByOwnerIdInOrderById(List.of(user1.getId()));
        var actualNames = groups.stream().filter(g -> !g.getDeleted()).map(g -> g.getName()).sorted().toList();
        var expectedNames = copy.groups().stream().filter(g -> !g.deleted()).map(g -> g.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
        // check account counts
        var actualAccs = groups.stream().flatMap(g -> g.getAccounts().stream()).filter(a -> !a.getDeleted()).count();
        var expectedAccs = copy.groups().stream().flatMap(g -> g.accounts().stream()).filter(a -> !a.deleted())
                .count();
        assertThat(actualAccs).isEqualTo(expectedAccs);
        // check acls
        var actualAcls = groups.stream().flatMap(g -> g.getAcls().stream()).mapToLong(a -> a.getUserId()).toArray();
        var expectedAcls = copy.groups().stream().flatMap(g -> g.acls().stream()).mapToLong(a -> a.userId())
                .toArray();
        assertThat(actualAcls).containsExactlyInAnyOrder(expectedAcls);
        // check transactions
        var transactions = transactionRepository.findAllByOwnerId(user1.getId());
        var actualDebit = transactions.mapToDouble(t -> t.getDebit()).sum();
        var expectedDebit = copy.transactions().stream().mapToDouble(t -> t.debit()).sum();
        assertThat(actualDebit).isEqualTo(expectedDebit);
        // check categories
        actualNames = categoryRepository.findByOwnerIdIsNullOrOwnerIdIn(List.of(user1.getId())).stream()
                .filter(c -> c.getParentId() != null).map(c -> c.getName()).sorted().toList();
        expectedNames = copy.categories().stream().map(c -> c.name()).sorted().toList();
        assertThat(actualNames).hasSameElementsAs(expectedNames);
    }
}
