package org.esa.nest.dataio.sentinel1;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.esa.nest.dataio.imageio.ImageIOFile;

import java.awt.*;

/**
 * Created by lveci on 20/05/2014.
 */
public class DataCache {

    private static final long MAX_SIZE = 2000;

    private final LoadingCache<DataKey, Data> cache;

    public DataCache() {
        cache = CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build(new CacheLoader<DataKey, Data>() {
                                                                          @Override
                                                                          public Data load(DataKey key) throws Exception {
                                                                              return new Data();
                                                                          }
                                                                      }
        );
    }

    public Data get(DataKey key) {
        return cache.getUnchecked(key);
    }

    public void put(DataKey key, Data value) {
        cache.put(key, value);
    }

    public static class DataKey {
        private final ImageIOFile img;
        private final Rectangle rect;

        DataKey(final ImageIOFile img, final Rectangle rect) {
            this.img = img;
            this.rect = rect;
        }

        @Override
        public boolean equals(Object obj) {

            DataKey key = (DataKey) obj;
            return (rect.x == key.rect.x &&
                    rect.y == key.rect.y &&
                    rect.width == key.rect.width &&
                    rect.height == key.rect.height &&
                    img == key.img);
        }

        @Override
        public int hashCode() {
            return img.hashCode();
        }

        @Override
        public String toString() {
            return rect.toString();
        }
    }

    public static class Data {

        public final boolean valid;
        public int[] intArray;

        public Data() {
            valid = false;
        }

        public Data(final int[] srcArray) {
            this.intArray = srcArray;
            this.valid = true;
        }
    }
}
