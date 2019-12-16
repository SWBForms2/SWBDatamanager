/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager.datastore;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import java.io.IOException;
import java.util.logging.Logger;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javiersolis
 */
public class DataStoreEmbedMongo extends DataStoreMongo {

    static private Logger log = Logger.getLogger(DataStoreEmbedMongo.class.getName());
    
    private static MongodExecutable mongodExecutable=null;
    private static MongodStarter starter=null;
    

    public DataStoreEmbedMongo(ScriptObject dataStore) {
        super(dataStore);
    }

    @Override
    protected void initDB() throws IOException {
        if (mongoClient == null) {
            synchronized (DataStoreEmbedMongo.class) {
                if (mongoClient == null) {
                    
                    String host=dataStore.getString("host","localhost");
                    int port=dataStore.getInt("port",12345);
                    String envHost=dataStore.getString("envHost");
                    String envPort=dataStore.getString("envPort");
                    if(envHost!=null)host=System.getenv(envHost);
                    if(envPort!=null)port=Integer.parseInt(System.getenv(envPort));
                    
                    if(starter==null)
                    {
                        log.info("Initialize embedmongodb...");
                        
                        Logger logger = Logger.getLogger(getClass().getName());  
                        
                        String dbPath=dataStore.getString("dbPath", "{appPath}/../embedmongo");
                        String dbDataPath=dataStore.getString("dbDataPath", "{appPath}/../embedmongo/data");
                        
                        dbPath=dbPath.replace("{appPath}", DataMgr.getApplicationPath());
                        dbDataPath=dbDataPath.replace("{appPath}", DataMgr.getApplicationPath());
                        
                        IDirectory artifactStorePath = new FixedPath(dbPath);
                        ITempNaming executableNaming = new UUIDTempNaming();

                        Command command = Command.MongoD;

                        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                                .defaults(command)
                                .defaultsWithLogger(Command.MongoD, logger)
                                .artifactStore(new ExtractedArtifactStoreBuilder()
                                        .defaults(command)
                                        .extractDir(artifactStorePath)
                                        .download(new DownloadConfigBuilder()
                                                .defaultsForCommand(command)
                                                .artifactStorePath(artifactStorePath))
                                        .executableNaming(executableNaming))
                                .build();                        
                        starter = MongodStarter.getInstance(runtimeConfig);    
                        
                        Storage replication = new Storage(dbDataPath,null,0);
                        IMongodConfig mongodConfig = new MongodConfigBuilder()
                                .version(Version.Main.PRODUCTION)
                                .net(new Net(host, port, Network.localhostIsIPv6())) 
                                .replication(replication)
                                .build();

                        mongodExecutable = starter.prepare(mongodConfig);
                        MongodProcess mongod = mongodExecutable.start();
                        
                        
                        DataMgr.subscribeStopNotification(new DataMgr.StopNotification() {
                            @Override
                            public void stop() {
                                mongodExecutable.stop();
                            }
                        });
                    }

                    mongoClient = new MongoClient(host, port);
                }
            }
        }
    }

}
