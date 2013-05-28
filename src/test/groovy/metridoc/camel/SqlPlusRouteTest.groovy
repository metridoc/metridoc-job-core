package metridoc.camel

import groovy.sql.Sql
import metridoc.core.tools.CamelTool
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

import javax.sql.DataSource
import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 4/1/13
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
class SqlPlusRouteTest {

    DataSource embeddedDataSource

    @Before
    void addEmbeddedDataSource() {
        embeddedDataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()
        def sql = new Sql(embeddedDataSource)
        sql.execute("create table foo(name varchar(50), age int)")
        sql.execute("create table bar(name varchar(50), age int)")
        sql.execute("insert into foo values ('joe', 50)")
        sql.execute("insert into foo values ('jack', 70)")
    }

    @After
    void shutdownEmbeddedDatabase() {
        embeddedDataSource.shutdown()
    }

    @Test
    void "test sql routing using the camel tool"() {
        def tool = new CamelTool()
        tool.binding = new Binding()
        tool.binding.dataSource = embeddedDataSource

        tool.with {
            consumeNoWait("sqlplus:foo?dataSource=dataSource") { ResultSet resultSet ->
                send("sqlplus:bar?dataSource=dataSource", resultSet)
            }
        }

        def sql = new Sql(embeddedDataSource)
        assert 2 == sql.firstRow("select count(*) as total from bar").total
    }
}