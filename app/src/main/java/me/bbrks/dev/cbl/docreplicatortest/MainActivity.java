package me.bbrks.dev.cbl.docreplicatortest;

import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.URLEndpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

//    private static final String SYNC_GATEWAY_URL = "ws://192.168.1.13:4984/";
    private static final String SYNC_GATEWAY_URL = "ws://10.0.2.2:4984/"; // Android emulator loopback address
    private static final String DB_NAME = "db1";
    private static final String SG_USERNAME = "demo";
    private static final String SG_PASSWORD = "password";

    private TextView txtStatus;
    private EditText txtDocID;
    private NumberPicker numCount;

    private Database cblDatabase;
    private Replicator cblReplicator;

    private void createDoc(String docID) {
        MutableDocument mutableDoc;

        // new doc with random ID
        if (docID.equals("")) {
            mutableDoc = new MutableDocument();
        } else {
            Document document = cblDatabase.getDocument(docID);
            if (document == null) {
                // new doc
                mutableDoc = new MutableDocument(docID);
            } else {
                // doc exists, so add/modify updated_at timestamp
                mutableDoc = document.toMutable();
            }
        }

        // TODO: Configurable channels
        MutableArray array = new MutableArray(new ArrayList<>());
        array.addString("foobar");
        mutableDoc.setArray("channels", array);
        mutableDoc.setLong("created_at", System.currentTimeMillis());

        // TODO: Configurable blobs?

        try {
            cblDatabase.save(mutableDoc);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        txtStatus.setText("Added "+mutableDoc.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        txtDocID  = findViewById(R.id.txtDocID);
        numCount  = findViewById(R.id.numCount);
        numCount.setValue(1);
        numCount.setMinValue(1);
        numCount.setMaxValue(Integer.MAX_VALUE);
        numCount.setWrapSelectorWheel(false);

        CouchbaseLite.init(getApplicationContext());
        Database.log.getConsole().setDomains(LogDomain.ALL_DOMAINS);
        Database.log.getConsole().setLevel(LogLevel.VERBOSE);

        findViewById(R.id.btnDocAdd).setOnClickListener(v -> {
            if (cblDatabase == null) {
               txtStatus.setText("No database! Can't create a doc...");
               return;
            }

            Editable docIDText = txtDocID.getText();
            String docID = "";
            if (docIDText != null) {
                docID = docIDText.toString();
            }

            String finalDocID = docID;
            int count = numCount.getValue();
            if (count == 1) {
                createDoc(docID);
            } else {
                // Create >1 docs in thread to avoid blocking UI
                Thread t = new Thread() {
                    public void run() {
                        for (int i = 0; i <numCount.getValue(); i++) {
                            createDoc(finalDocID);
                        }
                    }
                };
                t.start();
            }

        });

        findViewById(R.id.btnReplStart).setOnClickListener(v -> {
            if (cblReplicator == null) {
                txtStatus.setText("No replicator! Have you created a database yet?");
                return;
            }

            try {
                cblReplicator.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.btnReplStop).setOnClickListener(v -> {
            if (cblReplicator == null) {
                txtStatus.setText("No replicator! Have you created a database yet?");
                return;
            }

            try {
                cblReplicator.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.btnDBNew).setOnClickListener(v -> {
            if (cblDatabase != null) {
                try {
                    cblDatabase.delete();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                    return;
                }
            }

            DatabaseConfiguration config = new DatabaseConfiguration();
            Database db;
            try {
                db = new Database(DB_NAME, config);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
                return;
            }
            cblDatabase = db;

            URLEndpoint targetEndpoint;
            try {
                targetEndpoint = new URLEndpoint(new URI(SYNC_GATEWAY_URL +db.getName()));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }

            ReplicatorConfiguration replConfig = new ReplicatorConfiguration(db, targetEndpoint);
            replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);
            replConfig.setContinuous(true);

            replConfig.setAuthenticator(new BasicAuthenticator(SG_USERNAME, SG_PASSWORD.toCharArray()));

            Replicator r = new Replicator(replConfig);
            r.addChangeListener(change -> txtStatus.setText(change.getStatus().toString()));

            cblReplicator = r;
            txtStatus.setText("Created database "+cblDatabase.toString());
        });
    }
}
