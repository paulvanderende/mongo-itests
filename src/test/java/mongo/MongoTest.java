package mongo;

import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import com.mongodb.*;
import com.spotify.docker.client.DockerException;

import com.xebialabs.overcast.host.CloudHost;
import com.xebialabs.overcast.host.CloudHostFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class MongoTest {

    private CloudHost itestHost;
    private Mongo mongoClient;

    @Parameters("overcastConfig")
    @BeforeMethod
    public void before(String overcastConfig) throws UnknownHostException {
        itestHost = CloudHostFactory.getCloudHost(overcastConfig);
        itestHost.setup();

        String host = itestHost.getHostName();
        int port = itestHost.getPort(27017);

        MongoClientOptions options = MongoClientOptions.builder()
                .connectTimeout(300 * 1000)
                .maxWaitTime(300 * 1000)
                .build();

        mongoClient = new MongoClient(new ServerAddress(host, port), options);
        logger.info("Mongo connection: {}", mongoClient.toString());
    }

    @AfterMethod
    public void after(){
        mongoClient.close();
        itestHost.teardown();
    }

    @Test
    public void shouldCreateDatabase() throws DockerException, InterruptedException, UnknownHostException {

        DB db = mongoClient.getDB("mydb");
        DBCollection coll = db.getCollection("testCollection");
        BasicDBObject doc = new BasicDBObject("name", "MongoDB");
        coll.insert(doc);

        List<String> databaseNames = mongoClient.getDatabaseNames();
        logger.info("Databases: {}", databaseNames);

        assertThat(databaseNames, hasItems("mydb"));
    }

    @Test
    public void shouldCountDocuments() throws DockerException, InterruptedException, UnknownHostException {

        DB db = mongoClient.getDB("mydb");
        DBCollection coll = db.getCollection("testCollection");

        for (int i=0; i < 100; i++) {
            WriteResult writeResult = coll.insert(new BasicDBObject("i", i));
            logger.info("writing document: {}", writeResult);
        }

        int count = (int) coll.getCount();
        assertThat(count, equalTo(100));

    }

    @Test
    public void shouldReadAndWriteDocument() throws DockerException, InterruptedException, UnknownHostException {

        DB db = mongoClient.getDB("mydb");
        DBCollection coll = db.getCollection("testCollection");
        BasicDBObject doc = new BasicDBObject("name", "MongoDB")
                .append("type", "database")
                .append("count", 1);
        coll.insert(doc);

        DBObject myDoc = coll.findOne();
        logger.info("Reading document: {}", myDoc);

        assertThat((String) myDoc.get("name"), equalTo("MongoDB"));
        assertThat((String) myDoc.get("type"), equalTo("database"));
        assertThat((Integer) myDoc.get("count"), equalTo(1));

    }


    private static final Logger logger = LoggerFactory.getLogger(MongoTest.class);

}
