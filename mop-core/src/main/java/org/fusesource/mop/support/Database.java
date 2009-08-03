/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import org.apache.kahadb.page.PageFile;
import org.apache.kahadb.page.Transaction;
import org.apache.kahadb.page.Page;
import org.apache.kahadb.util.Marshaller;
import org.apache.kahadb.util.LockFile;
import org.apache.kahadb.index.BTreeIndex;
import org.fusesource.mop.support.ArtifactId;

import java.io.*;
import java.util.*;
import java.nio.channels.FileChannel;

/**
 * @author chirino
 */
public class Database {

    private PageFile pageFile;
    private boolean readOnly;
    private File directroy;
    private LockFile lock;

    public void delete() throws IOException {
        getReadOnlyFile().delete();
        getUpdateFile().delete();
        getUpdateRedoFile().delete();
    }

    public void open(boolean readOnly) throws IOException {
        if( pageFile!=null && pageFile.isLoaded() ) {
            throw new IllegalStateException("database allready opened.");
        }
        this.readOnly = readOnly;
        if (!getReadOnlyFile().exists()) {
            initialize();
        }
        if( readOnly ) {
            pageFile = new PageFile(directroy, "index");
            pageFile.setEnableWriteThread(false);
            pageFile.setEnableRecoveryFile(false);
        } else {
            lock = new LockFile(getUpdateFile(), false);
            lock.lock();
            pageFile = new PageFile(directroy, "update");
            pageFile.setEnableWriteThread(false);
            pageFile.setEnableRecoveryFile(true);

        }
        pageFile.load();
    }


    public void close() throws IOException {
        try {
            assertOpen();
            pageFile.flush();
            pageFile.unload();
            pageFile=null;
        } finally {
            if( !readOnly ) {
                copy(getUpdateFile(), getReadOnlyFile());
                lock.unlock();
                lock=null;
            }
        }
    }

    public void beginInstall(final String id) throws IOException {
        assertOpen();
        pageFile.tx().execute(new Transaction.Closure<IOException>() {
            public void execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                root.installingArtifact = id;
                root.tx_sequence++;
                root.store(tx);
            }
        });
        pageFile.flush();
    }

    public void installDone() {
        try {
            assertOpen();
            pageFile.tx().execute(new Transaction.Closure<IOException>() {
                public void execute(Transaction tx) throws IOException {
                    RootEntity root = RootEntity.load(tx);
                    root.installingArtifact = null;
                    root.tx_sequence++;
                    root.store(tx);
                }
            });
            pageFile.flush();
        } catch (Throwable e) {
        }
    }

    public void install(final LinkedHashSet<String> artifiactIds) throws IOException {
        if( artifiactIds.isEmpty() ) {
            throw new IllegalArgumentException("artifiactIds cannot be empty");
        }
        final String mainArtifact = artifiactIds.iterator().next();

        assertOpen();
        pageFile.tx().execute(new Transaction.Closure<IOException>() {
            public void execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> artifacts = root.artifacts.get(tx);
                BTreeIndex<String, HashSet<String>> artifactIdIndex = root.artifactIdIndex.get(tx);
                BTreeIndex<String, HashSet<String>> typeIndex = root.typeIndex.get(tx);
                BTreeIndex<String, HashSet<String>> explicityInstalledArtifacts = root.explicityInstalledArtifacts.get(tx);

                explicityInstalledArtifacts.put(tx, mainArtifact, new LinkedHashSet<String>(artifiactIds));
                for (String id : artifiactIds) {
                    ArtifactId a = new ArtifactId();
                    if (!a.strictParse(id)) {
                        throw new IOException("Invalid artifact id: " + id);
                    }
                    HashSet<String> rc = artifacts.get(tx, id);
                    if (rc == null) {
                        rc = new HashSet<String>();
                        rc.add(mainArtifact);
                        artifacts.put(tx, id, rc);
                        indexAdd(tx, artifactIdIndex, id, a.getArtifactId());
                        indexAdd(tx, typeIndex, id, a.getType());
                    } else {
                        rc.add(mainArtifact);
                        artifacts.put(tx, id, rc);
                    }
                }

                root.tx_sequence++;
                root.store(tx);
            }
        });
    }

    public TreeSet<String> uninstall(final String mainArtifact) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<TreeSet<String>, IOException>() {
            public TreeSet<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> artifacts = root.artifacts.get(tx);
                BTreeIndex<String, HashSet<String>> artifactIdIndex = root.artifactIdIndex.get(tx);
                BTreeIndex<String, HashSet<String>> typeIndex = root.typeIndex.get(tx);
                BTreeIndex<String, HashSet<String>> explicityInstalledArtifacts = root.explicityInstalledArtifacts.get(tx);

                TreeSet<String> unused = new TreeSet<String>();
                HashSet<String> artifiactIds = explicityInstalledArtifacts.remove(tx, mainArtifact);
                for (String id : artifiactIds) {
                    ArtifactId a = new ArtifactId();
                    if (!a.strictParse(id)) {
                        throw new IOException("Invalid artifact id: " + id);
                    }
                    HashSet<String> rc = artifacts.get(tx, id);
                    rc.remove(mainArtifact);
                    if( rc.isEmpty() ) {
                        unused.add(id);
                        artifacts.remove(tx, id);
                        indexRemove(tx, artifactIdIndex, id,  a.getArtifactId());
                        indexRemove(tx, typeIndex, id,  a.getType());
                    } else {
                        artifacts.put(tx, id, rc);
                    }
                }

                root.tx_sequence++;
                root.store(tx);
                return unused;
            }
        });
    }

    private void indexRemove(Transaction tx, BTreeIndex<String, HashSet<String>> artifactIdIndex, String id, String artifactId) {
    }


    public Set<String> findByArtifactId(final String artifactId) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<Set<String>, IOException>() {
            public Set<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> artifactIdIndex = root.artifactIdIndex.get(tx);
                HashSet<String> set = artifactIdIndex.get(tx, artifactId);
                return set == null ? new HashSet() : new HashSet(set);
            }
        });
    }


    public static Map<String, Set<String>> groupByGroupId(Set<String> values) {
        Map<String, Set<String>> rc = new LinkedHashMap<String, Set<String>>();
        for (String value : values) {
            ArtifactId id = new ArtifactId();
            id.strictParse(value);
            Set<String> t = rc.get(id.getGroupId());
            if( t == null ) {
                t = new LinkedHashSet(5);
                rc.put(id.getGroupId(), t);
            }
            t.add(value);
        }
        return rc;
    }

    public Set<String> findByType(final String type) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<Set<String>, IOException>() {
            public Set<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> typeIndex = root.typeIndex.get(tx);
                HashSet<String> set = typeIndex.get(tx, type);
                return set == null ? new HashSet() : new HashSet(set);
            }
        });
    }


    public TreeSet<String> listAll() throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<TreeSet<String>, IOException>() {
            public TreeSet<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> artifacts = root.artifacts.get(tx);
                Iterator<Map.Entry<String,HashSet<String>>> i = artifacts.iterator(tx);
                TreeSet<String> rc = new TreeSet<String>(); 
                while (i.hasNext()) {
                    Map.Entry<String,HashSet<String>> entry =  i.next();
                    rc.add(entry.getKey());
                }
                return rc;
            }
        });
    }

    public TreeSet<String> listInstalled() throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<TreeSet<String>, IOException>() {
            public TreeSet<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> explicityInstalledArtifacts = root.explicityInstalledArtifacts.get(tx);
                Iterator<Map.Entry<String, HashSet<String>>> i = explicityInstalledArtifacts.iterator(tx);

                TreeSet<String> rc = new TreeSet<String>();
                while (i.hasNext()) {
                    Map.Entry<String, HashSet<String>> entry =  i.next();
                    rc.add(entry.getKey());
                }
                return rc;
            }
        });
    }

    public TreeSet<String> listDependenants(final String artifact) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<TreeSet<String>, IOException>() {
            public TreeSet<String> execute(Transaction tx) throws IOException {
                RootEntity root = RootEntity.load(tx);
                BTreeIndex<String, HashSet<String>> artifacts = root.artifacts.get(tx);
                HashSet<String> deps = artifacts.get(tx, artifact);
                if( deps==null ) {
                    return null;
                }
                TreeSet<String> rc = new TreeSet<String>();

                rc.addAll(deps);
                rc.remove(artifact);
                return rc;
            }
        });
    }


    ///////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////
    static private void indexAdd(Transaction tx, BTreeIndex<String, HashSet<String>> index, String pk, String field) throws IOException {
        HashSet<String> ids = index.get(tx, field);
        if (ids == null) {
            ids = new HashSet<String>();
        }
        ids.add(pk);
        index.put(tx, field, ids);
    }

    private void assertOpen() {
        if (pageFile==null || !pageFile.isLoaded()) {
            throw new IllegalStateException("database not opened.");
        }
    }

    private void initialize() throws IOException {
        lock = new LockFile(getUpdateFile(), false);
        lock.lock();
        try {
            // Now that we have the lock.. lets check again..
            if (getReadOnlyFile().exists()) {
                return;
            }

            pageFile = new PageFile(directroy, "update");
            pageFile.setEnableWriteThread(false);
            pageFile.setEnableRecoveryFile(true);
            pageFile.load();
            pageFile.tx().execute(new Transaction.Closure<IOException>() {
                public void execute(Transaction tx) throws IOException {
                    RootEntity root = new RootEntity();
                    root.create(tx);
                }
            });
            pageFile.flush();;
            pageFile.unload();
            pageFile=null;

            copy(getUpdateFile(), getReadOnlyFile());
        } finally {
            lock.unlock();
            lock=null;
        }
    }

    static private void copy(File from, File to) throws IOException {
        to.delete();
        FileChannel in = new FileInputStream(from).getChannel();
        try {

            File tmp = File.createTempFile(to.getName(), ".part", to.getParentFile());
            FileChannel out = new FileOutputStream(tmp).getChannel();
            try {
                out.transferFrom(in, 0, from.length());
            } finally {
                out.close();
            }

            tmp.renameTo(to);
        } finally {
            in.close();
        }
    }

    private File getUpdateFile() {
        return new File(directroy, "update.data");
    }
    private File getUpdateRedoFile() {
        return new File(directroy, "update.redo");
    }
    private File getReadOnlyFile() {
        return new File(directroy, "index.data");
    }

    ///////////////////////////////////////////////////////////////////
    // Properties...
    ///////////////////////////////////////////////////////////////////
    public File getDirectroy() {
        return directroy;
    }

    public void setDirectroy(File directroy) {
        this.directroy = directroy;
    }


    ///////////////////////////////////////////////////////////////////
    // Helper Classes
    ///////////////////////////////////////////////////////////////////

    static private class RootEntity implements Serializable {
        static Marshaller<RootEntity> MARSHALLER = new ObjectMarshaller<RootEntity>();

        protected String installingArtifact;
        protected long tx_sequence;

        // This is map of the artifcact -> set of it's transitive dependencies.
        protected BTreeIndexReference<String, HashSet<String>> explicityInstalledArtifacts = new BTreeIndexReference<String, HashSet<String>>();
        // This is map of the artifcact -> set of installed artifacts that depend on it.
        protected BTreeIndexReference<String, HashSet<String>> artifacts = new BTreeIndexReference<String, HashSet<String>>();
        // This is map of the artifcactId -> set of artifacts that have the artifact id
        protected BTreeIndexReference<String, HashSet<String>> artifactIdIndex = new BTreeIndexReference<String, HashSet<String>>();
        // This is map of the type -> set of artifacts that have the type
        protected BTreeIndexReference<String, HashSet<String>> typeIndex = new BTreeIndexReference<String, HashSet<String>>();

        public void create(Transaction tx) throws IOException {
            // Allocate the root page.
            Page<RootEntity> page = tx.allocate();
            if (page.getPageId() != 0) {
                throw new IOException("RootEntity could not allocate page 0");
            }

            explicityInstalledArtifacts.create(tx);
            artifacts.create(tx);
            artifactIdIndex.create(tx);
            typeIndex.create(tx);

            page.set(this);
            tx.store(page, MARSHALLER, true);
        }

        static RootEntity load(Transaction tx) throws IOException {
            Page<RootEntity> rootPage = tx.load(0, RootEntity.MARSHALLER);
            return rootPage.get();
        }

        public void store(Transaction tx) throws IOException {
            Page<RootEntity> rootPage = tx.load(0, RootEntity.MARSHALLER);
            rootPage.set(this);
            tx.store(rootPage, RootEntity.MARSHALLER, true);
        }
    }
    
    static private class BTreeIndexReference<K, V> implements Serializable {
        protected long pageId;
        transient protected BTreeIndex<K, V> index;

        public BTreeIndex<K, V> get(Transaction tx) throws IOException {
            if (index == null) {
                index = new BTreeIndex<K, V>(tx.getPageFile(), pageId);
                index.setKeyMarshaller(new ObjectMarshaller<K>());
                index.setValueMarshaller(new ObjectMarshaller<V>());
                index.load(tx);
            }
            return index;
        }

        public void create(Transaction tx) throws IOException {
            pageId = tx.allocate().getPageId();
        }
    }

    static private class ObjectMarshaller<T> implements org.apache.kahadb.util.Marshaller<T> {
        public void writePayload(T object, DataOutput dataOutput) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(object);
            os.close();
            byte[] data = baos.toByteArray();
            dataOutput.writeInt(data.length);
            dataOutput.write(data);
        }

        public T readPayload(DataInput dataInput) throws IOException {
            byte data[] = new byte[dataInput.readInt()];
            dataInput.readFully(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream os = new ObjectInputStream(bais);
            T object = null;
            try {
                object = (T) os.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e.getMessage());
            }
            return object;
        }

        public int getFixedSize() {
            return 0;
        }

        public boolean isDeepCopySupported() {
            return false;
        }

        public T deepCopy(T object) {
            return null;
        }
   }
}