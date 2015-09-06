package ru.home.edu.mongodb.Homework5;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static java.util.Arrays.asList;

/**
 * Created by Василий on 06.09.2015.
 */
public class SmallZips {
    private static final String NY = "\"NY\"";
    private static final String CA = "\"CA\"";

    public static void main(String[] args) {
        MongoClient client = new MongoClient();
        MongoDatabase db = client.getDatabase("week5");
        MongoCollection<Document> col = db.getCollection("grads");

        Document unwind = new Document("$unwind", "$scores");
        Document quizFilter = new Document("$match", new Document("$or", asList(new Document("scores.type", "exam"), new Document("scores.type", "homework"))));
        Document groupByClassAndStudent = new Document("$group", new Document()
                .append("_id", new Document()
                        .append("class", "$class_id")
                        .append("student", "$student_id"))
                .append("avg_per_student", new Document("$avg", "$scores.score")));
        Document groupByClass = new Document("$group", new Document()
                .append("_id", "$_id.class")
                .append("avg_per_class", new Document("$avg", "$avg_per_student")));
        Document sort = new Document("$sort", new Document("avg_per_class", 1));

        MongoCursor<Document> cursor = col.aggregate(asList(unwind, quizFilter, groupByClassAndStudent, groupByClass, sort)).iterator();

        showColl(cursor);


    }

    private static void showColl(MongoCursor<Document> cursor) {
        while (cursor.hasNext()) {
            Document curr = cursor.next();
            System.out.println(curr.toJson());
        }
    }
}
