import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import sun.misc.BASE64Encoder;

import java.security.SecureRandom;

import static com.mongodb.client.model.Filters.eq;

public class SessionDAO {
    private final MongoCollection<Document> sessionCollection;

    public SessionDAO(final MongoDatabase blogDatabase) {
        sessionCollection = blogDatabase.getCollection("sessions");
    }

    public String findUsernameBySessionId(String sessionId) {
        Document session = getSession(sessionId);

        if (session == null) {
            return null;
        } else
            return session.get("username").toString();
    }

    public String startSession(String username) {
        SecureRandom generator = new SecureRandom();
        byte randomBytes[] = new byte[32];
        generator.nextBytes(randomBytes);

        BASE64Encoder encoder = new BASE64Encoder();

        String sessionID = encoder.encode(randomBytes);

        Document session = new Document()
                .append("username", username)
                .append("_id", sessionID);

        sessionCollection.insertOne(session);

        return session.getString("_id");
    }

    private Document getSession(String sessionID) {
        return sessionCollection.find(eq("_id", sessionID)).first();
    }

    public void endSession(String sessionID) {
        sessionCollection.deleteOne(eq("_id", sessionID));
    }
}
