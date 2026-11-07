package com.example.dqc.infrastructure.demo;

import com.example.dqc.application.model.RunListItem;
import com.example.dqc.application.service.RunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;

/**
 * Загружает демо-данные при первом запуске с профилем {@code demo}: данные недели 7, прогон RUN-007,
 * изменения недели 8, прогон RUN-008. Прогоны выполняет настоящий {@link RunService}, а не вставка готовых
 * результатов, поэтому демо проверяет весь конвейер.
 *
 * <p>Всё выполняется в одной транзакции: при сбое в БД не остаётся половины демо, и следующий запуск
 * начнёт с чистого места. Если в БД уже есть подразделения или прогоны, загрузка пропускается, данные не трогаются.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    static final String WEEK_7_SCRIPT = "demo/week-07.sql";
    static final String WEEK_8_SCRIPT = "demo/week-08.sql";
    static final LocalDate WEEK_7_DATE = LocalDate.of(2026, 10, 19);
    static final LocalDate WEEK_8_DATE = LocalDate.of(2026, 10, 26);

    private final JdbcClient jdbc;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final RunService runService;

    public DemoDataLoader(JdbcClient jdbc,
                          DataSource dataSource,
                          PlatformTransactionManager transactionManager,
                          RunService runService) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.runService = runService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long units = count("org_unit");
        long runs = count("kkd_run");
        if (units > 0 || runs > 0) {
            log.info("Демо-данные не загружаются: в БД уже есть данные (подразделений: {}, прогонов: {})", units, runs);
            return;
        }

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.executeWithoutResult(status -> {
            execute(WEEK_7_SCRIPT);
            RunListItem first = runService.start(WEEK_7_DATE);
            execute(WEEK_8_SCRIPT);
            RunListItem second = runService.start(WEEK_8_DATE);
            log.info("Демо-данные загружены: {} ({} проблем), {} ({} проблем)",
                    first.run().id(), first.issueCount(), second.run().id(), second.issueCount());
        });
    }

    private long count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }

    private void execute(String classpathScript) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(classpathScript));
        populator.setSqlScriptEncoding("UTF-8");
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
