/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.cache;

import android.os.SystemClock;

import com.android.volley.Cache;
import com.android.volley.VolleyLog;
import com.android.volley.misc.IOUtils;
import com.android.volley.misc.IOUtils.CountingInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache implementation that caches files directly onto the hard disk in the specified
 * directory. The default disk usage size is 5MB, but is configurable.
 */
public class DiskBasedCache implements Cache {

    /** Number of threads to use when loading cache from disk */
    private final int CACHE_LOAD_THREADS = 2;

    /** Map of the Key, CacheHeader pairs */
    private final CacheContainer mEntries = new CacheContainer();

    /** The root directory to use for the cache. */
    private final File mRootDirectory;

    /** The maximum size of the cache in bytes. */
    private final int mMaxCacheSizeInBytes;

    /** Default maximum disk usage in bytes. */
    private static final int DEFAULT_DISK_USAGE_BYTES = 5 * 1024 * 1024;

    /** High water mark percentage for the cache */
    private static final float HYSTERESIS_FACTOR = 0.9f;

    /** Magic number for current version of cache file format. */
    private static final int CACHE_MAGIC = 0x20150306;

    /**
     * Constructs an instance of the DiskBasedCache at the specified directory.
     * @param rootDirectory The root directory of the cache.
     * @param maxCacheSizeInBytes The maximum size of the cache in bytes.
     */
    public DiskBasedCache(File rootDirectory, int maxCacheSizeInBytes) {
        mRootDirectory = rootDirectory;
        mMaxCacheSizeInBytes = maxCacheSizeInBytes;
    }

    /**
     * Constructs an instance of the DiskBasedCache at the specified directory using
     * the default maximum cache size of 5MB.
     * @param rootDirectory The root directory of the cache.
     */
    public DiskBasedCache(File rootDirectory) {
        this(rootDirectory, DEFAULT_DISK_USAGE_BYTES);
    }

    /**
     * Clears the cache. Deletes all cached files from disk.
     */
    @Override
    public synchronized void clear() {
        File[] files = mRootDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        mEntries.clear();
        VolleyLog.d("Cache cleared.");
    }

    /**
     * Returns the cache entry with the specified key if it exists, null otherwise.
     */
    @Override
    public synchronized Entry get(String key) {
        CacheHeader entry = mEntries.get(key);
        // if the entry does not exist, return.
        if (entry == null) {
            return null;
        }

        File file = getFileForKey(key);
        CountingInputStream cis = null;
        try {
            cis = new CountingInputStream(new BufferedInputStream(new FileInputStream(file)));
            CacheHeader.readHeader(cis); // eat header
            byte[] data = IOUtils.streamToBytes(cis, (int) (file.length() - cis.getBytesRead()));
            return entry.toCacheEntry(data);
        } catch (IOException e) {
            VolleyLog.d("%s: %s", file.getAbsolutePath(), e.toString());
            remove(key);
            return null;
        } catch (OutOfMemoryError e) {
            VolleyLog.e("Caught OOM for %d byte image, path=%s: %s", file.length(), file.getAbsolutePath(), e.toString());
            return null;
        } finally {
            if (cis != null) {
                try {
                    cis.close();
                } catch (IOException ioe) {
                    return null;
                }
            }
        }
        
        
    }

    /**
     * Initializes the DiskBasedCache by scanning for all files currently in the
     * specified root directory. Creates the root directory if necessary.
     */
    @Override
    public synchronized void initialize() {
        mEntries.initialize();
    }

    /**
     * Invalidates an entry in the cache.
     * @param key Cache key
     * @param fullExpire True to fully expire the entry, false to soft expire
     */
    @Override
    public synchronized void invalidate(String key, boolean fullExpire) {
        Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = -1;
            if (fullExpire) {
                entry.ttl = -1;
            }
            put(key, entry);
        }

    }

    /**
     * Puts the entry with the specified key into the cache.
     */
    @Override
    public synchronized void put(String key, Entry entry) {
        pruneIfNeeded(entry.data.length);
        File file = getFileForKey(key);
        try {
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
            CacheHeader e = new CacheHeader(key, entry);
            boolean success = e.writeHeader(fos);
            if (!success) {
                fos.close();
                VolleyLog.d("Failed to write header for %s", file.getAbsolutePath());
                throw new IOException();
            }
            fos.write(entry.data);
            fos.close();
            putEntry(key, e);
            return;
        } catch (IOException e) {
        }
        boolean deleted = file.delete();
        if (!deleted) {
            VolleyLog.d("Could not clean up file %s", file.getAbsolutePath());
        }
    }

    /**
     * Removes the specified key from the cache if it exists.
     */
    @Override
    public synchronized void remove(String key) {
        boolean deleted = getFileForKey(key).delete();
        removeEntry(key);
        if (!deleted) {
            VolleyLog.d("Could not delete cache entry for key=%s, filename=%s",
                    key, getFilenameForKey(key));
        }
    }

    /**
     * Creates a pseudo-unique filename for the specified cache key.
     * @param key The key to generate a file name for.
     * @return A pseudo-unique filename.
     */
    private String getFilenameForKey(String key) {
        int firstHalfLength = key.length() / 2;
        String localFilename = String.valueOf(key.substring(0, firstHalfLength).hashCode());
        localFilename += String.valueOf(key.substring(firstHalfLength).hashCode());
        return localFilename;
    }

    /**
     * Returns a file object for the given cache key.
     */
    public File getFileForKey(String key) {
        return new File(mRootDirectory, getFilenameForKey(key));
    }

    /**
     * Prunes the cache to fit the amount of bytes specified.
     * @param neededSpace The amount of bytes we are trying to fit into the cache.
     */
    private void pruneIfNeeded(int neededSpace) {
        if (!mEntries.isLoaded()) {
            // the lru cache can go slightly above neededSpace if putting entries during cache initialization
            return;
        }
        if ((mEntries.getTotalSize() + neededSpace) < mMaxCacheSizeInBytes) {
            return;
        }
        if (VolleyLog.DEBUG) {
            VolleyLog.v("Pruning old cache entries.");
        }

        long before = mEntries.getTotalSize();
        int prunedFiles = 0;
        long startTime = SystemClock.elapsedRealtime();

        Iterator<Map.Entry<String, CacheHeader>> iterator = mEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheHeader> entry = iterator.next();
            CacheHeader e = entry.getValue();
            boolean deleted = getFileForKey(e.key).delete();
            if (!deleted) {
                VolleyLog.d("Could not delete cache entry for key=%s, filename=%s",
                        e.key, getFilenameForKey(e.key));
            }
            iterator.remove();
            prunedFiles++;

            if ((mEntries.getTotalSize() + neededSpace) < mMaxCacheSizeInBytes * HYSTERESIS_FACTOR) {
                break;
            }
        }

        if (VolleyLog.DEBUG) {
            VolleyLog.v("pruned %d files, %d bytes, %d ms",
                    prunedFiles, (mEntries.getTotalSize() - before), SystemClock.elapsedRealtime() - startTime);
        }
    }

    /**
     * Puts the entry with the specified key into the cache.
     * @param key The key to identify the entry by.
     * @param entry The entry to cache.
     */
    private void putEntry(String key, CacheHeader entry) {
        mEntries.put(key, entry);
    }

    /**
     * Removes the entry identified by 'key' from the cache.
     */
    private void removeEntry(String key) {
        CacheHeader entry = mEntries.get(key);
        if (entry != null) {
            mEntries.remove(key);
        }
    }

    /**
     * Container for CacheHeader, both before and after loading them into memory.
     */
    @SuppressWarnings("serial")
	private class CacheContainer extends ConcurrentHashMap<String, CacheHeader> {
        private final PriorityBlockingQueue<Runnable> mQueue = new PriorityBlockingQueue<Runnable>();
        private final Map<String, Future<CacheHeader>> mLoadingFiles = new ConcurrentHashMap<String, Future<CacheHeader>>();

        /** Total amount of space currently used by the cache in bytes. */
        private AtomicLong mTotalSize = new AtomicLong(0);

        /** Whether or not cache initialization has been started */
        private boolean mInitialized = false;

        public CacheContainer() {
            super(16, .75f, CACHE_LOAD_THREADS);
        }


        /**
         * Initializes the DiskBasedCache by scanning for all files currently in the
         * specified root directory. Creates the root directory if necessary.
         */
        public synchronized void initialize() {
            if (mInitialized) {
                return;
            }
            mInitialized = true;
            if (!mRootDirectory.exists()) {
                if (!mRootDirectory.mkdirs()) {
                    VolleyLog.e("Unable to create cache dir %s", mRootDirectory.getAbsolutePath());
                }
                return;
            }

            File[] files = mRootDirectory.listFiles();
            if (files == null) {
                return;
            }
            VolleyLog.d("Loading %d files from cache", files.length);

            ExecutorService executor = new ThreadPoolExecutor(CACHE_LOAD_THREADS, CACHE_LOAD_THREADS, 10, TimeUnit.MILLISECONDS, mQueue);
            for (File file : files) {
                Callable<CacheHeader> callable = new HeaderParserCallable(file);
                RunnableFuture<CacheHeader> submit = new ReorderingFutureTask(callable);
                mLoadingFiles.put(file.getName(), submit);
                executor.execute(submit);
            }
        }

        /** A task that reorders itself to the top of the queue if a thread requests access to it */
        private class ReorderingFutureTask extends FutureTask<CacheHeader> implements Comparable<ReorderingFutureTask> {
            private int mGetRequests = 0;

            public ReorderingFutureTask(Callable<CacheHeader> callable) {
                super(callable);
            }

            @Override
            public CacheHeader get() throws InterruptedException, ExecutionException {
                mGetRequests++;
                if (mQueue.contains(this)) {
                    mQueue.remove(this);
                    mQueue.add(this);
                }
                return super.get();
            }

            @Override
            public int compareTo(ReorderingFutureTask another) {
                return  mGetRequests > another.mGetRequests ? - 1 : mGetRequests < another.mGetRequests ? + 1 : 0;
            }
        }

        /** A callable that parses CacheHeader and returns a valid cache entry. */
        private class HeaderParserCallable implements Callable<CacheHeader> {
            private final File file;

            public HeaderParserCallable(File file) {
                this.file = file;
            }

            @Override
            public CacheHeader call() throws Exception {
                BufferedInputStream fis = null;
                try {
                    fis = new BufferedInputStream(new FileInputStream(file));
                    CacheHeader entry = CacheHeader.readHeader(fis);
                    entry.size = file.length();
                    CacheContainer.super.put(entry.key, entry);
                    mTotalSize.getAndAdd(entry.size);
                    return entry;
                } catch (IOException e) {
                    if (file != null) {
                        file.delete();
                    }
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException ignored) {
                    }
                    mLoadingFiles.remove(file.getName());
                }
                return null;
            }
        }

        /** Waits until the cache is ready and loaded */
        private void waitForCache() {
            while (mLoadingFiles.size() > 0) {
                Iterator<Map.Entry<String, Future<CacheHeader>>> iterator = mLoadingFiles.entrySet().iterator();
                if (iterator.hasNext()) {
                    Map.Entry<String, Future<CacheHeader>> entry = iterator.next();
                    try {
                        entry.getValue().get();
                    } catch (InterruptedException ignored) {
                    } catch (ExecutionException ignored) {
                    }
                }
            }
        }

        /** Waits until the specified cache key is ready and loaded. */
        private void waitForKey(Object key) {
            if (isLoaded()) {
                return;
            }
            String filename = getFilenameForKey((String) key);
            Future<CacheHeader> future = mLoadingFiles.get(filename);
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException ignored) {
                } catch (ExecutionException ignored) {
                }
            }
        }

        /** Returns true if the cache is 100% loaded. */
        public boolean isLoaded() {
            return mLoadingFiles.size() == 0;
        }

        /** Returns the total size of the cache */
        public long getTotalSize() {
            return mTotalSize.get();
        }

        /**
         * Gets an entry from the cache
         *
         * @param key   The key to identify the entry by.
         */
        @Override
        public CacheHeader get(Object key) {
            waitForKey(key);
            return super.get(key);
        }

        /**
         * Checks if an entry exists in the cache and returns true/false accordingly.
         *
         * @param key   The key to identify the entry by.
         */
        @Override
        public boolean containsKey(Object key) {
            waitForKey(key);
            return super.containsKey(key);
        }

        /**
         * Puts the entry with the specified key into the cache without waiting for cache key.
         *
         * @param key   The key to identify the entry by.
         * @param entry The entry to cache.
         */
        @Override
        public CacheHeader put(String key, CacheHeader entry) {
            waitForKey(key);
            if (super.containsKey(key)) {
                mTotalSize.getAndAdd(entry.size - super.get(key).size);
            } else {
                mTotalSize.getAndAdd(entry.size);
            }
            return super.put(key, entry);
        }

        /**
         * Remove an entry from the cache
         *
         * @param key   The key to identify the entry by.
         */
        @Override
        public CacheHeader remove(Object key) {
            waitForKey(key);
            if (super.containsKey(key)) {
                mTotalSize.getAndAdd(-1 * super.get(key).size);
            }
            return super.remove(key);
        }

        /**
         * Clears the cache
         */
        @Override
        public void clear() {
            waitForCache();
            mTotalSize.getAndSet(0);
            super.clear();
        }
    }

    /**
     * Handles holding onto the cache headers for an entry.
     */
    // Visible for testing.
    static class CacheHeader {
        /** The size of the data identified by this CacheHeader. (This is not
         * serialized to disk. */
        public long size;

        /** The key that identifies the cache entry. */
        public String key;

        /** ETag for cache coherence. */
        public String etag;

        /** Date of this response as reported by the server. */
        public long serverDate;

        /** The last modified date for the requested object. */
        public long lastModified;

        /** TTL for this record. */
        public long ttl;

        /** Soft TTL for this record. */
        public long softTtl;

        /** Headers from the response resulting in this cache entry. */
        public Map<String, String> responseHeaders;

        private CacheHeader() { }

        /**
         * Instantiates a new CacheHeader object
         * @param key The key that identifies the cache entry
         * @param entry The cache entry.
         */
        public CacheHeader(String key, Entry entry) {
            this.key = key;
            this.size = entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.lastModified = entry.lastModified;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeaders = entry.responseHeaders;
        }

        /**
         * Reads the header off of an InputStream and returns a CacheHeader object.
         * @param is The InputStream to read from.
         * @throws IOException
         */
        public static CacheHeader readHeader(InputStream is) throws IOException {
            CacheHeader entry = new CacheHeader();
            int magic = IOUtils.readInt(is);
            if (magic != CACHE_MAGIC) {
                // don't bother deleting, it'll get pruned eventually
                throw new IOException();
            }
            entry.key = IOUtils.readString(is);
            entry.etag = IOUtils.readString(is);
            if (entry.etag.equals("")) {
                entry.etag = null;
            }
            entry.serverDate = IOUtils.readLong(is);
            entry.lastModified = IOUtils.readLong(is);
            entry.ttl = IOUtils.readLong(is);
            entry.softTtl = IOUtils.readLong(is);
            entry.responseHeaders = IOUtils.readStringStringMap(is);

            return entry;
        }

        /**
         * Creates a cache entry for the specified data.
         */
        public Entry toCacheEntry(byte[] data) {
            Entry e = new Entry();
            e.data = data;
            e.etag = etag;
            e.serverDate = serverDate;
            e.lastModified = lastModified;
            e.ttl = ttl;
            e.softTtl = softTtl;
            e.responseHeaders = responseHeaders;
            return e;
        }


        /**
         * Writes the contents of this CacheHeader to the specified OutputStream.
         */
        public boolean writeHeader(OutputStream os) {
            try {
            	IOUtils.writeInt(os, CACHE_MAGIC);
                IOUtils.writeString(os, key);
                IOUtils.writeString(os, etag == null ? "" : etag);
                IOUtils.writeLong(os, serverDate);
                IOUtils.writeLong(os, lastModified);
                IOUtils.writeLong(os, ttl);
                IOUtils.writeLong(os, softTtl);
                IOUtils.writeStringStringMap(responseHeaders, os);
                os.flush();
                return true;
            } catch (IOException e) {
                VolleyLog.d("%s", e.toString());
                return false;
            }
        }
    }

	@Override
	public void flush() {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}
}