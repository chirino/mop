/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import org.apache.kahadb.page.PageFile;
import org.apache.kahadb.page.Transaction;
import org.apache.kahadb.page.Page;
import org.apache.kahadb.util.Marshaller;
import org.apache.kahadb.index.BTreeIndex;

import java.io.*;
import java.util.Set;
import java.util.HashSet;

/**
 * @author chirino
 */
public class Database {

    private PageFile pageFile;
    private boolean readOnly;
    private File directroy;

    public void delete() throws IOException {
        getPageFile().delete();
    }

    public void open(boolean readOnly) throws IOException {
        if( pageFile!=null && pageFile.isLoaded() ) {
            throw new IllegalStateException("database allready opened.");
        }
        this.readOnly = readOnly;

        boolean initializationNeeded = !getPageFile().getFile().exists();
        getPageFile().load();

        if (initializationNeeded) {
            getPageFile().tx().execute(new Transaction.Closure<IOException>() {
                public void execute(Transaction tx) throws IOException {
                    RootEntity root = new RootEntity();
                    root.create(tx);
                }
            });
        }
    }

    private PageFile getPageFile() {
        if (pageFile == null) {
            pageFile = new PageFile(directroy, "index");
            pageFile.setPageCacheSize(1024);
        }
        return pageFile;
    }

    public void close() throws IOException {
        assertOpen();
        pageFile.flush();
        pageFile.unload();
        pageFile=null;
    }

    public void install(final Set<String> dependencies) throws IOException {
        assertOpen();
        pageFile.tx().execute(new Transaction.Closure<IOException>() {
            public void execute(Transaction tx) throws IOException {
                RootEntity root = tx.load(0, RootEntity.MARSHALLER).get();
                BTreeIndex<String, String> artifacts = root.artifacts.get(tx);
                BTreeIndex<String, HashSet<String>> artifactIdIndex = root.artifactIdIndex.get(tx);
                BTreeIndex<String, HashSet<String>> typeIndex = root.typeIndex.get(tx);

                for (String id : dependencies) {
                    ArtifactId a = new ArtifactId();
                    if (!a.strictParse(id)) {
                        throw new IOException("Invalid artifact id: " + id);
                    }
                    String rc = artifacts.get(tx, id);
                    if (rc == null) {
                        artifacts.put(tx, id, id);
                        indexAdd(tx, artifactIdIndex, id, a.getArtifactId());
                        indexAdd(tx, typeIndex, id, a.getType());
                    }
                }

            }
        });
    }

    public Set<String> findByArtifactId(final String artifactId) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<Set<String>, IOException>() {
            public Set<String> execute(Transaction tx) throws IOException {
                RootEntity root = tx.load(0, RootEntity.MARSHALLER).get();
                BTreeIndex<String, HashSet<String>> artifactIdIndex = root.artifactIdIndex.get(tx);
                HashSet<String> set = artifactIdIndex.get(tx, artifactId);
                return set == null ? new HashSet() : new HashSet(set);
            }
        });
    }

    public Set<String> findByType(final String type) throws IOException {
        assertOpen();
        return pageFile.tx().execute(new Transaction.CallableClosure<Set<String>, IOException>() {
            public Set<String> execute(Transaction tx) throws IOException {
                RootEntity root = tx.load(0, RootEntity.MARSHALLER).get();
                BTreeIndex<String, HashSet<String>> typeIndex = root.typeIndex.get(tx);
                HashSet<String> set = typeIndex.get(tx, type);
                return set == null ? new HashSet() : new HashSet(set);
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

        protected BTreeIndexReference<String, HashSet<String>> explicityInstalledArtifacts = new BTreeIndexReference<String, HashSet<String>>();
        protected BTreeIndexReference<String, String> artifacts = new BTreeIndexReference<String, String>();
        protected BTreeIndexReference<String, HashSet<String>> artifactIdIndex = new BTreeIndexReference<String, HashSet<String>>();
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