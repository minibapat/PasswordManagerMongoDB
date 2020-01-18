package com.example.passwordmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoDatabase;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    StitchAppClient stitchClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("stitch", "log here");
        setContentView(R.layout.activity_main);


        final StitchAppClient client =
                Stitch.getDefaultAppClient(); // replace STITCH_CLIENT_APP_ID with App ID

        final RemoteMongoClient mongoClient =
                client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas"); // replace mongodb1 with mongodb-atlas

        final RemoteMongoCollection<Document> coll =
                mongoClient.getDatabase("sample_analytics").getCollection("customers"); // replace <DATABASE> with Stitch db and <COLLECTION> with Stitch collection

        client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                new Continuation<StitchUser, Task<RemoteUpdateResult>>() {

                    @Override
                    public Task<RemoteUpdateResult> then(@NonNull Task<StitchUser> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.e("STITCH", "Login failed!");
                            throw task.getException();
                        }

                        final Document updateDoc = new Document(
                                "owner_id",
                                task.getResult().getId()
                        );

                        updateDoc.put("number", 42);
                        return coll.updateOne(
                                null, updateDoc, new RemoteUpdateOptions().upsert(true)
                        );
                    }
                }
        ).continueWithTask(new Continuation<RemoteUpdateResult, Task<List<Document>>>() {
            @Override
            public Task<List<Document>> then(@NonNull Task<RemoteUpdateResult> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.e("STITCH", "Update failed!");
                    throw task.getException();
                }
                List<Document> docs = new ArrayList<>();
                return coll
                        .find(new Document("owner_id", client.getAuth().getUser().getId()))
                        .limit(100)
                        .into(docs);
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Document>>() {
            @Override
            public void onComplete(@NonNull Task<List<Document>> task) {
                if (task.isSuccessful()) {
                    Log.d("STITCH", "Found docs: " + task.getResult().toString());
                    return;
                }
                Log.e("STITCH", "Error: " + task.getException().toString());
                task.getException().printStackTrace();
            }
        });
      /*  this.stitchClient = Stitch.getDefaultAppClient();

        Log.d("stitch", "logging in anonymously");
        stitchClient.getAuth().loginWithCredential(new AnonymousCredential()
        ).addOnCompleteListener(new OnCompleteListener<StitchUser>() {
            @Override
            public void onComplete(@NonNull Task<StitchUser> task) {
                if (!task.isSuccessful()) {
                    Log.e("info", "Failed to log in!", task.getException());
                    return;
                }

                // Get the Atlas client.
                RemoteMongoClient mongoClient = stitchClient.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                RemoteMongoDatabase db = mongoClient.getDatabase("sample_analytics");
                RemoteMongoCollection<Document> details = db.getCollection("customers");

                // Find 20 documents
                details.find()
                        .limit(20)
                        .forEach(document -> {
                            // Print documents to the log.
                            Log.i("info", "Got document: " + document.toString());
                        });
            }
        }); */
    }


}
