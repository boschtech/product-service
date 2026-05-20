package com.boschtech.productservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests the URL-transformation logic in {@link DataSourceConfig#dataSource(DataSourceProperties)}.
 *
 * We mock {@link DataSourceProperties} to control what {@code getUrl()} returns and to
 * verify which setter mutations occur, while also stubbing
 * {@code initializeDataSourceBuilder().build()} so no real database connection is attempted.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class DataSourceConfigTest {

    private DataSourceConfig config;

    @BeforeEach
    void setUp() {
        config = new DataSourceConfig();
    }

    /** Creates a mocked {@link DataSourceProperties} backed by a no-op {@link DataSourceBuilder}. */
    private DataSourceProperties mockProps(String url) {
        DataSourceProperties props = mock(DataSourceProperties.class);
        when(props.getUrl()).thenReturn(url);
        DataSourceBuilder builder = mock(DataSourceBuilder.class);
        when(props.initializeDataSourceBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(mock(DataSource.class));
        return props;
    }

    // -------------------------------------------------------------------------
    // No-op paths
    // -------------------------------------------------------------------------

    @Test
    void shouldPassThroughWhenUrlIsNull() {
        DataSourceProperties props = mockProps(null);

        config.dataSource(props);

        verify(props, never()).setUrl(anyString());
        verify(props, never()).setUsername(anyString());
        verify(props, never()).setPassword(anyString());
    }

    @Test
    void shouldPassThroughWhenUrlAlreadyHasJdbcPrefix() {
        DataSourceProperties props = mockProps("jdbc:postgresql://host:5432/mydb");

        config.dataSource(props);

        verify(props, never()).setUrl(anyString());
    }

    // -------------------------------------------------------------------------
    // Successful parsing – postgres:// scheme
    // -------------------------------------------------------------------------

    @Test
    void shouldConvertPostgresUrlWithFullUserInfoPortPathAndQuery() {
        DataSourceProperties props = mockProps("postgres://user:pass@host:5432/mydb?ssl=true");

        config.dataSource(props);

        verify(props).setUsername("user");
        verify(props).setPassword("pass");
        verify(props).setUrl("jdbc:postgresql://host:5432/mydb?ssl=true");
    }

    @Test
    void shouldSetUsernameOnlyWhenPasswordMissingFromUserInfo() {
        DataSourceProperties props = mockProps("postgres://user@host:5432/mydb");

        config.dataSource(props);

        verify(props).setUsername("user");
        verify(props, never()).setPassword(anyString());
        verify(props).setUrl("jdbc:postgresql://host:5432/mydb");
    }

    @Test
    void shouldSkipCredentialsWhenNoUserInfoPresent() {
        DataSourceProperties props = mockProps("postgres://host:5432/mydb");

        config.dataSource(props);

        verify(props, never()).setUsername(anyString());
        verify(props, never()).setPassword(anyString());
        verify(props).setUrl("jdbc:postgresql://host:5432/mydb");
    }

    @Test
    void shouldOmitPortSegmentWhenPortIsAbsent() {
        DataSourceProperties props = mockProps("postgres://host/mydb");

        config.dataSource(props);

        verify(props).setUrl("jdbc:postgresql://host/mydb");
    }

    @Test
    void shouldOmitQueryStringWhenNoQueryPresent() {
        DataSourceProperties props = mockProps("postgres://user:pass@host:5432/mydb");

        config.dataSource(props);

        verify(props).setUrl("jdbc:postgresql://host:5432/mydb");
    }

    // -------------------------------------------------------------------------
    // Successful parsing – postgresql:// scheme
    // -------------------------------------------------------------------------

    @Test
    void shouldConvertPostgresqlUrlWithFullCredentials() {
        DataSourceProperties props = mockProps("postgresql://admin:secret@dbhost:5432/prod?sslmode=require");

        config.dataSource(props);

        verify(props).setUsername("admin");
        verify(props).setPassword("secret");
        verify(props).setUrl("jdbc:postgresql://dbhost:5432/prod?sslmode=require");
    }

    // -------------------------------------------------------------------------
    // Exception / fallback paths (invalid URI syntax triggers catch block)
    // -------------------------------------------------------------------------

    @Test
    void shouldFallbackToSimplePrependForMalformedPostgresUrl() {
        // A space in the host makes the URI invalid → URISyntaxException → catch block
        DataSourceProperties props = mockProps("postgres://bad host:5432/db");

        config.dataSource(props);

        verify(props).setUrl("jdbc:postgresql://bad host:5432/db");
    }

    @Test
    void shouldFallbackToJdbcPrependForMalformedPostgresqlUrl() {
        DataSourceProperties props = mockProps("postgresql://bad host:5432/db");

        config.dataSource(props);

        verify(props).setUrl("jdbc:postgresql://bad host:5432/db");
    }

    @Test
    void shouldNotModifyUrlForUnrecognisedSchemeOnParseError() {
        // Neither "postgres://" nor "postgresql://" prefix → no setUrl in catch block
        DataSourceProperties props = mockProps("mongodb://bad host:27017/db");

        config.dataSource(props);

        verify(props, never()).setUrl(anyString());
    }
}
