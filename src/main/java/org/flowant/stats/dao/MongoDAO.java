package org.flowant.stats.dao;

import static org.flowant.stats.Config.Keys.MongoClientUri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.flowant.stats.Config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * MongoDB Client에서 공통적으로 사용되는 기능 제공. Mongo Java Client에서 제공하는 Document 클래스를
 * 상속받아 재정의한 Doc 클래스 형태로 데이터를 입출력 하는 기능 제공
 *
 * @author "Kyengwhan Jee"
 *
 */
public class MongoDAO {
    protected Logger logger = Logger.getLogger(getClass().getSimpleName());

    protected MongoDatabase database;

    static class DocCodecProvider extends DocumentCodecProvider {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
            if (clazz == Doc.class) {
                return (Codec<T>) new DocCodec(registry, new BsonTypeClassMap());
            }
            return null;
        }
    }

    public MongoDAO(String datanaseName) {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(new DocCodecProvider()),
                MongoClient.getDefaultCodecRegistry());

        MongoClientOptions.Builder builder = MongoClientOptions.builder().codecRegistry(codecRegistry);
        String clientUri = Config.getConfig(MongoClientUri) + "/" + datanaseName;
        @SuppressWarnings("resource")
        MongoClient mongoClient = new MongoClient(new MongoClientURI(clientUri, builder));
        database = mongoClient.getDatabase(datanaseName);
    }

    MongoCollection<Doc> getCollection(String collectionName) {
        return database.getCollection(collectionName, Doc.class);
    }

    protected void insertMany(String collectionName, List<Doc> docList) throws Exception {
        insertMany(collectionName, docList, false);
    }

    protected void insertMany(String collectionName, List<Doc> docList, boolean dropCollection)
            throws Exception {

        MongoCollection<Doc> collection = getCollection(collectionName);
        if (dropCollection) {
            collection.drop();
        }
        collection.insertMany(docList);
    }

    protected long count(String collectionName, Optional<Doc> filter) throws IOException {
        MongoCollection<Doc> collection = getCollection(collectionName);
        return filter.isPresent() ? collection.count(filter.get()) : collection.count();
    }

    protected Stream<Doc> find(String collectionName, Optional<Doc> filter, Optional<Doc> sort,
            Optional<Integer> skip, Optional<Integer> limit) throws IOException {

        MongoCollection<Doc> collection = getCollection(collectionName);

        FindIterable<Doc> findIterable = filter.isPresent() ? collection.find(filter.get()) : collection.find();
        if (sort.isPresent())
            findIterable = findIterable.sort(sort.get());
        if (skip.isPresent())
            findIterable = findIterable.skip(skip.get());
        if (limit.isPresent())
            findIterable = findIterable.limit(limit.get());

        return StreamSupport.stream(findIterable.spliterator(), true);
    }

    protected Stream<Doc> match(String collectionName, Optional<Integer> skip, Optional<Integer> limit,
            Doc... matchGroupSort) throws IOException {
        MongoCollection<Doc> collection = getCollection(collectionName);

        List<Doc> pipeline = new ArrayList<Doc>(Arrays.asList(matchGroupSort));
        if (skip.isPresent())
            pipeline.add(new Doc("$skip", skip.get().intValue()));
        if (limit.isPresent())
            pipeline.add(new Doc("$limit", limit.get().intValue()));

        AggregateIterable<Doc> iterable = collection.aggregate(pipeline).allowDiskUse(true);
        Spliterator<Doc> spliterator = iterable.spliterator();
        return StreamSupport.stream(spliterator, true);
    }

}
