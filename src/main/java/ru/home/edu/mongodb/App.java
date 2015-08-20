package ru.home.edu.mongodb;


import com.mongodb.*;
import org.bson.Document;
import org.bson.conversions.Bson;

public class App {
    public static void main(String[] args) throws Exception {
        MongoClient client = new MongoClient(new ServerAddress("localhost", 27017));
        DB db = client.getDB("course");

        DBCollection coll = db.getCollection("students");

        coll.drop();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                coll.insert(new BasicDBObject("i", i).append("j", j));
            }
        }

        Bson sort = new Document("i", -1);

        DBCursor cur = coll.find()
                .sort(new BasicDBObject("i", -1))
//                .sort(new BasicDBObject("j", -1)
        ;
        show(cur);

    }

    private static void show(DBCursor cursor) {
        while (cursor.hasNext()) {
            System.out.println(cursor.next());
        }
    }
}
