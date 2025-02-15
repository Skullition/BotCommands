package io.github.freya022.botcommands.test.config.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.test.config.Config
import io.github.freya022.botcommands.test.switches.TestDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

// Interfaced service used to retrieve an SQL Connection
@BService
@TestDatabase(TestDatabase.DatabaseType.POSTGRESQL)
class PostgresDatabaseSource(config: Config) : HikariSourceSupplier {
    override val source = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.databaseConfig.url
        username = config.databaseConfig.user
        password = config.databaseConfig.password

        // idk bucket4j is confused without it
        schema = "public"

        maximumPoolSize = 2
        leakDetectionThreshold = 10.seconds.inWholeMilliseconds
    })

    init {
        //Migrate BC tables
        createFlyway("bc", "bc_database_scripts").migrate()

        //You can use the same function for your database, you just have to change the schema and scripts location
        //Migrate BC test tables
        createFlyway("public", "bc_test_database_scripts").migrate()

        logger.info { "Created database source" }
    }

    private fun createFlyway(schema: String, scriptsLocation: String): Flyway = Flyway.configure()
        .dataSource(source)
        .schemas(schema)
        .locations(scriptsLocation)
        .validateMigrationNaming(true)
        .loggers("slf4j")
        .load()
}