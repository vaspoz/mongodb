package ru.home.edu.mongodb.Homework3;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.print.Doc;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Василий on 22.08.2015.
 */
public class HW3_1 {
    public static void main(String[] args) {
        MongoClient client = new MongoClient();

        MongoDatabase db = client.getDatabase("school");
        MongoCollection<Document> students = db.getCollection("students");

        MongoCursor<Document> cursor = students.find().iterator();

        while (cursor.hasNext()) {
            Document student = cursor.next();

            List<Document> scores = (List<Document>) student.get("scores");

            Document minScoreDocument = getMinHWscoreDocument(scores);
            if (minScoreDocument == null) {
                throw new AssertionError("Unexpected exception. Check the code.");
            }

            scores.remove(minScoreDocument);

            students.updateOne(new Document("_id", student.getInteger("_id")),
                    new Document("$set", new Document("scores", scores)));

        }
    }

    private static Document getMinHWscoreDocument(List<Document> scoreList) {
        double minScore = 99.9;
        Document retVal = null;
        for (Document scoreDocument : scoreList) {
            String type = scoreDocument.getString("type");
            if (!type.equals("homework"))
                continue;

            double scoreValue = scoreDocument.getDouble("score");
            if (scoreValue < minScore) {
                minScore = scoreValue;
                retVal = scoreDocument;
            }
        }

        return retVal;
    }
}
